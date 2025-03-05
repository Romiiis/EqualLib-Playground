
public class City {

    private String name;
    private Country country;
    private int[] population;

    public City() {
        this.population = new int[10];
        population[3] = 1000;
        this.country = Country.CZ;
    }



}
