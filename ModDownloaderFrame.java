import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModDownloaderFrame extends JFrame {

    private static final int ARC = 8;
    private static final int PAGE_SIZE = 10;
    private static final String[] SORT_VALUES = {"relevance", "downloads", "follows", "newest", "updated"};
    private static final String[] SORT_KEYS = {"sort.relevance", "sort.downloads", "sort.follows", "sort.newest", "sort.updated"};

    private final ModrinthClient modrinthClient = new ModrinthClient();
    private final ImageCache imageCache = new ImageCache();

    private final JTextField pathField = new JTextField();
    private final JTextField browseSearchField = new JTextField();
    private final JComboBox<String> sortCombo = new JComboBox<>();
    private final JComboBox<String> mcVersionCombo = new JComboBox<>();
    private final List<String> mcVersionValues = new ArrayList<>();
    private final DefaultListModel<ModrinthSearchHit> browseListModel = new DefaultListModel<>();
    private final JList<ModrinthSearchHit> browseList = new JList<>(browseListModel);
    private final JButton prevButton = new JButton();
    private final JButton nextButton = new JButton();
    private final JLabel pageLabel = new JLabel();

    private final DefaultListModel<QueuedMod> modListModel = new DefaultListModel<>();
    private final JList<QueuedMod> modList = new JList<>(modListModel);
    private final JButton installButton = new JButton();
    private final JLabel statusLabel = new JLabel(" ");

    private final DefaultListModel<UpdateRow> updateListModel = new DefaultListModel<>();
    private final JList<UpdateRow> updateList = new JList<>(updateListModel);
    private final JComboBox<String> mcVersionComboUpdate = new JComboBox<>();
    private final JButton refreshUpdateButton = new JButton();
    private final JButton checkUpdatesButton = new JButton();
    private final JButton updateSelectedButton = new JButton();
    private final JButton deleteInstalledButton = new JButton();
    private final JLabel updateSectionLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel pathSectionLabel = new JLabel();
    private final JLabel browseSectionLabel = new JLabel();
    private final JLabel selectedSectionLabel = new JLabel();
    private final JButton browseDirButton = new JButton();
    private final JButton addButton = new JButton();
    private final JButton removeButton = new JButton();
    private final JToggleButton lightToggle = new JToggleButton();
    private final JToggleButton darkToggle = new JToggleButton();
    private final JComboBox<String> langCombo = new JComboBox<>();
    private final JTabbedPane tabs = new JTabbedPane();

    private final ActionListener sortListener = e -> {
        currentPage = 0;
        loadBrowsePage();
    };

    private final ActionListener versionListener = e -> {
        currentPage = 0;
        loadBrowsePage();
    };

    private final ActionListener updateVersionListener = e -> {
        if (!updateListModel.isEmpty()) {
            checkForUpdates();
        }
    };

    private final BusyGlassPane glassPane = new BusyGlassPane();
    private final Timer showOverlayTimer = new Timer(150, e -> glassPane.setVisible(true));
    private int busyCount = 0;

    private boolean darkMode = true;
    private boolean pathConfirmed = false;
    private int currentPage = 0;
    private int totalHits = 0;

    public ModDownloaderFrame() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(780, 540));
        setLocationRelativeTo(null);
        showOverlayTimer.setRepeats(false);
        setGlassPane(glassPane);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        content.add(buildTopBar(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 16));
        middle.add(buildHeader(), BorderLayout.NORTH);

        JPanel sections = new JPanel(new BorderLayout(0, 16));
        sections.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        sections.add(buildPathSection(), BorderLayout.NORTH);

        tabs.addTab("", buildDownloadTab());
        tabs.addTab("", buildUpdateTab());
        sections.add(tabs, BorderLayout.CENTER);

        middle.add(sections, BorderLayout.CENTER);

        content.add(middle, BorderLayout.CENTER);
        content.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(content);

        refreshTexts();
        refreshButtonStyles();
        updateSelectionGating();
        loadMcVersions();
        loadBrowsePage();
    }

    /** Marks a directory as the confirmed install target, unlocking mod selection. */
    private void confirmPath(File dir) {
        pathConfirmed = true;
        updateSelectionGating();
        loadModsFromDirectory(dir.toPath());
    }

    /** Mod selection (add button, double-click, drag-and-drop) stays locked until a directory is confirmed. */
    private void updateSelectionGating() {
        addButton.setEnabled(pathConfirmed);
    }

    /** Re-enables both tabs' primary action buttons — the shared install/update pipeline doesn't track which one started it. */
    private void enableActionButtons() {
        installButton.setEnabled(true);
        updateSelectedButton.setEnabled(true);
    }

    /** Reference-counted so overlapping background tasks (e.g. loading versions + browsing at startup) don't fight over the overlay. */
    private void setBusy(boolean busy) {
        if (busy) {
            busyCount++;
            if (busyCount == 1) {
                showOverlayTimer.restart();
            }
        } else {
            busyCount = Math.max(0, busyCount - 1);
            if (busyCount == 0) {
                showOverlayTimer.stop();
                glassPane.setVisible(false);
            }
        }
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());

        langCombo.addItem(I18n.t("lang.vi"));
        langCombo.addItem(I18n.t("lang.en"));
        langCombo.setSelectedIndex(I18n.getLang() == I18n.Lang.VI ? 0 : 1);
        langCombo.addActionListener(e -> {
            I18n.setLang(langCombo.getSelectedIndex() == 0 ? I18n.Lang.VI : I18n.Lang.EN);
            refreshTexts();
        });

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightToggle);
        themeGroup.add(darkToggle);
        darkToggle.setSelected(true);
        lightToggle.addActionListener(e -> setDarkMode(false));
        darkToggle.addActionListener(e -> setDarkMode(true));

        JPanel themeSwitch = new JPanel(new GridLayout(1, 2, 4, 0));
        themeSwitch.add(lightToggle);
        themeSwitch.add(darkToggle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(langCombo);
        right.add(themeSwitch);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        subtitleLabel.setForeground(mutedColor());
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    private JPanel buildPathSection() {
        JPanel section = new JPanel(new BorderLayout(0, 6));

        pathSectionLabel.setFont(pathSectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        pathSectionLabel.setForeground(mutedColor());

        JPanel row = new JPanel(new BorderLayout(10, 0));
        pathField.putClientProperty(FlatClientProperties.STYLE, "showClearButton:true");
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, I18n.t("path.placeholder"));
        pathField.addActionListener(e -> {
            File dir = parsePath(pathField.getText());
            if (dir != null) {
                pathField.setText(dir.getAbsolutePath());
                pathField.putClientProperty(FlatClientProperties.OUTLINE, null);
                confirmPath(dir);
            } else {
                pathField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            }
        });
        pathField.getDocument().addDocumentListener(clearOutlineOnEdit(pathField));
        installFolderDropTarget(pathField);

        browseDirButton.addActionListener(e -> chooseDirectory());

        row.add(pathField, BorderLayout.CENTER);
        row.add(browseDirButton, BorderLayout.EAST);

        section.add(pathSectionLabel, BorderLayout.NORTH);
        section.add(row, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildBrowsePanel() {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        browseSectionLabel.setFont(browseSectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        browseSectionLabel.setForeground(mutedColor());

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        browseSearchField.addActionListener(e -> {
            currentPage = 0;
            loadBrowsePage();
        });
        sortCombo.addActionListener(sortListener);
        mcVersionCombo.addActionListener(versionListener);
        mcVersionCombo.setToolTipText(I18n.t("version.tooltip"));
        sortCombo.setToolTipText(I18n.t("sort.tooltip"));

        JPanel filterCombos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterCombos.add(mcVersionCombo);
        filterCombos.add(sortCombo);

        searchRow.add(browseSearchField, BorderLayout.CENTER);
        searchRow.add(filterCombos, BorderLayout.EAST);

        browseList.setCellRenderer(new BrowseCellRenderer());
        browseList.setFixedCellHeight(64);
        browseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        browseList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addBrowseSelectionToInstallList();
                }
            }
        });
        browseList.setDragEnabled(true);
        browseList.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                ModrinthSearchHit selected = browseList.getSelectedValue();
                return selected == null ? null : new ModHitTransferable(selected);
            }
        });
        JScrollPane scrollPane = new JScrollPane(browseList);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:" + ARC);

        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadBrowsePage();
            }
        });
        nextButton.addActionListener(e -> {
            if ((currentPage + 1) * PAGE_SIZE < totalHits) {
                currentPage++;
                loadBrowsePage();
            }
        });
        addButton.addActionListener(e -> addBrowseSelectionToInstallList());

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        navButtons.add(prevButton);
        navButtons.add(pageLabel);
        navButtons.add(nextButton);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.add(navButtons, BorderLayout.CENTER);
        bottomRow.add(addButton, BorderLayout.EAST);

        JPanel listWrapper = new JPanel(new BorderLayout(0, 8));
        listWrapper.add(searchRow, BorderLayout.NORTH);
        listWrapper.add(scrollPane, BorderLayout.CENTER);
        listWrapper.add(bottomRow, BorderLayout.SOUTH);

        section.add(browseSectionLabel, BorderLayout.NORTH);
        section.add(listWrapper, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildSelectedPanel() {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        selectedSectionLabel.setFont(selectedSectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        selectedSectionLabel.setForeground(mutedColor());

        modList.setCellRenderer(new SelectedCellRenderer());
        modList.setFixedCellHeight(48);
        modList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modList.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return pathConfirmed
                        && (support.isDataFlavorSupported(ModHitTransferable.FLAVOR)
                        || support.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    if (support.isDataFlavorSupported(ModHitTransferable.FLAVOR)) {
                        ModrinthSearchHit hit = (ModrinthSearchHit) support.getTransferable().getTransferData(ModHitTransferable.FLAVOR);
                        pickVersionAndAdd(hit);
                        return true;
                    }
                    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        return importJarFiles(support);
                    }
                } catch (Exception ignored) {
                    // Unsupported/unreadable drop payload: nothing to import.
                }
                return false;
            }
        });

        JScrollPane scrollPane = new JScrollPane(modList);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:" + ARC);

        removeButton.addActionListener(e -> removeSelectedMod());
        JPanel removeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        removeRow.add(removeButton);

        JPanel listWrapper = new JPanel(new BorderLayout(0, 8));
        listWrapper.add(scrollPane, BorderLayout.CENTER);
        listWrapper.add(removeRow, BorderLayout.SOUTH);

        section.add(selectedSectionLabel, BorderLayout.NORTH);
        section.add(listWrapper, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildDownloadTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildBrowsePanel(), buildSelectedPanel());
        splitPane.setResizeWeight(0.62);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);

        JPanel footer = new JPanel(new BorderLayout());
        installButton.setPreferredSize(new Dimension(150, 42));
        installButton.setFont(installButton.getFont().deriveFont(Font.BOLD, 14f));
        installButton.addActionListener(e -> onInstallClicked());
        footer.add(installButton, BorderLayout.EAST);

        tab.add(splitPane, BorderLayout.CENTER);
        tab.add(footer, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel buildUpdateTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));

        JPanel topRow = new JPanel(new BorderLayout());
        updateSectionLabel.setFont(updateSectionLabel.getFont().deriveFont(Font.BOLD, 11f));
        updateSectionLabel.setForeground(mutedColor());

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        mcVersionComboUpdate.setToolTipText(I18n.t("version.update_tooltip"));
        mcVersionComboUpdate.addActionListener(updateVersionListener);
        refreshUpdateButton.addActionListener(e -> {
            String dirText = pathField.getText().trim();
            if (!pathConfirmed || dirText.isEmpty()) {
                notifyUser(I18n.t("status.empty_path"), Toast.Kind.INFO);
                return;
            }
            loadModsFromDirectory(Path.of(dirText));
        });
        checkUpdatesButton.addActionListener(e -> checkForUpdates());
        actionsRow.add(mcVersionComboUpdate);
        actionsRow.add(refreshUpdateButton);
        actionsRow.add(checkUpdatesButton);

        topRow.add(updateSectionLabel, BorderLayout.WEST);
        topRow.add(actionsRow, BorderLayout.EAST);

        updateList.setCellRenderer(new UpdateCellRenderer());
        updateList.setFixedCellHeight(52);
        updateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        updateList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = updateList.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                UpdateRow row = updateListModel.get(index);
                if (row.hasUpdate) {
                    row.selectedForUpdate = !row.selectedForUpdate;
                    updateList.repaint();
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(updateList);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "arc:" + ARC);

        JPanel footer = new JPanel(new BorderLayout());
        deleteInstalledButton.addActionListener(e -> deleteSelectedInstalledMods());
        JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footerLeft.add(deleteInstalledButton);

        updateSelectedButton.setPreferredSize(new Dimension(170, 42));
        updateSelectedButton.setFont(updateSelectedButton.getFont().deriveFont(Font.BOLD, 14f));
        updateSelectedButton.addActionListener(e -> runUpdateSelected());
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(updateSelectedButton, BorderLayout.EAST);

        tab.add(topRow, BorderLayout.NORTH);
        tab.add(scrollPane, BorderLayout.CENTER);
        tab.add(footer, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        statusLabel.setForeground(mutedColor());
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    // ---- Rendering helpers -------------------------------------------------

    private final class BrowseCellRenderer implements ListCellRenderer<ModrinthSearchHit> {
        private final JPanel panel = new JPanel(new BorderLayout(10, 0));
        private final JLabel iconLabel = new JLabel();
        private final JLabel cellTitle = new JLabel();
        private final JLabel cellDesc = new JLabel();

        BrowseCellRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            cellTitle.setFont(cellTitle.getFont().deriveFont(Font.BOLD, 13f));
            cellDesc.setFont(cellDesc.getFont().deriveFont(12f));
            textPanel.add(cellTitle);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(cellDesc);
            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModrinthSearchHit> list, ModrinthSearchHit value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            cellTitle.setText(value.title != null ? value.title : value.slug);
            cellDesc.setText(truncate(value.description, 78));
            Icon icon = imageCache.get(value.iconUrl, list::repaint);
            iconLabel.setIcon(icon != null ? icon : Icons.of(Icons.Kind.IMAGE, mutedColor(), 40));

            panel.setBackground(isSelected ? mix(accent(), pageBg(), 0.22) : pageBg());
            cellTitle.setForeground(pageFg());
            cellDesc.setForeground(mutedColor());
            return panel;
        }
    }

    private final class SelectedCellRenderer implements ListCellRenderer<QueuedMod> {
        private final JPanel panel = new JPanel(new BorderLayout(8, 0));
        private final JLabel iconLabel = new JLabel();
        private final JLabel cellTitle = new JLabel();
        private final JLabel cellVersion = new JLabel();

        SelectedCellRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            cellTitle.setFont(cellTitle.getFont().deriveFont(Font.BOLD, 13f));
            cellVersion.setFont(cellVersion.getFont().deriveFont(11f));
            textPanel.add(cellTitle);
            textPanel.add(cellVersion);

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends QueuedMod> list, QueuedMod value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            ModrinthSearchHit hit = value.hit;
            cellTitle.setText(hit.title != null ? hit.title : hit.slug);
            cellVersion.setText(value.version != null ? value.version.versionNumber : I18n.t("version.unresolved"));
            Icon icon = imageCache.get(hit.iconUrl, list::repaint);
            iconLabel.setIcon(icon != null ? icon : Icons.of(Icons.Kind.IMAGE, mutedColor(), 32));

            panel.setBackground(isSelected ? mix(accent(), pageBg(), 0.22) : pageBg());
            cellTitle.setForeground(pageFg());
            cellVersion.setForeground(mutedColor());
            return panel;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() > max ? oneLine.substring(0, max) + "..." : oneLine;
    }

    /** One row in the Update tab: a mod already in the folder, its installed version, and (once checked) the latest one. */
    private static final class UpdateRow {
        final ModrinthSearchHit hit;
        final String currentVersionId;
        final String currentVersionNumber;
        final Path filePath;
        ModrinthVersion latestVersion;
        boolean checked;
        boolean hasUpdate;
        boolean selectedForUpdate;

        UpdateRow(ModrinthSearchHit hit, String currentVersionId, String currentVersionNumber, Path filePath) {
            this.hit = hit;
            this.currentVersionId = currentVersionId;
            this.currentVersionNumber = currentVersionNumber;
            this.filePath = filePath;
        }
    }

    private final class UpdateCellRenderer implements ListCellRenderer<UpdateRow> {
        private final JPanel panel = new JPanel(new BorderLayout(10, 0));
        private final JCheckBox checkBox = new JCheckBox();
        private final JLabel iconLabel = new JLabel();
        private final JLabel cellTitle = new JLabel();
        private final JLabel cellVersion = new JLabel();

        UpdateCellRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            JPanel west = new JPanel(new BorderLayout(6, 0));
            west.setOpaque(false);
            checkBox.setOpaque(false);
            west.add(checkBox, BorderLayout.WEST);
            west.add(iconLabel, BorderLayout.CENTER);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            cellTitle.setFont(cellTitle.getFont().deriveFont(Font.BOLD, 13f));
            cellVersion.setFont(cellVersion.getFont().deriveFont(12f));
            textPanel.add(cellTitle);
            textPanel.add(cellVersion);

            panel.add(west, BorderLayout.WEST);
            panel.add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends UpdateRow> list, UpdateRow row, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            checkBox.setVisible(row.hasUpdate);
            checkBox.setSelected(row.selectedForUpdate);

            cellTitle.setText(row.hit.title != null ? row.hit.title : row.hit.slug);
            if (row.hasUpdate && row.latestVersion != null) {
                cellVersion.setText(row.currentVersionNumber + "  →  " + row.latestVersion.versionNumber);
                cellVersion.setForeground(accent());
            } else if (row.checked && row.latestVersion == null) {
                cellVersion.setText(I18n.t("update.no_version_for_target") + " (" + row.currentVersionNumber + ")");
                cellVersion.setForeground(mutedColor());
            } else if (row.checked) {
                cellVersion.setText(I18n.t("update.up_to_date") + " (" + row.currentVersionNumber + ")");
                cellVersion.setForeground(mutedColor());
            } else {
                cellVersion.setText(row.currentVersionNumber);
                cellVersion.setForeground(mutedColor());
            }

            Icon icon = imageCache.get(row.hit.iconUrl, list::repaint);
            iconLabel.setIcon(icon != null ? icon : Icons.of(Icons.Kind.IMAGE, mutedColor(), 32));

            panel.setBackground(isSelected ? mix(accent(), pageBg(), 0.22) : pageBg());
            cellTitle.setForeground(pageFg());
            return panel;
        }
    }

    // ---- Colors & button styling -------------------------------------------

    static Color pageBg() {
        return UIManager.getColor("Panel.background");
    }

    static Color pageFg() {
        return UIManager.getColor("Label.foreground");
    }

    static Color accent() {
        return UIManager.getColor("Component.accentColor");
    }

    static Color mutedColor() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : pageFg();
    }

    /** Surfaces a terminal/one-off result both in the status bar and as a floating toast. */
    private void notifyUser(String message, Toast.Kind kind) {
        statusLabel.setText(message);
        Toast.show(this, message, kind);
    }

    static Color mix(Color a, Color b, double ratioA) {
        int r = (int) Math.round(a.getRed() * ratioA + b.getRed() * (1 - ratioA));
        int g = (int) Math.round(a.getGreen() * ratioA + b.getGreen() * (1 - ratioA));
        int bl = (int) Math.round(a.getBlue() * ratioA + b.getBlue() * (1 - ratioA));
        return new Color(r, g, bl);
    }

    static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Solid, filled button — used for the single primary action. */
    static void styleSolid(JButton button, Icons.Kind icon) {
        Color bg = accent();
        Color fg = pageBg();
        button.setIcon(Icons.of(icon, fg, 16));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + ARC + ";background:" + hex(bg) + ";foreground:" + hex(fg)
                        + ";focusedBackground:" + hex(mix(bg, Color.BLACK, 0.9))
                        + ";hoverBackground:" + hex(mix(bg, Color.BLACK, 0.92))
                        + ";pressedBackground:" + hex(mix(bg, Color.BLACK, 0.85)));
    }

    /** Tonal button with a visible accent-tinted fill — used for frequent secondary actions. */
    static void styleTonal(JButton button, Icons.Kind icon) {
        Color bg = accent();
        Color page = pageBg();
        Color fill = mix(bg, page, 0.16);
        Color border = mix(bg, page, 0.55);
        button.setIcon(Icons.of(icon, pageFg(), 16));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + ARC + ";background:" + hex(fill) + ";foreground:" + hex(pageFg())
                        + ";borderWidth:1;borderColor:" + hex(border) + ";focusedBorderColor:" + hex(border)
                        + ";hoverBackground:" + hex(mix(bg, page, 0.28))
                        + ";pressedBackground:" + hex(mix(bg, page, 0.36)));
    }

    /** Outlined button with no fill — used for the destructive / low-emphasis action. */
    static void styleOutline(JButton button, Icons.Kind icon) {
        Color bg = accent();
        Color page = pageBg();
        Color border = mix(bg, page, 0.55);
        button.setIcon(Icons.of(icon, pageFg(), 16));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:" + ARC + ";background:" + hex(page) + ";foreground:" + hex(pageFg())
                        + ";borderWidth:1;borderColor:" + hex(border) + ";focusedBorderColor:" + hex(border)
                        + ";hoverBackground:" + hex(mix(bg, page, 0.14))
                        + ";pressedBackground:" + hex(mix(bg, page, 0.22)));
    }

    private void refreshButtonStyles() {
        styleSolid(installButton, Icons.Kind.DOWNLOAD);
        styleSolid(updateSelectedButton, Icons.Kind.DOWNLOAD);
        styleTonal(addButton, Icons.Kind.PLUS);
        styleTonal(browseDirButton, Icons.Kind.FOLDER);
        styleTonal(prevButton, Icons.Kind.CHEVRON_LEFT);
        styleTonal(nextButton, Icons.Kind.CHEVRON_RIGHT);
        styleTonal(refreshUpdateButton, Icons.Kind.FOLDER);
        styleTonal(checkUpdatesButton, Icons.Kind.DOWNLOAD);
        styleOutline(removeButton, Icons.Kind.TRASH);
        styleOutline(deleteInstalledButton, Icons.Kind.TRASH);
        styleToggle(lightToggle, !darkMode, Icons.Kind.SUN);
        styleToggle(darkToggle, darkMode, Icons.Kind.MOON);
        browseList.repaint();
        modList.repaint();
        updateList.repaint();
    }

    private static void styleToggle(JToggleButton button, boolean selected, Icons.Kind icon) {
        Color bg = accent();
        Color page = pageBg();
        if (selected) {
            button.setIcon(Icons.of(icon, page, 16));
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc:" + ARC + ";background:" + hex(bg) + ";foreground:" + hex(page)
                            + ";selectedBackground:" + hex(bg) + ";selectedForeground:" + hex(page));
        } else {
            Color border = mix(bg, page, 0.55);
            button.setIcon(Icons.of(icon, pageFg(), 16));
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc:" + ARC + ";background:" + hex(page) + ";foreground:" + hex(pageFg())
                            + ";borderWidth:1;borderColor:" + hex(border));
        }
    }

    private void setDarkMode(boolean dark) {
        if (this.darkMode == dark) {
            return;
        }
        this.darkMode = dark;
        try {
            UIManager.setLookAndFeel(dark ? new FlatDarkLaf() : new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
            return;
        }
        FlatLaf.updateUI();
        subtitleLabel.setForeground(mutedColor());
        pathSectionLabel.setForeground(mutedColor());
        browseSectionLabel.setForeground(mutedColor());
        selectedSectionLabel.setForeground(mutedColor());
        updateSectionLabel.setForeground(mutedColor());
        statusLabel.setForeground(mutedColor());
        refreshButtonStyles();
    }

    private void refreshTexts() {
        setTitle(I18n.t("app.title"));
        titleLabel.setText(I18n.t("app.title"));
        subtitleLabel.setText(I18n.t("app.subtitle"));
        pathSectionLabel.setText(I18n.t("section.path"));
        browseSectionLabel.setText(I18n.t("section.browse"));
        selectedSectionLabel.setText(I18n.t("section.selected"));
        browseDirButton.setText(I18n.t("button.browse"));
        addButton.setText(I18n.t("button.add"));
        removeButton.setText(I18n.t("button.remove"));
        installButton.setText(I18n.t("button.install"));
        prevButton.setText(I18n.t("button.prev"));
        nextButton.setText(I18n.t("button.next"));
        browseSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, I18n.t("search.placeholder"));
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, I18n.t("path.placeholder"));
        lightToggle.setText(I18n.t("theme.light"));
        darkToggle.setText(I18n.t("theme.dark"));
        statusLabel.setText(" ");

        tabs.setTitleAt(0, I18n.t("tab.download"));
        tabs.setTitleAt(1, I18n.t("tab.update"));
        updateSectionLabel.setText(I18n.t("section.installed"));
        refreshUpdateButton.setText(I18n.t("button.refresh"));
        checkUpdatesButton.setText(I18n.t("button.check_updates"));
        updateSelectedButton.setText(I18n.t("button.update_selected"));
        deleteInstalledButton.setText(I18n.t("button.delete_installed"));

        sortCombo.removeActionListener(sortListener);
        int selectedSortIndex = Math.max(0, sortCombo.getSelectedIndex());
        sortCombo.removeAllItems();
        for (String key : SORT_KEYS) {
            sortCombo.addItem(I18n.t(key));
        }
        sortCombo.setSelectedIndex(selectedSortIndex);
        sortCombo.addActionListener(sortListener);
        sortCombo.setToolTipText(I18n.t("sort.tooltip"));

        if (mcVersionCombo.getItemCount() > 0) {
            mcVersionCombo.removeActionListener(versionListener);
            int selectedVersionIndex = Math.max(0, mcVersionCombo.getSelectedIndex());
            mcVersionCombo.removeItemAt(0);
            mcVersionCombo.insertItemAt(I18n.t("version.all"), 0);
            mcVersionCombo.setSelectedIndex(selectedVersionIndex);
            mcVersionCombo.addActionListener(versionListener);
        }
        mcVersionCombo.setToolTipText(I18n.t("version.tooltip"));

        if (mcVersionComboUpdate.getItemCount() > 0) {
            mcVersionComboUpdate.removeActionListener(updateVersionListener);
            int selectedVersionIndex = Math.max(0, mcVersionComboUpdate.getSelectedIndex());
            mcVersionComboUpdate.removeItemAt(0);
            mcVersionComboUpdate.insertItemAt(I18n.t("version.all"), 0);
            mcVersionComboUpdate.setSelectedIndex(selectedVersionIndex);
            mcVersionComboUpdate.addActionListener(updateVersionListener);
        }
        mcVersionComboUpdate.setToolTipText(I18n.t("version.update_tooltip"));

        updatePagination();
    }

    private void loadMcVersions() {
        setBusy(true);
        new SwingWorker<List<GameVersionTag>, Void>() {
            @Override
            protected List<GameVersionTag> doInBackground() throws Exception {
                return modrinthClient.getReleaseGameVersions();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<GameVersionTag> tags = get();
                    mcVersionCombo.removeActionListener(versionListener);
                    mcVersionComboUpdate.removeActionListener(updateVersionListener);
                    mcVersionValues.clear();
                    mcVersionCombo.removeAllItems();
                    mcVersionComboUpdate.removeAllItems();
                    mcVersionValues.add(null);
                    mcVersionCombo.addItem(I18n.t("version.all"));
                    mcVersionComboUpdate.addItem(I18n.t("version.all"));
                    for (GameVersionTag tag : tags) {
                        mcVersionValues.add(tag.version);
                        mcVersionCombo.addItem(tag.version);
                        mcVersionComboUpdate.addItem(tag.version);
                    }
                    mcVersionCombo.setSelectedIndex(0);
                    mcVersionComboUpdate.setSelectedIndex(0);
                    mcVersionCombo.addActionListener(versionListener);
                    mcVersionComboUpdate.addActionListener(updateVersionListener);
                } catch (Exception ignored) {
                    // The version filter is a convenience; browsing still works without it.
                }
            }
        }.execute();
    }

    private String selectedMcVersion(JComboBox<String> combo) {
        int idx = combo.getSelectedIndex();
        if (idx <= 0 || idx >= mcVersionValues.size()) {
            return null;
        }
        return mcVersionValues.get(idx);
    }

    // ---- Browsing -----------------------------------------------------------

    private void loadBrowsePage() {
        String query = browseSearchField.getText().trim();
        String sortIndex = SORT_VALUES[Math.max(0, sortCombo.getSelectedIndex())];
        String gameVersion = selectedMcVersion(mcVersionCombo);
        int offset = currentPage * PAGE_SIZE;
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        statusLabel.setText(I18n.t("status.loading"));
        setBusy(true);

        new SwingWorker<BrowseResult, Void>() {
            @Override
            protected BrowseResult doInBackground() throws Exception {
                return modrinthClient.browseFabricMods(query, sortIndex, offset, PAGE_SIZE, gameVersion);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    BrowseResult result = get();
                    browseListModel.clear();
                    for (ModrinthSearchHit hit : result.hits) {
                        browseListModel.addElement(hit);
                    }
                    totalHits = result.totalHits;
                    statusLabel.setText(browseListModel.isEmpty() ? I18n.t("status.no_results") : " ");
                } catch (Exception e) {
                    Logger.error("Network request failed", e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
                updatePagination();
            }
        }.execute();
    }

    private void updatePagination() {
        int totalPages = Math.max(1, (int) Math.ceil(totalHits / (double) PAGE_SIZE));
        pageLabel.setText(String.format(I18n.t("pagination.page"), currentPage + 1, totalPages));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled((currentPage + 1) * PAGE_SIZE < totalHits);
    }

    private void addBrowseSelectionToInstallList() {
        if (!pathConfirmed) {
            notifyUser(I18n.t("status.choose_path_first"), Toast.Kind.INFO);
            return;
        }
        ModrinthSearchHit selected = browseList.getSelectedValue();
        if (selected != null) {
            pickVersionAndAdd(selected);
        }
    }

    /** Fetches the mod's versions (filtered to the browse tab's selected MC version, if any) and lets the user pick one. */
    private void pickVersionAndAdd(ModrinthSearchHit hit) {
        if (!pathConfirmed) {
            notifyUser(I18n.t("status.choose_path_first"), Toast.Kind.INFO);
            return;
        }
        String gameVersion = selectedMcVersion(mcVersionCombo);
        setBusy(true);

        new SwingWorker<List<ModrinthVersion>, Void>() {
            @Override
            protected List<ModrinthVersion> doInBackground() throws Exception {
                List<ModrinthVersion> versions = modrinthClient.getFabricVersions(hit.id);
                if (gameVersion != null) {
                    List<ModrinthVersion> filtered = versions.stream()
                            .filter(v -> v.gameVersions.contains(gameVersion))
                            .toList();
                    if (!filtered.isEmpty()) {
                        return filtered;
                    }
                }
                return versions;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<ModrinthVersion> versions = get();
                    if (versions.isEmpty()) {
                        notifyUser(I18n.t("status.no_compatible_version_for_mod"), Toast.Kind.INFO);
                        return;
                    }
                    ModrinthVersion chosen = promptVersionSelection(hit, versions);
                    if (chosen != null) {
                        addToInstallList(new QueuedMod(hit, chosen));
                    }
                } catch (Exception e) {
                    Logger.error("Failed to fetch versions for " + hit, e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    /** Shows every version as a row (number, game versions, date) and returns the one the user picked, or null if cancelled. */
    private ModrinthVersion promptVersionSelection(ModrinthSearchHit hit, List<ModrinthVersion> versions) {
        DefaultListModel<ModrinthVersion> listModel = new DefaultListModel<>();
        for (ModrinthVersion v : versions) {
            listModel.addElement(v);
        }
        JList<ModrinthVersion> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFixedCellHeight(40);
        list.setCellRenderer(new VersionCellRenderer());

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(440, Math.min(300, 20 + versions.size() * 40)));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 400px'>"
                + String.format(I18n.t("dialog.pick_version_body"), hit) + "</body></html>"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        boolean confirmed = AppDialog.confirm(this, I18n.t("dialog.pick_version_title"), panel,
                I18n.t("dialog.cancel"), I18n.t("dialog.continue"));
        return confirmed ? list.getSelectedValue() : null;
    }

    private final class VersionCellRenderer implements ListCellRenderer<ModrinthVersion> {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JLabel versionLabel = new JLabel();
        private final JLabel gameVersionsLabel = new JLabel();

        VersionCellRenderer() {
            panel.setOpaque(true);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            versionLabel.setFont(versionLabel.getFont().deriveFont(Font.BOLD, 13f));
            gameVersionsLabel.setFont(gameVersionsLabel.getFont().deriveFont(11f));
            textPanel.add(versionLabel);
            textPanel.add(gameVersionsLabel);
            panel.add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModrinthVersion> list, ModrinthVersion value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            versionLabel.setText(value.versionNumber);
            gameVersionsLabel.setText(String.join(", ", value.gameVersions));
            panel.setBackground(isSelected ? mix(accent(), pageBg(), 0.22) : pageBg());
            versionLabel.setForeground(pageFg());
            gameVersionsLabel.setForeground(mutedColor());
            return panel;
        }
    }

    /** Adds a mod to the install list, skipping it if the same project is already queued or no directory is confirmed yet. */
    private boolean addToInstallList(QueuedMod queued) {
        if (!pathConfirmed) {
            notifyUser(I18n.t("status.choose_path_first"), Toast.Kind.INFO);
            return false;
        }
        for (int i = 0; i < modListModel.size(); i++) {
            if (modListModel.get(i).hit.id.equals(queued.hit.id)) {
                return false;
            }
        }
        modListModel.addElement(queued);
        return true;
    }

    /** Handles files dropped onto the install list: identifies .jar files via Modrinth and queues recognized ones. */
    private boolean importJarFiles(TransferHandler.TransferSupport support) throws Exception {
        @SuppressWarnings("unchecked")
        List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        List<File> jars = files.stream()
                .filter(f -> f.isFile() && f.getName().toLowerCase().endsWith(".jar"))
                .toList();
        if (jars.isEmpty()) {
            return false;
        }
        identifyAndAddJars(jars);
        return true;
    }

    /** Hashes dropped .jar files and looks each one up on Modrinth in the background — the exact version is already known, so no picker is needed. */
    private void identifyAndAddJars(List<File> jars) {
        setBusy(true);
        statusLabel.setText(I18n.t("status.checking"));

        new SwingWorker<List<QueuedMod>, Void>() {
            @Override
            protected List<QueuedMod> doInBackground() {
                List<QueuedMod> queued = new ArrayList<>();
                for (File jar : jars) {
                    try {
                        String hash = sha1Hex(jar.toPath());
                        ModrinthVersion existing = modrinthClient.getVersionByFileHash(hash).orElse(null);
                        if (existing != null && existing.projectId != null) {
                            ModrinthSearchHit hit = toSearchHit(existing.projectId, existing.versionNumber);
                            queued.add(new QueuedMod(hit, existing));
                        }
                    } catch (Exception ignored) {
                        // Unreadable file or unknown to Modrinth: skip it.
                    }
                }
                return queued;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<QueuedMod> queued = get();
                    int added = 0;
                    for (QueuedMod q : queued) {
                        if (addToInstallList(q)) {
                            added++;
                        }
                    }
                    notifyUser(added > 0
                                    ? String.format(I18n.t("status.loaded_from_folder"), added)
                                    : I18n.t("status.no_results"),
                            added > 0 ? Toast.Kind.SUCCESS : Toast.Kind.INFO);
                } catch (Exception e) {
                    Logger.error("Network request failed", e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void removeSelectedMod() {
        int selectedIndex = modList.getSelectedIndex();
        if (selectedIndex != -1) {
            modListModel.remove(selectedIndex);
        }
    }

    /** Parses an Explorer-style path (quotes, %ENV% variables) and resolves it only if it's a real directory. */
    static File parsePath(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        trimmed = expandEnvVars(trimmed);
        if (trimmed.isEmpty()) {
            return null;
        }
        File file = new File(trimmed);
        return file.isDirectory() ? file : null;
    }

    private static String expandEnvVars(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("%([^%]+)%").matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = System.getenv(matcher.group(1));
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Clears a field's error outline as soon as the user edits it again. */
    static DocumentListener clearOutlineOnEdit(JTextField field) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                field.putClientProperty(FlatClientProperties.OUTLINE, null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                field.putClientProperty(FlatClientProperties.OUTLINE, null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                field.putClientProperty(FlatClientProperties.OUTLINE, null);
            }
        };
    }

    /** Lets a folder be dropped onto the field to set it, without losing normal text paste/drag behavior. */
    private void installFolderDropTarget(JTextField field) {
        TransferHandler original = field.getTransferHandler();
        field.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || original.canImport(support);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        File dir = files.stream().filter(File::isDirectory).findFirst().orElse(null);
                        if (dir != null) {
                            field.setText(dir.getAbsolutePath());
                            field.putClientProperty(FlatClientProperties.OUTLINE, null);
                            confirmPath(dir);
                            return true;
                        }
                    } catch (Exception ignored) {
                        // Fall through to the original handler below.
                    }
                }
                return original.importData(support);
            }
        });
    }

    private void chooseDirectory() {
        String initial = pathField.getText().trim();
        browseDirButton.setEnabled(false);
        setBusy(true);

        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return NativeFolderPicker.pick(initial);
            }

            @Override
            protected void done() {
                setBusy(false);
                browseDirButton.setEnabled(true);
                try {
                    File selected = get();
                    if (selected != null) {
                        pathField.setText(selected.getAbsolutePath());
                        confirmPath(selected);
                    }
                } catch (Exception e) {
                    Logger.error("Folder picker failed", e);
                    notifyUser(I18n.t("status.folder_picker_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    /** Populates the install list from a folder's manifest, or scans it fresh (and writes the manifest) if none exists yet. */
    /** Scans the mods folder (manifest first, hash-scan as fallback) to populate the Update tab's installed-mod list. */
    private void loadModsFromDirectory(Path dir) {
        statusLabel.setText(I18n.t("status.checking"));
        setBusy(true);

        new SwingWorker<List<UpdateRow>, Void>() {
            @Override
            protected List<UpdateRow> doInBackground() {
                List<UpdateRow> rows = new ArrayList<>();
                InstallManifest manifest = InstallManifest.load(dir);
                if (!manifest.mods.isEmpty()) {
                    for (InstalledMod m : manifest.mods) {
                        Path filePath = m.fileName != null ? dir.resolve(m.fileName) : null;
                        rows.add(new UpdateRow(toSearchHit(m.projectId, m.title), m.versionId, m.versionNumber, filePath));
                    }
                    return rows;
                }

                if (!Files.isDirectory(dir)) {
                    return rows;
                }

                List<Path> jars;
                try (var stream = Files.list(dir)) {
                    jars = stream.filter(p -> p.toString().toLowerCase().endsWith(".jar")).toList();
                } catch (IOException e) {
                    jars = List.of();
                }

                InstallManifest freshManifest = new InstallManifest();
                for (Path jar : jars) {
                    try {
                        String hash = sha1Hex(jar);
                        ModrinthVersion existing = modrinthClient.getVersionByFileHash(hash).orElse(null);
                        if (existing == null || existing.projectId == null) {
                            continue;
                        }
                        ModrinthSearchHit hit = toSearchHit(existing.projectId, existing.versionNumber);
                        rows.add(new UpdateRow(hit, existing.id, existing.versionNumber, jar));
                        recordInstalledMod(freshManifest, existing, jar.getFileName().toString(), hash, hit.toString());
                    } catch (Exception ignored) {
                        // Unreadable file or unknown to Modrinth: skip it.
                    }
                }
                if (!freshManifest.mods.isEmpty()) {
                    try {
                        freshManifest.save(dir);
                    } catch (IOException ignored) {
                        // Listing still works even if the manifest couldn't be written.
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<UpdateRow> rows = get();
                    updateListModel.clear();
                    for (UpdateRow row : rows) {
                        updateListModel.addElement(row);
                    }
                    if (rows.isEmpty()) {
                        statusLabel.setText(" ");
                    } else {
                        notifyUser(String.format(I18n.t("status.loaded_from_folder"), rows.size()), Toast.Kind.SUCCESS);
                    }
                } catch (Exception e) {
                    Logger.error("Network request failed", e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    /** Resolves the latest Fabric-compatible version for every row and flags the ones that differ from what's installed. */
    private void checkForUpdates() {
        if (updateListModel.isEmpty()) {
            return;
        }
        String gameVersion = selectedMcVersion(mcVersionComboUpdate);
        List<UpdateRow> rows = Collections.list(updateListModel.elements());
        checkUpdatesButton.setEnabled(false);
        setBusy(true);
        statusLabel.setText(I18n.t("status.checking"));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (UpdateRow row : rows) {
                    try {
                        List<ModrinthVersion> versions = modrinthClient.getFabricVersions(row.hit.id);
                        ModrinthVersion latest = versions.stream()
                                .filter(v -> gameVersion == null || v.gameVersions.contains(gameVersion))
                                .findFirst()
                                .orElse(null);
                        row.latestVersion = latest;
                        row.checked = true;
                        row.hasUpdate = latest != null && !latest.id.equals(row.currentVersionId);
                        row.selectedForUpdate = row.hasUpdate;
                    } catch (Exception e) {
                        Logger.error("Update check failed for " + row.hit, e);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                checkUpdatesButton.setEnabled(true);
                updateList.repaint();
                long updatable = rows.stream().filter(r -> r.hasUpdate).count();
                notifyUser(String.format(I18n.t("status.updates_found"), updatable),
                        updatable > 0 ? Toast.Kind.INFO : Toast.Kind.SUCCESS);
            }
        }.execute();
    }

    /** Sends the checked rows through the same incompatibility/conflict/archive pipeline used for fresh installs. */
    private void runUpdateSelected() {
        List<DownloadPlan> plans = new ArrayList<>();
        for (int i = 0; i < updateListModel.size(); i++) {
            UpdateRow row = updateListModel.get(i);
            if (row.hasUpdate && row.selectedForUpdate && row.latestVersion != null) {
                plans.add(new DownloadPlan(row.hit, row.latestVersion));
            }
        }
        if (plans.isEmpty()) {
            notifyUser(I18n.t("status.empty_list"), Toast.Kind.INFO);
            return;
        }
        String dirText = pathField.getText().trim();
        if (!pathConfirmed || dirText.isEmpty()) {
            notifyUser(I18n.t("status.empty_path"), Toast.Kind.INFO);
            return;
        }

        Path targetDir = Path.of(dirText);
        updateSelectedButton.setEnabled(false);
        setBusy(true);

        List<Incompatibility> incompatibilities = findIncompatibilities(plans);
        if (!incompatibilities.isEmpty()) {
            setBusy(false);
            Optional<Set<DownloadPlan>> toDrop = resolveIncompatibilities(incompatibilities);
            if (toDrop.isEmpty()) {
                updateSelectedButton.setEnabled(true);
                notifyUser(I18n.t("status.cancelled"), Toast.Kind.INFO);
                return;
            }
            plans.removeIf(toDrop.get()::contains);
            setBusy(true);
        }
        scanForConflictsThenInstall(targetDir, plans);
    }

    /** Deletes the rows currently selected in the installed-mods list (native JList multi-select), after confirmation. */
    private void deleteSelectedInstalledMods() {
        List<UpdateRow> selected = updateList.getSelectedValuesList();
        if (selected.isEmpty()) {
            notifyUser(I18n.t("status.empty_list"), Toast.Kind.INFO);
            return;
        }
        String dirText = pathField.getText().trim();
        if (!pathConfirmed || dirText.isEmpty()) {
            notifyUser(I18n.t("status.empty_path"), Toast.Kind.INFO);
            return;
        }
        if (!confirmDeleteInstalledMods(selected)) {
            return;
        }

        Path targetDir = Path.of(dirText);
        deleteInstalledButton.setEnabled(false);
        setBusy(true);

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int success = 0;
                int failed = 0;
                InstallManifest manifest = InstallManifest.load(targetDir);
                for (UpdateRow row : selected) {
                    try {
                        if (row.filePath != null) {
                            Files.deleteIfExists(row.filePath);
                        }
                        manifest.mods.removeIf(m -> row.hit.id.equals(m.projectId));
                        Logger.log("Deleted installed mod " + row.hit);
                        success++;
                    } catch (IOException e) {
                        Logger.error("Could not delete " + row.hit, e);
                        failed++;
                    }
                }
                try {
                    manifest.save(targetDir);
                } catch (IOException e) {
                    Logger.error("Could not save manifest after delete", e);
                }
                return new int[]{success, failed};
            }

            @Override
            protected void done() {
                setBusy(false);
                deleteInstalledButton.setEnabled(true);
                try {
                    int[] result = get();
                    for (UpdateRow row : selected) {
                        updateListModel.removeElement(row);
                    }
                    notifyUser(String.format(I18n.t("status.deleted_mods"), result[0], result[1]),
                            result[1] == 0 ? Toast.Kind.SUCCESS : Toast.Kind.INFO);
                } catch (Exception e) {
                    Logger.error("Delete worker crashed", e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private boolean confirmDeleteInstalledMods(List<UpdateRow> rows) {
        StringBuilder message = new StringBuilder();
        for (UpdateRow row : rows) {
            message.append("• ").append(row.hit).append("\n");
        }

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(pageBg());
        textArea.setForeground(pageFg());
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(420, Math.min(260, 30 + rows.size() * 22)));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 380px'>" + I18n.t("dialog.delete_mods_body") + "</body></html>"),
                BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return AppDialog.confirm(this, I18n.t("dialog.delete_mods_title"), panel,
                I18n.t("dialog.cancel"), I18n.t("dialog.delete_confirm"));
    }

    private ModrinthSearchHit toSearchHit(String projectId, String fallbackTitle) {
        try {
            ModrinthProjectSummary summary = modrinthClient.getProjectSummary(projectId).orElse(null);
            if (summary != null) {
                ModrinthSearchHit hit = new ModrinthSearchHit();
                hit.id = summary.id != null ? summary.id : projectId;
                hit.slug = summary.slug;
                hit.title = summary.title != null ? summary.title : summary.slug;
                hit.description = summary.description;
                hit.iconUrl = summary.iconUrl;
                return hit;
            }
        } catch (Exception ignored) {
            // Fall back to whatever we already know below.
        }
        ModrinthSearchHit hit = new ModrinthSearchHit();
        hit.id = projectId;
        hit.title = fallbackTitle != null ? fallbackTitle : projectId;
        return hit;
    }

    /** A mod queued for install on the Download tab, with the exact version the user picked for it. */
    private static final class QueuedMod {
        final ModrinthSearchHit hit;
        final ModrinthVersion version;

        QueuedMod(ModrinthSearchHit hit, ModrinthVersion version) {
            this.hit = hit;
            this.version = version;
        }

        @Override
        public String toString() {
            return hit.toString();
        }
    }

    // ---- Install --------------------------------------------------------

    private static final class DownloadPlan {
        final ModrinthSearchHit item;
        final ModrinthVersion version;
        Path oldFileToDelete;

        DownloadPlan(ModrinthSearchHit item, ModrinthVersion version) {
            this.item = item;
            this.version = version;
        }
    }

    private static final class VersionConflict {
        final String modTitle;
        final String oldVersionNumber;
        final String newVersionNumber;
        final String oldFileName;
        final boolean downgrade;

        VersionConflict(String modTitle, String oldVersionNumber, String newVersionNumber, String oldFileName, boolean downgrade) {
            this.modTitle = modTitle;
            this.oldVersionNumber = oldVersionNumber;
            this.newVersionNumber = newVersionNumber;
            this.oldFileName = oldFileName;
            this.downgrade = downgrade;
        }
    }

    private static final class DependencySuggestion {
        final String projectId;
        final String title;
        final String dependencyType;
        final ModrinthVersion version;

        DependencySuggestion(String projectId, String title, String dependencyType, ModrinthVersion version) {
            this.projectId = projectId;
            this.title = title;
            this.dependencyType = dependencyType;
            this.version = version;
        }
    }

    private static final class ResolvedItems {
        final List<DownloadPlan> plans;
        final List<DependencySuggestion> suggestions;

        ResolvedItems(List<DownloadPlan> plans, List<DependencySuggestion> suggestions) {
            this.plans = plans;
            this.suggestions = suggestions;
        }
    }

    private static final class PreparedInstall {
        final List<DownloadPlan> plans;
        final List<VersionConflict> conflicts;
        final InstallManifest manifest;

        PreparedInstall(List<DownloadPlan> plans, List<VersionConflict> conflicts, InstallManifest manifest) {
            this.plans = plans;
            this.conflicts = conflicts;
            this.manifest = manifest;
        }
    }

    private void onInstallClicked() {
        if (modListModel.isEmpty()) {
            notifyUser(I18n.t("status.empty_list"), Toast.Kind.INFO);
            return;
        }
        String dirText = pathField.getText().trim();
        if (!pathConfirmed || dirText.isEmpty()) {
            notifyUser(I18n.t("status.empty_path"), Toast.Kind.INFO);
            return;
        }

        Path targetDir = Path.of(dirText);
        List<QueuedMod> items = Collections.list(modListModel.elements());
        String gameVersion = selectedMcVersion(mcVersionCombo);
        installButton.setEnabled(false);
        statusLabel.setText(I18n.t("status.checking"));
        setBusy(true);

        new SwingWorker<ResolvedItems, Void>() {
            @Override
            protected ResolvedItems doInBackground() {
                List<DownloadPlan> plans = new ArrayList<>();
                List<DependencySuggestion> suggestions = new ArrayList<>();
                java.util.Set<String> seenDependencyIds = new java.util.HashSet<>();

                for (QueuedMod item : items) {
                    ModrinthVersion chosen = item.version;
                    plans.add(new DownloadPlan(item.hit, chosen));

                    if (chosen != null && chosen.dependencies != null) {
                        for (ModrinthDependency dep : chosen.dependencies) {
                            if (dep.projectId == null
                                    || !("required".equals(dep.dependencyType) || "optional".equals(dep.dependencyType))
                                    || alreadySelected(items, dep.projectId)
                                    || !seenDependencyIds.add(dep.projectId)) {
                                continue;
                            }
                            try {
                                ModrinthProjectSummary summary = modrinthClient.getProjectSummary(dep.projectId).orElse(null);
                                if (summary == null) {
                                    continue;
                                }
                                // Honor the exact version the mod author pinned, if any — it's what they actually tested against.
                                ModrinthVersion depChosen = dep.versionId != null
                                        ? modrinthClient.getVersion(dep.versionId).orElse(null)
                                        : null;
                                if (depChosen == null) {
                                    List<ModrinthVersion> depVersions = modrinthClient.getFabricVersions(dep.projectId);
                                    depChosen = depVersions.stream()
                                            .filter(v -> gameVersion == null || v.gameVersions.contains(gameVersion))
                                            .findFirst()
                                            .orElse(null);
                                }
                                String title = summary.title != null ? summary.title : summary.slug;
                                suggestions.add(new DependencySuggestion(dep.projectId, title, dep.dependencyType, depChosen));
                            } catch (Exception ignored) {
                                // Dependency lookup is best-effort; skip it if Modrinth can't be reached.
                            }
                        }
                    }
                }
                return new ResolvedItems(plans, suggestions);
            }

            @Override
            protected void done() {
                ResolvedItems resolved;
                try {
                    resolved = get();
                } catch (Exception e) {
                    Logger.error("Network request failed", e);
                    setBusy(false);
                    installButton.setEnabled(true);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                    return;
                }

                List<DownloadPlan> plans = resolved.plans;
                if (!resolved.suggestions.isEmpty()) {
                    setBusy(false);
                    List<DependencySuggestion> accepted = promptDependencySelection(resolved.suggestions);
                    if (accepted == null) {
                        installButton.setEnabled(true);
                        notifyUser(I18n.t("status.cancelled"), Toast.Kind.INFO);
                        return;
                    }
                    setBusy(true);
                    for (DependencySuggestion s : accepted) {
                        if (s.version == null) {
                            continue;
                        }
                        ModrinthSearchHit hit = new ModrinthSearchHit();
                        hit.id = s.projectId;
                        hit.title = s.title;
                        plans.add(new DownloadPlan(hit, s.version));
                    }
                }

                List<Incompatibility> incompatibilities = findIncompatibilities(plans);
                if (!incompatibilities.isEmpty()) {
                    setBusy(false);
                    Optional<Set<DownloadPlan>> toDrop = resolveIncompatibilities(incompatibilities);
                    if (toDrop.isEmpty()) {
                        installButton.setEnabled(true);
                        notifyUser(I18n.t("status.cancelled"), Toast.Kind.INFO);
                        return;
                    }
                    plans.removeIf(toDrop.get()::contains);
                    setBusy(true);
                }
                scanForConflictsThenInstall(targetDir, plans);
            }
        }.execute();
    }

    private static final class Incompatibility {
        final DownloadPlan planA;
        final DownloadPlan planB;

        Incompatibility(DownloadPlan planA, DownloadPlan planB) {
            this.planA = planA;
            this.planB = planB;
        }
    }

    /** Cross-checks each resolved version's declared "incompatible" dependencies against the rest of the plan. */
    private static List<Incompatibility> findIncompatibilities(List<DownloadPlan> plans) {
        List<Incompatibility> result = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        for (DownloadPlan plan : plans) {
            if (plan.version == null || plan.version.dependencies == null) {
                continue;
            }
            for (ModrinthDependency dep : plan.version.dependencies) {
                if (!"incompatible".equals(dep.dependencyType) || dep.projectId == null) {
                    continue;
                }
                for (DownloadPlan other : plans) {
                    if (other == plan || !dep.projectId.equals(other.item.id)) {
                        continue;
                    }
                    String key = plan.item.id.compareTo(other.item.id) < 0
                            ? plan.item.id + "|" + other.item.id
                            : other.item.id + "|" + plan.item.id;
                    if (seenPairs.add(key)) {
                        result.add(new Incompatibility(plan, other));
                    }
                }
            }
        }
        return result;
    }

    /** Empty = cancel the whole install; otherwise the set of plans the user chose to drop (may be empty if they kept everything). */
    private Optional<Set<DownloadPlan>> resolveIncompatibilities(List<Incompatibility> incompatibilities) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<ButtonGroup> groups = new ArrayList<>();
        List<JRadioButton> dropAButtons = new ArrayList<>();
        List<JRadioButton> dropBButtons = new ArrayList<>();

        for (Incompatibility incompatibility : incompatibilities) {
            JLabel pairLabel = new JLabel(incompatibility.planA.item + "  ⚠  " + incompatibility.planB.item);
            pairLabel.setFont(pairLabel.getFont().deriveFont(Font.BOLD, 13f));
            pairLabel.setForeground(pageFg());
            pairLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            pairLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));

            JRadioButton dropA = new JRadioButton(String.format(I18n.t("dialog.incompatible_drop"), incompatibility.planA.item));
            JRadioButton dropB = new JRadioButton(String.format(I18n.t("dialog.incompatible_drop"), incompatibility.planB.item));
            JRadioButton keepBoth = new JRadioButton(I18n.t("dialog.incompatible_keep_both"));
            keepBoth.setSelected(true);
            for (JRadioButton rb : List.of(dropA, dropB, keepBoth)) {
                rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                rb.setForeground(pageFg());
            }

            ButtonGroup group = new ButtonGroup();
            group.add(dropA);
            group.add(dropB);
            group.add(keepBoth);
            groups.add(group);
            dropAButtons.add(dropA);
            dropBButtons.add(dropB);

            listPanel.add(pairLabel);
            listPanel.add(dropA);
            listPanel.add(dropB);
            listPanel.add(keepBoth);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(460, Math.min(320, 20 + incompatibilities.size() * 100)));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 420px'>" + I18n.t("dialog.incompatible_body") + "</body></html>"),
                BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        boolean confirmed = AppDialog.confirm(this, I18n.t("dialog.incompatible_title"), panel,
                I18n.t("dialog.cancel"), I18n.t("dialog.continue"));
        if (!confirmed) {
            return Optional.empty();
        }

        Set<DownloadPlan> toDrop = new java.util.HashSet<>();
        for (int i = 0; i < incompatibilities.size(); i++) {
            if (dropAButtons.get(i).isSelected()) {
                toDrop.add(incompatibilities.get(i).planA);
            } else if (dropBButtons.get(i).isSelected()) {
                toDrop.add(incompatibilities.get(i).planB);
            }
        }
        return Optional.of(toDrop);
    }

    private static boolean alreadySelected(List<QueuedMod> items, String projectId) {
        for (QueuedMod item : items) {
            if (projectId.equals(item.hit.id)) {
                return true;
            }
        }
        return false;
    }

    private List<DependencySuggestion> promptDependencySelection(List<DependencySuggestion> suggestions) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (DependencySuggestion s : suggestions) {
            boolean required = "required".equals(s.dependencyType);
            String suffix = required ? I18n.t("dependency.required") : I18n.t("dependency.optional");
            JCheckBox checkBox = new JCheckBox(s.title + "  (" + suffix + ")");
            checkBox.setSelected(required);
            checkBoxes.add(checkBox);
            listPanel.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(420, Math.min(280, 10 + suggestions.size() * 28)));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("<html><body style='width: 380px'>" + I18n.t("dialog.dependencies_body") + "</body></html>"),
                BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        boolean confirmed = AppDialog.confirm(this, I18n.t("dialog.dependencies_title"), panel,
                I18n.t("dialog.cancel"), I18n.t("dialog.continue"));
        if (!confirmed) {
            return null;
        }

        List<DependencySuggestion> accepted = new ArrayList<>();
        for (int i = 0; i < suggestions.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                accepted.add(suggestions.get(i));
            }
        }
        return accepted;
    }

    private void scanForConflictsThenInstall(Path targetDir, List<DownloadPlan> plans) {
        statusLabel.setText(I18n.t("status.checking"));

        new SwingWorker<PreparedInstall, Void>() {
            @Override
            protected PreparedInstall doInBackground() {
                InstallManifest manifest = InstallManifest.load(targetDir);
                List<VersionConflict> conflicts = new ArrayList<>();
                if (Files.isDirectory(targetDir)) {
                    List<Path> jars;
                    try (var stream = Files.list(targetDir)) {
                        jars = stream.filter(p -> p.toString().toLowerCase().endsWith(".jar")).toList();
                    } catch (IOException e) {
                        jars = List.of();
                    }
                    for (Path jar : jars) {
                        try {
                            String hash = sha1Hex(jar);
                            ModrinthVersion existing = modrinthClient.getVersionByFileHash(hash).orElse(null);
                            if (existing == null || existing.projectId == null) {
                                continue;
                            }

                            recordInstalledMod(manifest, existing, jar.getFileName().toString(), hash,
                                    resolveTitle(plans, existing.projectId));

                            for (DownloadPlan plan : plans) {
                                if (plan.version == null
                                        || !existing.projectId.equals(plan.item.id)
                                        || existing.id.equals(plan.version.id)) {
                                    continue;
                                }
                                int cmp = safe(existing.datePublished).compareTo(safe(plan.version.datePublished));
                                if (cmp == 0) {
                                    continue;
                                }
                                plan.oldFileToDelete = jar;
                                conflicts.add(new VersionConflict(plan.item.toString(), existing.versionNumber,
                                        plan.version.versionNumber, jar.getFileName().toString(), cmp > 0));
                            }
                        } catch (Exception ignored) {
                            // Unreadable file or unknown to Modrinth: nothing to compare it against.
                        }
                    }
                }
                return new PreparedInstall(plans, conflicts, manifest);
            }

            @Override
            protected void done() {
                PreparedInstall prepared;
                try {
                    prepared = get();
                } catch (Exception e) {
                    Logger.error("Network request failed", e);
                    setBusy(false);
                    enableActionButtons();
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                    return;
                }
                boolean archiveOldFiles = false;
                if (!prepared.conflicts.isEmpty()) {
                    setBusy(false);
                    Optional<Boolean> resolution = confirmVersionChange(prepared.conflicts);
                    if (resolution.isEmpty()) {
                        enableActionButtons();
                        notifyUser(I18n.t("status.cancelled"), Toast.Kind.INFO);
                        return;
                    }
                    archiveOldFiles = resolution.get();
                    setBusy(true);
                }
                runDownloads(targetDir, prepared.plans, prepared.manifest, archiveOldFiles);
            }
        }.execute();
    }

    private static String resolveTitle(List<DownloadPlan> plans, String projectId) {
        for (DownloadPlan plan : plans) {
            if (projectId.equals(plan.item.id)) {
                return plan.item.toString();
            }
        }
        return projectId;
    }

    private void recordInstalledMod(InstallManifest manifest, ModrinthVersion version, String fileName, String sha1, String title) {
        InstalledMod entry = new InstalledMod();
        entry.projectId = version.projectId;
        entry.title = title;
        entry.versionId = version.id;
        entry.versionNumber = version.versionNumber;
        entry.gameVersions = version.gameVersions;
        entry.fileName = fileName;
        entry.sha1 = sha1;
        entry.installedAt = manifest.installedAtOrNow(version.projectId);
        manifest.put(entry);
    }

    /** Empty = cancelled; otherwise true means archive the replaced files instead of deleting them. */
    private Optional<Boolean> confirmVersionChange(List<VersionConflict> conflicts) {
        StringBuilder message = new StringBuilder();
        for (VersionConflict c : conflicts) {
            String arrow = c.downgrade ? "↓ " : "↑ ";
            message.append(arrow).append(c.modTitle).append(": ")
                    .append(c.oldVersionNumber).append(" → ").append(c.newVersionNumber)
                    .append("  (").append(c.oldFileName).append(")\n");
        }

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(pageBg());
        textArea.setForeground(pageFg());
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(520, Math.min(320, 30 + conflicts.size() * 22)));

        JCheckBox archiveCheckbox = new JCheckBox(I18n.t("dialog.archive_checkbox"));
        archiveCheckbox.setOpaque(false);
        archiveCheckbox.setForeground(pageFg());
        archiveCheckbox.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(archiveCheckbox, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 480px'>" + I18n.t("dialog.version_change_body") + "</body></html>"),
                BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        boolean confirmed = AppDialog.confirm(this, I18n.t("dialog.version_change_title"), panel,
                I18n.t("dialog.cancel"), I18n.t("dialog.continue"));
        if (!confirmed) {
            return Optional.empty();
        }
        return Optional.of(archiveCheckbox.isSelected());
    }

    private static final class InstallOutcome {
        int success;
        int failed;
        final List<String> failureDetails = new ArrayList<>();
    }

    private void runDownloads(Path targetDir, List<DownloadPlan> plans, InstallManifest manifest, boolean archiveOldFiles) {
        Logger.log("Install started: " + plans.size() + " mod(s) -> " + targetDir);

        new SwingWorker<InstallOutcome, String>() {
            @Override
            protected InstallOutcome doInBackground() {
                InstallOutcome outcome = new InstallOutcome();
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    Logger.error("Could not create install directory " + targetDir, e);
                    outcome.failed = plans.size();
                    publish(I18n.t("status.cannot_create_dir"));
                    return outcome;
                }

                for (DownloadPlan plan : plans) {
                    publish(I18n.t("status.downloading") + " " + plan.item);
                    try {
                        ModrinthFile file = plan.version == null ? null : plan.version.files.stream()
                                .filter(f -> f.primary)
                                .findFirst()
                                .orElseGet(() -> plan.version.files.isEmpty() ? null : plan.version.files.get(0));
                        if (file == null) {
                            outcome.failed++;
                            String reason = I18n.t("log.no_compatible_version");
                            outcome.failureDetails.add(plan.item + ": " + reason);
                            Logger.log("Skipped " + plan.item + ": " + reason);
                            continue;
                        }
                        Path downloaded = targetDir.resolve(file.filename);
                        modrinthClient.downloadFile(file.url, downloaded);
                        if (plan.oldFileToDelete != null) {
                            try {
                                if (archiveOldFiles) {
                                    archiveOldFile(targetDir, plan.oldFileToDelete);
                                } else {
                                    Files.deleteIfExists(plan.oldFileToDelete);
                                }
                            } catch (IOException e) {
                                Logger.error("Could not clean up old file for " + plan.item, e);
                            }
                        }
                        try {
                            recordInstalledMod(manifest, plan.version, file.filename, sha1Hex(downloaded), plan.item.toString());
                        } catch (Exception ignored) {
                            // Manifest bookkeeping is a convenience; the mod itself installed fine.
                        }
                        outcome.success++;
                        Logger.log("Installed " + plan.item + " (" + file.filename + ")");
                    } catch (Exception e) {
                        outcome.failed++;
                        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        outcome.failureDetails.add(plan.item + ": " + reason);
                        Logger.error("Install failed for " + plan.item, e);
                    }
                }
                try {
                    manifest.save(targetDir);
                } catch (IOException e) {
                    Logger.error("Could not write install manifest", e);
                }
                Logger.log("Install finished: " + outcome.success + " succeeded, " + outcome.failed + " failed");
                return outcome;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                enableActionButtons();
                try {
                    InstallOutcome outcome = get();
                    Toast.Kind kind = outcome.failed == 0 ? Toast.Kind.SUCCESS : Toast.Kind.INFO;
                    notifyUser(String.format(I18n.t("status.install_done"), outcome.success, outcome.failed), kind);
                    if (!outcome.failureDetails.isEmpty()) {
                        showFailureDetails(outcome.failureDetails);
                    }
                } catch (Exception e) {
                    Logger.error("Install worker crashed", e);
                    notifyUser(I18n.t("status.network_error"), Toast.Kind.ERROR);
                }
            }
        }.execute();
    }

    private void showFailureDetails(List<String> failureDetails) {
        JTextArea textArea = new JTextArea(String.join("\n", failureDetails));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(pageBg());
        textArea.setForeground(pageFg());
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(480, Math.min(280, 30 + failureDetails.size() * 22)));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 440px'>"
                + String.format(I18n.t("dialog.errors_body"), Logger.file().toAbsolutePath())
                + "</body></html>"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        AppDialog.info(this, I18n.t("dialog.errors_title"), panel, I18n.t("dialog.close"));
    }

    private static String sha1Hex(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(Files.readAllBytes(file));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Moves a replaced mod file into an "archive" subfolder instead of deleting it, renaming on collision. */
    private static void archiveOldFile(Path targetDir, Path oldFile) throws IOException {
        Path archiveDir = targetDir.resolve("archive");
        Files.createDirectories(archiveDir);
        String fileName = oldFile.getFileName().toString();
        Path dest = archiveDir.resolve(fileName);
        if (Files.exists(dest)) {
            String base = fileName.replaceFirst("(?i)\\.jar$", "");
            dest = archiveDir.resolve(base + "-" + System.currentTimeMillis() + ".jar");
        }
        Files.move(oldFile, dest, StandardCopyOption.REPLACE_EXISTING);
    }
}
