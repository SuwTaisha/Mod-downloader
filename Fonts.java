import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

public final class Fonts {

    public static final String FAMILY = "Inter";

    private static final String[] RESOURCES = {
            "/resources/fonts/Inter-Regular.ttf",
            "/resources/fonts/Inter-Medium.ttf",
            "/resources/fonts/Inter-SemiBold.ttf",
            "/resources/fonts/Inter-Bold.ttf",
    };

    private Fonts() {
    }

    /** Loads from the classpath (not a raw file path) so it works both in dev and once bundled inside a jar. */
    public static void registerAll() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String resource : RESOURCES) {
            try (InputStream in = Fonts.class.getResourceAsStream(resource)) {
                if (in == null) {
                    continue;
                }
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                ge.registerFont(font);
            } catch (FontFormatException | IOException e) {
                // Bundled font is a cosmetic enhancement; fall back to the system font if missing.
            }
        }
    }
}
