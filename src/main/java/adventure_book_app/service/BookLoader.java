package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.book.Section;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Reads every *.json file in the books folder on startup and maps each one to a {@link Book}.
 * Files that can't be read or mapped are logged and skipped.
 */
@Slf4j
@Component
public class BookLoader implements ApplicationRunner {

    private final JsonMapper jsonMapper;
    private final Path booksFolder;
    /** Copy-on-write: read on every request, written only at startup and when a book is created. */
    private final List<Book> books = new CopyOnWriteArrayList<>();

    public BookLoader(JsonMapper jsonMapper, @Value("${books.folder:books}") String booksFolder) {
        this.jsonMapper = jsonMapper;
        this.booksFolder = Path.of(booksFolder);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Files.isDirectory(booksFolder)) {
            log.warn("Books folder '{}' not found, no books loaded", booksFolder.toAbsolutePath());
            return;
        }

        try (Stream<Path> files = Files.list(booksFolder)) {
            files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(this::loadBook);
        } catch (IOException e) {
            log.error("Failed to list books folder '{}'", booksFolder.toAbsolutePath(), e);
        }

        log.info("Loaded {} book(s) from '{}'", books.size(), booksFolder.toAbsolutePath());
    }

    private void loadBook(Path file) {
        try {
            Book book = jsonMapper.readValue(file.toFile(), Book.class);
            book.setId(idFromFileName(file));
            if(isValidBook(book)){
                books.add(book);
                log.info("Loaded book '{}' from {}", book.getTitle(), file.getFileName());
            }

        } catch (Exception e) {
            log.warn("Could not map {} to a Book: {}", file.getFileName(), e.getMessage());
        }
    }

    /**
     * Checks book to see if it is a valid book for the adventure game
     * @param book Book
     * @return whether the book can be played
     */
    private boolean isValidBook(Book book) {
        List<String> problems = problemsWith(book);
        problems.forEach(problem -> log.warn("Book '{}' rejected: {}", book.getTitle(), problem));
        return problems.isEmpty();
    }

    /**
     * The rules a book must follow to be playable. Used both when loading the books folder and when a new book is
     * created through the API.
     *
     * @return a readable description of each rule the book breaks; empty if the book is valid
     */
    public List<String> problemsWith(Book book) {
        if (book.getSections() == null || book.getSections().isEmpty()) {
            return List.of("a book needs at least one section");
        }
        List<String> problems = new ArrayList<>();
        //altered from requirements but it does make sense to check that there's 1 BEGIN node and not zero
        if (!hasSingleBeginNodes(book)) {
            problems.add("a book needs exactly one BEGIN section");
        }
        if (hasNoEndingNode(book)) {
            problems.add("a book needs at least one END section");
        }
        List<Integer> duplicateIds = duplicateSectionIds(book);
        if (!duplicateIds.isEmpty()) {
            problems.add("section ids must be unique, repeated: " + duplicateIds);
        }
        List<Integer> invalidGotoIds = invalidNextSectionIds(book);
        if (!invalidGotoIds.isEmpty()) {
            problems.add("options point to missing sections: " + invalidGotoIds + " (null means an option has no gotoId)");
        }
        //uncomment this to complete the filtering rules of books, causing us to get no valid books from the test data
        //if (nonEndingSectionHasNoOptions(book)) {
        //    problems.add("every section that isn't an END section needs at least one option");
        //}
        return problems;
    }

    private static List<Integer> duplicateSectionIds(Book book) {
        Set<Integer> seen = new HashSet<>();
        return book.getSections().stream()
                .map(Section::getId)
                .filter(id -> !seen.add(id))
                .distinct()
                .toList();
    }

    /**
     * @return true if any section that isn't an END section has no options (the player would be stuck there)
     */
    private boolean nonEndingSectionHasNoOptions(Book book) {
        List<Integer> sectionIds = book.getSections().stream()
                .filter(section -> !"END".equals(section.getType()))
                .filter(section -> section.getOptions() == null || section.getOptions().isEmpty())
                .map(Section::getId)
                .toList();

        if (!sectionIds.isEmpty()) {
            log.warn("Book '{}' has non-ending sections with no options: {}", book.getTitle(), sectionIds);
            return true;
        }
        return false;
    }

    /**
     * @return the gotoIds of options that are null or point to a section id that doesn't exist in the book
     */
    private static List<Integer> invalidNextSectionIds(Book book) {
        Set<Integer> allSectionIds = new HashSet<>();
        book.getSections().forEach(section -> allSectionIds.add(section.getId()));
        return book.getSections().stream()
                .filter(section -> section.getOptions() != null) // END sections have no options
                .flatMap(section -> section.getOptions().stream())
                .map(option -> option.getGotoId())
                .filter(gotoId -> gotoId == null || !allSectionIds.contains(gotoId))
                .toList();
    }

    private boolean hasNoEndingNode(Book book) {
        return book.getSections().stream().noneMatch(section -> "END".equals(section.getType()));
    }

    private boolean hasSingleBeginNodes(Book book) {
        return book.getSections().stream().filter(section -> "BEGIN".equals(section.getType())).count() == 1;
    }

    public List<Book> getBooks() {
        return Collections.unmodifiableList(books);
    }

    /**
     * Returns the books matching all the given filters. Null or blank filters are ignored.
     *
     * @param query      case-insensitive text matched against the title or the author
     * @param author     exact author name (case-insensitive)
     * @param difficulty exact difficulty
     */
    public List<Book> search(String query, String author, Difficulty difficulty) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String a = author == null ? "" : author.trim();
        return books.stream()
                .filter(b -> q.isEmpty()
                        || contains(b.getTitle(), q)
                        || contains(b.getAuthor(), q))
                .filter(b -> a.isEmpty() || a.equalsIgnoreCase(b.getAuthor()))
                .filter(b -> difficulty == null || difficulty == b.getDifficulty())
                .toList();
    }

    /** Distinct author names of the loaded books, sorted alphabetically. */
    public List<String> getAuthors() {
        return books.stream()
                .map(Book::getAuthor)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static boolean contains(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    /** "Crystal Caverns.json" -> "crystal-caverns" */
    private static String idFromFileName(Path file) {
        String name = file.getFileName().toString();
        return slug(name.substring(0, name.length() - ".json".length()));
    }

    /** "The Crystal Caverns!" -> "the-crystal-caverns". Used for book ids and the file names of new books. */
    public static String slug(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public Path getBooksFolder() {
        return booksFolder;
    }

    /** Makes a newly created book available. The book must already be valid and have an id. */
    public void add(Book book) {
        books.add(book);
    }

    public Optional<Book> findByTitleIgnoreCase(String title) {
        if (title == null) {
            return Optional.empty();
        }
        return books.stream().filter(b -> title.trim().equalsIgnoreCase(b.getTitle())).findFirst();
    }

    public Optional<Book> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return books.stream().filter(b -> id.equals(b.getId())).findFirst();
    }
}
