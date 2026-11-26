package adventure_book_app.api.dto;

import adventure_book_app.model.book.Difficulty;

import java.util.List;

/**
 * JSON shapes exchanged with the frontend. These deliberately expose only what the player may see:
 * never the whole book, and never where options lead.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    /** A book in the list. {@code inProgress} is true if the current player has a saved game in it. */
    public record BookSummary(String id, String title, String author, Difficulty difficulty, boolean inProgress) {
    }

    public record PlayerView(String name) {
    }

    /*
     * Body of POST /api/books: a new book in the same shape as the files in the books folder.
     * Mapped to the model by adventure_book_app.mapper.BookMapper.
     */

    public record NewBook(String title, String author, String difficulty, List<NewSection> sections) {
    }

    public record NewSection(Integer id, String text, String type, List<NewOption> options) {
    }

    public record NewOption(String description, Integer gotoId, NewConsequence consequence) {
    }

    public record NewConsequence(String type, String value, String text) {
    }

    /** Result of checking a new book. {@code problems} is empty when {@code valid} is true. */
    public record BookValidation(boolean valid, List<String> problems) {
        public static BookValidation of(List<String> problems) {
            return new BookValidation(problems.isEmpty(), problems);
        }
    }

    public record RegisterRequest(String name) {
    }

    public record ChoiceRequest(Integer fromSectionId, Integer optionIndex) {
    }

    public enum PlayStatus { PLAYING, DEAD, FINISHED }

    public record OptionView(int index, String description) {
    }

    /**
     * The player's current place in a book.
     *
     * @param sectionId send this back as {@code fromSectionId} when choosing, so stale choices are rejected
     * @param message   one-time consequence text from the last choice (may be null)
     * @param options   empty when the game is over (or if the section is a dead end)
     */
    public record PlayState(String bookId, String bookTitle, int sectionId, String text, List<OptionView> options,
                            int health, int maxHealth, String message, PlayStatus status) {
    }

    /** Error body. {@code state} is set when the error happened mid-game, so the frontend can resync. */
    public record ApiError(String message, PlayState state) {
        public ApiError(String message) {
            this(message, null);
        }
    }
}
