import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Best-effort append-only log file so failures can be reviewed after the fact, not just guessed at. */
final class Logger {

    private static final Path LOG_FILE = Path.of("logs", "modsoft.log");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Logger() {
    }

    static Path file() {
        return LOG_FILE;
    }

    static synchronized void log(String message) {
        write("INFO", message);
    }

    static synchronized void error(String message, Throwable t) {
        String detail = t == null ? "" : " -> " + t;
        write("ERROR", message + detail);
    }

    private static void write(String level, String message) {
        try {
            if (LOG_FILE.getParent() != null) {
                Files.createDirectories(LOG_FILE.getParent());
            }
            String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] [" + level + "] " + message + System.lineSeparator();
            Files.writeString(LOG_FILE, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging is best-effort; never let a logging failure break the app itself.
        }
    }
}
