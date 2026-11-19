package adventure_book_app.model.book;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    /**
     * Maps a string such as "EASY" (case-insensitive, surrounding whitespace ignored)
     * to the matching Difficulty.
     *
     * @throws IllegalArgumentException if the value is null or doesn't match any difficulty
     */
    @JsonCreator
    public static Difficulty fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Difficulty value must not be null");
        }
        for (Difficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(value.trim())) {
                return difficulty;
            }
        }
        throw new IllegalArgumentException("Unknown difficulty: " + value);
    }
}
