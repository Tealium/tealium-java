package com.tealium;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class for handling persistent text
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TextStorage {
    private final Path path;

    public TextStorage(Path path) {
        this.path = path;
    }

    /**
     * Read persistent text from storage using this TextStorage's path
     *
     * @return the stored text
     * @throws IOException
     */
    public String readText() throws IOException {
        byte[] bytes = Files.readAllBytes(this.path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Write persistent text to storage using this TextStorage's path
     *
     * @param persistentText
     * @throws IOException
     */
    public void writeText(String persistentText) throws IOException {
        byte[] bytes = persistentText.getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(this.path.getParent());
        Files.write(this.path, bytes);
    }

    /**
     * If a file exists at this TextStorage's path, then the persistent text exists
     *
     * @return true if exists, false otherwise
     */
    public Boolean exists() {
        return Files.isRegularFile(this.path);
    }
}