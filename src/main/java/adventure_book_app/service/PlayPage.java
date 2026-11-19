package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import adventure_book_app.model.player.BookProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static adventure_book_app.service.HtmlLayout.escape;

/** Generates the page for the player's current section of a book. */
@Service
@RequiredArgsConstructor
public class PlayPage {

    private final GameService gameService;

    /**
     * @param message one-time consequence text from the last choice (may be null)
     * @param error   error to show, e.g. an invalid choice (may be null)
     */
    public String generateHtml(Book book, BookProgress progress, String message, String error) {
        Section section = gameService.findSection(book, progress.getSectionId()).orElseThrow();
        String title = escape(book.getTitle());
        StringBuilder body = new StringBuilder();

        body.append("  <p><a href=\"/\">&larr; All books</a></p>\n")
                .append("  <h1>").append(title).append("</h1>\n")
                .append("  <p class=\"meta\">Health: <strong>").append(Math.max(0, progress.getHealth()))
                .append("</strong> / ").append(GameService.STARTING_HEALTH).append("</p>\n");

        if (error != null) {
            body.append("  <p class=\"error\">").append(escape(error)).append("</p>\n");
        }
        if (message != null) {
            body.append("  <p class=\"consequence\">").append(escape(message)).append("</p>\n");
        }

        body.append("  <p class=\"story\">").append(escape(section.getText())).append("</p>\n");

        if (gameService.isDead(progress)) {
            body.append("  <h2>You have died.</h2>\n").append(restartForm(title, "Try again"));
        } else if (gameService.isFinished(book, progress)) {
            body.append("  <h2>The End</h2>\n").append(restartForm(title, "Play again"));
        } else {
            List<Option> options = section.getOptions() == null ? List.of() : section.getOptions();
            if (options.isEmpty()) {
                body.append("  <p class=\"error\">This section has no way forward.</p>\n").append(restartForm(title, "Start over"));
            } else {
                body.append("  <form method=\"post\" action=\"/play\" class=\"options\">\n")
                        .append("    <input type=\"hidden\" name=\"book\" value=\"").append(title).append("\">\n")
                        .append("    <input type=\"hidden\" name=\"from\" value=\"").append(section.getId()).append("\">\n");
                for (int i = 0; i < options.size(); i++) {
                    body.append("    <button type=\"submit\" name=\"option\" value=\"").append(i).append("\">")
                            .append(escape(options.get(i).getDescription())).append("</button>\n");
                }
                body.append("  </form>\n");
            }
        }

        return HtmlLayout.page(book.getTitle(), body.toString());
    }

    private static String restartForm(String escapedTitle, String label) {
        return "  <form method=\"post\" action=\"/play/restart\">\n"
                + "    <input type=\"hidden\" name=\"book\" value=\"" + escapedTitle + "\">\n"
                + "    <button type=\"submit\">" + label + "</button>\n"
                + "  </form>\n";
    }
}
