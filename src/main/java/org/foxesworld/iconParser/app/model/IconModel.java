package org.foxesworld.iconParser.app.model;

import org.foxesworld.iconParser.app.util.IconExporter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Model class for managing icon data and selection state.
 */
public class IconModel {
    private List<BufferedImage> icons = Collections.emptyList();
    private File sourceFile;
    private int selectedIndex = -1;

    private final List<BiConsumer<BufferedImage, Integer>> selectionListeners = new ArrayList<>();
    private final List<BiConsumer<File, Integer>> loadListeners = new ArrayList<>();

    /**
     * Sets the list of icons and their source file.
     *
     * @param icons The list of icon images
     * @param file The source ICO file
     */
    public void setIcons(List<BufferedImage> icons, File file) {
        this.icons = new ArrayList<>(icons);
        this.sourceFile = file;
        this.selectedIndex = -1;

        // Notify listeners about loaded icons
        notifyLoadListeners();
    }

    /**
     * Gets the current list of icons.
     *
     * @return Unmodifiable list of icons
     */
    public List<BufferedImage> getIcons() {
        return Collections.unmodifiableList(icons);
    }

    /**
     * Gets the source file.
     *
     * @return The ICO file
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the selected icon by index.
     *
     * @param index The index of the selected icon
     */
    public void setSelectedIcon(int index) {
        if (index >= -1 && index < icons.size()) {
            selectedIndex = index;

            // Notify selection listeners
            if (index >= 0) {
                BufferedImage selectedIcon = icons.get(index);
                notifySelectionListeners(selectedIcon, index);
            }
        }
    }

    /**
     * Gets the currently selected icon.
     *
     * @return The selected icon or null if none selected
     */
    public BufferedImage getSelectedIcon() {
        if (selectedIndex >= 0 && selectedIndex < icons.size()) {
            return icons.get(selectedIndex);
        }
        return null;
    }

    /**
     * Gets the index of the selected icon.
     *
     * @return The selected icon index or -1 if none selected
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Exports the currently selected icon using IconExporter.
     */
    public void exportSelectedIcon() {
        if (selectedIndex >= 0 && selectedIndex < icons.size()) {
            IconExporter.exportIcon(icons.get(selectedIndex), sourceFile);
        }
    }

    /**
     * Adds a listener to be notified when an icon is selected.
     *
     * @param listener The selection listener
     */
    public void addIconSelectionListener(BiConsumer<BufferedImage, Integer> listener) {
        selectionListeners.add(listener);
    }

    /**
     * Adds a listener to be notified when icons are loaded.
     *
     * @param listener The load listener
     */
    public void addIconLoadListener(BiConsumer<File, Integer> listener) {
        loadListeners.add(listener);
    }

    /**
     * Notifies all selection listeners.
     */
    private void notifySelectionListeners(BufferedImage icon, int index) {
        selectionListeners.forEach(listener -> listener.accept(icon, index));
    }

    /**
     * Notifies all load listeners.
     */
    private void notifyLoadListeners() {
        if (sourceFile != null) {
            loadListeners.forEach(listener -> listener.accept(sourceFile, icons.size()));
        }
    }
}