package adventure_book_app.service;

import adventure_book_app.api.dto.ApiDtos.NewBook;
import adventure_book_app.mapper.BookMapper;
import adventure_book_app.model.book.Book;
import lombok.Getter;
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
 * Creates a book from an API request: maps it with {@link BookMapper}, checks it with {@link BookValidator} (the same
 * rules used when loading the books folder), writes it to {@code <books folder>/<id>.json} and makes it available straight away.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookCreationService {

    private final BookMapper bookMapper;
    private final BookLoader bookLoader;
    private final BookValidator bookValidator;
    private final JsonMapper jsonMapper;

    /** The book can't be played as sent; {@code problems} says why. */
    @Getter
    public static class
    InvalidBookException extends RuntimeException {
        private final List<String> problems;

        public InvalidBookException(List<String> problems) {
            super("Invalid book: " + String.join("; ", problems));
            this.problems = List.copyOf(problems);
        }

    }

    /**
     * Every reason the book can't be saved, without saving it: the {@link BookValidator} rules, and duplicates. Used by the "New book" page while the book is being written.
     *
     * @return the problems found; empty if {@link #create} would succeed
     */
    public List<String> problems(NewBook request) {
        if (request == null) {
            return List.of("The request body is empty");
        }
        List<String> problems = new ArrayList<>();
        try {
            problems.addAll(bookValidator.problemsWith(bookMapper.toBook(request)));
        } catch (IllegalArgumentException e) { // unknown difficulty: the request can't be mapped to a Book
            problems.add(e.getMessage());
        }

        if (!isBlank(request.title())) {
            String title = request.title().trim();
            String id = BookLoader.slug(title);
            if (id.isEmpty()) {
                problems.add("the title must contain at least one letter or digit");
            } else if (bookLoader.findById(id).isPresent() || bookLoader.findByTitleIgnoreCase(title).isPresent()) {
                problems.add("a book called '" + title + "' already exists");
            } else if (Files.exists(bookLoader.getBooksFolder().resolve(id + ".json"))) {
                problems.add("a file named '" + id + ".json' already exists in the books folder; choose another title");
            }
        }
        return List.copyOf(problems);
    }

    /**
     * @return the saved book, with its id set
     * @throws InvalidBookException if {@link #problems} finds anything wrong, including a duplicate title
     */
    public synchronized Book create(NewBook request) {
        List<String> problems = problems(request);
        if (!problems.isEmpty()) {
            throw new InvalidBookException(problems);
        }

        Book book = bookMapper.toBook(request);
        String id = BookLoader.slug(book.getTitle());
        book.setId(id);

        Path file = bookLoader.getBooksFolder().resolve(id + ".json");
        write(book, file);
        bookLoader.add(book);
        log.info("Created book '{}' in {}", book.getTitle(), file.toAbsolutePath());
        return book;
    }

    private void write(Book book, Path file) {
        try {
            Files.createDirectories(file.getParent());
            // CREATE_NEW: never overwrite a file, even one that failed to load (e.g. an empty or broken book).
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(out, book);
            }
        } catch (FileAlreadyExistsException e) {
            throw new InvalidBookException(List.of("a file named '" + file.getFileName() + "' already exists in the books folder"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
