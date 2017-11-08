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
     * Read serialized Map from storage.
     * 
     * Maps are persisted using percent encoding so as to be dependency
     * Independent; and immune to the vulnerabilities of the native
     * serialization API.
     * 
     * @return The deserialized map.
     * @throws PersistentDataAccessException
     *             If the file doesn't exist or the map is malformed.
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
     * Serialize Map to storage.
     *
     * Maps are persisted using percent encoding so as to be dependency
     * Independent; and immune to the vulnerabilities of the native
     * serialization API.
     *
     *
     * @param data
     *            The map to serialize, this will overwrite the existing map.
     * @throws IOException
     *             If ~/.tealium/ does not exist or cannot be created.
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
