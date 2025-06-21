package org.foxesworld.iconParser.app.model;

import java.util.prefs.Preferences;

/**
 * Manages user preferences that persist between application sessions.
 */
public class UserPreferences {
    private final Preferences prefs;

    // Preference keys
    private static final String PREF_THEME = "theme";
    private static final String PREF_VIEW_MODE = "viewMode";
    private static final String PREF_LAST_DIR = "lastDirectory";

    // Default values
    private static final String DEFAULT_THEME = "Light";
    private static final boolean DEFAULT_VIEW_MODE = true; // Grid view

    /**
     * Constructs the UserPreferences manager.
     */
    public UserPreferences() {
        prefs = Preferences.userNodeForPackage(UserPreferences.class);
    }

    /**
     * Gets the saved theme name.
     *
     * @return The theme name
     */
    public String getTheme() {
        return prefs.get(PREF_THEME, DEFAULT_THEME);
    }

    /**
     * Sets the theme name.
     *
     * @param theme The theme name to save
     */
    public void setTheme(String theme) {
        prefs.put(PREF_THEME, theme);
    }

    /**
     * Gets the saved view mode.
     *
     * @return true for grid view, false for list view
     */
    public boolean isGridView() {
        return prefs.getBoolean(PREF_VIEW_MODE, DEFAULT_VIEW_MODE);
    }

    /**
     * Sets the view mode.
     *
     * @param isGridView true for grid view, false for list view
     */
    public void setGridView(boolean isGridView) {
        prefs.putBoolean(PREF_VIEW_MODE, isGridView);
    }

    /**
     * Gets the last directory used for file operations.
     *
     * @return The last directory path or null if not set
     */
    public String getLastDirectory() {
        return prefs.get(PREF_LAST_DIR, null);
    }

    /**
     * Sets the last directory used for file operations.
     *
     * @param path The directory path to save
     */
    public void setLastDirectory(String path) {
        if (path != null) {
            prefs.put(PREF_LAST_DIR, path);
        }
    }
}