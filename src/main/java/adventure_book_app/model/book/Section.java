package adventure_book_app.model.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // written files leave out empty fields, like the hand-made ones
@JsonPropertyOrder({"id", "text", "type", "options"})
public class Section {
    /** Integer, not int, so a missing id stays null and BookValidator can report it instead of it becoming 0. */
    private Integer id;
    private String text;
    private String type;
    private List<Option> options;
}
