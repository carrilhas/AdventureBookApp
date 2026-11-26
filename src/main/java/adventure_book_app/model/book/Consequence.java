package adventure_book_app.model.book;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // written files leave out empty fields, like the hand-made ones
@JsonPropertyOrder({"type", "value", "text"})
public class Consequence {
    private String type;
    private String value;
    private String text;
}
