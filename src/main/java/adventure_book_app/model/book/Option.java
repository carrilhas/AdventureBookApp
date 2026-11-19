package adventure_book_app.model.book;

import lombok.Data;

@Data
public class Option {
    private String description;
    private Integer gotoId;
    private Consequence consequence;
}
