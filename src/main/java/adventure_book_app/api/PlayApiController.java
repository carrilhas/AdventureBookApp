package adventure_book_app.api;

import adventure_book_app.api.dto.ApiDtos.ApiError;
import adventure_book_app.api.dto.ApiDtos.ChoiceRequest;
import adventure_book_app.api.dto.ApiDtos.OptionView;
import adventure_book_app.api.dto.ApiDtos.PlayState;
import adventure_book_app.api.dto.ApiDtos.PlayStatus;
import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import adventure_book_app.model.player.BookProgress;
import adventure_book_app.model.player.Player;
import adventure_book_app.service.BookLoader;
import adventure_book_app.service.GameService;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Playing a book. All game rules stay on the server (in {@link GameService}); the frontend only ever sees the
 * current section and the descriptions of its options.
 */
@RestController
@RequestMapping("/api/books/{bookId}/play")
@RequiredArgsConstructor
public class PlayApiController {

    private final BookLoader bookLoader;
    private final PlayerService playerService;
    private final GameService gameService;

    /** The current state, starting the book if needed. The one-time message is returned once, then cleared. */
    @GetMapping
    public ResponseEntity<?> current(@PathVariable String bookId,
                                     @CookieValue(name = PlayerCookie.NAME, required = false) String playerId) {
        return withPlayerAndBook(playerId, bookId, (player, book) -> {
            BookProgress progress = gameService.progressFor(player, book);
            PlayState state = toState(book, progress);
            gameService.clearMessage(player, progress);
            return ResponseEntity.ok(state);
        });
    }

    /**
     * Makes a choice. If it ends the game, the returned state is the ending (status DEAD or FINISHED) and the
     * saved progress has already been reset, so a later GET starts the book again.
     * A stale or invalid choice returns 400 with the player's actual current state.
     */
    @PostMapping("/choice")
    public ResponseEntity<?> choose(@PathVariable String bookId,
                                    @RequestBody ChoiceRequest request,
                                    @CookieValue(name = PlayerCookie.NAME, required = false) String playerId) {
        return withPlayerAndBook(playerId, bookId, (player, book) -> {
            if (request == null || request.fromSectionId() == null || request.optionIndex() == null) {
                return ResponseEntity.badRequest().body(new ApiError("fromSectionId and optionIndex are required",
                        toState(book, gameService.progressFor(player, book))));
            }
            try {
                BookProgress result = gameService.choose(player, book, request.fromSectionId(), request.optionIndex());
                PlayState state = toState(book, result);
                if (!gameService.isOver(book, result)) {
                    gameService.clearMessage(player, result); // already delivered in this response
                }
                return ResponseEntity.ok(state);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ApiError(e.getMessage(),
                        toState(book, gameService.progressFor(player, book))));
            }
        });
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restart(@PathVariable String bookId,
                                     @CookieValue(name = PlayerCookie.NAME, required = false) String playerId) {
        return withPlayerAndBook(playerId, bookId,
                (player, book) -> ResponseEntity.ok(toState(book, gameService.restart(player, book))));
    }

    private ResponseEntity<?> withPlayerAndBook(String playerId, String bookId,
                                                BiFunction<Player, Book, ResponseEntity<?>> action) {
        Optional<Player> player = playerService.findById(playerId);
        if (player.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("Not registered"));
        }
        Optional<Book> book = bookLoader.findById(bookId);
        if (book.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Book not found"));
        }
        return action.apply(player.get(), book.get());
    }

    private PlayState toState(Book book, BookProgress progress) {
        Section section = gameService.findSection(book, progress.getSectionId()).orElseThrow();
        PlayStatus status = gameService.isDead(progress) ? PlayStatus.DEAD
                : gameService.isFinished(book, progress) ? PlayStatus.FINISHED
                : PlayStatus.PLAYING;

        List<OptionView> options = new ArrayList<>();
        if (status == PlayStatus.PLAYING && section.getOptions() != null) {
            List<Option> sectionOptions = section.getOptions();
            for (int i = 0; i < sectionOptions.size(); i++) {
                options.add(new OptionView(i, sectionOptions.get(i).getDescription()));
            }
        }

        return new PlayState(book.getId(), book.getTitle(), section.getId(), section.getText(), options,
                Math.max(0, progress.getHealth()), GameService.STARTING_HEALTH, progress.getLastMessage(), status);
    }
}
