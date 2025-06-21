package org.foxesworld.iconParser.app.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Component that displays an icon in either grid or list view.
 */
public class IconComponent extends JPanel {
    private final BufferedImage icon;
    private final int index;
    private final boolean isGridView;
    private final float zoomFactor;
    private boolean isSelected;

    private final List<Runnable> actionListeners = new ArrayList<>();

    /**
     * Creates a new icon component.
     *
     * @param icon The icon to display
     * @param index The index of the icon
     * @param isGridView Whether to display in grid view
     * @param zoomFactor The zoom factor
     * @param isSelected Whether the icon is selected
     */
    public IconComponent(BufferedImage icon, int index, boolean isGridView,
                         float zoomFactor, boolean isSelected) {
        this.icon = icon;
        this.index = index;
        this.isGridView = isGridView;
        this.zoomFactor = zoomFactor;
        this.isSelected = isSelected;

        initializeComponent();
    }

    /**
     * Initializes the component's UI.
     */
    private void initializeComponent() {
        setBorder(new EmptyBorder(5, 5, 5, 5));

        if (isGridView) {
            initializeGridView();
        } else {
            initializeListView();
        }

        if (isSelected) {
            setBackground(UIManager.getColor("Component.focusColor"));
        }

        // Mouse listeners for hover and click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                notifyActionListeners();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    setBackground(UIManager.getColor("Component.hoverBackground"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    setBackground(UIManager.getColor("Panel.background"));
                }
            }
        });
    }

    /**
     * Initializes the grid view layout.
     */
    private void initializeGridView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Scale icon for display
        int displayWidth = (int)(icon.getWidth() * zoomFactor);
        int displayHeight = (int)(icon.getHeight() * zoomFactor);

        // Create scaled icon
        ImageIcon scaledIcon = createScaledIcon(displayWidth, displayHeight);

        JLabel imageLabel = new JLabel(scaledIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Size label
        JLabel sizeLabel = new JLabel(icon.getWidth() + " × " + icon.getHeight());
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(10f));
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(imageLabel);
        add(Box.createVerticalStrut(3));
        add(sizeLabel);
    }

    /**
     * Initializes the list view layout.
     */
    private void initializeListView() {
        setLayout(new BorderLayout(10, 0));

        // Scale icon for display (fixed height in list view)
        int displayHeight = (int)(32 * zoomFactor);
        int displayWidth = (int)((icon.getWidth() * displayHeight) / (double)icon.getHeight());

        // Create scaled icon
        ImageIcon scaledIcon = createScaledIcon(displayWidth, displayHeight);

        JLabel imageLabel = new JLabel(scaledIcon);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        // Details panel
        JPanel detailsContainer = new JPanel();
        detailsContainer.setLayout(new BoxLayout(detailsContainer, BoxLayout.Y_AXIS));

        JLabel sizeLabel = new JLabel(icon.getWidth() + " × " + icon.getHeight() + " pixels");
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.BOLD));

        JLabel infoLabel = new JLabel(icon.getColorModel().getPixelSize() + "-bit color depth");

        detailsContainer.add(sizeLabel);
        detailsContainer.add(infoLabel);

        add(imageLabel, BorderLayout.WEST);
        add(detailsContainer, BorderLayout.CENTER);
    }

    /**
     * Creates a scaled icon for display, preserving transparent background.
     */
    private ImageIcon createScaledIcon(int width, int height) {
        if (width <= 0 || height <= 0) {
            return new ImageIcon(icon);
        }

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(icon, 0, 0, width, height, null);
        g.dispose();

        return new ImageIcon(scaled);
    }

    /**
     * Sets whether this component is selected.
     *
     * @param selected true if selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        setBackground(selected ?
                UIManager.getColor("Component.focusColor") :
                UIManager.getColor("Panel.background"));
        repaint();
    }

    /**
     * Adds an action listener to this component.
     *
     * @param listener The listener to add
     */
    public void addActionListener(Runnable listener) {
        actionListeners.add(listener);
    }

    /**
     * Notifies all action listeners.
     */
    private void notifyActionListeners() {
        actionListeners.forEach(Runnable::run);
    }
}