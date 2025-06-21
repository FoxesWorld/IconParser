package org.foxesworld.iconParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class ByteParser<T> {

    protected abstract T parseBytes(byte[] data) throws IOException;

    public T parse(byte[] data) throws IOException {
        Objects.requireNonNull(data, "Data byte array cannot be null");
        return parseBytes(data);
    }

    public T parse(InputStream input) throws IOException {
        Objects.requireNonNull(input, "InputStream cannot be null");
        byte[] data = input.readAllBytes();
        return parseBytes(data);
    }

    public T parse(File file) throws IOException {
        Objects.requireNonNull(file, "File cannot be null");
        try (InputStream in = new FileInputStream(file)) {
            return parse(in);
        }
    }

    public T parse(Path path) throws IOException {
        Objects.requireNonNull(path, "Path cannot be null");
        try (InputStream in = Files.newInputStream(path)) {
            return parse(in);
        }
    }
}
