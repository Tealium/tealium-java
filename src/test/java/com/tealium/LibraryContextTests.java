package com.tealium;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test logic related to LibraryContext
 */
public class LibraryContextTests {
    @Test
    public void testVersionMatchesProjectVersion() {
        String projectVersion = System.getProperty("tealium.projectVersion");
        assertNotNull("tealium.projectVersion is set by the Gradle test task", projectVersion);
        assertEquals(projectVersion, LibraryContext.version);
    }
}
