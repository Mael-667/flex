import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class Flex {

    Connection conn;
    String braveToken;

    public Flex(Connection conn){

        this.conn = conn;

        try {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS folder (\n"
                        + "	path TEXT NOT NULL,\n"
                        + "	lastupdate TEXT NOT NULL\n"
                        + ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS info (\n"
                        + "	rowid integer PRIMARY KEY,\n"
                        + "	titreparsed TEXT NOT NULL,\n"
                        + "	titre TEXT NOT NULL,\n"
                        + "	titrevo TEXT,\n"
                        + "	minia TEXT NOT NULL,\n"
                        + "	serie TEXT NOT NULL,\n"
                        + "	saison INTEGER,\n"
                        + "	nbep INTEGER\n"
                        + ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS fichier (\n"
                        + "	url TEXT NOT NULL,\n"
                        + "	info INTEGER,\n"
                        + "	ep INTEGER,\n"
                        + "	duree INTEGER NOT NULL\n"
                        + ");");
            String sql = "SELECT token FROM brave";
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery(); 
            braveToken = rs.getString("token");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public int filesLeft = 0;
    public int filesScanned = 0;




    public void updateFlex(){
        try {
            String sql = "SELECT path, lastUpdate FROM folder";
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery();    
            while(rs.next()){
                deepScan(new File(rs.getString("path")));
            }
            filesLeft = 0;
            filesScanned = 0;
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void addFolder(String folder){
        String sql = "SELECT path FROM folder WHERE path = ?";
        // set the value
        try {
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setString(1, folder);
            ResultSet rs  = pstmt.executeQuery();    
            if(!rs.next()){
                String sql2 = "INSERT INTO folder(path) VALUES(?)";
                PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                pstmt2.setString(1, folder);
                pstmt2.executeUpdate();
            }
            File fldr = new File(folder);
            startScan(fldr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startScan(File fldr) throws Exception{
        deepScan(fldr);
        filesLeft = 0;
        filesScanned = 0;
    }
    
    public void deepScan(File fldr) throws Exception{
        ArrayList<File> al = new ArrayList<>();
        File[] files = fldr.listFiles();
        for(File file: files){
            if(file.isDirectory()){
                deepScan(file);
            } else {
                if(Explorer.getMimeType(file).startsWith("video")){
                    if(alreadyScaned(file.getAbsolutePath())){

                    } else {
                        al.add(file);
                    }
                };
            }
        }
        filesLeft += al.size();
        al.forEach((file) -> {
            try{
                scan(file);
            } catch(Exception e){
                e.printStackTrace();
            }
            filesScanned++;
        });
    }

    private boolean alreadyScaned(String name) throws SQLException{
        String sql = "SELECT url FROM fichier WHERE url = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        ResultSet rs  = pstmt.executeQuery();    
        if(!rs.next()){
            return false;
        } else {
            return true;
        }
    }

    private int alreadyInDB(String tempnom) throws SQLException{
        String sql = "SELECT rowid FROM info WHERE titreparsed = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        pstmt.setString(1, tempnom);
        ResultSet rs  = pstmt.executeQuery();    
        if(rs.next()){
            return rs.getInt("rowid");
        } else {
            return -1;
        }
    }

    private boolean isSerie(String tempnom) throws SQLException{
        String sql = "SELECT serie FROM info WHERE titreparsed = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        pstmt.setString(1, tempnom);
        ResultSet rs  = pstmt.executeQuery();    
        if(rs.next()){
            return Boolean.valueOf(rs.getString("serie"));
        } else {
            return false;
        }
    }

    public void scan(File fichier) throws Exception{
        String tempnom = getname(fichier.getName());
        int duree = metadata(fichier.getAbsolutePath());
        int info = alreadyInDB(tempnom);
        int ep = 0;
        if(info >= 0){
            if(isSerie(tempnom)){
                String[] metadata = metadataep(fichier.getAbsolutePath());
                ep = Integer.parseInt(metadata[1]);
            }
            String sql = "INSERT INTO fichier(url, info, ep, duree) VALUES(?,?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, fichier.getPath());
            pstmt.setInt(2, info);
            pstmt.setInt(3, ep);
            pstmt.setInt(4, duree);
            pstmt.executeUpdate();
            return;
        }

        System.out.println(tempnom);
        String titre = tempnom;
        String titrevo = "";
        String minia = "";
        boolean serie = false;
        String nbep = "";
        String saison = "";
        if(duree > 7*60){
            String[] data = search(tempnom);
            titre = data[0];
            titrevo = data[1];
            minia = data[2];
            serie = Boolean.parseBoolean(data[3]);
            nbep = data[4];
            if(serie || duree < 40*60){
                String[] metadata = metadataep(fichier.getAbsolutePath());
                saison = metadata[0];
                ep = Integer.parseInt(metadata[1]);
            }
            /*System.out.println("Titre : " + titre);
            System.out.println("Durée : " + duree);
            System.out.println("Titre Original : " + titrevo);
            System.out.println("Minia : " + minia);
            if(serie){
                System.out.println("Saison : " + saison + " Épisode : " + ep);
                System.out.println("Nombre d'épisodes dans la saison : " + nbep);
            }
            System.out.println("\n");*/
            Files.createDirectories(Paths.get("cover"));
            minia = dlImage(minia, "cover/"+Explorer.getChksum(titre.getBytes())+".jpg");
            String sql = "INSERT INTO info(titreparsed, titre, titrevo, minia, serie, saison, nbep) VALUES(?,?,?,?,?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tempnom);
            pstmt.setString(2, titre);
            pstmt.setString(3, titrevo);
            pstmt.setString(4, minia);
            pstmt.setString(5, Boolean.toString(serie));
            if(serie){
                pstmt.setInt(6, Integer.valueOf(saison.equals("") ? "1" : saison));
                pstmt.setInt(7, Integer.valueOf(nbep));
            }
            pstmt.executeUpdate();


            String sql2 = "INSERT INTO fichier(url, info, ep, duree) VALUES(?,?,?,?)";
            PreparedStatement pstmt2 = conn.prepareStatement(sql2);
            pstmt2.setString(1, fichier.getPath());
            pstmt2.setInt(2, alreadyInDB(tempnom));
            pstmt2.setInt(3, ep);
            pstmt2.setInt(4, duree);
            pstmt2.executeUpdate();
        }
    }

    public String rgx(String regex, String string, String defaut){
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(string);
        return  m.find() ? m.group() : defaut;
    }


    public static int metadata(String path) throws Exception{
        //String[] arg = {Explorer.ffprobeVer(), "\""+path+"\"", "-hide_banner"};
        String[] arg = {Explorer.ffprobeVer(), path, "-hide_banner"};
        ProcessBuilder pb = new ProcessBuilder(arg);
        //File log = File.createTempFile("log", "txt");
        File log = new File("log.txt");
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.to(log));
        Process pr = pb.start();
        pr.waitFor();
        BufferedReader br = new BufferedReader(new FileReader(log));
        Pattern pp = Pattern.compile( "(?<=Duration: ).*?(?=,)");
        String line ="";
        while((line = br.readLine()) != null){
            Matcher m = pp.matcher(line);
            if(m.find()){
                String[] issou = m.group().split("\\.")[0].split(":");
                br.close();
                return Integer.parseInt(issou[0])*60*60 + Integer.parseInt(issou[1])*60+Integer.parseInt(issou[0]);
            }
        }
        br.close();
        return -1;
    }

    public String[] metadataep(String temp){
        Pattern p = Pattern.compile( "(?<=s)\\d+");
        Matcher m = p.matcher(temp.toLowerCase());
        String s = m.find() ? m.group() : "";

        p = Pattern.compile( "(?<=e)\\d+");
        m = p.matcher(temp.toLowerCase());
        String ep = m.find() ? m.group() : planb(temp);
        //"\\d{2,3}(?![\\w\\d])" "(?<=s)\\d+" "(?<=e)\\d+"
        return new String[]{s, ep};
    }

    public String planb(String temp){
        Pattern pp = Pattern.compile( "(?<!\\d|x|h.)\\d{2,3}(?![\\w\\d])");
        Matcher mm = pp.matcher(temp.toLowerCase());
        return  mm.find() ? mm.group() : "";
    }

    public String getname(String nom){
        nom = nom.substring(0, nom.lastIndexOf("."))
                .replaceAll("\\.", " ")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .trim();
        String[] items = nom.split(" ");
        String[] banword = new String[]{"vostfr", "vf", "720p", "1080p", "4k", "fhd", "bdrip", "x?26.", "aac", "fhd", "web", "hdr", "nf", "multi", "h[^a-z]",
                            "hevc", "s\\d*e\\d*", "e\\d+", "\\d{2,3}(?!.)"};
        boolean issou = true;
        StringBuilder titre = new StringBuilder("");
        for(int i = 0; i < items.length; i++){
            for(int j = 0; j < banword.length; j++){
                if(items[i].toLowerCase().matches(banword[j])){
                    issou = false;
                    break;
                }
            }
            if(issou == false){
                break;
            } else {
                titre.append(items[i]);
                titre.append(items[i].equals("") ? "" : " ");
            }
        }
        return titre.toString();
    }

    public String[] search(String nom) throws Exception{
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        .header("Accept", "application/json")
        .header("Accept-Encoding", "identity")
        .header("X-Subscription-Token", braveToken)
        .uri(new URI("https://api.search.brave.com/res/v1/web/search?q="+nom.replaceAll(" ", "+"))).build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        JSONObject jo = new JSONObject(response.body());
        jo = jo.getJSONObject("web");
        JSONArray oui = jo.getJSONArray("results");
        for(int i = 0; i < oui.length(); i++){
            JSONObject resultat = oui.getJSONObject(i);
            String url = resultat.getString("url");
            if(url.contains("https://myanimelist.net/anime/")){
                return mal(rgx("(?<=anime/)\\d+", url, ""));
            };
        }
        for(int i = 0; i < oui.length(); i++){
            JSONObject resultat = oui.getJSONObject(i);
            String url = resultat.getString("url");
            if(url.contains("https://www.imdb.com/")){
                return imdb(rgx("(?<=com/title/)tt\\d+", url, ""));
            };
        }
        return new String[]{""};
    }

    public String[] mal(String id) throws Exception{
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        .header("Accept-Encoding", "identity")
        .uri(new URI("https://api.jikan.moe/v4/anime/" + id)).build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        JSONObject jo = new JSONObject(response.body());
        jo = jo.getJSONObject("data");

        String titre = jo.getString("title");
        String titre_eng = jo.getString("title_english");
        String minia = jo.getJSONObject("images").getJSONObject("jpg").getString("image_url");
        int nbep = jo.getInt("episodes");
        String serie = nbep > 1 ? "true" : "false";
        return new String[]{titre, titre_eng, minia, serie, String.valueOf(nbep)};    
    }

    public String[] imdb(String id) throws Exception{
        Document doc = Jsoup.connect("https://www.imdb.com/title/"+id+"/").get();
        Elements content = doc.getElementsByClass("ipc-image");
        Elements bruh = doc.getElementsByAttributeValue("data-testid", "hero__pageTitle");

        String minia = content.get(0).attr("src");
        String titre = bruh.get(0).firstElementChild().wholeOwnText();
        String uwu = bruh.get(0).nextElementSibling().text();
        String titre_eng = bruh.get(0).nextElementSibling().wholeOwnText().replace("Titre original : ", "").replace("Original title: ", "");
        String serie = "false";
        //System.out.println(titre_eng);
        //System.out.println(uwu);
        if(uwu.contains("Série télévisée") || (uwu.contains("TV") && uwu.contains("Series"))){
            serie = "true";
        } else {
            try{
                serie = bruh.get(0).nextElementSibling().nextElementSibling().getElementsByClass("ipc-inline-list__item").get(0).wholeOwnText();
                if(titre_eng.contains("Série télévisée") || (titre_eng.contains("TV") && titre_eng.contains("Series"))){
                    serie = "true";
                }
            } catch(Exception e){
                serie = "false";
            }
        }
        String nbep = doc.getElementsByClass("ipc-title__subtext").get(0).wholeOwnText();
        if(Integer.valueOf(nbep) > 1){
            serie = "true";
        }
        return new String[]{titre, titre_eng, minia, serie, nbep};  
    }

    private String dlImage(String link, String name){
        String ans = link;
        try {
            URL url = new URL(link);
            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n = 0;
            while (-1!=(n=in.read(buf)))
            {
            out.write(buf, 0, n);
            }
            out.close();
            in.close();
            byte[] response = out.toByteArray();
            File file = new File(name);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(response);
            fos.close();
            ans = name;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

}