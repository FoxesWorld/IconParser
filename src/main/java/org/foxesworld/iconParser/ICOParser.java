package org.foxesworld.iconParser;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance ICO parser with advanced icon selection capabilities and efficient memory usage.
 * <p>
 * Features:
 * <ul>
 *   <li>Fast, memory-efficient parsing with minimal object creation</li>
 *   <li>Support for embedded PNG icons and BMP-based icons with various bit depths</li>
 *   <li>Multiple icon selection strategies (exact size, best fit, highest quality)</li>
 *   <li>Proper alpha channel and transparency handling</li>
 *   <li>Complete ICO directory information access</li>
 *   <li>Cached image loading for improved performance</li>
 * </ul>
 */
public class ICOParser extends ByteParser<List<BufferedImage>> {

    private static final int DEFAULT_CACHE_SIZE = 16;

    // Cache for parsed icons to avoid repeated decoding of the same data
    private final Map<IconKey, BufferedImage> imageCache;

    // Original ICO data and parsed directory entries
    private byte[] icoData;
    private List<IconDirEntry> entries;

    /**
     * Creates an ICO parser with default settings.
     */
    public ICOParser() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates an ICO parser with specified cache size.
     *
     * @param cacheSize maximum number of decoded images to cache
     * @throws IllegalArgumentException if cacheSize is negative
     */
    public ICOParser(int cacheSize) {
        if (cacheSize < 0) {
            throw new IllegalArgumentException("Cache size cannot be negative");
        }

        if (cacheSize > 0) {
            // Use LoadingCache pattern with size limit
            imageCache = Collections.synchronizedMap(
                    new LinkedHashMap<IconKey, BufferedImage>(16, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<IconKey, BufferedImage> eldest) {
                            return size() > cacheSize;
                        }
                    }
            );
        } else {
            imageCache = new ConcurrentHashMap<>();
        }
    }

    /**
     * Parses ICO file data and extracts all contained images.
     *
     * @param data raw ICO file bytes
     * @return list of decoded BufferedImage instances
     * @throws IOException if parsing fails
     */
    @Override
    protected List<BufferedImage> parseBytes(byte[] data) throws IOException {
        if (data == null || data.length < 6) {
            throw new IOException("Invalid ICO data: too short or null");
        }

        // Store data reference for later use when loading individual images
        this.icoData = data;

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            // Read ICO header
            int reserved = readLEShort(dis);
            int type = readLEShort(dis);
            int count = readLEShort(dis);

            if (reserved != 0 || (type != 1 && type != 2) || count <= 0 || count > 1024) {
                throw new IOException(String.format(
                        "Invalid ICO header: reserved=%d, type=%d, count=%d", reserved, type, count));
            }

            // Read directory entries
            entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int width = dis.readUnsignedByte();
                int height = dis.readUnsignedByte();
                int colorCount = dis.readUnsignedByte();
                int reserved2 = dis.readUnsignedByte();
                int planes = readLEShort(dis);
                int bitCount = readLEShort(dis);
                int bytesInRes = readLEInt(dis);
                int imageOffset = readLEInt(dis);

                // Validate entry bounds
                if (imageOffset < 0 || bytesInRes <= 0 ||
                        imageOffset + bytesInRes > data.length) {
                    throw new IOException(String.format(
                            "Invalid icon entry %d: offset=%d, size=%d exceeds data bounds (%d)",
                            i, imageOffset, bytesInRes, data.length));
                }

                // ICO quirk: 0 means 256
                int actualWidth = width == 0 ? 256 : width;
                int actualHeight = height == 0 ? 256 : height;

                entries.add(new IconDirEntry(
                        actualWidth, actualHeight, colorCount, planes, bitCount,
                        bytesInRes, imageOffset, reserved2
                ));
            }

            // Eagerly load all images
            List<BufferedImage> images = new ArrayList<>(count);
            for (IconDirEntry entry : entries) {
                BufferedImage img = loadIconImage(entry);
                if (img != null) {
                    images.add(img);
                }
            }

            return images;
        }
    }

    /**
     * Loads a specific icon entry as a BufferedImage.
     *
     * @param entry the icon directory entry to load
     * @return the decoded BufferedImage or null if decoding fails
     * @throws IOException if reading fails
     */
    private BufferedImage loadIconImage(IconDirEntry entry) throws IOException {
        if (icoData == null) {
            throw new IllegalStateException("ICO data not loaded. Call parseBytes() first.");
        }

        // Check cache first
        IconKey key = new IconKey(entry.width, entry.height, entry.bitCount);
        BufferedImage cached = imageCache.get(key);
        if (cached != null) {
            return cached;
        }

        // Extract image data
        byte[] imageData = extractImageData(entry);

        // Decode image based on format
        BufferedImage img = decodeIconImage(imageData, entry);

        // Cache the result if successful
        if (img != null) {
            imageCache.put(key, img);
        }

        return img;
    }

    /**
     * Extracts image data for a specific icon entry.
     *
     * @param entry the icon directory entry
     * @return byte array containing the image data
     */
    private byte[] extractImageData(IconDirEntry entry) {
        byte[] imageData = new byte[entry.bytesInRes];
        System.arraycopy(icoData, entry.imageOffset, imageData, 0, entry.bytesInRes);
        return imageData;
    }

    /**
     * Decodes an icon image from its raw data.
     *
     * @param imageData the raw image data
     * @param entry the icon directory entry
     * @return the decoded BufferedImage or null if decoding fails
     * @throws IOException if decoding fails
     */
    private BufferedImage decodeIconImage(byte[] imageData, IconDirEntry entry) throws IOException {
        try {
            // Check for PNG signature
            if (isPng(imageData)) {
                return decodePng(imageData);
            } else {
                return decodeBmp(imageData, entry);
            }
        } catch (Exception e) {
            throw new IOException("Failed to decode icon: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a PNG image from its raw data.
     *
     * @param imageData the raw PNG data
     * @return the decoded BufferedImage
     * @throws IOException if decoding fails
     */
    private BufferedImage decodePng(byte[] imageData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new IOException("Failed to decode PNG data");
            }
            return img;
        }
    }

    /**
     * Decodes a BMP-like icon from its raw data.
     *
     * @param data the raw BMP-like data
     * @param entry the icon directory entry
     * @return the decoded BufferedImage
     * @throws IOException if decoding fails
     */
    private BufferedImage decodeBmp(byte[] data, IconDirEntry entry) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            int headerSize = readLEInt(dis);

            // Handle different DIB header types
            if (headerSize < 12) {
                throw new IOException("Invalid BMP header size: " + headerSize);
            }

            int width = readLEInt(dis);
            int heightWithMask = readLEInt(dis);
            int height = heightWithMask / 2; // Real height excludes AND mask

            // Some ICO files have incorrect height values (should be 2x actual)
            if (height != entry.height && heightWithMask == entry.height) {
                height = entry.height;
            }

            int planes = readLEShort(dis);
            int bitCount = readLEShort(dis);

            // Sanity checks
            if (width <= 0 || height <= 0 || width > 1024 || height > 1024) {
                throw new IOException(String.format(
                        "Invalid dimensions: %dx%d", width, height));
            }

            // Handle different bit depths
            switch (bitCount) {
                case 1:
                    return decode1BitBmp(dis, width, height);
                case 4:
                    return decode4BitBmp(dis, width, height);
                case 8:
                    return decode8BitBmp(dis, width, height);
                case 24:
                case 32:
                    // Skip compression and other header fields
                    dis.skipBytes(headerSize - 16);
                    return decodeTrueColorBmp(dis, width, height, bitCount);
                default:
                    throw new IOException("Unsupported bit depth: " + bitCount);
            }
        }
    }

    /**
     * Decodes a 1-bit BMP icon.
     */
    private BufferedImage decode1BitBmp(DataInputStream dis, int width, int height) throws IOException {
        // Skip rest of header
        dis.skipBytes(24);

        // Read 2-color palette
        byte[] reds = new byte[2];
        byte[] greens = new byte[2];
        byte[] blues = new byte[2];

        for (int i = 0; i < 2; i++) {
            int b = dis.readUnsignedByte();
            int g = dis.readUnsignedByte();
            int r = dis.readUnsignedByte();
            dis.readByte(); // reserved
            blues[i] = (byte) b;
            greens[i] = (byte) g;
            reds[i] = (byte) r;
        }

        // Create indexed color model
        IndexColorModel colorModel = new IndexColorModel(1, 2, reds, greens, blues);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, colorModel);

        // Calculate row stride (padded to 4-byte boundary)
        int rowSize = ((width + 31) / 32) * 4;
        byte[] row = new byte[rowSize];

        // BMP rows are stored bottom to top
        for (int y = height - 1; y >= 0; y--) {
            dis.readFully(row, 0, rowSize);
            // Set each row of data
            img.getRaster().setDataElements(0, y, width, 1, row);
        }

        // Skip the AND mask
        int maskRowSize = ((width + 31) / 32) * 4;
        dis.skipBytes(maskRowSize * height);

        return img;
    }

    /**
     * Decodes a 4-bit BMP icon.
     */
    private BufferedImage decode4BitBmp(DataInputStream dis, int width, int height) throws IOException {
        // Skip rest of header
        dis.skipBytes(24);

        // Read 16-color palette
        byte[] reds = new byte[16];
        byte[] greens = new byte[16];
        byte[] blues = new byte[16];

        for (int i = 0; i < 16; i++) {
            int b = dis.readUnsignedByte();
            int g = dis.readUnsignedByte();
            int r = dis.readUnsignedByte();
            dis.readByte(); // reserved
            blues[i] = (byte) b;
            greens[i] = (byte) g;
            reds[i] = (byte) r;
        }

        // Create indexed color model
        IndexColorModel colorModel = new IndexColorModel(4, 16, reds, greens, blues);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

        // Calculate row stride (padded to 4-byte boundary)
        int rowSize = ((width * 4 + 31) / 32) * 4;
        byte[] row = new byte[rowSize];

        // BMP rows are stored bottom to top
        for (int y = height - 1; y >= 0; y--) {
            dis.readFully(row, 0, rowSize);
            // Set each row of data
            img.getRaster().setDataElements(0, y, width, 1, row);
        }

        // Skip the AND mask
        int maskRowSize = ((width + 31) / 32) * 4;
        dis.skipBytes(maskRowSize * height);

        return img;
    }

    /**
     * Decodes an 8-bit BMP icon.
     */
    private BufferedImage decode8BitBmp(DataInputStream dis, int width, int height) throws IOException {
        // Skip compression and other header fields
        dis.skipBytes(24);

        // Read 256-color palette
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];

        for (int i = 0; i < 256; i++) {
            int b = dis.readUnsignedByte();
            int g = dis.readUnsignedByte();
            int r = dis.readUnsignedByte();
            dis.readByte(); // reserved
            blues[i] = (byte) b;
            greens[i] = (byte) g;
            reds[i] = (byte) r;
        }

        // Create indexed color model
        IndexColorModel colorModel = new IndexColorModel(8, 256, reds, greens, blues);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

        // Calculate row stride (padded to 4-byte boundary)
        int rowSize = ((width * 8 + 31) / 32) * 4;
        byte[] pixels = new byte[width * height];
        byte[] row = new byte[rowSize];

        // BMP rows are stored bottom to top
        for (int y = height - 1; y >= 0; y--) {
            dis.readFully(row, 0, rowSize);
            System.arraycopy(row, 0, pixels, y * width, width);
        }

        img.getRaster().setDataElements(0, 0, width, height, pixels);

        // Skip the AND mask
        int maskRowSize = ((width + 31) / 32) * 4;
        dis.skipBytes(maskRowSize * height);

        return img;
    }

    /**
     * Decodes a 24-bit or 32-bit true color BMP icon.
     */
    private BufferedImage decodeTrueColorBmp(DataInputStream dis, int width, int height, int bitCount) throws IOException {
        boolean hasAlpha = bitCount == 32;

        // RGB(A) image
        BufferedImage img = new BufferedImage(width, height,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        // Calculate row stride (padded to 4-byte boundary)
        int bytesPerPixel = bitCount / 8;
        int rowSize = ((width * bitCount + 31) / 32) * 4;
        byte[] row = new byte[rowSize];

        // BMP rows are stored bottom to top
        for (int y = height - 1; y >= 0; y--) {
            dis.readFully(row, 0, rowSize);
            for (int x = 0; x < width; x++) {
                int offset = x * bytesPerPixel;
                if (offset + bytesPerPixel > row.length) break;

                int b = row[offset] & 0xFF;
                int g = row[offset + 1] & 0xFF;
                int r = row[offset + 2] & 0xFF;
                int a = hasAlpha ? (row[offset + 3] & 0xFF) : 255;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;

                img.setRGB(x, y, argb);
            }
        }

        // If 24-bit, apply the AND mask for transparency
        if (!hasAlpha) {
            applyAndMask(dis, img, width, height);
        }

        return img;
    }

    /**
     * Applies AND mask (transparency mask) to an image.
     */
    private void applyAndMask(DataInputStream dis, BufferedImage img, int width, int height) throws IOException {
        try {
            // Calculate mask row stride (padded to 4-byte boundary)
            int maskRowSize = ((width + 31) / 32) * 4;
            byte[] maskRow = new byte[maskRowSize];

            // Create a new ARGB image
            BufferedImage argbImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // BMP rows are stored bottom to top
            for (int y = height - 1; y >= 0; y--) {
                dis.readFully(maskRow, 0, maskRowSize);
                for (int x = 0; x < width; x++) {
                    int byteIndex = x / 8;
                    int bitIndex = 7 - (x % 8);

                    if (byteIndex >= maskRow.length) {
                        continue;
                    }

                    // Get RGB from original image
                    int rgb = img.getRGB(x, y);

                    // Check if bit is set in mask (1 = transparent)
                    boolean isTransparent = ((maskRow[byteIndex] >> bitIndex) & 1) == 1;

                    // Apply transparency if mask bit is set
                    if (isTransparent) {
                        argbImg.setRGB(x, y, rgb & 0x00FFFFFF); // Set alpha to 0
                    } else {
                        argbImg.setRGB(x, y, rgb | 0xFF000000); // Set alpha to 255
                    }
                }
            }

            // Copy ARGB image data back to original image
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    img.setRGB(x, y, argbImg.getRGB(x, y));
                }
            }
        } catch (IOException e) {
            // If mask reading fails, just keep the original image
            // This is common for some ICO files with truncated masks
        }
    }

    /**
     * Checks if the byte array contains a PNG image.
     *
     * @param data the image data
     * @return true if data has PNG signature
     */
    private static boolean isPng(byte[] data) {
        return data.length >= 8
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50  // P
                && data[2] == 0x4E  // N
                && data[3] == 0x47  // G
                && data[4] == 0x0D
                && data[5] == 0x0A
                && data[6] == 0x1A
                && data[7] == 0x0A;
    }

    /**
     * Gets an icon with exact dimensions (width and height).
     *
     * @param icons list of icons
     * @param width desired width
     * @param height desired height
     * @return matching icon or null if none found
     * @throws IllegalArgumentException if width or height is negative
     */
    public BufferedImage getIconExactSize(List<BufferedImage> icons, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be non-negative");
        }

        if (icons == null || icons.isEmpty()) {
            return null;
        }

        for (BufferedImage icon : icons) {
            if (icon.getWidth() == width && icon.getHeight() == height) {
                return icon;
            }
        }

        return null;
    }

    /**
     * Gets the best matching icon for the given dimensions.
     * Prefers exact matches, then higher resolution icons that can be scaled down,
     * and finally lower resolution icons that would need to be scaled up.
     *
     * @param icons list of icons
     * @param width desired width
     * @param height desired height
     * @return best matching icon or null if no icons available
     * @throws IllegalArgumentException if width or height is negative
     */
    public BufferedImage getBestMatchingIcon(List<BufferedImage> icons, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be non-negative");
        }

        if (icons == null || icons.isEmpty()) {
            return null;
        }

        // First try exact match
        BufferedImage exactMatch = getIconExactSize(icons, width, height);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Try to find closest match
        BufferedImage bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (BufferedImage icon : icons) {
            int iconWidth = icon.getWidth();
            int iconHeight = icon.getHeight();

            // Calculate score (lower is better)
            // We prefer larger icons over smaller ones, since downscaling looks better than upscaling
            int score;
            if (iconWidth >= width && iconHeight >= height) {
                // Larger icon: score = excess area (prefer closest size)
                score = (iconWidth - width) * (iconHeight - height);
            } else {
                // Smaller icon: penalize more heavily
                score = 1000 + (width - iconWidth) * (height - iconHeight);
            }

            if (score < bestScore) {
                bestScore = score;
                bestMatch = icon;
            }
        }

        return bestMatch;
    }

    /**
     * Gets the icon with the highest quality (based on bit depth and dimensions).
     *
     * @param icons list of icons
     * @return highest quality icon or null if none available
     */
    public BufferedImage getHighestQualityIcon(List<BufferedImage> icons) {
        if (icons == null || icons.isEmpty()) {
            return null;
        }

        BufferedImage bestIcon = icons.get(0);
        int bestScore = scoreIconQuality(bestIcon);

        for (BufferedImage icon : icons) {
            int score = scoreIconQuality(icon);
            if (score > bestScore) {
                bestScore = score;
                bestIcon = icon;
            }
        }

        return bestIcon;
    }

    /**
     * Gets all available icon sizes in this ICO file.
     *
     * @return set of dimensions for all icons
     */
    public Set<Dimension> getAvailableSizes() {
        if (entries == null) {
            return Collections.emptySet();
        }

        Set<Dimension> sizes = new HashSet<>();
        for (IconDirEntry entry : entries) {
            sizes.add(new Dimension(entry.width, entry.height));
        }
        return sizes;
    }

    /**
     * Gets the meta-information for all icons in this ICO file.
     * This doesn't decode the actual image data.
     *
     * @return list of icon information entries
     */
    public List<IconInfo> getIconInfo() {
        if (entries == null) {
            return Collections.emptyList();
        }

        List<IconInfo> info = new ArrayList<>(entries.size());
        for (IconDirEntry entry : entries) {
            boolean isPngIcon = false;
            if (icoData != null) {
                try {
                    byte[] imageData = extractImageData(entry);
                    isPngIcon = isPng(imageData);
                } catch (Exception ignored) {
                    // If we can't determine the format, assume it's not PNG
                }
            }

            info.add(new IconInfo(
                    entry.width,
                    entry.height,
                    entry.bitCount,
                    entry.colorCount,
                    isPngIcon ? "PNG" : "BMP",
                    entry.bytesInRes
            ));
        }
        return info;
    }

    /**
     * Gets the largest icon (by area).
     *
     * @param icons list of icons
     * @return largest icon or null if none available
     */
    public BufferedImage getLargestIcon(List<BufferedImage> icons) {
        if (icons == null || icons.isEmpty()) {
            return null;
        }

        BufferedImage largest = icons.get(0);
        int largestArea = largest.getWidth() * largest.getHeight();

        for (BufferedImage icon : icons) {
            int area = icon.getWidth() * icon.getHeight();
            if (area > largestArea) {
                largestArea = area;
                largest = icon;
            }
        }

        return largest;
    }

    /**
     * Scores an icon's quality based on bit depth and dimensions.
     * Higher scores indicate higher quality.
     *
     * @param icon the icon to score
     * @return quality score
     */
    private int scoreIconQuality(BufferedImage icon) {
        if (icon == null) {
            return 0;
        }

        int area = icon.getWidth() * icon.getHeight();
        int colorDepth = getEffectiveBitDepth(icon);

        // Scoring formula: area * sqrt(colorDepth)
        // This gives reasonable weight to both factors
        return (int)(area * Math.sqrt(colorDepth));
    }

    /**
     * Gets the effective bit depth of an image.
     *
     * @param image the image
     * @return bit depth (1, 4, 8, 24, or 32)
     */
    private int getEffectiveBitDepth(BufferedImage image) {
        ColorModel cm = image.getColorModel();

        if (cm instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) cm;
            int mapSize = icm.getMapSize();

            if (mapSize <= 2) return 1;
            if (mapSize <= 16) return 4;
            return 8;
        }

        return cm.hasAlpha() ? 32 : 24;
    }

    /**
     * Clears the image cache.
     */
    public void clearCache() {
        imageCache.clear();
    }

    /**
     * Reads a little-endian short.
     */
    private static int readLEShort(DataInputStream dis) throws IOException {
        int b1 = dis.readUnsignedByte();
        int b2 = dis.readUnsignedByte();
        return (b2 << 8) | b1;
    }

    /**
     * Reads a little-endian int.
     */
    private static int readLEInt(DataInputStream dis) throws IOException {
        int b1 = dis.readUnsignedByte();
        int b2 = dis.readUnsignedByte();
        int b3 = dis.readUnsignedByte();
        int b4 = dis.readUnsignedByte();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    /**
     * Icon directory entry containing metadata for an individual icon.
     */
    private record IconDirEntry(
            int width,
            int height,
            int colorCount,
            int planes,
            int bitCount,
            int bytesInRes,
            int imageOffset,
            int reserved
    ) {
        @Override
        public String toString() {
            return String.format("%dx%d, %d-bit (%d bytes at offset %d)",
                    width, height, bitCount, bytesInRes, imageOffset);
        }
    }

    /**
     * Icon information for public consumption.
     */
    public record IconInfo(
            int width,
            int height,
            int bitDepth,
            int colors,
            String format,
            int dataSize
    ) {
        @Override
        public String toString() {
            return String.format("%dx%d, %d-bit %s, %d colors, %d bytes",
                    width, height, bitDepth, format, colors, dataSize);
        }
    }

    /**
     * Key for image cache lookup.
     */
    private record IconKey(
            int width,
            int height,
            int bitDepth
    ) {
        // Uses default implementation of equals() and hashCode()
    }

    /**
     * Helper class for dynamic byte buffer management.
     */
    private static class ByteBufferReader {
        private final ByteBuffer buffer;

        public ByteBufferReader(byte[] data) {
            buffer = ByteBuffer.wrap(data);
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }

        public int readInt() {
            return buffer.getInt();
        }

        public short readShort() {
            return buffer.getShort();
        }

        public byte readByte() {
            return buffer.get();
        }

        public void readBytes(byte[] dst, int offset, int length) {
            buffer.get(dst, offset, length);
        }

        public void position(int newPosition) {
            buffer.position(newPosition);
        }

        public int position() {
            return buffer.position();
        }

        public int remaining() {
            return buffer.remaining();
        }
    }
}