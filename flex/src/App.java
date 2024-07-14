import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.swing.filechooser.FileSystemView;
import javax.swing.Icon;

import com.sun.net.httpserver.HttpServer;

public class App {
    public static void main(String[] args){
        try{
            System.out.println("Hello, World!");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:db.db");
            //Explorer ex = new Explorer(conn);
            Flex flex = new Flex(conn);
            String[] bru = new String[]{"D:\\Users\\Mael\\Desktop\\[KURISU_] Dragon Ball Super Super Hero VOSTFR 1080P BDRIP X265.mkv",
                                        "D:\\Users\\Mael\\Desktop\\PLUTO.S01.1080p.NF.WEB-DL.DDP5.1.H.264-VARYG\\PLUTO.S01E01.1080p.NF.WEB-DL.DDP5.1.H.264-VARYG.mkv", 
                                        "D:\\Users\\Mael\\Desktop\\[IssouCorp] One Piece (Live Action) - 01 - 08 MULTI (VF-VO-JAP Dub) [1080p]\\[IssouCorp] One Piece (Live Action) - 05 MULTI [1080p].mkv",
                                        "D:\\Users\\Mael\\Desktop\\John Wick Chapitre 4 [FR-EN] (2023).mkv",
                                        "D:\\Users\\Mael\\Desktop\\Cyberpunk Edgerunners WEB-DL 1080P HDR DV EAC3 VF VOSTFR-LTPD v2\\Cyberpunk Edgerunners 10 WEB-DL 1080P HDR DV EAC3 VF VOSTFR-LTPD.mkv",
                                        "D:\\Users\\Mael\\Desktop\\[Trix] Yofukashi no Uta (1080p AV1 WEB)[Multi Subs]\\[Trix] Yofukashi no Uta E02 (1080p AV1 Opus)[Multi Subs].mkv",
                                        "D:\\Users\\Mael\\Desktop\\Jujutsu Kaisen S02E03 VOSTFR 1080p WEB x264 AAC -Tsundere-Raws (CR).mkv",
                                        "D:\\Users\\Mael\\Desktop\\[Erai-raws] Evangelion 2.0 You Can (Not) Advance - Movie [1080p][Multiple Subtitle].mkv"};
            for (String br : bru){
                //bruh.scan(br);
            }

            /*File bruh = new File("/home/mael/Vidéos/");
            Runnable runnable = () -> { try {
                flex.deepScan(bruh);
                System.out.println("bruh2");
            } catch (Exception e) {
                e.printStackTrace();
            } };
            Thread thread = new Thread(runnable);
            thread.start();
            System.out.println("bruh");*/
            //FileInputStream fis = new FileInputStream(bruh);
            //fis.readAllBytes();
            //Handler.formDataParser(fis.readAllBytes());
            //fis.close();

            /*File outputFile = new File("bruh.jpg");
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            FileInputStream fis = new FileInputStream("avatar/goat.png");
            outputStream.write(fis.readAllBytes());
            outputStream.close();
            fis.close();*/
            //System.out.println(System.getProperty("user.home"));
            //Handler.setup(null);

            //Explorer.miniature(new File("D:\\Users\\Mael\\Desktop\\[Trix] Yofukashi no Uta (1080p AV1 WEB)[Multi Subs]\\[Trix] Yofukashi no Uta E02 (1080p AV1 Opus)[Multi Subs].mkv"));
            //Explorer.miniature(new File("1-01 Main Theme.m4a"));
            //Explorer.miniature(new File("C:\\Users\\Mael\\Downloads\\1-01 Heat Haze Shadow.flac"));

            //try {
                //Document doc = Jsoup.connect("https://www.google.com/search?q=bruh").get();
                //Elements content = doc.getElementsByAttribute("href");
                //System.out.println(content.html());
            //} catch (IOException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            //}


            /*Document doc = Jsoup.connect("https://www.imdb.com/title/tt11032374/").get();
            Elements content = doc.getElementsByClass("ipc-image");
            Elements bh = doc.getElementsByAttributeValue("data-testid", "hero__pageTitle");
            String minia = content.get(0).attr("currentSrc");
            String titre = bh.get(0).firstElementChild().wholeOwnText();
            String titre_eng = bh.get(0).nextElementSibling().wholeOwnText().replace("Titre original : ", "");
            System.out.println(minia);*/

            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);
            server.createContext("/", new Handler(conn));
            server.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
