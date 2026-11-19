package adventure_book_app.model.book;

import lombok.Data;

import java.util.List;

@Data
public class Book {
    /** URL-safe id, set by {@code BookLoader} from the file name (e.g. "crystal-caverns"). Not read from the JSON. */
    private String id;
    private String title;
    private String author;
    private Difficulty difficulty;
    private List<Section> sections;
}
