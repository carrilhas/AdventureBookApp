package adventure_book_app.mapper;

import adventure_book_app.api.dto.ApiDtos.NewBook;
import adventure_book_app.api.dto.ApiDtos.NewConsequence;
import adventure_book_app.api.dto.ApiDtos.NewOption;
import adventure_book_app.api.dto.ApiDtos.NewSection;
import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Consequence;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Maps a new book sent to the API ({@link NewBook}) to the {@link Book} model that is saved in the books folder.
 * <p>
 * MapStruct generates the implementation ({@code BookMapperImpl}) at compile time and registers it as a Spring bean,
 * so it can be injected like any other service. Fields with the same name are copied automatically; the methods
 * below cover the ones that need converting. {@code unmappedTargetPolicy = ERROR} makes the build fail if a field is
 * added to the model and forgotten here.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookMapper {

    /** The id comes from the file name the book is saved under, not from the request. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", qualifiedByName = "trim")
    @Mapping(target = "author", qualifiedByName = "trim")
    Book toBook(NewBook source);

    @Mapping(target = "type", qualifiedByName = "code")
    Section toSection(NewSection source);

    Option toOption(NewOption source);

    @Mapping(target = "type", qualifiedByName = "code")
    Consequence toConsequence(NewConsequence source);

    /**
     * "easy" / " Hard " -> the matching {@link Difficulty}; blank -> null.
     *
     * @throws IllegalArgumentException for an unknown difficulty
     */
    default Difficulty toDifficulty(String value) {
        return value == null || value.isBlank() ? null : Difficulty.fromString(value);
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Section and consequence types are stored upper case, like in the existing books: " begin " -> "BEGIN". */
    @Named("code")
    default String code(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
