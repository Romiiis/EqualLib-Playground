

import java.util.HashSet;
import java.util.Set;


/**
 * This class represents player in game
 * Holds nickname, type, pits and house
 *
 * @author Roman Pejs
 * @version 1.0
 * @see GamePanel
 */
public class Player {

    // Nickname of the user
    private final String nickname;

    // Index of user store
    private final int storeIndex;

    // Indexes of user Pits in order
    private final Set<Integer> pitIndexes; // Set for fast lookups



    public Player() {
        this.pitIndexes = new HashSet<>();
        this.nickname = "Player";
        this.storeIndex = 0;

    }


}


