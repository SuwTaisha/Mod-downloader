import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

/** A self-styled, undecorated confirmation dialog matching the app's own theme — no native OS chrome. */
final class AppDialog {

    private AppDialog() {
    }

    /** Shows the content with Cancel/Confirm actions and blocks until the user picks one. Returns true only if confirmed. */
    static boolean confirm(JFrame owner, String title, JComponent content, String cancelLabel, String confirmLabel) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setUndecorated(true);
        dialog.setResizable(false);

        boolean[] result = {false};
        JPanel root = buildRoot(title, content);

        JButton cancelButton = new JButton(cancelLabel);
        ModDownloaderFrame.styleOutline(cancelButton, Icons.Kind.CLOSE);
        JButton confirmButton = new JButton(confirmLabel);
        ModDownloaderFrame.styleSolid(confirmButton, Icons.Kind.CHECK);

        cancelButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });
        confirmButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(confirmButton);
        root.add(buttonRow, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
                e -> {
                    result[0] = false;
                    dialog.dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(confirmButton);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    /** Shows the content with a single acknowledge button and blocks until it's dismissed. */
    static void info(JFrame owner, String title, JComponent content, String closeLabel) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setUndecorated(true);
        dialog.setResizable(false);

        JPanel root = buildRoot(title, content);

        JButton closeButton = new JButton(closeLabel);
        ModDownloaderFrame.styleSolid(closeButton, Icons.Kind.CHECK);
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(closeButton);
        root.add(buttonRow, BorderLayout.SOUTH);

        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(closeButton);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel buildRoot(String title, JComponent content) {
        JPanel root = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ModDownloaderFrame.pageBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        Color border = ModDownloaderFrame.mix(ModDownloaderFrame.accent(), ModDownloaderFrame.pageBg(), 0.4);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 18, 22)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(ModDownloaderFrame.pageFg());
        root.add(titleLabel, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        return root;
    }
}
