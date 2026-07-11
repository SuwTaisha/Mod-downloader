import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public final class Icons {

    public enum Kind { FOLDER, PLUS, TRASH, DOWNLOAD, SUN, MOON, IMAGE, CHEVRON_LEFT, CHEVRON_RIGHT, CHECK, CLOSE }

    private Icons() {
    }

    public static Icon of(Kind kind, Color color, int size) {
        return new VectorIcon(kind, color, size);
    }

    private static final class VectorIcon implements Icon {
        private final Kind kind;
        private final Color color;
        private final int size;

        VectorIcon(Kind kind, Color color, int size) {
            this.kind = kind;
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case FOLDER:
                    paintFolder(g2, s);
                    break;
                case PLUS:
                    paintPlus(g2, s);
                    break;
                case TRASH:
                    paintTrash(g2, s);
                    break;
                case DOWNLOAD:
                    paintDownload(g2, s);
                    break;
                case SUN:
                    paintSun(g2, s);
                    break;
                case MOON:
                    paintMoon(g2, s);
                    break;
                case IMAGE:
                    paintImagePlaceholder(g2, s);
                    break;
                case CHEVRON_LEFT:
                    paintChevron(g2, s, true);
                    break;
                case CHEVRON_RIGHT:
                    paintChevron(g2, s, false);
                    break;
                case CHECK:
                    paintCheck(g2, s);
                    break;
                case CLOSE:
                    paintClose(g2, s);
                    break;
            }
            g2.dispose();
        }

        private void paintFolder(Graphics2D g2, float s) {
            float m = s * 0.14f;
            Path2D p = new Path2D.Float();
            p.moveTo(m, s * 0.32f);
            p.lineTo(s * 0.42f, s * 0.32f);
            p.lineTo(s * 0.50f, s * 0.42f);
            p.lineTo(s - m, s * 0.42f);
            p.lineTo(s - m, s - m);
            p.lineTo(m, s - m);
            p.closePath();
            g2.draw(p);
        }

        private void paintPlus(Graphics2D g2, float s) {
            float m = s * 0.16f;
            g2.draw(new Line2D.Float(s / 2, m, s / 2, s - m));
            g2.draw(new Line2D.Float(m, s / 2, s - m, s / 2));
        }

        private void paintTrash(Graphics2D g2, float s) {
            float m = s * 0.22f;
            RoundRectangle2D body = new RoundRectangle2D.Float(m, s * 0.34f, s - 2 * m, s * 0.52f, s * 0.08f, s * 0.08f);
            g2.draw(body);
            g2.draw(new Line2D.Float(s * 0.34f, s * 0.20f, s * 0.66f, s * 0.20f));
            g2.draw(new Line2D.Float(s * 0.5f, s * 0.46f, s * 0.5f, s * 0.74f));
        }

        private void paintDownload(Graphics2D g2, float s) {
            g2.draw(new Line2D.Float(s / 2, s * 0.14f, s / 2, s * 0.58f));
            Path2D arrow = new Path2D.Float();
            arrow.moveTo(s * 0.30f, s * 0.40f);
            arrow.lineTo(s * 0.5f, s * 0.62f);
            arrow.lineTo(s * 0.70f, s * 0.40f);
            g2.draw(arrow);
            g2.draw(new Line2D.Float(s * 0.20f, s * 0.82f, s * 0.80f, s * 0.82f));
        }

        private void paintSun(Graphics2D g2, float s) {
            float r = s * 0.18f;
            g2.draw(new Ellipse2D.Float(s / 2 - r, s / 2 - r, r * 2, r * 2));
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                float x1 = (float) (s / 2 + Math.cos(angle) * r * 1.55);
                float y1 = (float) (s / 2 + Math.sin(angle) * r * 1.55);
                float x2 = (float) (s / 2 + Math.cos(angle) * r * 2.15);
                float y2 = (float) (s / 2 + Math.sin(angle) * r * 2.15);
                g2.draw(new Line2D.Float(x1, y1, x2, y2));
            }
        }

        private void paintMoon(Graphics2D g2, float s) {
            Ellipse2D full = new Ellipse2D.Float(s * 0.20f, s * 0.16f, s * 0.60f, s * 0.60f);
            Ellipse2D cut = new Ellipse2D.Float(s * 0.36f, s * 0.08f, s * 0.60f, s * 0.60f);
            Area area = new Area(full);
            area.subtract(new Area(cut));
            g2.draw(area);
        }

        private void paintImagePlaceholder(Graphics2D g2, float s) {
            float m = s * 0.12f;
            g2.draw(new RoundRectangle2D.Float(m, m, s - 2 * m, s - 2 * m, s * 0.15f, s * 0.15f));
            float r = s * 0.09f;
            g2.draw(new Ellipse2D.Float(s * 0.30f - r, s * 0.34f - r, r * 2, r * 2));
            Path2D mountains = new Path2D.Float();
            mountains.moveTo(m + s * 0.03f, s - m - s * 0.06f);
            mountains.lineTo(s * 0.40f, s * 0.52f);
            mountains.lineTo(s * 0.56f, s * 0.68f);
            mountains.lineTo(s * 0.70f, s * 0.50f);
            mountains.lineTo(s - m - s * 0.03f, s - m - s * 0.06f);
            g2.draw(mountains);
        }

        private void paintChevron(Graphics2D g2, float s, boolean left) {
            Path2D p = new Path2D.Float();
            if (left) {
                p.moveTo(s * 0.62f, s * 0.20f);
                p.lineTo(s * 0.34f, s * 0.5f);
                p.lineTo(s * 0.62f, s * 0.80f);
            } else {
                p.moveTo(s * 0.38f, s * 0.20f);
                p.lineTo(s * 0.66f, s * 0.5f);
                p.lineTo(s * 0.38f, s * 0.80f);
            }
            g2.draw(p);
        }

        private void paintCheck(Graphics2D g2, float s) {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.22f, s * 0.52f);
            p.lineTo(s * 0.42f, s * 0.72f);
            p.lineTo(s * 0.78f, s * 0.30f);
            g2.draw(p);
        }

        private void paintClose(Graphics2D g2, float s) {
            float m = s * 0.26f;
            g2.draw(new Line2D.Float(m, m, s - m, s - m));
            g2.draw(new Line2D.Float(s - m, m, m, s - m));
        }
    }
}
