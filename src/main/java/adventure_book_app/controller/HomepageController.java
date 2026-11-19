package adventure_book_app.controller;

import adventure_book_app.service.Homepage;
import adventure_book_app.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomepageController {

    private final Homepage homepage;
    private final PlayerService playerService;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String homepage(@CookieValue(name = RegisterController.PLAYER_COOKIE, required = false) String playerId,
                           @RequestParam(name = "q", required = false) String query,
                           @RequestParam(name = "author", required = false) String author,
                           @RequestParam(name = "difficulty", required = false) String difficulty) {
        return homepage.generateHtml(playerService.findById(playerId).orElse(null), query, author, difficulty);
    }
}
