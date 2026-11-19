package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final List<Book> books = new ArrayList<>();

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
            books.add(book);
            log.info("Loaded book '{}' from {}", book.getTitle(), file.getFileName());
        } catch (Exception e) {
            log.warn("Could not map {} to a Book: {}", file.getFileName(), e.getMessage());
        }
    }

    public List<Book> getBooks() {
        return Collections.unmodifiableList(books);
    }
}
