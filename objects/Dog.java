import java.util.ArrayList;
import java.util.HashSet;

public class Dog extends Animal {

    private String breed;
    ArrayList<String> names;
    HashSet<String> names2;

    public Dog() {
        super();
        this.names2 = new HashSet<>();
        this.names = new ArrayList<>();
        names.add("Rex");
    }
}