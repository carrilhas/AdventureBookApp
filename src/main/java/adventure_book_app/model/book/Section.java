package adventure_book_app.model.book;

import lombok.Data;

import java.util.List;

@Data
public class Section {
    private int id;
    private String text;
    private String type;
    private List<Option> options;
}
