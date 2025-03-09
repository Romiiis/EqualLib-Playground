import java.util.HashMap;
import java.util.Map;

public class Address {

    private String street;
    private City[][] city;
    Map<String, City> cityMap;

    public Address() {
        this.city = new City[10][5];
        city[3][2] = new City();
        cityMap = new HashMap<>();
    }



}
