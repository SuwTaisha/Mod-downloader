import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        Fonts.registerAll();
        FlatLaf.registerCustomDefaultsSource("theme");
        FlatDarkLaf.setup();
        JFrame.setDefaultLookAndFeelDecorated(true);

        UIManager.put("defaultFont", new FontUIResource(Fonts.FAMILY, Font.PLAIN, 13));
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 8);

        SwingUtilities.invokeLater(() -> new ModDownloaderFrame().setVisible(true));
    }
}
