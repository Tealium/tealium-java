package com.tealium;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

import com.tealium.DataManager.Key;
import com.tealium.DataManager.EventType;

/**
 * Tealium library for conversion and dispatch handling of natively triggered
 * events.
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public final class Tealium {

    // Public API Fields should never be public; see Effective Java Item 14
    private final DataManager dataManager;
    private final CollectDispatcher collectDispatcher;
    private final LibraryContext libraryContext;

    // =========================================================================
    // PUBLIC BUILDER
    // =========================================================================

    public static class Builder {

        private final String account;
        private final String profile;
        private String environment;
        private String datasource;
        private CollectDispatcher collectDispatcher = null;
        private PersistentUdo persistentData = null;
        private LogLevel logLevel = LogLevel.VERBOSE;
        private int timeout = 5000;

        /**
         * Constructor for a new Tealium object.
         * 
         * @param account
         *            Required. Tealium account name.
         * @param profile
         *            Required. Tealium profile name.
         */
        public Builder(String account, String profile) {
            if (stringIsNullOrEmpty(account) || stringIsNullOrEmpty(profile)) {
                throw new IllegalArgumentException("Neither account nor profile may be null or empty.");
            }

            this.account = account;
            this.profile = profile;
        }

        /**
         * Convenience function for making code prettier and more readable.
         *
         * @param str
         * @return true if the string is either null or empty
         */
        private static boolean stringIsNullOrEmpty(String str) {return str == null || str.length() == 0;}
        
        /**
         * Constructor for a new Tealium object.
         * 
         * The preferred way to set environment is with the "setEnvironment" method.
         * Environment is therefore no longer required as a constructor argument, and this
         * constructor is deprecated.
         * 
         * @deprecated use {@link #Builder(String, String)} ()} instead.
         * 
         * @param account
         *            Required. Tealium account name.
         * @param profile
         *            Required. Tealium profile name.
         * @param environment
         *            Required. Tealium environment. Usually dev, qa, or prod.
         */
        @Deprecated
        public Builder(String account, String profile, String environment) {
            if ((account == null) || (account.length() == 0)
                    || (profile == null) || (profile.length() == 0)
                    || (environment == null) || (environment.length() == 0)) {
                throw new IllegalArgumentException("Account, profile, & environment must not be empty.");
            }

            this.account = account;
            this.profile = profile;
            this.environment = environment;
        }

        /**
         * Executes actual build.
         * 
         * @return Instance of Tealium.
         */
        public Tealium build() {
            Path persistentFilePath = Paths.get(System.getProperty("user.home"), ".tealium",
                    String.format(Locale.ROOT, "%s.%s.data", this.account, this.profile));

            LibraryContext libraryContext = new LibraryContext(this.account, this.profile,
                    this.environment, this.datasource, new Logger(this.logLevel));


            // set the persistent data if it hasn't been explicitly set with the setPersistentData() method.
            if(this.persistentData == null) {
                this.persistentData = new PersistentUdo(new TextStorage(persistentFilePath));
            }

            // set the collect dispatcher if it hasn't been explicitly set with the setCollectDispatcher() method.
            if(this.collectDispatcher == null) {
                this.collectDispatcher = new CollectDispatcher(CollectDispatcher.DEFAULT_URL, libraryContext, timeout);
            }

            return new Tealium(libraryContext, this.collectDispatcher, this.persistentData, this.timeout);
        }

        public Builder setLogLevel(LogLevel level) {
            if (level == null) {
                throw new IllegalArgumentException("Invalid log level.");
            }
            this.logLevel = level;
            return this;
        }
        
        public Builder setEnvironment(String environment) {
        	if (environment == null) {
                throw new IllegalArgumentException("Invalid environment.");
            }
            this.environment = environment;
            return this;
        }
        
        public Builder setDatasource(String datasource) {
        	if (datasource == null) {
                throw new IllegalArgumentException("Invalid datasource.");
            }
            this.datasource = datasource;
            return this;
        }

        public Builder setCollectDispatcher(CollectDispatcher collectDispatcher) {
            this.collectDispatcher = collectDispatcher;
            return this;
        }

        public Builder setPersistentData(PersistentUdo persistentData) {
            this.persistentData = persistentData;
            return this;
        }

        public Builder setTimeout(int timeout) {
            if (timeout == 0) {
                throw new IllegalArgumentException("Timeout should be greater than zero.");
            }
            this.timeout = timeout;
            return this;
        }

    }

    // =========================================================================
    // PUBLIC CALLBACK INTERFACE
    // =========================================================================

    /**
     * Optional interface for a dispatch call-back object that reports when a
     * track event has completed.
     */
    public interface DispatchCallback {
        /**
         * Required method to handle callback.
         * 
         * @param success
         * @param info
         *            Map containing call info, typical keys: dispatch_service, payload
         * @param error
         *            null or string whether an occurred.
         * 
         * @return String encoded URL string
         */
        public void dispatchComplete(boolean success, Map<String, Object> info, String error);
    }

    // =========================================================================
    // PUBLIC
    // =========================================================================

    public String getAccount() {
        return this.libraryContext.getAccount();
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public String getProfile() {
        return this.libraryContext.getProfile();
    }

    public String getEnvironment() {
        return this.libraryContext.getEnvironment();
    }
    
    public String getDatasource() {
    	return this.libraryContext.getDatasource();
    }

    /**
     * Convenient basic track event.
     * 
     * @param eventTitle
     *            Required title of event.
     */
    public void track(String eventTitle) {
        track(eventTitle, null, null);
    }

    /**
     * Convenient tracking event with optional data.
     *
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional udo of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     */
    public void track(String eventTitle, Udo eventData) {
        track(eventTitle, eventData, null);
    }

    /**
     * Convenient tracking event with optional data.
     *
     * @deprecated
     *            Use the implementation of track that accepts a Udo type for event data, instead of the ambiguous
     *            Map<String, ?> argument this one takes.
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     */
    public void track(String eventTitle, Map<String, ?> eventData) {
        track(eventTitle, eventData == null ? null : new Udo(eventData), null);
    }

    /**
     * Convenient Track method for activities.
     *
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional udo of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventTitle, Udo eventData, Tealium.DispatchCallback callback) {

        track(EventType.ACTIVITY, eventTitle, eventData, callback);
    }

    /**
     * Convenient Track method for activities.
     *
     * @deprecated
     *            Use the implementation of track that accepts a Udo type for event data, instead of the ambiguous
     *            Map<String, ?> argument this one takes.
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventTitle, Map<String, ?> eventData, Tealium.DispatchCallback callback) {

        track(eventTitle, eventData == null ? null : new Udo(eventData), callback);
    }

    /**
     * Primary Track method.
     * 
     * @param eventType
     *            Optional track type (VIEW, ACTIVITY, INTERACTION, DERIVED, CONVERSION). Defaults
     *            to ACTIVITY if nil.
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional udo of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventType, String eventTitle, Udo eventData, Tealium.DispatchCallback callback) {

        Udo payloadData = new Udo(this.dataManager.getPersistentData());

        if (eventType == null) {
            eventType = EventType.ACTIVITY;
        }
        payloadData.put(Key.TEALIUM_EVENT_TYPE, eventType);

        if (eventTitle != null) {
            //Legacy - will be deprecated
            payloadData.put(Key.EVENT_NAME, eventTitle);
            payloadData.put(Key.TEALIUM_EVENT, eventTitle);
        }
        
        if (this.getDatasource() != null) {
        	payloadData.put(Key.TEALIUM_DATASOURCE, this.getDatasource());
        }
        
        payloadData.putAll(this.dataManager.getVolatileData());

        if (eventData != null) {
            payloadData.putAll(new Udo(eventData));
        }

        try {
            this.collectDispatcher.dispatch(payloadData, callback);
        } catch (CollectDispatchException e) {
            this.libraryContext.getLogger().log(e, LogLevel.ERRORS);
        }
    }

    /**
     * Primary Track method.
     *
     * @deprecated
     *            Use the implementation of track that accepts a Udo type for event data, instead of the ambiguous
     *            Map<String, ?> argument this one takes.
     * @param eventType
     *            Optional track type (VIEW, ACTIVITY, INTERACTION, DERIVED, CONVERSION). Defaults
     *            to ACTIVITY if nil.
     * @param eventTitle
     *            Required title of event.
     * @param eventData
     *            Optional map of additional data to pass with call. Values
     *            should be Strings or Array of Strings.
     * @param callback
     *            Object conforming to the CollectCallback interface.
     */
    public void track(String eventType, String eventTitle, Map<String, ?> eventData, Tealium.DispatchCallback callback) {
        this.track(eventType, eventTitle, eventData == null ? null : new Udo(eventData), callback);
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private Tealium(LibraryContext libraryContext, CollectDispatcher collectDispatcher, PersistentUdo persistentData, int timeout) {
        super();
        this.libraryContext = libraryContext;
        this.dataManager = new DataManager(this.libraryContext, persistentData);
        // Is the URL in the constructor future proofing?
        this.collectDispatcher = collectDispatcher;
    }

}
