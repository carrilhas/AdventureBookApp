package adventure_book_app.service;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/** Shared page shell and helpers for the server-generated HTML pages. */
public final class HtmlLayout {

    private HtmlLayout() {
    }

    public static String page(String title, String body) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  <style>
                    body { font-family: system-ui, sans-serif; max-width: 720px; margin: 2rem auto; padding: 0 1rem; }
                    ul { list-style: none; padding: 0; }
                    li { border: 1px solid #ccc; border-radius: 8px; padding: 1rem; margin-bottom: 1rem; }
                    h2 { margin: 0 0 .25rem; }
                    .meta { color: #555; margin: 0 0 .75rem; }
                    .error { color: #b00020; }
                    input[type=text] { padding: .4rem; font-size: 1rem; }
                    button { padding: .4rem .8rem; font-size: 1rem; }
                  </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(escape(title), body);
    }

    public static String escape(String value) {
        return value == null ? "" : htmlEscape(value);
    }
}
