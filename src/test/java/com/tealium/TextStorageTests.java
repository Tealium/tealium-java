package com.tealium;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test logic related to storing persistent text
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TextStorageTests {
    private static final Path testFilePath = Paths.get(System.getProperty("user.home"), "tealiumpersistencetestfile.txt");

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(testFilePath);
    }

    @Test
    public void testTextIsStored() throws IOException {
        // delete file if it already exists
        this.tearDown();

        // write some text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is a test");
        assertEquals("this is a test", new TextStorage(testFilePath).readText());

        this.tearDown();
    }

    @Test
    public void testTextIsOverwritten() throws IOException {
        // delete file if it already exists
        this.tearDown();

        // write some text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is a test");
        assertEquals("this is a test", new TextStorage(testFilePath).readText());

        // make sure the file is still there
        assertTrue(Files.exists(testFilePath));

        // write some new text with one TextStorage instance, and read it with another, verifying they're equal
        new TextStorage(testFilePath).writeText("this is another test");
        assertEquals("this is another test", new TextStorage(testFilePath).readText());

        this.tearDown();
    }

    @Test
    public void testExistsReturnsFalseWhenFileDoesNotExistAtPath() throws IOException {
        this.tearDown();
        assertFalse(new TextStorage(testFilePath).exists());
    }

    @Test
    public void testExistsReturnsTrueWhenFileExistsAtPath() throws IOException {
        Files.createDirectories(testFilePath.getParent());
        Files.write(testFilePath, "test content".getBytes(StandardCharsets.UTF_8));
        assertTrue(new TextStorage(testFilePath).exists());
        this.tearDown();
    }
}
