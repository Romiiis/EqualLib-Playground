import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

public class ColMap {

    ArrayList<City> cities;
    HashSet<Player> players;
    HashMap<String, Dog> dogs;
    HashMap<String, String>[] arrayMaps;
    ArrayList<String>[] arrayLists;

    public ColMap() {
        super();
        this.cities = new ArrayList<>();
        this.players = new HashSet<>();
        this.dogs = new HashMap<>();
        this.arrayMaps = new HashMap[10];
        this.arrayLists = new ArrayList[10];
        for (int i = 0; i < 10; i++) {
            this.arrayMaps[i] = new HashMap<>();
            this.arrayLists[i] = new ArrayList<>();
        }
    }
}