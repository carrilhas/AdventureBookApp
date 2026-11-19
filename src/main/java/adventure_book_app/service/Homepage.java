package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static adventure_book_app.service.HtmlLayout.escape;

/**
 * Generates the homepage HTML listing every book loaded by {@link BookLoader}.
 */
@Service
@RequiredArgsConstructor
public class Homepage {

    private final BookLoader bookLoader;

    /**
     * @param player the current player, or null if nobody has registered in this browser
     */
    public String generateHtml(Player player) {
        List<Book> books = bookLoader.getBooks();
        StringBuilder body = new StringBuilder("  <h1>Choose your adventure</h1>\n");

        if (player == null) {
            body.append("  <p><a href=\"/register\">Register your name</a> to save your progress.</p>\n");
        } else {
            body.append("  <p>Playing as <strong>").append(escape(player.getName()))
                    .append("</strong> &middot; <a href=\"/register\">change</a></p>\n");
        }

        if (books.isEmpty()) {
            body.append("  <p>No books available.</p>\n");
        } else {
            body.append("  <ul>\n");
            for (Book book : books) {
                Integer savedSection = player == null ? null : player.getProgress().get(book.getTitle());
                body.append("    <li>\n")
                        .append("      <h2>").append(escape(book.getTitle())).append("</h2>\n")
                        .append("      <p class=\"meta\">by ").append(escape(book.getAuthor()))
                        .append(" &middot; ").append(book.getDifficulty() == null ? "Unknown" : book.getDifficulty().name())
                        .append("</p>\n")
                        .append("      <a href=\"/play?book=").append(escape(urlEncode(book.getTitle())))
                        .append("\">").append(savedSection == null ? "Play" : "Continue").append("</a>\n")
                        .append("    </li>\n");
            }
            body.append("  </ul>\n");
        }

        return HtmlLayout.page("Adventure Books", body.toString());
    }

    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
