package com.tealium;

import java.io.IOException;

/**
 * Tealium data object for processing generation of standardized universal data
 * points.
 *
 * @author Chad Hartman, Jason Koo, Merritt Tidwell, Karen Tamayo, Chris Anderberg
 */
class PersistentUdo {

    private TextStorage textStorage;

    public PersistentUdo(TextStorage textStorage) {
        this.textStorage = textStorage;
    }

    /**
     * Read serialized Udo from storage.
     *
     * Tries to decode the stored text as JSON first (the format written by
     * {@link #writeData(Udo)} since SDK 1.3.0), falling back to the legacy
     * percent encoded format (see {@link Udo#fromPercentEncoded(String)}) for files
     * written by SDK 1.2.0 and earlier. If the file doesn't exist or neither decoding
     * succeeds, defaultData is used and persisted instead.
     *
     * @return The deserialized Udo, or defaultData if none could be loaded.
     * @throws UdoSerializationException
     *             If defaultData needs to be persisted and can't be serialized to JSON.
     */
    public Udo readOrCreateUdo(Udo defaultData) throws UdoSerializationException {
        Udo loadedUdo = null;

        try {
            // Try loading the persistent text, nested try blocks depend on this to work.
            String loadedText = this.textStorage.readText();
            try {
                // Try to decode from JSON first.
                loadedUdo = Udo.fromJson(loadedText);
            } catch(UdoDeserializationException e) {
                try {
                    // Decoding from JSON didn't work. Perhaps it's old percent encoded text, so try that.
                    loadedUdo = Udo.fromPercentEncoded(loadedText);
                } catch(UdoDeserializationException e2) {}
            }
        } catch(IOException e) {}

        // If unable to load existing udo, use the default.
        if(loadedUdo == null) {
            loadedUdo = defaultData;
            this.writeData(loadedUdo);
        }

        return loadedUdo;
    }

    /**
     * Serialize Udo to storage.
     *
     * Data is persisted as JSON, via {@link Udo#toJson()}, overwriting any
     * existing stored data. If the underlying write fails (e.g. ~/.tealium/
     * does not exist and cannot be created), the failure is swallowed and
     * the data is simply kept in memory.
     *
     * @param data
     *            The Udo to serialize, this will overwrite the existing data.
     * @throws UdoSerializationException
     *             If data cannot be serialized to JSON.
     */
    public void writeData(Udo data) throws UdoSerializationException {
        try {
            this.textStorage.writeText(data.toJson());
        } catch(IOException e) {} // just use data in memory if can't write
    }

    /**
     * If there is a persistent udo available to read, then return true
     *
     * @return true if exists, false otherwise
     */
    public Boolean exists() {
        return this.textStorage.exists();
    }
}
