package com.tealium;

import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Test logic related to the Logger
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class LoggerTests {
	@Test
	public void testMatchingVerbosity() {
		String message = "Successful Log Test";
		String output = new Logger(LogLevel.VERBOSE).log(message, LogLevel.VERBOSE);

        assertEquals(output, message);
	}

	@Test
	public void testAboveVerbosity() {

		Logger logger = new Logger(LogLevel.VERBOSE);

		String message = "Successful Warning Log Test";
		String output = logger.log(message, LogLevel.WARNINGS);

        assertEquals(output, message);
	}

	@Test
	public void testBelowVerbosity() {
		Logger logger = new Logger(LogLevel.ERRORS);

		String message = "Error Log Message";
		String output = logger.log(message, LogLevel.VERBOSE);

		assertNull(output);
	}
}
