package adventure_book_app.service;

import org.springframework.stereotype.Service;

import static adventure_book_app.service.HtmlLayout.escape;

/** Generates the register page where the player enters their name. */
@Service
public class RegisterPage {

    /**
     * @param name  value to pre-fill the input with (may be null)
     * @param error error message to show (may be null)
     */
    public String generateHtml(String name, String error) {
        String body = """
                  <h1>Register</h1>
                  <p>Enter your name. If you've played before, use the same name to pick up where you left off.</p>
                  %s
                  <form method="post" action="/register">
                    <label for="name">Name</label>
                    <input type="text" id="name" name="name" value="%s" maxlength="%d" required autofocus>
                    <button type="submit">Start</button>
                  </form>
                  <p><a href="/">Back to books</a></p>
                """.formatted(
                error == null ? "" : "<p class=\"error\">" + escape(error) + "</p>",
                escape(name),
                PlayerService.MAX_NAME_LENGTH);
        return HtmlLayout.page("Register", body);
    }
}
