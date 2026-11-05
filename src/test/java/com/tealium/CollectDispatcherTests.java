package com.tealium;

import com.sun.net.httpserver.HttpServer;
import com.tealium.Tealium.DispatchCallback;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test logic related to CollectDispatcher
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class CollectDispatcherTests {

    // SIMPLE START UP TEST
    @Test
    public void testInit() {
        new CollectDispatcher(CollectDispatcher.DEFAULT_URL, TestLibraryContext.newInstance(), 3000);
    }

    
    // INTEGRATION DISPATCH TESTS
    @Test
    public void testSuccessfulDispatch() throws Exception {

        CollectDispatcher collect = new CollectDispatcher(CollectDispatcher.DEFAULT_URL,
                TestLibraryContext.newInstance(), 3000);

        final CountDownLatch barrier = new CountDownLatch(1);

        DispatchCallback callBack = (success, info, error) -> {
            System.out.println(error);
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

        barrier.await(5, TimeUnit.SECONDS);

    }

    @Test
    public void testFailingDispatch() throws Exception {

        CollectDispatcher collect = new CollectDispatcher("http://url.2.nowhere", TestLibraryContext
                .newInstance(), 3000);

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

        barrier.await(5, TimeUnit.SECONDS);

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
        HttpServer server = startServer(400, "invalid payload");
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CollectDispatcher collect = newDispatcher(urlOf(server), output);

            collect.dispatch(new Udo(), null);

            assertTrue(logged(output).contains("Dispatch failed with response code 400: invalid payload"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSuccessfulDispatchLogsNothing() throws Exception {
        HttpServer server = startServer(200, null);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CollectDispatcher collect = newDispatcher(urlOf(server), output);

            collect.dispatch(new Udo(), null);

            assertEquals("", logged(output));
        } finally {
            server.stop(0);
        }
    }

    private static CollectDispatcher newDispatcher(String endpoint, ByteArrayOutputStream output) {
        Logger logger = new Logger(LogLevel.VERBOSE, new PrintStream(output, true));
        return new CollectDispatcher(endpoint, TestLibraryContext.newInstance(logger), 3000);
    }

    private static HttpServer startServer(int responseCode, String xError) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/event", exchange -> {
            if (xError != null) {
                exchange.getResponseHeaders().add("x-error", xError);
            }
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String urlOf(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort() + "/event";
    }

    private static String logged(ByteArrayOutputStream output) {
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
