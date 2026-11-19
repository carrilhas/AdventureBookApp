package adventure_book_app.model.book;

import lombok.Data;

import java.util.List;

@Data
public class Book {
    private String title;
    private String author;
    private Difficulty difficulty;
    private List<Section> sections;
}
