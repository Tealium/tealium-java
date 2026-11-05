package com.tealium;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test logic related to storing persistent text
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TextStorageTests {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path testFilePath() {
        return temporaryFolder.getRoot().toPath().resolve("tealiumpersistencetestfile.txt");
    }

    @Test
    public void testTextIsStored() throws IOException {
        Path testFilePath = testFilePath();

        // write some text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is a test");
        assertEquals("this is a test", new TextStorage(testFilePath).readText());
    }

    @Test
    public void testTextIsOverwritten() throws IOException {
        Path testFilePath = testFilePath();

        // write some text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is a test");
        assertEquals("this is a test", new TextStorage(testFilePath).readText());

        // make sure the file is still there
        assertTrue(Files.exists(testFilePath));

        // write some new text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is another test");
        assertEquals("this is another test", new TextStorage(testFilePath).readText());
    }

    @Test
    public void testExistsReturnsFalseWhenFileDoesNotExistAtPath() {
        assertFalse(new TextStorage(testFilePath()).exists());
    }

    @Test
    public void testExistsReturnsTrueWhenFileExistsAtPath() throws IOException {
        Path testFilePath = testFilePath();
        Files.write(testFilePath, "test content".getBytes(StandardCharsets.UTF_8));
        assertTrue(new TextStorage(testFilePath).exists());
    }
}
