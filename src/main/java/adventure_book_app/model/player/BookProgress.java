package adventure_book_app.model.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A player's state in one book. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookProgress {
    private int sectionId;
    private int health;

    /** Consequence text from the last choice, shown once on the next page. */
    private String lastMessage;
}
