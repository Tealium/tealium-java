package com.tealium;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test logic related to PersistentUdo
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class PersistentUdoTests {

    private static Udo testUdo() {
        // create a dummy udo with some data for testing
        final Udo udo = new Udo();
        udo.put("currentTimeMillis", "" + System.currentTimeMillis());
        udo.put("nanoTime", "" + System.nanoTime());
        udo.put("greek", new String[] { "alpha", "beta", "gamma" });
        udo.put("cs", new String[] { "foo", "bar,\r\n\tand some extra stuff" });
        return udo;
    }

    @Test
    public void writeDataWritesJsonEncodedUdo() throws UdoSerializationException, IOException {
        // initialize dummy udo, same udo as JSON encoded string, and mock of TextStorage
        final Udo udo = testUdo();
        final String udoAsJson = udo.toJson();
        TextStorage mockedTextStorage = mock(TextStorage.class);

        // create a test instance and call the writeData() method on the udo
        new PersistentUdo(mockedTextStorage).writeData(udo);

        // verify the udo was encoded and written
        verify(mockedTextStorage).writeText(udoAsJson);
    }

    @Test
    public void readOrCreateUdoReturnsJsonDecodedUdoWhenPersistentTextExists()
            throws UdoSerializationException, IOException {
        // initialize dummy udo, same udo as JSON encoded string, and mock of TextStorage
        final Udo udo = testUdo();
        final String udoAsJson = udo.toJson();
        TextStorage mockedTextStorage = mock(TextStorage.class);

        // return the JSON encoded udo when readText() is called on the mock
        when(mockedTextStorage.readText()).thenReturn(udoAsJson);

        // create the test instance and call readOrCreateUdo using an empty udo as the argument, and check result
        assertEquals(udo, new PersistentUdo(mockedTextStorage).readOrCreateUdo(new Udo()));
    }

    @Test
    public void readOrCreateUdoReturnsDefaultUdoWhenPersistentTextDoesNotExist()
            throws UdoSerializationException, IOException {
        // initialize dummy udo, same udo as JSON encoded string, and mock of TextStorage
        final Udo udo = testUdo();
        TextStorage mockedTextStorage = mock(TextStorage.class);

        // throw an IOException when readText() is called on the mock
        when(mockedTextStorage.readText()).thenThrow(new IOException("Testing behavior when persistent text does not exist"));

        // create the test instance and call readOrCreateUdo with the test udo, and check result
        assertEquals(udo, new PersistentUdo(mockedTextStorage).readOrCreateUdo(udo));
    }

    @Test
    public void readOrCreateUdoWritesDefaultUdoWhenPersistentTextDoesNotExist()
            throws UdoSerializationException, IOException {
        // initialize dummy udo, same udo as JSON encoded string, and mock of TextStorage
        final Udo udo = testUdo();
        final String udoAsJson = udo.toJson();
        TextStorage mockedTextStorage = mock(TextStorage.class);

        // throw an IOException when readText() is called on the mock
        when(mockedTextStorage.readText()).thenThrow(new IOException("Testing behavior when persistent text does not exist"));

        // create the test instance and call readOrCreateUdo with the test udo
        new PersistentUdo(mockedTextStorage).readOrCreateUdo(udo);

        // verify the writeText method was called with the JSON encoded udo
        verify(mockedTextStorage).writeText(udoAsJson);
    }

    @Test
    public void readOrCreateUdoReturnsPercentDecodedUdoWhenPersistentTextIsLegacyFormat()
            throws UdoSerializationException, IOException {
        // a handwritten legacy (pre-1.3.0) percent encoded persistent file: a plain string value,
        // a value that needs percent decoding, and a list value
        final String legacyText = "plainKey=plainValue&spacedKey=hello%20world&greek=alpha,beta,gamma";

        // the udo we expect to be decoded from the legacy text above
        final Udo expectedUdo = new Udo();
        expectedUdo.put("plainKey", "plainValue");
        expectedUdo.put("spacedKey", "hello world");
        expectedUdo.put("greek", new String[] { "alpha", "beta", "gamma" });

        TextStorage mockedTextStorage = mock(TextStorage.class);

        // return the legacy percent encoded text when readText() is called on the mock
        when(mockedTextStorage.readText()).thenReturn(legacyText);

        // create the test instance and call readOrCreateUdo using an empty udo as the default, and check result
        assertEquals(expectedUdo, new PersistentUdo(mockedTextStorage).readOrCreateUdo(new Udo()));

        // the legacy file decoded successfully, so the default should never have been written back
        verify(mockedTextStorage, never()).writeText(anyString());
    }

    @Test
    public void readOrCreateUdoPropagatesIllegalArgumentExceptionWhenLegacyTextHasInvalidEscape()
            throws IOException {
        // legacy text that is neither valid JSON nor validly percent encoded (invalid hex escape)
        final String legacyText = "key=%ZZ";

        TextStorage mockedTextStorage = mock(TextStorage.class);
        when(mockedTextStorage.readText()).thenReturn(legacyText);

        assertThrows(IllegalArgumentException.class,
                () -> new PersistentUdo(mockedTextStorage).readOrCreateUdo(new Udo()));

        // the invalid legacy text should not have been overwritten with defaultData
        verify(mockedTextStorage, never()).writeText(anyString());
    }

    @Test
    public void readOrCreateUdoPropagatesIllegalArgumentExceptionWhenStoredTextIsTruncatedJson()
            throws IOException {
        // text left behind by a half-finished write: JSON decoding fails, and the legacy
        // percent-decoding fallback then fails too, on the incomplete trailing "%"
        final String truncatedText = "{\"discount\":\"x=50%";

        TextStorage mockedTextStorage = mock(TextStorage.class);
        when(mockedTextStorage.readText()).thenReturn(truncatedText);

        assertThrows(IllegalArgumentException.class,
                () -> new PersistentUdo(mockedTextStorage).readOrCreateUdo(new Udo()));

        // the truncated text should not have been overwritten with defaultData
        verify(mockedTextStorage, never()).writeText(anyString());
    }

    @Test
    public void existsReturnsTrueWhenTextStorageExists() {
        // create a mock TextStorage that exists
        TextStorage mockedTextStorage = mock(TextStorage.class);
        when(mockedTextStorage.exists()).thenReturn(true);

        // assert a PersistentUdo exists when its TextStorage exists
        assertTrue(new PersistentUdo(mockedTextStorage).exists());

        // the exist() method on the mock TextStorage should have been called
        verify(mockedTextStorage).exists();
    }

    @Test
    public void existsReturnsFalseWhenTextStorageDoesNotExist() {
        // create a mock TextStorage that does not exist
        TextStorage mockedTextStorage = mock(TextStorage.class);
        when(mockedTextStorage.exists()).thenReturn(false);

        // assert a PersistentUdo does not exist when its TextStorage does not exist
        assertFalse(new PersistentUdo(mockedTextStorage).exists());

        // the exist() method on the mock TextStorage should have been called
        verify(mockedTextStorage).exists();
    }

}
