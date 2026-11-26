package adventure_book_app.api;

import adventure_book_app.model.player.Player;
import adventure_book_app.service.PlayerService;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/** The cookie that remembers which player is using this browser. */
public final class PlayerCookie {

    public static final String NAME = "player";

    private PlayerCookie() {
    }

    public static ResponseCookie forPlayer(Player player) {
        return ResponseCookie.from(NAME, PlayerService.idFor(player.getName()))
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(365))
                .build();
    }
}
