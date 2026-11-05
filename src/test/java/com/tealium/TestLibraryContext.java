package com.tealium;

/**
 * Test logic related to LibraryContext
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
final class TestLibraryContext extends LibraryContext {
    private TestLibraryContext() {
        super("tealiummobile", "demo", "dev", "datasource", new Logger(LogLevel.VERBOSE));
    }

    static TestLibraryContext newInstance() {
        return new TestLibraryContext();
    }

}
