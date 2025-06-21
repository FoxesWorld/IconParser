package org.foxesworld.iconParser.app.ui;

import org.foxesworld.iconParser.app.util.FormatUtils;
import org.foxesworld.iconParser.app.util.IconExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel that displays details about the selected icon.
 */
public class IconDetailsPanel extends JPanel {
    private BufferedImage currentIcon;

    /**
     * Constructs an IconDetailsPanel.
     */
    public IconDetailsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        showEmptyState();
    }

    /**
     * Shows the empty state with placeholder message.
     */
    private void showEmptyState() {
        removeAll();

        JLabel titleLabel = new JLabel("Icon Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);

        add(Box.createVerticalStrut(15));

        JLabel placeholderLabel = new JLabel("Select an icon to view details");
        placeholderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(placeholderLabel);

        add(Box.createVerticalGlue());

        revalidate();
        repaint();
    }

    /**
     * Displays details for the selected icon.
     *
     * @param icon The selected icon
     * @param index The index of the icon
     */
    public void displayIconDetails(BufferedImage icon, int index) {
        this.currentIcon = icon;
        removeAll();

        JLabel titleLabel = new JLabel("Icon Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);

        add(Box.createVerticalStrut(15));

        // Preview
        JLabel previewLabel = new JLabel("Preview:");
        previewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(previewLabel);

        add(Box.createVerticalStrut(5));

        JLabel imageLabel = new JLabel(new ImageIcon(icon));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(imageLabel);

        add(Box.createVerticalStrut(15));

        // Size info
        addDetailRow("Index:", String.valueOf(index + 1));
        addDetailRow("Dimensions:", icon.getWidth() + " × " + icon.getHeight() + " pixels");

        // Get color model info
        int bitDepth = icon.getColorModel().getPixelSize();
        boolean hasAlpha = icon.getColorModel().hasAlpha();

        addDetailRow("Color Depth:", bitDepth + " bits");
        addDetailRow("Transparency:", hasAlpha ? "Yes" : "No");
        addDetailRow("Memory Size:", FormatUtils.formatSize(icon.getWidth() * icon.getHeight() * (bitDepth / 8)));

        // Export button
        add(Box.createVerticalStrut(20));

        JButton exportButton = new JButton("Export This Icon...");
        exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportButton.addActionListener(e -> exportIcon());
        add(exportButton);

        add(Box.createVerticalGlue());

        revalidate();
        repaint();
    }

    /**
     * Adds a row of information to the details panel.
     *
     * @param label The label text
     * @param value The value text
     */
    private void addDetailRow(String label, String value) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setPreferredSize(new Dimension(100, labelComponent.getPreferredSize().height));

        JLabel valueComponent = new JLabel(value);

        rowPanel.add(labelComponent);
        rowPanel.add(valueComponent);
        rowPanel.add(Box.createHorizontalGlue());

        add(rowPanel);
        add(Box.createVerticalStrut(5));
    }

    /**
     * Exports the current icon.
     */
    private void exportIcon() {
        if (currentIcon != null) {
            IconExporter.exportIcon(currentIcon, null);
        }
    }
}