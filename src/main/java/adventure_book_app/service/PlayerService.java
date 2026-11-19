package adventure_book_app.service;

import adventure_book_app.model.player.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers players and stores their progress. Each player is saved as a JSON file in the
 * players folder, so progress survives restarts.
 */
@Slf4j
@Service
public class PlayerService {

    public static final int MAX_NAME_LENGTH = 30;

    private final JsonMapper jsonMapper;
    private final Path playersFolder;
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    public PlayerService(JsonMapper jsonMapper, @Value("${players.folder:players}") String playersFolder) {
        this.jsonMapper = jsonMapper;
        this.playersFolder = Path.of(playersFolder);
    }

    /**
     * Turns a name into a stable id (used for the cookie and the file name).
     * Names are case-insensitive, so "Miguel" and "miguel" are the same player.
     */
    public static String idFor(String name) {
        String id = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return id.isEmpty() ? null : id;
    }

    /** Returns the existing player with this name, or creates and saves a new one. */
    public Player register(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name must be between 1 and " + MAX_NAME_LENGTH + " characters");
        }
        String id = idFor(trimmed);
        if (id == null) {
            throw new IllegalArgumentException("Name must contain at least one letter or number");
        }

        return findById(id).orElseGet(() -> {
            Player player = new Player();
            player.setName(trimmed);
            save(id, player);
            log.info("Registered new player '{}'", trimmed);
            return player;
        });
    }

    public Optional<Player> findById(String id) {
        if (id == null || !id.matches("[a-z0-9-]+")) {
            return Optional.empty();
        }
        Player cached = players.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Path file = fileFor(id);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            Player player = jsonMapper.readValue(file.toFile(), Player.class);
            players.put(id, player);
            return Optional.of(player);
        } catch (Exception e) {
            log.warn("Could not read player file {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    /** Saves the player's current state (including progress) to disk. */
    public synchronized void save(Player player) {
        save(idFor(player.getName()), player);
    }

    private synchronized void save(String id, Player player) {
        try {
            Files.createDirectories(playersFolder);
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(fileFor(id).toFile(), player);
            players.put(id, player);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save player " + player.getName(), e);
        }
    }

    private Path fileFor(String id) {
        return playersFolder.resolve(id + ".json");
    }
}
