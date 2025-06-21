package org.foxesworld.iconParser.app.util;


/**
 * Utility methods for formatting values.
 */
public class FormatUtils {

    private FormatUtils() {
        // Utility class, no instantiation
    }

    /**
     * Formats a file size in bytes to a human-readable string.
     *
     * @param size Size in bytes
     * @return Formatted size string (e.g., "1.25 MB")
     */
    public static String formatSize(long size) {
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeInUnit = size;

        while (sizeInUnit >= 1024 && unitIndex < units.length - 1) {
            sizeInUnit /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", sizeInUnit, units[unitIndex]);
    }
}