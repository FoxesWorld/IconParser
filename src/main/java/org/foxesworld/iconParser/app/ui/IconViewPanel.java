package org.foxesworld.iconParser.app.ui;

import org.foxesworld.iconParser.app.components.IconComponent;
import org.foxesworld.iconParser.app.components.WrapLayout;
import org.foxesworld.iconParser.app.model.IconModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Panel that displays icons in either grid or list view.
 */
public class IconViewPanel extends JPanel {
    private final IconModel model;
    private boolean isGridView = true;
    private float zoomFactor = 1.0f;

    // UI constants
    private static final int PADDING = 15;

    // Listeners
    private final List<IntConsumer> selectionListeners = new ArrayList<>();

    /**
     * Constructs an IconViewPanel with the given model.
     *
     * @param model The icon model
     */
    public IconViewPanel(IconModel model) {
        this.model = model;
        setupLayout();
    }

    /**
     * Sets up the initial panel layout.
     */
    private void setupLayout() {
        setLayout(new WrapLayout(FlowLayout.LEFT, PADDING, PADDING));
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
    }

    /**
     * Sets the view mode (grid or list).
     *
     * @param isGridView true for grid view, false for list view
     */
    public void setGridView(boolean isGridView) {
        this.isGridView = isGridView;
        refreshView();
    }

    /**
     * Sets the zoom factor for icon display.
     *
     * @param zoomFactor The zoom factor (1.0 = 100%)
     */
    public void setZoomFactor(float zoomFactor) {
        this.zoomFactor = zoomFactor;
        refreshView();
    }

    /**
     * Refreshes the view with current data and settings.
     */
    public void refreshView() {
        removeAll();

        // Set appropriate layout
        if (isGridView) {
            setLayout(new WrapLayout(FlowLayout.LEFT, PADDING, PADDING));
        } else {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        List<BufferedImage> icons = model.getIcons();

        if (icons.isEmpty()) {
            addEmptyMessage();
        } else {
            addIconComponents(icons);
        }

        revalidate();
        repaint();
    }

    /**
     * Adds a message for when no icons are available.
     */
    private void addEmptyMessage() {
        JLabel noIconsLabel = new JLabel("No icons loaded. Open an ICO file or drag and drop one here.");
        noIconsLabel.setHorizontalAlignment(JLabel.CENTER);
        noIconsLabel.setFont(noIconsLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(noIconsLabel);
    }

    /**
     * Adds icon components to the view.
     *
     * @param icons The list of icons to display
     */
    private void addIconComponents(List<BufferedImage> icons) {
        int selectedIndex = model.getSelectedIndex();

        for (int i = 0; i < icons.size(); i++) {
            BufferedImage icon = icons.get(i);
            final int index = i;

            IconComponent iconComponent = new IconComponent(
                    icon, index, isGridView, zoomFactor, index == selectedIndex);

            iconComponent.addActionListener(() -> {
                // Clear previous selection
                for (Component c : getComponents()) {
                    if (c instanceof IconComponent) {
                        ((IconComponent) c).setSelected(false);
                    }
                }

                // Set new selection
                iconComponent.setSelected(true);

                // Notify listeners
                notifySelectionListeners(index);
            });

            add(iconComponent);

            // Add separator in list view
            if (!isGridView && i < icons.size() - 1) {
                JSeparator separator = new JSeparator();
                separator.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(separator);
            }
        }
    }

    /**
     * Adds an icon selection listener.
     *
     * @param listener The listener to add
     */
    public void addIconSelectionListener(IntConsumer listener) {
        selectionListeners.add(listener);
    }

    /**
     * Notifies all selection listeners.
     *
     * @param index The selected index
     */
    private void notifySelectionListeners(int index) {
        selectionListeners.forEach(listener -> listener.accept(index));
    }
}