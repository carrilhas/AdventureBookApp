package adventure_book_app.model.player;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Player {
    private String name;
    private int score;
    private int health;

    /** Current section id the player is on, keyed by book title. */
    private Map<String, Integer> progress = new HashMap<>();
}
