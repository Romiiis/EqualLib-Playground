import java.util.ArrayList;
import java.util.HashSet;


public class Person {
    private String name;
    private int age;
    private Address address;
    private String[][][] multiArray;
    private ArrayList<City> cities;
    private HashSet<Country> countries;

    public Person() {
        this.address = new Address();
        this.multiArray = new String[2][2][2];
        this.cities = new ArrayList<>();
        this.countries = new HashSet<>();
    }


}
