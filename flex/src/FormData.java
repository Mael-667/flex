import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FormData {
    String filename;
    String mimetype;
    ArrayList<Byte> data;
    public FormData(ArrayList<Byte> data, String mimetype, String filename){
        this.data = data;
        this.mimetype = mimetype;
        this.filename = filename;
    }
    public String getString(){
        byte[] stringarr = new byte[data.size()];
        for(int h = 0; h < data.size(); h++){
            stringarr[h] = data.get(h);
        }
        return new String(stringarr, StandardCharsets.UTF_8);
    }
    public File getFile(String path){
        return getFile(path, filename);
    }
    public File getFile(String path, String name){
        byte[] stringarr = new byte[data.size()];
        for(int h = 0; h < data.size(); h++){
            stringarr[h] = data.get(h);
        }
        File outputFile = new File(path+name);
        try{
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(stringarr);
            outputStream.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        return outputFile;
    }
}
