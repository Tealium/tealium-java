package com.tealium;

import com.fasterxml.jackson.jr.ob.JSON;
import com.tealium.Tealium.DispatchCallback;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test logic related to CollectDispatcher
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class CollectDispatcherTests {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() {
        server.close();
    }

    // SIMPLE START UP TEST
    @Test
    public void testInit() {
        new CollectDispatcher(server.url("/event").toString(), TestLibraryContext.newInstance(), 3000);
    }

    // DISPATCH TESTS
    @Test
    public void testSuccessfulDispatch() throws Exception {
        server.enqueue(new MockResponse.Builder().code(200).build());

        CollectDispatcher collect = new CollectDispatcher(server.url("/event").toString(),
                TestLibraryContext.newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = (success, info, error) -> {
            assertTrue(success);
            barrier.countDown();
        };

        Udo data = new Udo();
        data.put("autotracked", "false");
        data.put("tealium_account", "tealiummobile");
        data.put("tealium_profile", "demo");
        data.put("tealium_visitor_id", "612040730c8c11e6b4cbtest329f41b7");
        data.put("tealium_vid", "612040730c8c11e6b4cbtest329f41b7");
        collect.dispatch(data, callBack);

        assertTrue(barrier.await(5, TimeUnit.SECONDS));

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("application/json; charset=UTF-8", recordedRequest.getHeaders().get("Content-Type"));

        String requestBody = recordedRequest.getBody().utf8();
        Map<String, Object> bodyMap = JSON.std.mapFrom(requestBody);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            assertEquals(entry.getValue(), bodyMap.get(entry.getKey()));
        }
    }

    @Test
    public void testServerErrorCallsCallbackWithFailure() throws Exception {
        server.enqueue(new MockResponse.Builder().code(500).build());

        CollectDispatcher collect = new CollectDispatcher(server.url("/event").toString(),
                TestLibraryContext.newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = (success, info, error) -> {
            assertFalse(success);
            barrier.countDown();
        };

        Udo data = new Udo();
        data.put("autotracked", "false");
        data.put("tealium_account", "tealiummobile");
        data.put("tealium_profile", "demo");
        data.put("tealium_visitor_id", "612040730c8c11e6b4cbtest329f41b7");
        data.put("tealium_vid", "612040730c8c11e6b4cbtest329f41b7");
        collect.dispatch(data, callBack);

        assertTrue(barrier.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testFailingDispatch() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        CollectDispatcher collect = new CollectDispatcher("http://localhost:" + closedPort + "/event",
                TestLibraryContext.newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = (success, info, error) -> {
            assertFalse(success);
            barrier.countDown();
        };

        Udo data = new Udo();
        data.put("autotracked", "false");
        data.put("tealium_account", "tealiummobile");
        data.put("tealium_profile", "demo");
        data.put("tealium_visitor_id", "612040730c8c11e6b4cbtest329f41b7");
        data.put("tealium_vid", "612040730c8c11e6b4cbtest329f41b7");
        collect.dispatch(data, callBack);

        assertTrue(barrier.await(5, TimeUnit.SECONDS));
    }

    // LOGGING TESTS
    @Test
    public void testFailedConnectionIsLoggedWithoutCallback() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CollectDispatcher collect = newDispatcher("http://localhost:" + closedPort + "/event", output);

        collect.dispatch(new Udo(), null);

        assertTrue(logged(output).contains(FailedConnectionException.class.getName()));
    }

    @Test
    public void testRejectedRequestIsLoggedWithoutCallback() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(400)
                .addHeader("x-error", "invalid payload")
                .build());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CollectDispatcher collect = newDispatcher(server.url("/event").toString(), output);

        collect.dispatch(new Udo(), null);

        assertTrue(logged(output).contains("Dispatch failed with response code 400: invalid payload"));
    }

    @Test
    public void testSuccessfulDispatchLogsNothing() throws Exception {
        server.enqueue(new MockResponse.Builder().code(200).build());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CollectDispatcher collect = newDispatcher(server.url("/event").toString(), output);

        collect.dispatch(new Udo(), null);

        assertEquals("", logged(output));
    }

    private static CollectDispatcher newDispatcher(String endpoint, ByteArrayOutputStream output) {
        Logger logger = new Logger(LogLevel.VERBOSE, new PrintStream(output, true));
        return new CollectDispatcher(endpoint, TestLibraryContext.newInstance(logger), 3000);
    }

    private static String logged(ByteArrayOutputStream output) {
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
