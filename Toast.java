import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/** A small, self-dismissing notification that floats over the app — replaces native message popups. */
final class Toast {

    enum Kind { INFO, SUCCESS, ERROR }

    private static final int FADE_STEP_MS = 20;
    private static final float FADE_IN_STEP = 0.12f;
    private static final float FADE_OUT_STEP = 0.08f;
    private static final int HOLD_MS = 2800;

    private Toast() {
    }

    static void show(JFrame owner, String message, Kind kind) {
        JWindow window = new JWindow(owner);
        window.setFocusableWindowState(false);
        window.setAlwaysOnTop(true);

        Color accentStripe = kind == Kind.ERROR
                ? ModDownloaderFrame.mix(ModDownloaderFrame.accent(), Color.BLACK, 0.55)
                : ModDownloaderFrame.accent();

        JPanel panel = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ModDownloaderFrame.pageBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(accentStripe);
                g2.fillRoundRect(0, 0, 4, getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 18));

        JLabel label = new JLabel("<html><body style='width: 280px'>" + message + "</body></html>");
        label.setForeground(ModDownloaderFrame.pageFg());
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label, BorderLayout.CENTER);

        window.setContentPane(panel);
        window.pack();
        Dimension size = window.getSize();
        window.setSize(Math.min(size.width, 340), size.height);

        Point ownerLoc = owner.getLocationOnScreen();
        int x = ownerLoc.x + owner.getWidth() - window.getWidth() - 28;
        int y = ownerLoc.y + owner.getHeight() - window.getHeight() - 28;
        window.setLocation(x, y);

        window.setOpacity(0f);
        window.setVisible(true);

        Timer fadeIn = new Timer(FADE_STEP_MS, null);
        fadeIn.addActionListener(e -> {
            float opacity = Math.min(1f, window.getOpacity() + FADE_IN_STEP);
            window.setOpacity(opacity);
            if (opacity >= 1f) {
                fadeIn.stop();
                Timer hold = new Timer(HOLD_MS, holdEvent -> fadeOut(window));
                hold.setRepeats(false);
                hold.start();
            }
        });
        fadeIn.start();
    }

    private static void fadeOut(JWindow window) {
        Timer fadeOut = new Timer(FADE_STEP_MS, null);
        fadeOut.addActionListener(e -> {
            float opacity = Math.max(0f, window.getOpacity() - FADE_OUT_STEP);
            window.setOpacity(opacity);
            if (opacity <= 0f) {
                fadeOut.stop();
                window.dispose();
            }
        });
        fadeOut.start();
    }
}
