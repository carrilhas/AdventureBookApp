package adventure_book_app.api;

import adventure_book_app.api.dto.ApiDtos.ApiError;
import adventure_book_app.api.dto.ApiDtos.PlayerView;
import adventure_book_app.api.dto.ApiDtos.RegisterRequest;
import adventure_book_app.model.player.Player;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerApiController {

    private final PlayerService playerService;

    /** Registers (or signs back in) a player by name and sets the player cookie. */
    @PostMapping
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, ServerWebExchange exchange) {
        Player player;
        try {
            player = playerService.register(request == null ? null : request.name());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
        exchange.getResponse().addCookie(PlayerCookie.forPlayer(player));
        return ResponseEntity.ok(new PlayerView(player.getName()));
    }

    /** The player using this browser, or 401 if nobody has registered. */
    @GetMapping("/me")
    public ResponseEntity<?> me(@CookieValue(name = PlayerCookie.NAME, required = false) String playerId) {
        return playerService.findById(playerId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(new PlayerView(p.getName())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("Not registered")));
    }
}
