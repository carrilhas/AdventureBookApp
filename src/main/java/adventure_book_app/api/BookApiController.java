package adventure_book_app.api;

import adventure_book_app.api.dto.ApiDtos.BookValidation;
import adventure_book_app.api.dto.ApiDtos.BookSummary;
import adventure_book_app.api.dto.ApiDtos.NewBook;
import adventure_book_app.model.book.Book;
import adventure_book_app.service.BookCreationService;
import adventure_book_app.service.BookCreationService.InvalidBookException;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.player.Player;
import adventure_book_app.service.BookLoader;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookApiController {

    private final BookLoader bookLoader;
    private final PlayerService playerService;
    private final BookCreationService bookCreationService;

    /** Lists books, optionally filtered. An unknown difficulty is ignored, like on the HTML page. */
    @GetMapping("/books")
    public List<BookSummary> books(@RequestParam(name = "q", required = false) String query,
                                   @RequestParam(name = "author", required = false) String author,
                                   @RequestParam(name = "difficulty", required = false) String difficulty,
                                   @CookieValue(name = PlayerCookie.NAME, required = false) String playerId) {
        Player player = playerService.findById(playerId).orElse(null);
        return bookLoader.search(query, author, parseDifficulty(difficulty)).stream()
                .map(b -> new BookSummary(b.getId(), b.getTitle(), b.getAuthor(), b.getDifficulty(),
                        player != null && player.getProgress().containsKey(b.getTitle())))
                .toList();
    }

    /**
     * Checks a new book with the same rules {@link #create} uses, without saving it. Always 200: the body says whether
     * the book is valid and, if not, why. The "New book" page calls this while the book is being written.
     */
    @PostMapping("/books/validate")
    public BookValidation validate(@RequestBody(required = false) NewBook request) {
        return BookValidation.of(bookCreationService.problems(request));
    }

    /**
     * Creates a book and saves it to the books folder as {@code <id>.json}, where the id comes from the title.
     * The body has the same shape as the book files. Returns 201 with the new book, or 400 with a
     * {@link BookValidation} listing the problems (including a title that's already taken).
     */
    @PostMapping("/books")
    public ResponseEntity<?> create(@RequestBody(required = false) NewBook request) {
        try {
            Book book = bookCreationService.create(request);
            return ResponseEntity.created(URI.create("/api/books/" + book.getId() + "/play"))
                    .body(new BookSummary(book.getId(), book.getTitle(), book.getAuthor(), book.getDifficulty(), false));
        } catch (InvalidBookException e) {
            return ResponseEntity.badRequest().body(BookValidation.of(e.getProblems()));
        }
    }

    @GetMapping("/authors")
    public List<String> authors() {
        return bookLoader.getAuthors();
    }

    private static Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Difficulty.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
