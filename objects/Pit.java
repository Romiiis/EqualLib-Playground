

/**
 * Pit is a special type of Hole, can be clicked
 *
 * @author Roman Pejs
 * @version 1.0
 * @see Hole
 */
public class Pit extends Hole {

    private boolean[][] holes;
    private String[][] names;
    private Country[] countries;
    private byte [] bytes;

    /**
     * Constructor
     *
     * @param holes array of holes
     * @param names array of names
     * @param countries array of countries
     * @param bytes array of bytes
     */
    public Pit() {
        this.holes = new boolean[6][7];
        this.names = new String[6][8];
        this.countries = new Country[6];
        this.bytes = new byte[6];
    }


}
