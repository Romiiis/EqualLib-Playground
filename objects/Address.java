
public class Address {

    private String street;
    private City[] city;

    public Address() {
        this.city = new City[10];
        city[3] = new City();
    }



}
