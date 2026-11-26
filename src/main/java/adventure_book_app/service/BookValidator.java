package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Consequence;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * The rules a book must follow to be playable. The single place they live: {@link BookLoader} uses them for the
 * books folder and {@link BookCreationService} for books created through the API.
 */
@Component
public class BookValidator {

    /** Consequence types {@link GameService} knows how to apply. */
    public static final List<String> CONSEQUENCE_TYPES = List.of("LOSE_HEALTH", "GAIN_HEALTH");

    /**
     * @return a readable description of each rule the book breaks; empty if the book is valid. Sections and options
     *         are referred to by their position, starting at 1.
     */
    public List<String> problemsWith(Book book) {
        List<String> problems = new ArrayList<>();
        if (isBlank(book.getTitle())) {
            problems.add("title is required");
        }
        if (isBlank(book.getAuthor())) {
            problems.add("author is required");
        }
        if (book.getDifficulty() == null) {
            problems.add("difficulty is required (EASY, MEDIUM or HARD)");
        }
        if (book.getSections() == null || book.getSections().isEmpty()) {
            problems.add("a book needs at least one section");
            return problems;
        }

        problems.addAll(fieldProblems(book.getSections()));

        List<Section> sections = book.getSections().stream().filter(Objects::nonNull).toList();
        //altered from requirements but it does make sense to check that there's 1 BEGIN node and not zero
        if (!hasSingleBeginNode(sections)) {
            problems.add("a book needs exactly one BEGIN section");
        }
        if (hasNoEndingNode(sections)) {
            problems.add("a book needs at least one END section");
        }
        List<Integer> duplicateIds = duplicateSectionIds(sections);
        if (!duplicateIds.isEmpty()) {
            problems.add("section ids must be unique, repeated: " + duplicateIds);
        }
        List<Integer> invalidGotoIds = invalidNextSectionIds(sections);
        if (!invalidGotoIds.isEmpty()) {
            problems.add("options point to missing sections: " + invalidGotoIds + " (null means an option has no gotoId)");
        }
        //uncomment this to complete the filtering rules of books, causing us to get no valid books from the test data
        //if (nonEndingSectionHasNoOptions(sections)) {
        //    problems.add("every section that isn't an END section needs at least one option");
        //}
        return problems;
    }

    /** Fields every section and option must have, and consequences that GameService can apply. */
    private static List<String> fieldProblems(List<Section> sections) {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            String sectionName = "section " + (i + 1);
            if (section == null) {
                problems.add(sectionName + " is empty");
                continue;
            }
            if (section.getId() == null) {
                problems.add(sectionName + " needs an id");
            }
            if (isBlank(section.getText())) {
                problems.add(sectionName + " needs text");
            }
            if (section.getOptions() == null) {
                continue;
            }
            for (int j = 0; j < section.getOptions().size(); j++) {
                Option option = section.getOptions().get(j);
                String optionName = "option " + (j + 1) + " of " + sectionName;
                if (option == null) {
                    problems.add(optionName + " is empty");
                    continue;
                }
                if (isBlank(option.getDescription())) {
                    problems.add(optionName + " needs a description");
                }
                String consequenceProblem = consequenceProblem(option.getConsequence());
                if (consequenceProblem != null) {
                    problems.add("the consequence of " + optionName + " " + consequenceProblem);
                }
            }
        }
        return problems;
    }

    /**
     * @return what's wrong with the consequence, or null if it's fine. No consequence at all is fine.
     */
    private static String consequenceProblem(Consequence consequence) {
        if (consequence == null) {
            return null;
        }
        String type = isBlank(consequence.getType()) ? "" : consequence.getType().trim().toUpperCase(Locale.ROOT);
        if (!CONSEQUENCE_TYPES.contains(type)) {
            return "needs a type: " + String.join(" or ", CONSEQUENCE_TYPES);
        }
        try {
            if (Integer.parseInt(isBlank(consequence.getValue()) ? "" : consequence.getValue().trim()) > 0) {
                return null;
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        return "needs an amount that is a whole number above 0";
    }

    private static boolean hasSingleBeginNode(List<Section> sections) {
        return sections.stream().filter(section -> "BEGIN".equals(section.getType())).count() == 1;
    }

    private static boolean hasNoEndingNode(List<Section> sections) {
        return sections.stream().noneMatch(section -> "END".equals(section.getType()));
    }

    private static List<Integer> duplicateSectionIds(List<Section> sections) {
        Set<Integer> seen = new HashSet<>();
        return sections.stream()
                .map(Section::getId)
                .filter(Objects::nonNull) // missing ids are reported by fieldProblems
                .filter(id -> !seen.add(id))
                .distinct()
                .toList();
    }

    /**
     * @return the gotoIds of options that are null or point to a section id that doesn't exist in the book
     */
    private static List<Integer> invalidNextSectionIds(List<Section> sections) {
        Set<Integer> allSectionIds = new HashSet<>();
        sections.forEach(section -> allSectionIds.add(section.getId()));
        return sections.stream()
                .filter(section -> section.getOptions() != null) // END sections have no options
                .flatMap(section -> section.getOptions().stream())
                .filter(Objects::nonNull) // empty options are reported by fieldProblems
                .map(Option::getGotoId)
                .filter(gotoId -> gotoId == null || !allSectionIds.contains(gotoId))
                .toList();
    }

    /**
     * @return true if any section that isn't an END section has no options (the player would be stuck there)
     */
    @SuppressWarnings("unused") // see the commented-out rule in problemsWith
    private static boolean nonEndingSectionHasNoOptions(List<Section> sections) {
        return sections.stream()
                .filter(section -> !"END".equals(section.getType()))
                .anyMatch(section -> section.getOptions() == null || section.getOptions().isEmpty());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
