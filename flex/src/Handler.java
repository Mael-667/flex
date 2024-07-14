import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Handler implements HttpHandler {

    Connection conn;
    String[] adminurl = new String[]{"src/site/setupFlex.html", "src/site/setup.html", "src/site/braveSetup.html"};

    public Handler(Connection conn){
        this.conn = conn;

        try {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS folders (\n"
                        + "folder TEXT NOT NULL,\n"
                        + "auth TEXT NOT NULL\n"
                        + ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS users (\n"
                        + "name TEXT NOT NULL,\n"
                        + "password TEXT NOT NULL,\n"
                        + "avatar TEXT NOT NULL,\n"
                        + "token TEXT NOT NULL,\n"
                        + "admin TEXT NOT NULL\n"
                        + ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS brave (\n"
                        + "token TEXT NOT NULL\n"
                        + ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void handle(HttpExchange e) throws IOException {
        Runnable runnable = () -> {main(e);};
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void main(HttpExchange e){
        try{
            String requestMethod = e.getRequestMethod();
            String query = e.getRequestURI().getQuery(); //query = lien/?var=value
            switch(requestMethod){
                case("GET"):
                    e.getResponseHeaders();
                    queryParser("");
                    String uri = e.getRequestURI().toString();
                    if(uri.equals("/")){
                        sendHTML(e, "src/site/html.html");
                    } else if(query==null){
                        file(e, uri);
                    } else {
                        Map<String, String> map = queryParser(query);
                        if(map.containsKey("req")){
                            switch(map.get("req")){
                                case "main":
                                    sendHTML(e, "src/site/main.html");
                                    break;
                                case "getProfiles":
                                    sendJSON(e, getProfiles());
                                    break;
                                case "login":
                                    sendHTML(e, "src/site/login.html");
                                    break;
                                case "createAcc":
                                    sendHTML(e, "src/site/createAcc.html");
                                    break;
                                case "getDefaultFolder":
                                    if(isAdmin(e)){
                                        sendJSON(e, file2String(System.getProperty("user.home")));
                                    }
                                    break;
                                case "getFolder":
                                    if(isAdmin(e)){
                                        sendJSON(e, file2String(java.net.URLDecoder.decode(map.get("path"), StandardCharsets.UTF_8)));
                                    }
                                    break;
                                case "getBraveSetup":
                                    if(isAdmin(e)){
                                        sendHTML(e, "src/site/braveSetup.html");
                                    }
                                    break;
                                case "getFlexSetup":
                                    if(isAdmin(e)){
                                        sendHTML(e, "src/site/setupFlex.html");;
                                    }
                                    break;
                                case "setupFlex":
                                    if(isAdmin(e)){
                                        String path = java.net.URLDecoder.decode(map.get("path"), StandardCharsets.UTF_8);
                                        System.out.println(path);
                                        Flex flex = new Flex(conn);
                                        Runnable runnable = () -> { try {
                                            flex.addFolder(path);
                                        } catch (Exception e4) {
                                            e4.printStackTrace();
                                        } };
                                        Thread thread = new Thread(runnable);
                                        thread.start();
                                        sendHTML(e, "src/site/main.html");
                                    }
                                    break;
                                case "getPlexData":
                                    sendJSON(e, getPlexData());
                                    break;
                                default:
                                    break;
                            }

                        }
                    }
                break;
                case("POST"):
                    Map<String, String> map = queryParser(query);
                        if(map.containsKey("req")){
                            switch(map.get("req")){
                                case "createAcc":
                                    createAcc(e);
                                    break;
                                case "login":
                                    login(e);
                                    break;
                                case "setupBrave":
                                    setupBrave(e);
                                    break;
                                default:
                                    break;
                            }

                    }
                    break;
            }
        } catch(Exception e2){
            e2.printStackTrace();
        }
    }

    private void sendJSON(HttpExchange e, String json) throws IOException{
            String response = json;
            byte[] rep = response.getBytes("UTF-8");
            e.sendResponseHeaders(200, rep.length);
            OutputStream os = e.getResponseBody();
            os.write(rep);
            os.close();
    }

    private void sendHTML(HttpExchange e, String html) throws IOException, SQLException{
        if(protectedUrl(e, html)){
            BufferedReader br = new BufferedReader(new FileReader(html));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine())!=null){
                sb.append(line);
            }
            br.close();
            byte[] response = sb.toString().getBytes("UTF-8");
            e.sendResponseHeaders(200, response.length);
            OutputStream os = e.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private void sendHTML(HttpExchange e, String html, boolean admin) throws IOException, SQLException{
        if(admin){
            BufferedReader br = new BufferedReader(new FileReader(html));
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine())!=null){
                sb.append(line);
            }
            br.close();
            byte[] response = sb.toString().getBytes("UTF-8");
            e.sendResponseHeaders(200, response.length);
            OutputStream os = e.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private void file(HttpExchange e, String uri) throws IOException, SQLException{
        uri = uri.substring(1, uri.length());
         if(protectedUrl(e, uri)){
            OutputStream os = e.getResponseBody();
            e.sendResponseHeaders(200, 0);
            Path path = FileSystems.getDefault().getPath(uri);
            FileInputStream in = new FileInputStream(path.toFile());
            int line;
            while((line = in.read()) != -1){
                os.write(line);
            }
            in.close();
            os.close();
        }
    }

    private void sendError(HttpExchange e, int code) throws IOException{
        byte[] response = "noob".getBytes("UTF-8");
        e.sendResponseHeaders(code, response.length);
        OutputStream os = e.getResponseBody();
        os.write(response);
        os.close();
    }

    public String getProfiles() throws Exception{
        String sql = "SELECT name,avatar FROM users";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        ResultSet rs  = pstmt.executeQuery();   
        JSONStringer json = new JSONStringer();
        json.array();
        while(rs.next()){
            json.object();
            json.key("name");
            json.value(rs.getString("name"));
            json.key("avatar");
            json.value(rs.getString("avatar"));
            json.endObject();
        }
        json.endArray();
        return json.toString();
    }


    
    public ArrayList<Fichier> mainExplorer(){
        ArrayList<Fichier> liste = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs   = stmt.executeQuery("SELECT folder FROM folders");
            while (rs.next()) {
                Fichier fichier = new Fichier(rs.getString("folder"));
                System.out.println(rs.getString("folder"));
                fichier.minia = Explorer.iconeDoss;
                liste.add(fichier);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return liste;
    }

    private void login(HttpExchange e) throws Exception{
        InputStream is = e.getRequestBody();
        byte[] ii = is.readAllBytes();
        Map<String, FormData> formdata = formDataParser(ii);
        String name = formdata.get("name").getString();
        byte[] mdp = getPasswordHash(formdata.get("password").getString().toCharArray());

        String sql = "SELECT name, avatar, token FROM users WHERE password = ? AND name = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        pstmt.setString(1, new String(mdp, StandardCharsets.UTF_8));
        pstmt.setString(2, name);
        ResultSet rs  = pstmt.executeQuery();   
        if(rs.next()){
            e.getResponseHeaders().add("token", rs.getString("token"));
            e.getResponseHeaders().add("name", rs.getString("name"));
            e.getResponseHeaders().add("avatar", rs.getString("avatar"));
            sendHTML(e, "src/site/main.html");
        } else {
            sendError(e, 418);
        }
    }



    private void createAcc(HttpExchange e) throws Exception{
        InputStream is = e.getRequestBody();
        int i = Integer.parseInt(e.getRequestHeaders().get("Content-Length").get(0));
        System.out.println(i);
        byte[] ii = new byte[i];
        ii = is.readAllBytes();
        System.out.println(ii[0]);
        /*File outputFile = new File("bruh");
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(ii);
        }*/
        Map<String, FormData> formdata = formDataParser(ii);

        String name = formdata.get("name").getString();

        PreparedStatement ps  = conn.prepareStatement("SELECT name FROM users WHERE name = ?");
        ps.setString(1, name);
        ResultSet rs1  = ps.executeQuery();
        if(rs1.next()){
            sendError(e, 403);
            return;
        }


        byte[] mdp = getPasswordHash(formdata.get("password").getString().toCharArray());
        Files.createDirectories(Paths.get("avatar"));
        String avatararray = formdata.get("avatar").filename;
        File avatar;
        if(avatararray.equals("")){
            avatar = new File("defaultAvatar.jpg");
            FileInputStream fis = new FileInputStream(avatar);
            byte[] bs = fis.readAllBytes();
            ArrayList<Byte> al = new ArrayList<>(bs.length);
            for(byte b: bs){
                al.add(b);
            }
            formdata.get("avatar").data = al;
            fis.close();
        }
        avatar = formdata.get("avatar").getFile("avatar/", Explorer.getChksum(name.getBytes())+".jpg");

        String sql = "INSERT INTO users(name,password,avatar,token,admin) VALUES(?,?,?,?,?)";
        String token = tokenGenerator();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setString(2, new String(mdp, StandardCharsets.UTF_8));
        pstmt.setString(3, avatar.getPath());
        pstmt.setString(4, token);

        
        String sql2 = "SELECT name FROM users";
        
        Statement stmt  = conn.createStatement();
        ResultSet rs    = stmt.executeQuery(sql2);
        e.getResponseHeaders().add("token", token);
        if(!rs.next()){ 
            pstmt.setString(5, "true");
            pstmt.executeUpdate();
            sendHTML(e, "src/site/setup.html", true);
        } else {
            pstmt.setString(5, "false");
            pstmt.executeUpdate();
            sendHTML(e, "src/site/main.html");
        }
    }

    private void setupBrave(HttpExchange e) throws Exception{
        PreparedStatement pstm = conn.prepareStatement("SELECT * FROM brave");
        ResultSet rs  = pstm.executeQuery();
        String sql;
        if(rs.next()){
            sql = "UPDATE brave SET token = ?";
        } else {
            sql = "INSERT INTO brave(token) VALUES(?)";
        }
        InputStream is = e.getRequestBody();
        byte[] ii = is.readAllBytes();
        Map<String, FormData> formdata = formDataParser(ii);
        String token = formdata.get("token").getString();
        if(isTokenValid(token)){
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, token);
            pstmt.executeUpdate();
            byte[] response = "bruh".getBytes("UTF-8");
            e.sendResponseHeaders(200, response.length);
            OutputStream os = e.getResponseBody();
            os.write(response);
            os.close();
        } else {
            sendError(e, 413);
        }
    }

    private boolean isTokenValid(String token){
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Accept-Encoding", "identity")
                .header("X-Subscription-Token", token)
                .uri(new URI("https://api.search.brave.com/res/v1/web/search?q=bruh")).build();
                HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
                HttpHeaders headers = response.headers();
                Map<String, List<String>> map = headers.map();
                List<String> list = map.get(":status");
                String responseCode = list.get(0);
                if(responseCode.equals("200")){
                    return true;
                } else {
                    return false;
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void setup(HttpExchange e){
        file2String(System.getProperty("user.home"));
    }

    static String file2String(String folder){
        ArrayList<Fichier> files = Explorer.getFiles(folder);
        JSONStringer json = new JSONStringer();
        json.array();
        for(Fichier file: files){
            json.object();
            json.key("name");
            json.value(file.name);
            json.key("url");
            json.value(file.url);
            json.key("minia");
            json.value(file.minia);
            json.key("file");
            json.value(file.file);
            json.endObject();
        }
        json.endArray();
        return json.toString();
    }


    public String getPlexData() throws SQLException{
        String sql = "SELECT rowid, titre, titrevo, minia, serie, saison, nbep FROM info";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        ResultSet rs  = pstmt.executeQuery();
        JSONStringer json = new JSONStringer();
        json.array();    
        while(rs.next()){
            json.object();
            json.key("rowid");
            json.value(rs.getInt("rowid"));
            json.key("titre");
            json.value(rs.getString("titre"));
            json.key("titrevo");
            json.value(rs.getString("titrevo"));
            json.key("minia");
            json.value(rs.getString("minia"));
            json.key("serie");
            json.value(rs.getBoolean("serie"));
            json.key("saison");
            json.value(rs.getInt("saison"));
            json.key("nbep");
            json.value(rs.getInt("nbep"));
            json.endObject();
        }
        json.endArray();
        return json.toString();
    }

    private String getFileData(int rowid) throws SQLException{
        String sql = "SELECT url, ep, duree FROM fichier WHERE info = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        pstmt.setInt(1, rowid);
        ResultSet rs  = pstmt.executeQuery();
        JSONStringer json = new JSONStringer();
        json.array();    
        while(rs.next()){
            json.object();
            json.key("url");
            json.value(rs.getString("url"));
            json.key("ep");
            json.value(rs.getInt("ep"));
            json.key("duree");
            json.value(rs.getInt("duree"));
            json.endObject();
        }
        json.endArray();
        return json.toString();
    }

    private boolean isAdmin(String token) throws SQLException{
        String sql = "SELECT admin FROM users WHERE token = ?";
        
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        // set the value
        pstmt.setString(1, token);
        ResultSet rs  = pstmt.executeQuery();    
        // loop through the result set
        rs.next();
        return Boolean.valueOf(rs.getString("admin"));
    }

    private boolean isAdmin(HttpExchange e) throws SQLException{
        List<String> bruh = e.getRequestHeaders().get("token");
        String token = bruh != null ? bruh.get(0) : "bru";
        String sql = "SELECT admin FROM users WHERE token = ?";
        PreparedStatement pstmt  = conn.prepareStatement(sql);
        // set the value
        pstmt.setString(1, token);
        ResultSet rs  = pstmt.executeQuery();    
        // loop through the result set
        rs.next();
        return Boolean.valueOf(rs.getString("admin"));
    }

    private boolean protectedUrl(HttpExchange e, String url) throws SQLException{
        List<String> bruh = e.getRequestHeaders().get("token");
        String token = bruh != null ? bruh.get(0) : "bru";
        for(String link: adminurl){
            //System.out.println(link + "     :   "+url);
            if(link.equals(url)){
                if(isAdmin(token)){
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private String tokenGenerator(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        StringBuilder sb = new StringBuilder();
        random.nextBytes(salt);
        for(byte b : salt){
            sb.append(b);
            if(b!=salt[salt.length-1]){
                sb.append(";");
            }
        }
        return sb.toString();
    }


    private byte[] getPasswordHash(char[] password) throws Exception{
        //SecureRandom random = new SecureRandom();
        byte[] salt = "XENOBLADE".getBytes(); //byte[] salt = new byte[16];random.nextBytes(salt); idéalement le hash doit etre random et stocké sous forme de hash+mdp-hashé
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return hash;
    }


    private Map<String, String> queryParser(String query){
        Map<String, String> map = new LinkedHashMap<String, String>();
        if(query.equals("")){
            return map;
        }
        for(String entry : query.split("&")){
            String[] kv = entry.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    static Map<String, FormData> formDataParser(byte[] b) throws FileNotFoundException, IOException{
        Map<String, FormData> items = new LinkedHashMap<>();
        int i = 0;
        ArrayList<Byte> delimiteur = new ArrayList<>(39);
        delimiteur.add((byte)13);
        delimiteur.add((byte)10);
        while(b[i+1] != 13 && b[i+2] != 10){
            delimiteur.add(b[i]);
            i++;
        }
        int icursor = 0;
        while(i != b.length){
            int k = i;
            for(int j = 0; j < delimiteur.size(); j++){
                if(k >= b.length){
                    i = k;
                    break;
                }
                if(delimiteur.get(j)!=b[k]){
                    ArrayList<Byte> name = new ArrayList<>();
                    ArrayList<Byte> data = new ArrayList<>();
                    if(b[i-6]==110 && b[i-5]==97 && b[i-4]==109 && b[i-3]==101 && b[i-2]==61 && b[i-1]==34){
                        while(b[i]!=34){
                            name.add(b[i]);
                            i++;
                        }
                        //System.out.println(al.get(j)+" "+al.get(j+1)+" "+al.get(j+2)+" "+al.get(j+4)+" ");
                        ArrayList<Byte> filename = new ArrayList<>();
                        ArrayList<Byte> mimetype = new ArrayList<>();
                        if(b[i+1] == 59 && b[i+2]==32){
                            if(b[i+3] == 'f'&&b[i+4] == 'i'&&b[i+5] == 'l'&&b[i+6] == 'e'){
                                i+=13;
                                while(b[i]!=34){
                                    filename.add(b[i]);
                                    i++;
                                }
                                i+=17;
                                while(!(b[i]==13&&b[i+1]==10&&b[i+2]==13&&b[i+3]==10)){
                                    mimetype.add(b[i]);
                                    i++;
                                }
                            }
                        }
                        //System.out.println(byte2string(name));
                        while(!(b[i-1]==10&&b[i-2]==13&&b[i-3]==10&&b[i-4]==13)){
                            i++;
                        }
                        while(!chkifdelimiteur(b, delimiteur, i)){
                            data.add(b[i]);
                            i++;
                        }
                        i--;
                        //System.out.println(b[i]+""+b[i+1]+""+b[i+2]+""+b[i+3]);
                        items.put(byte2string(name), new FormData(data, byte2string(mimetype), byte2string(filename)));
                        /*System.out.println(items.get(icursor).name);
                        //System.out.println(items.get(icursor).mimetype);
                        System.out.println(items.get(icursor).filename);
                        System.out.println(icursor);*/
                        if(byte2string(name).equals("avatar")){
                            byte[] stringarr = new byte[items.get("avatar").data.size()];
                            for(int h = 0; h < items.get("avatar").data.size(); h++){
                                stringarr[h] = items.get("avatar").data.get(h);
                            }
                            //String str = new String(stringarr, StandardCharsets.UTF_8);
                            File outputFile = new File("avatar.jpg");
                            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                                outputStream.write(stringarr);
                            }
                        } else {
                            //System.out.println(byte2string(items.get(icursor).value));
                        }
                        //icursor++;
                        //items.add(icursor, new FormData());
                    }
                    i++;
                    break;
                } else {
                    k++;
                    //System.out.println(delimiteur.get(j)+" : "+b[k]+" : "+j+" : "+k);
                    if(j+1 == delimiteur.size()){
                        i=k;
                        icursor++;
                        //break;
                    }
                }
                
            }
        }
        return items;
        /*Map<String, ArrayList<Byte>> map = new LinkedHashMap<String, ArrayList<Byte>>();
        for(ArrayList<Byte> al: items){
            int j = 6;
            ArrayList<Byte> name = new ArrayList<>();
            ArrayList<Byte> data = new ArrayList<>();
            //System.out.println(al.size());
            while(j < al.size()){
                //System.out.println(j);
                if(al.get(j-6)==110 && al.get(j-5)==97 && al.get(j-4)==109 && al.get(j-3)==101 && al.get(j-2)==61 && al.get(j-1)==34){
                    while(al.get(j)!=34){
                        name.add(al.get(j));
                        j++;
                    }
                    //System.out.println(al.get(j)+" "+al.get(j+1)+" "+al.get(j+2)+" "+al.get(j+4)+" ");
                    byte[] stringarr = new byte[name.size()];
                    for(int h = 0; h < name.size(); h++){
                        stringarr[h] = name.get(h);
                    }
                    String str = new String(stringarr, StandardCharsets.UTF_8);
                    System.out.println(str);
                    ArrayList<Byte> filename = new ArrayList<>();
                    ArrayList<Byte> mimetype = new ArrayList<>();
                    if(al.get(j+1) == 59 && al.get(j+2)==32){
                        if(al.get(j+3) == 'f'&&al.get(j+4) == 'i'&&al.get(j+5) == 'l'&&al.get(j+6) == 'e'){
                            j+=13;
                            while(al.get(j)!=34){
                                filename.add(al.get(j));
                                j++;
                            }
                            j+=17;
                            while(!(al.get(j)==13&&al.get(j+1)==10&&al.get(j+2)==13&&al.get(j+3)==10)){
                                mimetype.add(al.get(j));
                                j++;
                            }
                        }
                    }
                    while(!(al.get(j-1)==10&&al.get(j-2)==13&&al.get(j-3)==10&&al.get(j-4)==13)){
                        j++;
                    }
                    while(j != al.size()){
                        data.add(al.get(j));
                        j++;
                    }
                    System.out.println(byte2string(filename));
                    System.out.println(byte2string(mimetype));
                    map.put(str, data);
                }
                j++;
            }
        }
        //System.out.println(map.get("password"));
        byte[] stringarr = new byte[map.get("avatar").size()];
        for(int h = 0; h < map.get("avatar").size(); h++){
            stringarr[h] = map.get("avatar").get(h);
        }
        //String str = new String(stringarr, StandardCharsets.UTF_8);
        File outputFile = new File("avatar.jpg");
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(stringarr);
        }
        //System.out.println(str);*/
    }

    public static String byte2string(ArrayList<Byte> map){
        byte[] stringarr = new byte[map.size()];
        for(int h = 0; h < map.size(); h++){
            stringarr[h] = map.get(h);
        }
        return new String(stringarr, StandardCharsets.UTF_8);
    }

    public static boolean chkifdelimiteur(byte[] b, ArrayList<Byte> delimiteur, int i){
        for(int j = 0; j < (delimiteur.size()-2); j++){
            if(b[i+j] != delimiteur.get(j)){
                return false;
            }
        }
        return true;
    }

}
