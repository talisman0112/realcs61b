package gitlet;
import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable,Dumpable {
    private  byte[] content;
    private  String hash;


    public Blob(File file) {
        this.content = Utils.readContents(file);
        this.hash = Utils.sha1(content);
    }
    public Blob(byte[] content) {
        this.content = content;
        this.hash = Utils.sha1(content);
    }
    public byte[] getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public void dump(){
        System.out.println("blob hash"+hash);
        System.out.println("content length"+content.length+"byte");
    }

}
