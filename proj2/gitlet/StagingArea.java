package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public class StagingArea implements Serializable {
    private final Map<String,String>addition=new HashMap<>();
    private final Set<String>removal= new HashSet<>();
    public void temp_add(String filename, String hash){
        addition.put(filename,hash);
        removal.remove(filename);
    }
    public void temp_move(String filename,String hash){
        addition.remove(filename);
        removal.add(filename);
    }
    public void clear() {
        addition.clear();
        removal.clear();
    }
    public Map<String, String> getAddition() { return addition; }
    public Set<String> getRemoval() { return removal; }
    public boolean isEmpty() { return addition.isEmpty() && removal.isEmpty(); }

}