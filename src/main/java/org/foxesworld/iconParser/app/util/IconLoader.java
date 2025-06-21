package org.foxesworld.iconParser.app.util;

import org.foxesworld.iconParser.ICOParser;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Worker class for loading ICO files in the background.
 */
public class IconLoader extends SwingWorker<List<BufferedImage>, Void> {
    private final File file;
    private Consumer<List<BufferedImage>> successCallback;
    private Consumer<Exception> failureCallback;

    /**
     * Creates a new icon loader for the specified file.
     *
     * @param file The ICO file to load
     */
    public IconLoader(File file) {
        this.file = file;
    }

    /**
     * Sets a callback for successful loading.
     *
     * @param callback The success callback
     * @return This loader for method chaining
     */
    public IconLoader setOnSuccess(Consumer<List<BufferedImage>> callback) {
        this.successCallback = callback;
        return this;
    }

    /**
     * Sets a callback for loading failures.
     *
     * @param callback The failure callback
     * @return This loader for method chaining
     */
    public IconLoader setOnFailure(Consumer<Exception> callback) {
        this.failureCallback = callback;
        return this;
    }

    /**
     * Loads the ICO file in the background.
     */
    @Override
    protected List<BufferedImage> doInBackground() throws Exception {
        try (InputStream icoStream = new FileInputStream(file)) {
            ICOParser parser = new ICOParser();
            return parser.parse(icoStream);
        }
    }

    /**
     * Called when loading is complete.
     */
    @Override
    protected void done() {
        try {
            List<BufferedImage> icons = get();
            if (successCallback != null) {
                successCallback.accept(icons);
            }
        } catch (Exception e) {
            if (failureCallback != null) {
                failureCallback.accept(e);
            }
        }
    }
}