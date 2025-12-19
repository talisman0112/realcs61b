//package gitlet;
//
//import java.io.Serializable;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//public class Commit implements Serializable {
//    private final String message;
//    private final String parent;
//    private final Date timestamp;
//    private final Map<String, String> blobs;
//    private final String hash;
//    private final String secondParent;
//    public Commit() {
//        this("initial commit", null, null, new HashMap<>());
//    }
//    public Commit(String message, String parent, String secondParent, Map<String, String> blobs) {
//        this.message = message;
//        this.parent = parent;
//        this.secondParent = secondParent;
//        this.timestamp = new Date();
//        this.blobs = new HashMap<>(blobs);
//        this.hash = calculateHash();
//    }
//
//    public Commit(String message, String parent, Map<String, String> blobs) {
//        this.message = message;
//        this.parent = parent;
//        this.secondParent=null;
//        this.timestamp = new Date();
//        this.blobs = new HashMap<>(blobs);
//        this.hash = calculateHash();
//    }
//
//    private String calculateHash() {
//        List<Object> list = new ArrayList<>();
//        list.add(message == null ? "" : message);
//        list.add(timestamp.getTime() + "");           // 关键！转 String
//        list.add(parent == null ? "" : parent);
//        list.add(secondParent == null ? "" : secondParent);
//        TreeMap<String, String> sorted = new TreeMap<>(blobs);
//        for (Map.Entry<String, String> e : sorted.entrySet()) {
//            list.add(e.getKey());
//            list.add(e.getValue());
//        }
//        return Utils.sha1(list);
//    }
//    public String getSecondParent() { return secondParent; }
//    public String getHash() { return hash; }
//    public String getMessage() { return message; }
//    public String getParent() { return parent; }
//    public Date getTimestamp() { return timestamp; }
//    public Map<String, String> getBlobs() { return Collections.unmodifiableMap(blobs); }
//
//    public String getTimestampString() {
//        SimpleDateFormat f = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
//        return f.format(getTimestamp());
//    }
//}
package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Commit implements Serializable {

    private final String message;
    private final String parent;
    private final String secondParent;
    private final Date timestamp;
    private final Map<String, String> blobs;
    private final String hash;

    private Commit(String message, String parent, String secondParent,
                   Map<String, String> blobs, Date timestamp) {
        this.message = message;
        this.parent = parent;
        this.secondParent = secondParent;
        this.timestamp = timestamp;
        this.blobs = blobs == null ? new HashMap<>() : new HashMap<>(blobs);
        this.hash = calculateHash();
    }

    public Commit() {
        this("initial commit", null, null, new HashMap<>(), new Date(0));
    }

    public Commit(String message, String parent, Map<String, String> blobs) {
        this(message, parent, null, blobs, new Date());
    }

    public Commit(String message, String parent, String secondParent, Map<String, String> blobs) {
        this(message, parent, secondParent, blobs, new Date());
    }

    private String calculateHash() {
        List<Object> list = new ArrayList<>();
        list.add(message == null ? "" : message);
        list.add(timestamp.getTime() + "");
        list.add(parent == null ? "" : parent);
        list.add(secondParent == null ? "" : secondParent);

        TreeMap<String, String> sorted = new TreeMap<>(blobs);
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            list.add(e.getKey());
            list.add(e.getValue());
        }
        return Utils.sha1(list.toArray());
    }
    public String getHash() { return hash; }
    public String getMessage() { return message; }
    public String getParent() { return parent; }
    public String getSecondParent() { return secondParent; }
    public Date getTimestamp() { return timestamp; }
    public Map<String, String> getBlobs() { return Collections.unmodifiableMap(blobs); }

    public String getTimestampString() {
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return f.format(timestamp);
    }
}