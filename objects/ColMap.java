import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

public class ColMap {

    ArrayList<City> cities;
    HashSet<Player> players;
    HashMap<String, Dog> dogs;

    public ColMap() {
        super();
        this.cities = new ArrayList<>();
        this.players = new HashSet<>();
        this.dogs = new HashMap<>();
    }
}