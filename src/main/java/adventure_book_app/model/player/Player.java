package adventure_book_app.model.player;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Player {
    private String name;
    private int score;

    /** Progress in each book the player has started, keyed by book title. Health is tracked per book. */
    private Map<String, BookProgress> progress = new HashMap<>();
}
