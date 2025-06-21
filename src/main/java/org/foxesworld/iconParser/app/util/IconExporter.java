package org.foxesworld.iconParser.app.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility for exporting icons to various formats.
 */
public class IconExporter {

    private IconExporter() {
        // Utility class, no instantiation
    }

    /**
     * Exports an icon to a file.
     *
     * @param icon The icon to export
     * @param sourceFile The original source file (for naming suggestions)
     */
    public static void exportIcon(BufferedImage icon, File sourceFile) {
        if (icon == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Icon");

        // Try to use the same directory as the source file
        if (sourceFile != null) {
            fileChooser.setCurrentDirectory(sourceFile.getParentFile());

            // Suggest a filename based on the original file
            String baseName = sourceFile.getName();
            if (baseName.toLowerCase().endsWith(".ico")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            String suggestedName = baseName + "_" + icon.getWidth() + "x" + icon.getHeight() + ".png";
            fileChooser.setSelectedFile(new File(sourceFile.getParentFile(), suggestedName));
        }

        fileChooser.setAcceptAllFileFilterUsed(false);

        // Add export format options
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Image (*.png)", "png");
        FileNameExtensionFilter icoFilter = new FileNameExtensionFilter("ICO File (*.ico)", "ico");

        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(icoFilter);
        fileChooser.setFileFilter(pngFilter); // Default to PNG

        int result = fileChooser.showSaveDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

        // Add extension if missing
        String extension = selectedFilter == pngFilter ? ".png" : ".ico";
        if (!file.getName().toLowerCase().endsWith(extension)) {
            file = new File(file.getAbsolutePath() + extension);
        }

        // Confirm overwrite
        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(null,
                    "The file already exists. Overwrite?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        // Export in background
        File finalFile = file;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (selectedFilter == pngFilter) {
                    exportToPng(icon, finalFile);
                } else {
                    exportToIco(icon, finalFile);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    JOptionPane.showMessageDialog(null,
                            "Icon exported successfully to " + finalFile.getAbsolutePath(),
                            "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Failed to export icon: " + e.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Exports an image to PNG format.
     *
     * @param icon The icon to export
     * @param file The destination file
     * @throws IOException if export fails
     */
    private static void exportToPng(BufferedImage icon, File file) throws IOException {
        ImageIO.write(icon, "png", file);
    }

    /**
     * Exports an image to ICO format.
     *
     * @param icon The icon to export
     * @param file The destination file
     * @throws IOException if export fails
     */
    private static void exportToIco(BufferedImage icon, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Write ICO header
            writeShortLE(fos, 0); // Reserved
            writeShortLE(fos, 1); // Type (1 = ICO)
            writeShortLE(fos, 1); // Number of images

            // Write directory entry
            int width = icon.getWidth();
            int height = icon.getHeight();
            fos.write(width > 255 ? 0 : width); // Width (0 means 256)
            fos.write(height > 255 ? 0 : height); // Height (0 means 256)
            fos.write(0); // Color count (0 means no palette)
            fos.write(0); // Reserved
            writeShortLE(fos, 1); // Color planes
            writeShortLE(fos, 32); // Bits per pixel

            // We'll use PNG format for the embedded image for simplicity
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(icon, "png", baos);
            byte[] pngData = baos.toByteArray();

            writeIntLE(fos, pngData.length); // Image size
            writeIntLE(fos, 22); // Image offset (header + directory entry)

            // Write the image data
            fos.write(pngData);
        }
    }

    /**
     * Writes a short in little endian format.
     */
    private static void writeShortLE(FileOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Writes an int in little endian format.
     */
    private static void writeIntLE(FileOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}