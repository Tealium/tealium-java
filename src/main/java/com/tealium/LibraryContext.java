package com.tealium;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Data class for defining library configuration and other information
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
class LibraryContext {
    public static final String version = "1.3.0";

    private final String account;
    private final String profile;
    private final String environment;
    private final String datasource;
    private final Logger logger;

    LibraryContext(String account, String profile, String environment, String datasource, Logger logger) {
        super();
        this.account = account;
        this.profile = profile;
        this.environment = environment;
        this.datasource = datasource;
        this.logger = logger;
    }

    final String getAccount() {
        return account;
    }

    final String getProfile() {
        return profile;
    }

    final String getEnvironment() {
        return environment;
    }
    
    final String getDatasource() {
    	return datasource;
    }

    final Logger getLogger() {
        return logger;
    }

    final Path getPersistentFilePath() {
        return Paths.get(System.getProperty("user.home"), ".tealium",
                String.format(Locale.ROOT, "%s.%s.data", this.getAccount(), this.getProfile()));
    }

}
