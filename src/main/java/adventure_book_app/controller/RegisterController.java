package adventure_book_app.controller;

import adventure_book_app.model.player.Player;
import adventure_book_app.service.PlayerService;
import adventure_book_app.service.RegisterPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class RegisterController {

    /** Cookie holding the current player's id. */
    public static final String PLAYER_COOKIE = "player";

    private final PlayerService playerService;
    private final RegisterPage registerPage;

    @GetMapping(value = "/register", produces = MediaType.TEXT_HTML_VALUE)
    public String registerForm(@RequestParam(name = "next", required = false) String next) {
        return registerPage.generateHtml(null, null, safeNext(next));
    }

    /** Only allow redirects to local paths, so the "next" parameter can't send players to another site. */
    private static String safeNext(String next) {
        if (next == null || !next.startsWith("/") || next.startsWith("//") || next.contains("\\")) {
            return "/";
        }
        return next;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<String>> register(ServerWebExchange exchange) {
        return exchange.getFormData().map(form -> {
            String name = form.getFirst("name");
            String next = safeNext(form.getFirst("next"));
            Player player;
            try {
                player = playerService.register(name);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .body(registerPage.generateHtml(name, e.getMessage(), next));
            }

            ResponseCookie cookie = ResponseCookie.from(PLAYER_COOKIE, PlayerService.idFor(player.getName()))
                    .path("/")
                    .httpOnly(true)
                    .sameSite("Lax")
                    .maxAge(Duration.ofDays(365))
                    .build();
            exchange.getResponse().addCookie(cookie);
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(UriComponentsBuilder.fromUriString(next).build().encode().toUri()).<String>build();
        });
    }
}
