import java.util.HashMap;

public class test {
    public static void main(String[] args) {
        BPlusTree<String, HashMap<String,String>> btree = new BPlusTree<>();
        HashMap<String,String> mp = new HashMap<>();
        mp.put("attr","value");
        mp.put("attr2","value2");
        btree.insert("value",mp);
        HashMap<String,String> mp1 = new HashMap<>();
        mp1.put("attr","value1");
        mp1.put("attr2","value2");
        btree.insert("value1",mp1);
        btree.remove("value1");
        System.out.println(btree.query("value").get(0));
    }
}
