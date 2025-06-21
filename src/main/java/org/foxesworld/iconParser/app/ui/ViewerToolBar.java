package org.foxesworld.iconParser.app.ui;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.foxesworld.iconParser.IcoParserApp;
import org.foxesworld.iconParser.app.model.IconModel;
import org.foxesworld.iconParser.app.model.UserPreferences;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Toolbar for the ICO Viewer application.
 */
public class ViewerToolBar extends JToolBar {
    private final IcoParserApp app;
    private final IconModel model;
    private final JToggleButton gridViewButton;
    private final JToggleButton listViewButton;
    private final JSlider zoomSlider;
    private final JComboBox<String> themeComboBox;

    /**
     * Constructs the application toolbar.
     *
     * @param app The main application
     * @param model The icon model
     * @param preferences User preferences
     */
    public ViewerToolBar(IcoParserApp app, IconModel model, UserPreferences preferences) {
        this.app = app;
        this.model = model;

        setFloatable(false);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        // Open button
        JButton openButton = createToolbarButton("Open", "folder-open");
        openButton.addActionListener(e -> app.openIcoFile());
        add(openButton);

        // Export button
        JButton exportButton = createToolbarButton("Export", "save");
        exportButton.addActionListener(e -> model.exportSelectedIcon());
        add(exportButton);

        addSeparator();

        // View toggle buttons
        ButtonGroup viewGroup = new ButtonGroup();

        gridViewButton = new JToggleButton();
        gridViewButton.setIcon(new FlatSVGIcon("icons/gridView.svg", 20,20));
        gridViewButton.setToolTipText("Grid View");
        gridViewButton.addActionListener(e -> app.setViewMode(true));

        listViewButton = new JToggleButton();
        listViewButton.setIcon(new FlatSVGIcon("icons/listView.svg", 20, 20));
        listViewButton.setToolTipText("List View");
        listViewButton.addActionListener(e -> app.setViewMode(false));

        viewGroup.add(gridViewButton);
        viewGroup.add(listViewButton);
        add(gridViewButton);
        add(listViewButton);

        // Set initial state from preferences
        gridViewButton.setSelected(preferences.isGridView());
        listViewButton.setSelected(!preferences.isGridView());

        addSeparator();

        // Zoom slider
        add(new JLabel("Zoom: "));
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        zoomSlider.setMaximumSize(new Dimension(150, zoomSlider.getPreferredSize().height));
        zoomSlider.addChangeListener(e -> app.setZoomFactor(zoomSlider.getValue() / 100.0f));
        add(zoomSlider);

        addSeparator();

        // Theme selection
        add(new JLabel("Theme: "));
        themeComboBox = new JComboBox<>(new String[]{"Light", "Dark", "Material Light", "Material Dark"});
        themeComboBox.setSelectedItem(preferences.getTheme());
        themeComboBox.addActionListener(e -> {
            String selected = (String) themeComboBox.getSelectedItem();
            app.setTheme(selected);
        });
        add(themeComboBox);
    }

    private JButton createToolbarButton(String tooltip, String iconName) {
        JButton button = new JButton();

        FlatSVGIcon icon = new FlatSVGIcon("icons/" + iconName + ".svg", 20, 20);
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        return button;
    }



    /**
     * Updates the view mode selection.
     *
     * @param isGridView true for grid view, false for list view
     */
    public void setViewMode(boolean isGridView) {
        gridViewButton.setSelected(isGridView);
        listViewButton.setSelected(!isGridView);
    }
}