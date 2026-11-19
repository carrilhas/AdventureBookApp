package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Difficulty;
import adventure_book_app.model.player.BookProgress;
import adventure_book_app.model.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static adventure_book_app.service.HtmlLayout.escape;

/**
 * Generates the homepage HTML listing the books loaded by {@link BookLoader}, with search and filters.
 */
@Service
@RequiredArgsConstructor
public class Homepage {

    private final BookLoader bookLoader;

    /**
     * @param player     the current player, or null if nobody has registered in this browser
     * @param query      text to search for in titles and authors (may be null)
     * @param author     author to filter by (may be null)
     * @param difficulty difficulty to filter by (may be null or invalid, in which case it's ignored)
     */
    public String generateHtml(Player player, String query, String author, String difficulty) {
        Difficulty difficultyFilter = parseDifficulty(difficulty);
        boolean filtering = notBlank(query) || notBlank(author) || difficultyFilter != null;
        List<Book> books = bookLoader.search(query, author, difficultyFilter);
        StringBuilder body = new StringBuilder("  <h1>Choose your adventure</h1>\n");

        if (player == null) {
            body.append("  <p><a href=\"/register\">Register your name</a> to save your progress.</p>\n");
        } else {
            body.append("  <p>Playing as <strong>").append(escape(player.getName()))
                    .append("</strong> &middot; <a href=\"/register\">change</a></p>\n");
        }

        if (!bookLoader.getBooks().isEmpty()) {
            body.append(filterForm(query, author, difficultyFilter, filtering));
        }

        if (bookLoader.getBooks().isEmpty()) {
            body.append("  <p>No books available.</p>\n");
        } else if (books.isEmpty()) {
            body.append("  <p>No books match your search.</p>\n");
        } else {
            body.append("  <ul>\n");
            for (Book book : books) {
                BookProgress savedSection = player == null ? null : player.getProgress().get(book.getTitle());
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

    private String filterForm(String query, String author, Difficulty difficulty, boolean filtering) {
        StringBuilder form = new StringBuilder()
                .append("  <form method=\"get\" action=\"/\" class=\"filters\">\n")
                .append("    <input type=\"search\" name=\"q\" placeholder=\"Search title or author\" aria-label=\"Search\" value=\"")
                .append(escape(query)).append("\">\n")
                .append("    <select name=\"author\" aria-label=\"Author\">\n")
                .append("      <option value=\"\">All authors</option>\n");
        for (String a : bookLoader.getAuthors()) {
            form.append("      <option value=\"").append(escape(a)).append("\"")
                    .append(a.equalsIgnoreCase(author == null ? "" : author.trim()) ? " selected" : "")
                    .append(">").append(escape(a)).append("</option>\n");
        }
        form.append("    </select>\n")
                .append("    <select name=\"difficulty\" aria-label=\"Difficulty\">\n")
                .append("      <option value=\"\">All difficulties</option>\n");
        for (Difficulty d : Difficulty.values()) {
            form.append("      <option value=\"").append(d.name()).append("\"")
                    .append(d == difficulty ? " selected" : "")
                    .append(">").append(d.name()).append("</option>\n");
        }
        form.append("    </select>\n")
                .append("    <button type=\"submit\">Filter</button>\n");
        if (filtering) {
            form.append("    <a href=\"/\">Clear</a>\n");
        }
        return form.append("  </form>\n").toString();
    }

    private static Difficulty parseDifficulty(String value) {
        if (!notBlank(value)) {
            return null;
        }
        try {
            return Difficulty.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
