package com.tealium;

/**
 * Test logic related to LibraryContext
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
final class TestLibraryContext extends LibraryContext {
    private TestLibraryContext(Logger logger) {
        super("tealiummobile", "demo", "dev", "datasource", logger);
    }

    static TestLibraryContext newInstance() {
        return newInstance(new Logger(LogLevel.VERBOSE));
    }

    static TestLibraryContext newInstance(Logger logger) {
        return new TestLibraryContext(logger);
    }

}
