package adventure_book_app.service;

import adventure_book_app.model.book.Book;
import adventure_book_app.model.book.Consequence;
import adventure_book_app.model.book.Option;
import adventure_book_app.model.book.Section;
import adventure_book_app.model.player.BookProgress;
import adventure_book_app.model.player.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * The game rules: starting a book, choosing options, applying consequences and detecting the end.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    public static final int STARTING_HEALTH = 10;

    private final PlayerService playerService;

    /**
     * Returns the player's progress in this book, starting the book if needed (no saved progress, the saved
     * section no longer exists, or the saved game is already over).
     */
    public BookProgress progressFor(Player player, Book book) {
        BookProgress progress = player.getProgress().get(book.getTitle());
        if (progress == null || findSection(book, progress.getSectionId()).isEmpty() || isOver(book, progress)) {
            progress = restart(player, book);
        }
        return progress;
    }

    /** Puts the player back at the BEGIN section with full health. */
    public BookProgress restart(Player player, Book book) {
        BookProgress progress = new BookProgress(startSection(book).getId(), STARTING_HEALTH, null);
        player.getProgress().put(book.getTitle(), progress);
        playerService.save(player);
        return progress;
    }

    /**
     * Applies the option at {@code optionIndex} of the player's current section.
     * If the choice ends the adventure (an END section or death), the player's progress in this book is reset,
     * so the returned state is the only record of how it ended.
     *
     * @param fromSectionId the section the choice was made on; must match the player's current section,
     *                      so a stale page (back button, refresh, other tab) can't apply an old choice
     * @return the player's state after the choice
     * @throws IllegalArgumentException if the choice isn't valid from the current state
     */
    public BookProgress choose(Player player, Book book, int fromSectionId, int optionIndex) {
        BookProgress progress = progressFor(player, book);
        if (progress.getSectionId() != fromSectionId) {
            throw new IllegalArgumentException("That choice is no longer available. Here is where you are now.");
        }
        Section current = findSection(book, progress.getSectionId()).orElseThrow();
        List<Option> options = current.getOptions() == null ? List.of() : current.getOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option");
        }
        Option option = options.get(optionIndex);
        Section next = findSection(book, option.getGotoId())
                .orElseThrow(() -> new IllegalArgumentException("That path leads nowhere (section " + option.getGotoId() + " is missing)"));

        progress.setLastMessage(applyConsequence(progress, option.getConsequence()));
        progress.setSectionId(next.getId());
        if (isOver(book, progress)) {
            player.getProgress().remove(book.getTitle());
        }
        playerService.save(player);
        return progress;
    }

    public boolean isDead(BookProgress progress) {
        return progress.getHealth() <= 0;
    }

    public boolean isFinished(Book book, BookProgress progress) {
        return findSection(book, progress.getSectionId())
                .map(s -> "END".equalsIgnoreCase(s.getType()))
                .orElse(false);
    }

    public boolean isOver(Book book, BookProgress progress) {
        return isDead(progress) || isFinished(book, progress);
    }

    public Optional<Section> findSection(Book book, Integer id) {
        if (id == null || book.getSections() == null) {
            return Optional.empty();
        }
        return book.getSections().stream().filter(s -> s.getId() == id).findFirst();
    }

    /** Clears the one-time consequence message after it has been shown. */
    public void clearMessage(Player player, BookProgress progress) {
        if (progress.getLastMessage() != null) {
            progress.setLastMessage(null);
            playerService.save(player);
        }
    }

    private Section startSection(Book book) {
        List<Section> sections = book.getSections();
        if (sections == null || sections.isEmpty()) {
            throw new IllegalStateException("Book '" + book.getTitle() + "' has no sections");
        }
        return sections.stream()
                .filter(s -> "BEGIN".equalsIgnoreCase(s.getType()))
                .findFirst()
                .orElse(sections.getFirst());
    }

    /** Updates health and returns the text to show the player (may be null). */
    private String applyConsequence(BookProgress progress, Consequence consequence) {
        if (consequence == null) {
            return null;
        }
        int amount = parseAmount(consequence.getValue());
        String type = consequence.getType() == null ? "" : consequence.getType().toUpperCase();
        String change = switch (type) {
            case "LOSE_HEALTH" -> {
                progress.setHealth(progress.getHealth() - amount);
                yield "-" + amount + " health";
            }
            case "GAIN_HEALTH" -> {
                progress.setHealth(progress.getHealth() + amount);
                yield "+" + amount + " health";
            }
            default -> null;
        };
        String text = consequence.getText();
        if (change == null) {
            return text;
        }
        return text == null || text.isBlank() ? change : text + " (" + change + ")";
    }

    private static int parseAmount(String value) {
        try {
            return value == null ? 0 : Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
