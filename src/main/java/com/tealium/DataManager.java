package com.tealium;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * Tealium data manager object for processing generation of standardized
 * universal data points.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public final class DataManager {
    private static final long RANDOM_MAX_PLUS_ONE = 10000000000000000L;

    private final LibraryContext libraryContext;
    private final Random randomGenerator;
    private final Udo volatileData;
    private String sessionId;
    private PersistentUdo persistentData = null;
    private Udo persistentCache; // use this instead of reading from storage all the time

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Constructor for new DataManager Object
     */
    public DataManager(LibraryContext libraryContext, PersistentUdo persistentData) {
        super();
        this.libraryContext = libraryContext;
        this.sessionId = getTimestampInMilliseconds();
        this.randomGenerator = new SecureRandom();
        this.volatileData = new Udo();
        this.persistentData = persistentData;

        try {
            this.persistentCache = this.persistentData.readOrCreateUdo(this.createNewPersistentData());
        } catch (UdoSerializationException e) {
            // File must be corrupt/unreadable etc
            this.persistentCache = new Udo(); // persistent cache is empty when something goes wrong.
            this.libraryContext.getLogger().log(e, LogLevel.ERRORS);
        }
    }

    /**
     * Retrieve Session ID
     * 
     * @return sessionId
     */
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Retrieve persistent data map
     * 
     * @return persistent data map
     */
    public Udo getPersistentData() {
        return this.persistentCache;
    }

    /**
     * Convenience to add to persistent data map
     * 
     * @param data
     *
     * @throws PersistentDataAccessException
     */
    public void addPersistentData(Udo data) throws UdoSerializationException {
        Udo persistent = getPersistentData();
        persistent.putAll(data);
        this.persistentData.writeData(persistent);
    }

    /**
     * Convenience to remove specific keys from the persistent data map
     *
     * @param keys The list of keys to be removed.
     */
    public void deletePersistentData(List<String> keys) throws UdoSerializationException {
        Udo persistent = getPersistentData();
        for (String key : keys) {
            persistent.remove(key);
        }
        this.persistentData.writeData(persistent);
    }

    /**
     * Convenience to reset session ID
     * 
     */
    public String resetSessionId() {
        this.sessionId = getTimestampInMilliseconds();
        return this.sessionId;
    }

    // =========================================================================
    // PROTECTED
    // =========================================================================

    protected Udo getVolatileData() {
        volatileData.put(Key.TEALIUM_TIMESTAMP_EPOCH, getTimestampInSeconds());
        volatileData.put(Key.TEALIUM_RANDOM, getRandom());
        volatileData.put(Key.TEALIUM_SESSION_ID, getSessionId());
        return volatileData;
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private Udo createNewPersistentData() {
        Udo data = new Udo();
        data.put(Key.TEALIUM_LIBRARY_NAME, "java");
        data.put(Key.TEALIUM_LIBRARY_VERSION, LibraryContext.version);
        data.put(Key.TEALIUM_ACCOUNT, this.libraryContext.getAccount());
        data.put(Key.TEALIUM_PROFILE, this.libraryContext.getProfile());
        if(this.libraryContext.getEnvironment() != null)
        	data.put(Key.TEALIUM_ENVIRONMENT, this.libraryContext.getEnvironment());
        if(this.libraryContext.getDatasource() != null)
        	data.put(Key.TEALIUM_DATASOURCE, this.libraryContext.getDatasource());
        return data;
    }

    private static String getTimestampInSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private static String getTimestampInMilliseconds() {
        return String.valueOf(System.currentTimeMillis());
    }

    private String getRandom() {
        long randomNumber = randomGenerator.nextLong() % RANDOM_MAX_PLUS_ONE;
        String stringRandom = String.format("%016d", Math.abs(randomNumber));
        return stringRandom;
    }

    public static class Key {
        private Key() {}

        public static final String EVENT_NAME = "event_name";
        public static final String TEALIUM_ACCOUNT = "tealium_account";
        public static final String TEALIUM_ENVIRONMENT = "tealium_environment";
        public static final String TEALIUM_DATASOURCE = "tealium_datasource";
        public static final String TEALIUM_EVENT = "tealium_event";
        public static final String TEALIUM_EVENT_TYPE = "tealium_event_type";
        public static final String TEALIUM_LIBRARY_NAME = "tealium_library_name";
        public static final String TEALIUM_LIBRARY_VERSION = "tealium_library_version";
        public static final String TEALIUM_PROFILE = "tealium_profile";
        public static final String TEALIUM_RANDOM = "tealium_random";
        public static final String TEALIUM_SESSION_ID = "tealium_session_id";
        public static final String TEALIUM_TIMESTAMP_EPOCH = "tealium_timestamp_epoch";
        public static final String TEALIUM_VISITOR_ID = "tealium_visitor_id";

    }

    public static class EventType {
        private EventType() {}

        public static final String ACTIVITY = "activity";
        public static final String CONVERSION = "conversion";
        public static final String DERIVED = "derived";
        public static final String INTERACTION = "interaction";
        public static final String VIEW = "view";

    }

    public static class InfoKey {
        private InfoKey() {}

        public static final String DISPATCH_SERVICE = "dispatch_service";
        public static final String ENCODED_URL = "encoded_url";
        public static final String RESPONSE_HEADERS = "response_headers";
        public static final String PAYLOAD = "payload";

    }
}
