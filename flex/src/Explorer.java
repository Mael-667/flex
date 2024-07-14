import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.codec.digest.DigestUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.images.Artwork;

public class Explorer {

    static String iconeDoss = "src/site/folder.png";
    static String iconeMusique = "src/site/audio.png";
    static String iconePdf = "src/site/pdf.png";
    static String iconeDefaut = "src/site/file.png";


    public static ArrayList<Fichier> getFiles(String url){
        ArrayList<Fichier> liste = new ArrayList<>();
        try{
            File folder = new File(url);
            File[] files = folder.listFiles();
            for(File file : files){
                Fichier fichier = new Fichier(file.getAbsolutePath());
                fichier.name = file.getName();
                if(file.isFile()){
                    fichier.file = true;
                    String urlminia = "minia/"+getChksum(file.getAbsolutePath())+".jpg";
                    if(!(new File(urlminia)).isFile()){
                        switch(miniature(file)){
                            case 0:
                                fichier.minia = urlminia;
                                break;
                            case 7:
                                fichier.minia = getAudioMinia(file);
                            break;
                            default:
                                fichier.minia = getDefault(file);
                            break;
                        }
                    } else {
                        fichier.minia = urlminia;
                    }
                } else {
                    fichier.minia = iconeDoss;
                }
                liste.add(fichier);
            }        
        }catch(Exception e){
            e.printStackTrace();
        }
        return liste;
    }


    public static int miniature(File file){
        //System.out.println(file.getAbsolutePath());
        String name = getChksum(file.getAbsolutePath());
        int returnvalue = 0;
        try {
            BufferedImage minia = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            if(getMimeType(file).startsWith("image")){
                minia = ImageIO.read(file);
            }else if(getMimeType(file).startsWith("video")){

                int length = Flex.metadata(file.getAbsolutePath());
                String[] arg = new String[]{};
                if(length > 60){
                    int sec = (new Random()).nextInt(length > 7 ? 7 : length);
                    arg = new String[]{ffmpegVer(), "-y", "-ss", "00:0"+Integer.toString(sec)+":00", "-i", file.getAbsolutePath(), "-vframes", "1", "frame.jpg", "-hide_banner"};
                } else {
                    arg = new String[]{ffmpegVer(), "-y", "-ss", "00:00:"+length, "-i", file.getAbsolutePath(), "-vframes", "1", "frame.jpg", "-hide_banner"};
                }
                ProcessBuilder pb = new ProcessBuilder(arg);
                File log = new File("log2.txt");
                pb.redirectErrorStream(true);
                pb.redirectOutput(Redirect.to(log));
                Process pr = pb.start();
                pr.waitFor();
                minia = ImageIO.read(new File("frame.jpg"));
            } else if(getMimeType(file).startsWith("audio")) {
                AudioFile audio = AudioFileIO.read(file);
                Tag tags = audio.getTag();
                Artwork art = tags.getFirstArtwork();
                if(art != null){
                    String album = tags.getFirst(FieldKey.ALBUM);
                    String artist = tags.getFirst(FieldKey.ARTIST);
                    if(new File("minia/"+getChksum((album+artist).getBytes())+".jpg").isFile()){
                        return 7;
                    } else {
                        minia = (BufferedImage) art.getImage();
                        name = getChksum((album+artist).getBytes());
                        returnvalue = 7;
                    }
                } else {
                    return -1;
                }
            } else {
                return -1;
            }

            BufferedImage mini = new BufferedImage(minia.getWidth(), minia.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = mini.createGraphics();
            g.fillRect(0, 0, minia.getWidth(), minia.getHeight());
            g.drawImage(minia, 0, 0, null);
            g.dispose();
            minia = new BufferedImage(mini.getWidth(), mini.getHeight(), BufferedImage.TYPE_INT_RGB);
            float[ ] masqueFlou = 
            {
                0.0875f, 0.0875f, 0.0875f,
                0.0875f, 0.3f, 0.0875f,
                0.0875f, 0.0875f, 0.0875f
            };
            Kernel masque = new Kernel(3, 3, masqueFlou);
            ConvolveOp operation = new ConvolveOp(masque);
            operation.filter(mini, minia);


            int w = 300;
            BufferedImage ans = new BufferedImage(w, (w * minia.getHeight())/minia.getWidth(), BufferedImage.TYPE_INT_RGB);
            double coeff = 100.0d*(double)w/(double)minia.getWidth();
            System.out.println(coeff/=100.0d);
            AffineTransform reduire = AffineTransform.getScaleInstance(coeff, coeff);
            int interpolation = AffineTransformOp.TYPE_BICUBIC;
            AffineTransformOp retaillerImage = new AffineTransformOp(reduire, interpolation);
            retaillerImage.filter(minia, ans);
            Files.createDirectories(Paths.get("minia"));
            File outputfile = new File("minia/"+name+".jpg");
            save(ans, outputfile, 0.9f);
            return returnvalue;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void save(BufferedImage img, File output, float quality) throws IOException{
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(quality);

        FileImageOutputStream outputStream = new FileImageOutputStream(output); // For example implementations see below
        jpgWriter.setOutput(outputStream);
        IIOImage outputImage = new IIOImage(img, null, null);
        jpgWriter.write(null, outputImage, jpgWriteParam);

        jpgWriter.dispose();
    }

    public static String getMimeType(File file){
        Path path = file.toPath();
        String mimeType = " ";
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mimeType == null ? " " : mimeType;
    }

    public static String getChksum(String path){
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] data = new byte[7777777];
            fis.read(data);
            String checksum = DigestUtils.md5Hex(data);
            return checksum;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getChksum(byte[] data){
        String checksum = DigestUtils.md5Hex(data);
        return checksum;
    }

    public static String getAudioMinia(File file) throws Exception{
        AudioFile audio = AudioFileIO.read(file);
        Tag tags = audio.getTag();
        String album = tags.getFirst(FieldKey.ALBUM);
        String artist = tags.getFirst(FieldKey.ARTIST);
        return "minia/"+getChksum((album+artist).getBytes())+".jpg";
    }

    private static String getDefault(File file){
        String mime = getMimeType(file);
        if(mime.startsWith("audio")){
            return iconeMusique;
        } else if(mime.startsWith("application/pdf")){
            return iconePdf;
        } else {
            return iconeDefaut;
        }
    }

    private void fallback(File file){
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon( file );
        Image img = ((ImageIcon) icon).getImage();
        BufferedImage bi = new BufferedImage(img.getWidth(null),img.getHeight(null),BufferedImage.TYPE_INT_ARGB);
        BufferedImage ans = new BufferedImage(500, (500 * img.getHeight(null))/img.getWidth(null), BufferedImage.TYPE_INT_RGB);
        double coeff = 100.0d*(double)500/(double)img.getWidth(null);
        System.out.println(coeff/=100);
        AffineTransform reduire = AffineTransform.getScaleInstance(coeff, coeff);
        int interpolation = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        AffineTransformOp retaillerImage = new AffineTransformOp(reduire, interpolation);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        retaillerImage.filter(bi, ans);
        try {
            ImageIO.write(ans, "png", new File("img.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String ffmpegVer(){
        if(System.getProperty("os.name").contains("Linux")){
            return "src/ffmpeg/ffmpeg";
        } else if(System.getProperty("os.name").contains("Windows")){
            return "src/ffmpeg/ffmpeg.exe";
        }
        return "src/ffmpeg/ffmpeg.exe";
    }

    public static String ffprobeVer(){
        if(System.getProperty("os.name").contains("Linux")){
            return "src/ffmpeg/ffprobe";
        } else if(System.getProperty("os.name").contains("Windows")){
            return "src/ffmpeg/ffprobe.exe";
        }
        return "src/ffmpeg/ffprobe.exe";
    }

}
