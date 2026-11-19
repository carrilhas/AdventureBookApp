package adventure_book_app.controller;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.player.BookProgress;
import adventure_book_app.model.player.Player;
import adventure_book_app.service.BookLoader;
import adventure_book_app.service.GameService;
import adventure_book_app.service.HtmlLayout;
import adventure_book_app.service.PlayPage;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PlayController {

    private final BookLoader bookLoader;
    private final PlayerService playerService;
    private final GameService gameService;
    private final PlayPage playPage;

    @GetMapping(value = "/play", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> play(@RequestParam(name = "book", required = false) String title,
                                       @CookieValue(name = RegisterController.PLAYER_COOKIE, required = false) String playerId) {
        Optional<Player> player = playerService.findById(playerId);
        if (player.isEmpty()) {
            return redirect(UriComponentsBuilder.fromPath("/register").queryParam("next", playUrl(title)).build().encode().toUri());
        }
        Optional<Book> book = bookLoader.findByTitle(title);
        if (book.isEmpty()) {
            return bookNotFound();
        }

        BookProgress progress = gameService.progressFor(player.get(), book.get());
        String message = progress.getLastMessage();
        gameService.clearMessage(player.get(), progress);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body(playPage.generateHtml(book.get(), progress, message, null));
    }

    @PostMapping(value = "/play", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<String>> choose(ServerWebExchange exchange,
                                               @CookieValue(name = RegisterController.PLAYER_COOKIE, required = false) String playerId) {
        return exchange.getFormData().map(form -> {
            String title = form.getFirst("book");
            Optional<Player> player = playerService.findById(playerId);
            Optional<Book> book = bookLoader.findByTitle(title);
            if (player.isEmpty()) {
                return redirect(URI.create("/register"));
            }
            if (book.isEmpty()) {
                return bookNotFound();
            }
            BookProgress result;
            try {
                result = gameService.choose(player.get(), book.get(),
                        Integer.parseInt(String.valueOf(form.getFirst("from"))),
                        Integer.parseInt(String.valueOf(form.getFirst("option"))));
            } catch (IllegalArgumentException e) { // includes NumberFormatException
                BookProgress progress = gameService.progressFor(player.get(), book.get());
                return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)
                        .body(playPage.generateHtml(book.get(), progress, null, e.getMessage()));
            }
            if (gameService.isOver(book.get(), result)) {
                // Progress for this book has been reset, so show the ending directly instead of redirecting
                // (a GET /play would now start the book again).
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                        .body(playPage.generateHtml(book.get(), result, result.getLastMessage(), null));
            }
            return redirect(playUri(title));
        });
    }

    @PostMapping(value = "/play/restart", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<String>> restart(ServerWebExchange exchange,
                                                @CookieValue(name = RegisterController.PLAYER_COOKIE, required = false) String playerId) {
        return exchange.getFormData().map(form -> {
            String title = form.getFirst("book");
            Optional<Player> player = playerService.findById(playerId);
            Optional<Book> book = bookLoader.findByTitle(title);
            if (player.isEmpty()) {
                return redirect(URI.create("/register"));
            }
            if (book.isEmpty()) {
                return bookNotFound();
            }
            gameService.restart(player.get(), book.get());
            return redirect(playUri(title));
        });
    }

    private static String playUrl(String title) {
        return title == null ? "/" : "/play?book=" + title;
    }

    private static URI playUri(String title) {
        return UriComponentsBuilder.fromPath("/play").queryParam("book", title).build().encode().toUri();
    }

    private static ResponseEntity<String> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(location).build();
    }

    private static ResponseEntity<String> bookNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                .body(HtmlLayout.page("Book not found",
                        "  <h1>Book not found</h1>\n  <p><a href=\"/\">Back to books</a></p>\n"));
    }
}
