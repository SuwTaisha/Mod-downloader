import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public final class Fonts {

    public static final String FAMILY = "Inter";

    private static final String[] FILES = {
            "resources/fonts/Inter-Regular.ttf",
            "resources/fonts/Inter-Medium.ttf",
            "resources/fonts/Inter-SemiBold.ttf",
            "resources/fonts/Inter-Bold.ttf",
    };

    private Fonts() {
    }

    public static void registerAll() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String path : FILES) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, new File(path));
                ge.registerFont(font);
            } catch (FontFormatException | IOException e) {
                // Bundled font is a cosmetic enhancement; fall back to the system font if missing.
            }
        }
    }
}
