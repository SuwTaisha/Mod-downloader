import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

/** A frame-wide overlay that blocks input and shows a spinning indicator while a background task runs. */
final class BusyGlassPane extends JComponent {

    private int angle = 0;
    private final Timer spinTimer = new Timer(16, e -> {
        angle = (angle + 8) % 360;
        repaint();
    });

    BusyGlassPane() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // An empty listener is enough to make the glass pane swallow all mouse input beneath it.
        addMouseListener(new MouseAdapter() {
        });
        addMouseMotionListener(new MouseMotionAdapter() {
        });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            spinTimer.start();
        } else {
            spinTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int size = 40;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawOval(x, y, size, size);
        g2.setColor(ModDownloaderFrame.accent());
        g2.drawArc(x, y, size, size, angle, 110);

        g2.dispose();
    }
}
