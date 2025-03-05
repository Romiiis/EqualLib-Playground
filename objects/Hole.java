

/**
 * This abstract class represents hole and provides basic operations
 *
 * @author Roman Pejs
 * @version 1.0
 * @see Pit
 * @see JPanel
 * @see House
 */
public abstract class Hole {

    /* Initial peas count */
    protected final static int INIT_PEAS_COUNT = 4;
    protected final static double SCALE_WIDTH = 0.90;
    protected final static double SCALE_HEIGHT = 0.70;

    /* Actual count of peas in hole */
    private int peasCount;

    /* Hovered flag */
    private boolean isHovered = false;


}
