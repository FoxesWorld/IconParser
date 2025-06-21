package org.foxesworld.iconParser;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import org.foxesworld.iconParser.app.components.ThemeManager;
import org.foxesworld.iconParser.app.model.IconModel;
import org.foxesworld.iconParser.app.model.UserPreferences;
import org.foxesworld.iconParser.app.ui.IconDetailsPanel;
import org.foxesworld.iconParser.app.ui.IconViewPanel;
import org.foxesworld.iconParser.app.ui.StatusPanel;
import org.foxesworld.iconParser.app.ui.ViewerToolBar;
import org.foxesworld.iconParser.app.util.FormatUtils;
import org.foxesworld.iconParser.app.util.IconLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

/**
 * Main application class for the ICO Viewer.
 * Acts as a controller coordinating between model and UI components.
 */
public class IcoParserApp extends JFrame {

    // Model
    private final IconModel iconModel;
    private final UserPreferences preferences;

    // UI Components
    private final IconViewPanel iconViewPanel;
    private final IconDetailsPanel detailsPanel;
    private final ViewerToolBar toolBar;
    private final StatusPanel statusPanel;
    private final JSplitPane splitPane;

    // Constants
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 600;

    /**
     * Constructs the main application window.
     */
    public IcoParserApp() {
        setTitle("ICO Viewer Pro");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize model components
        iconModel = new IconModel();
        preferences = new UserPreferences();

        // Initialize UI components
        iconViewPanel = new IconViewPanel(iconModel);
        detailsPanel = new IconDetailsPanel();
        statusPanel = new StatusPanel();
        toolBar = new ViewerToolBar(this, iconModel, preferences);

        // Setup split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(iconViewPanel), detailsPanel);
        splitPane.setResizeWeight(0.8);
        splitPane.setDividerLocation(DEFAULT_WIDTH - 250);
        splitPane.setBorder(null);

        // Setup listeners
        setupEventHandlers();

        // Layout components
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(toolBar, BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);

        // Setup drag and drop
        setupDragAndDrop();

        // Setup keyboard shortcuts
        setupKeyboardShortcuts();

        // Load preferences
        loadUserPreferences();
    }

    /**
     * Sets up event handlers for communication between components.
     */
    private void setupEventHandlers() {
        // Connect model events to UI updates
        iconModel.addIconSelectionListener((icon, index) -> {
            detailsPanel.displayIconDetails(icon, index);
        });

        iconModel.addIconLoadListener((file, iconCount) -> {
            statusPanel.setStatus(file.getName() + " - " + iconCount +
                    " icons loaded (" + FormatUtils.formatSize(file.length()) + ")");
            setTitle("ICO Viewer Pro - " + file.getName());
        });

        // Connect view events to model updates
        iconViewPanel.addIconSelectionListener(iconModel::setSelectedIcon);
    }

    /**
     * Sets up drag and drop functionality for ICO files.
     */
    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    List<?> files = (List<?>) event.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!files.isEmpty() && files.get(0) instanceof File) {
                        File file = (File) files.get(0);
                        if (file.getName().toLowerCase().endsWith(".ico")) {
                            loadIcoFile(file);
                        } else {
                            showError("Please drop a valid ICO file.");
                        }
                    }

                    event.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    event.dropComplete(false);
                }
            }
        });
    }

    /**
     * Sets up keyboard shortcuts for common actions.
     */
    private void setupKeyboardShortcuts() {
        // Open file (Ctrl+O)
        KeyStroke openKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().registerKeyboardAction(e -> openIcoFile(),
                openKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Export selected (Ctrl+E)
        KeyStroke exportKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_E,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().registerKeyboardAction(e -> iconModel.exportSelectedIcon(),
                exportKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Add other keyboard shortcuts as needed
    }

    /**
     * Opens an ICO file selection dialog.
     */
    public void openIcoFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an ICO file");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "ICO Files (*.ico)", "ico"));

        // Set starting directory from preferences
        String lastDir = preferences.getLastDirectory();
        if (lastDir != null) {
            fileChooser.setCurrentDirectory(new File(lastDir));
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            preferences.setLastDirectory(file.getParent());
            loadIcoFile(file);
        }
    }

    /**
     * Loads an ICO file and processes it.
     */
    public void loadIcoFile(File file) {
        if (!file.getName().toLowerCase().endsWith(".ico")) {
            showError("Selected file is not an ICO file.");
            return;
        }

        statusPanel.setStatus("Loading " + file.getName() + "...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Use IconLoader to load in background
        IconLoader loader = new IconLoader(file);
        loader.setOnSuccess(icons -> {
            iconModel.setIcons(icons, file);
            iconViewPanel.refreshView();
            setCursor(Cursor.getDefaultCursor());
        });

        loader.setOnFailure(error -> {
            showError("Failed to load ICO file: " + error.getMessage());
            statusPanel.setStatus("Error loading file");
            setCursor(Cursor.getDefaultCursor());
        });

        loader.execute();
    }

    /**
     * Sets the view mode (grid or list).
     */
    public void setViewMode(boolean isGridView) {
        iconViewPanel.setGridView(isGridView);
        preferences.setGridView(isGridView);
    }

    /**
     * Sets the zoom level.
     */
    public void setZoomFactor(float factor) {
        iconViewPanel.setZoomFactor(factor);
    }

    /**
     * Changes the application theme.
     */
    public void setTheme(String themeName) {
        FlatAnimatedLafChange.showSnapshot();
        ThemeManager.setTheme(themeName);
        preferences.setTheme(themeName);
        SwingUtilities.updateComponentTreeUI(this);
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    /**
     * Loads saved user preferences.
     */
    private void loadUserPreferences() {
        // Load theme
        setTheme(preferences.getTheme());

        // Load view mode
        boolean isGridView = preferences.isGridView();
        setViewMode(isGridView);
        toolBar.setViewMode(isGridView);
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        // Set system properties for better rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Install FlatLaf as default Look & Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Start application
        SwingUtilities.invokeLater(() -> {
            IcoParserApp app = new IcoParserApp();
            app.setVisible(true);

            // Open file from command line args if provided
            if (args.length > 0 && args[0].toLowerCase().endsWith(".ico")) {
                File file = new File(args[0]);
                if (file.exists()) {
                    app.loadIcoFile(file);
                }
            }
        });
    }
}