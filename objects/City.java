
public class City {

    private String name;
    private Country country;

    public City(String name, Country country) {
        this.name = name;
        this.country = country;
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
        City city = (City) obj;
        return this.name.equals(city.name) && this.country.equals(city.country);
    }

}
