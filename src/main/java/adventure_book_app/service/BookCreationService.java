package adventure_book_app.service;

import adventure_book_app.api.dto.ApiDtos.NewBook;
import adventure_book_app.mapper.BookMapper;
import adventure_book_app.model.book.Book;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a book from an API request: maps it with {@link BookMapper}, checks it with the same rules used when
 * loading the books folder, writes it to {@code <books folder>/<id>.json} and makes it available straight away.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookCreationService {

    private final BookMapper bookMapper;
    private final BookLoader bookLoader;
    private final JsonMapper jsonMapper;

    /** The book can't be played as sent; {@code problems} says why. */
    public static class InvalidBookException extends RuntimeException {
        private final List<String> problems;

        public InvalidBookException(List<String> problems) {
            super("Invalid book: " + String.join("; ", problems));
            this.problems = List.copyOf(problems);
        }

        public List<String> getProblems() {
            return problems;
        }
    }

    /** A book with the same title (or the same file name) already exists. */
    public static class DuplicateBookException extends RuntimeException {
        public DuplicateBookException(String message) {
            super(message);
        }
    }

    /**
     * Every reason the book can't be saved, without saving it: missing fields, the {@link BookLoader} rules, and
     * duplicates. Used by the "New book" page while the book is being written.
     *
     * @return the problems found; empty if {@link #create} would succeed
     */
    public List<String> problems(NewBook request) {
        return check(request).problems();
    }

    /**
     * @return the saved book, with its id set
     * @throws InvalidBookException   if anything reported by {@link #problems} is wrong with the book
     * @throws DuplicateBookException if the only problem is that a book with this title already exists
     */
    public synchronized Book create(NewBook request) {
        Check check = check(request);
        if (check.duplicate() && check.problems().size() == 1) {
            throw new DuplicateBookException(check.problems().getFirst());
        }
        if (!check.problems().isEmpty()) {
            throw new InvalidBookException(check.problems());
        }

        Book book = check.book();
        String id = BookLoader.slug(book.getTitle());
        book.setId(id);

        Path file = bookLoader.getBooksFolder().resolve(id + ".json");
        write(book, file);
        bookLoader.add(book);
        log.info("Created book '{}' in {}", book.getTitle(), file.toAbsolutePath());
        return book;
    }

    /**
     * @param book      the mapped book, or null if the request couldn't be mapped
     * @param duplicate whether one of the problems is that the book already exists
     */
    private record Check(Book book, List<String> problems, boolean duplicate) {
    }

    private Check check(NewBook request) {
        if (request == null) {
            return new Check(null, List.of("The request body is empty"), false);
        }
        List<String> problems = new ArrayList<>(missingFields(request));

        Book book = null;
        try {
            book = bookMapper.toBook(request);
        } catch (IllegalArgumentException e) { // unknown difficulty
            problems.add(e.getMessage());
        }
        // The structural rules need complete sections and options, so they run once the fields are filled in.
        if (book != null && problems.isEmpty()) {
            problems.addAll(bookLoader.problemsWith(book));
        }

        boolean duplicate = false;
        if (!isBlank(request.title())) {
            String title = request.title().trim();
            String id = BookLoader.slug(title);
            if (id.isEmpty()) {
                problems.add("the title must contain at least one letter or digit");
            } else if (bookLoader.findById(id).isPresent() || bookLoader.findByTitleIgnoreCase(title).isPresent()) {
                problems.add("a book called '" + title + "' already exists");
                duplicate = true;
            } else if (Files.exists(bookLoader.getBooksFolder().resolve(id + ".json"))) {
                problems.add("a file named '" + id + ".json' already exists in the books folder; choose another title");
                duplicate = true;
            }
        }
        return new Check(book, List.copyOf(problems), duplicate);
    }

    private void write(Book book, Path file) {
        try {
            Files.createDirectories(file.getParent());
            // CREATE_NEW: never overwrite a file, even one that failed to load (e.g. an empty or broken book).
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(out, book);
            }
        } catch (FileAlreadyExistsException e) {
            throw new DuplicateBookException("A book file named '" + file.getFileName() + "' already exists");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }

    /** Checks the request itself; structural rules (BEGIN/END, gotoIds...) are checked on the mapped book. */
    private static List<String> missingFields(NewBook request) {
        List<String> problems = new ArrayList<>();
        if (isBlank(request.title())) {
            problems.add("title is required");
        }
        if (isBlank(request.author())) {
            problems.add("author is required");
        }
        if (isBlank(request.difficulty())) {
            problems.add("difficulty is required (EASY, MEDIUM or HARD)");
        }
        if (request.sections() == null || request.sections().isEmpty()) {
            problems.add("at least one section is required");
            return problems;
        }
        for (int i = 0; i < request.sections().size(); i++) {
            var section = request.sections().get(i);
            if (section == null) {
                problems.add("section " + (i + 1) + " is empty");
                continue;
            }
            if (section.id() == null) {
                problems.add("section " + (i + 1) + " needs an id");
            }
            if (isBlank(section.text())) {
                problems.add("section " + (i + 1) + " needs text");
            }
            if (section.options() != null) {
                for (int j = 0; j < section.options().size(); j++) {
                    var option = section.options().get(j);
                    if (option == null || isBlank(option.description())) {
                        problems.add("option " + (j + 1) + " of section " + (i + 1) + " needs a description");
                    }
                }
            }
        }
        return problems;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
