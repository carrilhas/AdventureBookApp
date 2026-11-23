package adventure_book_app.model.book;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // written files leave out empty fields, like the hand-made ones
@JsonPropertyOrder({"title", "author", "difficulty", "sections"})
public class Book {
    /** URL-safe id, set by {@code BookLoader} from the file name (e.g. "crystal-caverns"). Not part of the JSON. */
    @JsonIgnore
    private String id;
    private String title;
    private String author;
    private Difficulty difficulty;
    private List<Section> sections;
}
