package org.foxesworld.iconParser.app.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Status panel for displaying application messages.
 */
public class StatusPanel extends JPanel {
    private final JLabel statusLabel;

    /**
     * Constructs a status panel.
     */
    public StatusPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(3, 8, 3, 8));

        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.WEST);
    }

    /**
     * Sets the status message.
     *
     * @param message The status message to display
     */
    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}