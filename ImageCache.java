import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads and caches mod thumbnails off the EDT, keeping the UI responsive while icons stream in. */
final class ImageCache {

    private static final int SIZE = 40;
    private static final int FADE_DURATION_MS = 240;
    private static final int FADE_TICK_MS = 30;

    private final Map<String, Icon> cache = new ConcurrentHashMap<>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "thumbnail-loader");
        t.setDaemon(true);
        return t;
    });

    /** Returns the cached icon if available, otherwise kicks off a background load and returns null. */
    Icon get(String url, Runnable onLoaded) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Icon cached = cache.get(url);
        if (cached != null) {
            return cached;
        }
        if (loading.add(url)) {
            executor.submit(() -> {
                try {
                    BufferedImage image = ImageIO.read(URI.create(url).toURL());
                    if (image != null) {
                        Image scaled = image.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH);
                        cache.put(url, new FadingIcon(new ImageIcon(scaled)));
                        SwingUtilities.invokeLater(() -> animateFadeIn(onLoaded));
                    }
                } catch (IOException | IllegalArgumentException ignored) {
                    // Missing/unsupported thumbnail: caller keeps showing the placeholder icon.
                } finally {
                    loading.remove(url);
                }
            });
        }
        return null;
    }

    /** Repaints the requesting list repeatedly while the fade-in plays, then stops on its own. */
    private static void animateFadeIn(Runnable onLoaded) {
        long start = System.currentTimeMillis();
        Timer timer = new Timer(FADE_TICK_MS, null);
        timer.addActionListener(e -> {
            onLoaded.run();
            if (System.currentTimeMillis() - start >= FADE_DURATION_MS) {
                timer.stop();
            }
        });
        timer.start();
    }
}
