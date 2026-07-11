import javax.swing.Icon;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Wraps an icon so it fades in smoothly over its first ~220ms after being painted for the first time. */
final class FadingIcon implements Icon {

    private static final long FADE_MS = 220;

    private final Icon delegate;
    private final long startTime = System.currentTimeMillis();

    FadingIcon(Icon delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getIconWidth() {
        return delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return delegate.getIconHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        float alpha = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) FADE_MS);
        if (alpha >= 1f) {
            delegate.paintIcon(c, g, x, y);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        delegate.paintIcon(c, g2, x, y);
        g2.dispose();
    }
}
