package adventure_book_app.api;

import adventure_book_app.api.dto.ApiDtos.BookSummary;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.player.Player;
import adventure_book_app.service.BookLoader;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookApiController {

    private final BookLoader bookLoader;
    private final PlayerService playerService;

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
