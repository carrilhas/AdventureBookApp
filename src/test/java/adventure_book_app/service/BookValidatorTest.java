package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Consequence;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookValidatorTest {

    private final BookValidator validator = new BookValidator();

    @Test
    void acceptsAPlayableBook() {
        assertThat(validator.problemsWith(validBook())).isEmpty();
    }

    @Test
    void requiresTitleAuthorDifficultyAndSections() {
        Book book = new Book();
        assertThat(validator.problemsWith(book)).containsExactly(
                "title is required",
                "author is required",
                "difficulty is required (EASY, MEDIUM or HARD)",
                "a book needs at least one section");
    }

    @Test
    void reportsSectionAndOptionFieldsByPosition() {
        Book book = validBook();
        book.getSections().get(0).setText(" ");
        book.getSections().get(0).getOptions().get(0).setDescription(null);
        book.getSections().get(1).setId(null);

        assertThat(validator.problemsWith(book)).contains(
                "section 1 needs text",
                "option 1 of section 1 needs a description",
                "section 2 needs an id");
    }

    @Test
    void reportsEveryStructuralRuleAtOnce() {
        Section first = section(1, "NODE", option("go", 9, null));
        Section second = section(1, "NODE", option("back", 1, null));
        Book book = book(first, second);

        assertThat(validator.problemsWith(book)).containsExactly(
                "a book needs exactly one BEGIN section",
                "a book needs at least one END section",
                "section ids must be unique, repeated: [1]",
                "options point to missing sections: [9] (null means an option has no gotoId)");
    }

    @Test
    void checksConsequenceTypeAndAmount() {
        Book book = validBook();
        List<Option> options = book.getSections().get(0).getOptions();
        options.add(option("a", 2, consequence("lose_health", "3"))); // any case is fine
        options.add(option("b", 2, consequence("FOO", "1")));
        options.add(option("c", 2, consequence("GAIN_HEALTH", "")));
        options.add(option("d", 2, consequence("GAIN_HEALTH", "0")));

        assertThat(validator.problemsWith(book)).containsExactly(
                "the consequence of option 3 of section 1 needs a type: LOSE_HEALTH or GAIN_HEALTH",
                "the consequence of option 4 of section 1 needs an amount that is a whole number above 0",
                "the consequence of option 5 of section 1 needs an amount that is a whole number above 0");
    }

    @Test
    void reportsEmptySectionsAndOptionsWithoutCrashing() {
        Book book = validBook();
        book.getSections().get(0).getOptions().add(null);
        book.getSections().add(null);

        assertThat(validator.problemsWith(book)).containsExactly(
                "option 2 of section 1 is empty",
                "section 3 is empty");
    }

    // --- builders ---

    private static Book validBook() {
        return book(
                section(1, "BEGIN", option("Climb", 2, consequence("LOSE_HEALTH", "3"))),
                section(2, "END"));
    }

    private static Book book(Section... sections) {
        Book book = new Book();
        book.setTitle("The Lighthouse");
        book.setAuthor("Test Author");
        book.setDifficulty(Difficulty.EASY);
        book.setSections(new ArrayList<>(Arrays.asList(sections)));
        return book;
    }

    private static Section section(Integer id, String type, Option... options) {
        Section section = new Section();
        section.setId(id);
        section.setType(type);
        section.setText("Some text");
        section.setOptions(options.length == 0 ? null : new ArrayList<>(Arrays.asList(options)));
        return section;
    }

    private static Option option(String description, Integer gotoId, Consequence consequence) {
        Option option = new Option();
        option.setDescription(description);
        option.setGotoId(gotoId);
        option.setConsequence(consequence);
        return option;
    }

    private static Consequence consequence(String type, String value) {
        Consequence consequence = new Consequence();
        consequence.setType(type);
        consequence.setValue(value);
        return consequence;
    }
}
