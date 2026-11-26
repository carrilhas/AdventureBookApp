package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Difficulty;
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
 * Files that can't be read or mapped, or that break a {@link BookValidator} rule, are logged and skipped.
 */
@Slf4j
@Component
public class BookLoader implements ApplicationRunner {

    private final JsonMapper jsonMapper;
    private final BookValidator bookValidator;
    private final Path booksFolder;
    /** Copy-on-write: read on every request, written only at startup and when a book is created. */
    private final List<Book> books = new CopyOnWriteArrayList<>();

    public BookLoader(JsonMapper jsonMapper, BookValidator bookValidator,
                      @Value("${books.folder:books}") String booksFolder) {
        this.jsonMapper = jsonMapper;
        this.bookValidator = bookValidator;
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
     * Checks book to see if it is a valid book for the adventure game, logging each rule it breaks
     * @param book Book
     * @return whether the book can be played
     */
    private boolean isValidBook(Book book) {
        List<String> problems = bookValidator.problemsWith(book);
        problems.forEach(problem -> log.warn("Book '{}' rejected: {}", book.getTitle(), problem));
        return problems.isEmpty();
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
