package org.foxesworld.iconParser.app.components;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;

/**
 * Manages application themes.
 */
public class ThemeManager {

    private ThemeManager() {
        // Utility class, no instantiation
    }

    /**
     * Sets the application theme.
     *
     * @param theme The theme name to set
     */
    public static void setTheme(String theme) {
        try {
            switch (theme) {
                case "Dark" -> FlatDarkLaf.setup();
                case "Material Light" -> FlatMaterialLighterIJTheme.setup();
                case "Material Dark" -> FlatMaterialDarkerIJTheme.setup();
                default -> FlatLightLaf.setup();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to light theme
            FlatLightLaf.setup();
        }
    }
}