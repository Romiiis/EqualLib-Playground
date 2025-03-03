
public class Address {

    private String street;
    private City city;

    public Address(String street, City city) {
        this.street = street;
        this.city = city;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        Address address = (Address) obj;
        return this.street.equals(address.street) && this.city.equals(address.city);
    }

}
