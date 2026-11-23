package com.tealium;

import com.tealium.Tealium.DispatchCallback;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.SocketEffect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.net.ServerSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests how CollectDispatcher classifies responses and releases connections, so the JDK can keep them alive.
 *
 * Successful responses use an empty chunked body rather than the default "Content-Length: 0" reply: the JDK
 * returns a zero-length response's connection to its keep-alive pool as soon as the headers are read, so it
 * would be reused whatever the dispatcher did afterwards.
 */
public class CollectDispatcherConnectionTests {

    private static final int SHORT_READ_TIMEOUT = 100;
    private static final int SMALL_BODY_SIZE = 1024;
    private static final int LARGE_BODY_SIZE = 16 * 1024;
    private static final int BURST_SIZE = 50;

    private CountingServerSocketFactory socketFactory;
    private MockWebServer server;
    private ByteArrayOutputStream log;

    @Before
    public void setUp() throws Exception {
        socketFactory = new CountingServerSocketFactory();
        server = new MockWebServer();
        server.setServerSocketFactory(socketFactory);
        server.start();
        log = new ByteArrayOutputStream();
    }

    @After
    public void tearDown() {
        server.close();
    }

    // RESPONSE CLASSIFICATION

    @Test
    public void test204IsSuccess() throws Exception {
        server.enqueue(new MockResponse.Builder().code(204).build());

        Result result = dispatch(newDispatcher(3000));

        assertTrue(result.success);
        assertNull(result.error);
    }

    @Test
    public void test200WithXErrorIsFailure() throws Exception {
        server.enqueue(successResponse().addHeader("x-error", "invalid payload").build());

        Result result = dispatch(newDispatcher(3000));

        assertFalse(result.success);
        assertFailedRequest(result);
    }

    @Test
    public void test302IsFailureNotFollowedAndConnectionIsReused() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(302)
                .addHeader("Location", server.url("/elsewhere").toString())
                .body("moved")
                .build());
        server.enqueue(successResponse().build());
        CollectDispatcher collect = newDispatcher(3000);

        Result redirected = dispatch(collect);
        Result next = dispatch(collect);

        assertFalse(redirected.success);
        assertFailedRequest(redirected);
        assertTrue(next.success);

        RecordedRequest first = takeRequest();
        RecordedRequest second = takeRequest();
        assertEquals("/event", first.getUrl().encodedPath());
        assertEquals("POST", second.getMethod());
        assertEquals("/event", second.getUrl().encodedPath());
        assertEquals(first.getConnectionIndex(), second.getConnectionIndex());
        assertEquals(2, server.getRequestCount());
    }

    // CONNECTION REUSE

    @Test
    public void testSuccessesAndSmallFailuresReuseOneConnection() throws Exception {
        for (int i = 0; i < 20; i++) {
            server.enqueue(successResponse().build());
        }
        for (int i = 0; i < 20; i++) {
            server.enqueue(failureResponse(SMALL_BODY_SIZE));
        }
        CollectDispatcher collect = newDispatcher(3000);

        for (int i = 0; i < 20; i++) {
            assertTrue("success " + i, dispatch(collect).success);
        }
        for (int i = 0; i < 20; i++) {
            assertFalse("failure " + i, dispatch(collect).success);
        }

        int connectionIndex = takeRequest().getConnectionIndex();
        for (int i = 1; i < 40; i++) {
            assertEquals("request " + i, connectionIndex, takeRequest().getConnectionIndex());
        }
    }

    @Test
    public void testBurstsLeaveAtMostOneConnectionOpen() throws Exception {
        CollectDispatcher collect = newDispatcher(3000);

        for (int i = 0; i < BURST_SIZE; i++) {
            server.enqueue(failureResponse(SMALL_BODY_SIZE));
        }
        for (int i = 0; i < BURST_SIZE; i++) {
            assertFalse(dispatch(collect).success);
        }
        assertAtMostOneConnectionOpen("500 with a body under 8 KB");

        for (int i = 0; i < BURST_SIZE; i++) {
            server.enqueue(failureResponse(LARGE_BODY_SIZE));
        }
        for (int i = 0; i < BURST_SIZE; i++) {
            assertFalse(dispatch(collect).success);
        }
        assertAtMostOneConnectionOpen("500 with a body over 8 KB");

        for (int i = 0; i < BURST_SIZE; i++) {
            server.enqueue(successResponse()
                    .headersDelay(SHORT_READ_TIMEOUT * 2, TimeUnit.MILLISECONDS)
                    .build());
        }
        CollectDispatcher timingOut = newDispatcher(SHORT_READ_TIMEOUT);
        for (int i = 0; i < BURST_SIZE; i++) {
            assertFalse(dispatch(timingOut).success);
        }
        assertAtMostOneConnectionOpen("read timeout");
    }

    // BROKEN RESPONSE BODIES

    @Test
    public void testSuccessWithCutOffBodyIsSuccessAndNextRequestUsesNewConnection() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .chunkedBody(body(SMALL_BODY_SIZE), Integer.MAX_VALUE)
                .onResponseBody(new SocketEffect.CloseSocket())
                .build());
        server.enqueue(successResponse().build());
        CollectDispatcher collect = newDispatcher(3000);

        assertTrue(dispatch(collect).success);
        assertTrue(dispatch(collect).success);

        assertNotEquals(takeRequest().getConnectionIndex(), takeRequest().getConnectionIndex());
        assertTrue(logged().contains("Could not read the response body"));
    }

    @Test
    public void testSuccessWithBodyDelayedPastReadTimeoutIsSuccessAndNextRequestUsesNewConnection()
            throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .chunkedBody(body(SMALL_BODY_SIZE), Integer.MAX_VALUE)
                .bodyDelay(SHORT_READ_TIMEOUT * 3, TimeUnit.MILLISECONDS)
                .build());
        server.enqueue(successResponse().build());
        CollectDispatcher collect = newDispatcher(SHORT_READ_TIMEOUT);

        assertTrue(dispatch(collect).success);
        assertTrue(dispatch(collect).success);

        assertNotEquals(takeRequest().getConnectionIndex(), takeRequest().getConnectionIndex());
        assertTrue(logged().contains("Could not read the response body"));
    }

    @Test
    public void testFailureWithCutOffBodyIsReportedAsFailedRequest() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(500)
                .chunkedBody(body(SMALL_BODY_SIZE), Integer.MAX_VALUE)
                .onResponseBody(new SocketEffect.CloseSocket())
                .build());
        server.enqueue(successResponse().build());
        CollectDispatcher collect = newDispatcher(3000);

        Result result = dispatch(collect);
        assertTrue(dispatch(collect).success);

        assertFalse(result.success);
        assertFailedRequest(result);
        assertTrue(result.error, result.error.contains("; body: "));
        assertTrue(result.error, result.error.contains("x"));
        assertNotEquals(takeRequest().getConnectionIndex(), takeRequest().getConnectionIndex());
    }

    // ERROR MESSAGES

    @Test
    public void test400WithShortBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder().code(400).body("bad request").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 400; body: bad request", result.error);
    }

    @Test
    public void test404WithShortBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder().code(404).body("not found").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 404; body: not found", result.error);
    }

    @Test
    public void test429WithShortBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder().code(429).body("too many requests").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 429; body: too many requests", result.error);
    }

    @Test
    public void test500WithShortBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder().code(500).body("internal error").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 500; body: internal error", result.error);
    }

    @Test
    public void test200WithXErrorAndNoBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(successResponse().addHeader("x-error", "invalid account").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 200; x-error: invalid account",
                result.error);
    }

    @Test
    public void test200WithXErrorAndBodyProducesPinnedErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("x-error", "invalid account")
                .body("rejected")
                .build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals(
                "com.tealium.FailedRequestException: Response code 200; x-error: invalid account; body: rejected",
                result.error);
    }

    @Test
    public void test500WithEmptyBodyOmitsBodyPartOfErrorMessage() throws Exception {
        server.enqueue(new MockResponse.Builder().code(500).body("").build());

        Result result = dispatch(newDispatcher(3000));

        assertEquals("com.tealium.FailedRequestException: Response code 500", result.error);
    }

    @Test
    public void test500WithLongBodyContainingLineBreaksProducesTruncatedPinnedErrorMessage() throws Exception {
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            // alternate line endings so the body exercises both "\r\n" and a bare "\n"
            longBody.append("ab").append(i % 2 == 0 ? "\r\n" : "\n");
        }
        // 100 * "ab" + a line break collapses to 300 chars, well over the 256 char cut
        server.enqueue(new MockResponse.Builder().code(500).body(longBody.toString()).build());

        Result result = dispatch(newDispatcher(3000));

        String collapsed = longBody.toString().replace("\r\n", " ").replace("\r", " ").replace("\n", " ");
        String expectedBody = collapsed.substring(0, 256) + "…";
        assertEquals("com.tealium.FailedRequestException: Response code 500; body: " + expectedBody, result.error);
    }

    // NO RETRIES

    @Test
    public void testServerDisconnectAfterReadingRequestIsFailureWithoutResendingPost() throws Exception {
        server.enqueue(new MockResponse.Builder()
                .onResponseStart(new SocketEffect.CloseSocket())
                .build());
        // a resent POST would get this response, making the dispatch succeed
        server.enqueue(successResponse().build());

        Result result = dispatch(newDispatcher(3000));

        assertFalse(result.success);
        assertTrue(result.error, result.error.startsWith(FailedConnectionException.class.getName()));
        assertEquals("POST", takeRequest().getMethod());
        assertNull(server.takeRequest(500, TimeUnit.MILLISECONDS));
        assertEquals(1, server.getRequestCount());
    }

    // RELEASE CONNECTION (UNIT)

    @Test
    public void testReleaseConnectionReadsAtMost8KbOfFailureBody() throws Exception {
        CountingInputStream body = new CountingInputStream(LARGE_BODY_SIZE);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(body, true);

        byte[] result = newDispatcher(3000).releaseConnection(connection, 500, false);

        assertEquals(CollectDispatcher.MAX_FAILURE_BODY_BYTES, body.bytesRead);
        assertEquals(CollectDispatcher.MAX_FAILURE_BODY_BYTES, result.length);
        assertTrue(body.closed);
        assertFalse(connection.disconnected);
    }

    @Test
    public void testReleaseConnectionReadsEntireSuccessBodyAndReturnsNull() throws Exception {
        int bodySize = 1024 * 1024;
        CountingInputStream body = new CountingInputStream(bodySize);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(body, false);

        byte[] result = newDispatcher(3000).releaseConnection(connection, 200, true);

        assertEquals(bodySize, body.bytesRead);
        assertNull(result);
        assertTrue(body.closed);
        assertFalse(connection.disconnected);
    }

    @Test
    public void testReleaseConnectionReturnsNullWhenThereIsNoStream() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(null, true);

        byte[] result = newDispatcher(3000).releaseConnection(connection, 500, false);

        assertNull(result);
        assertFalse(connection.disconnected);
    }

    @Test
    public void testReleaseConnectionReturnsBytesReadBeforeAReadFailure() throws Exception {
        byte[] partialBody = "partial body before the stream breaks".getBytes(StandardCharsets.UTF_8);
        FakeHttpURLConnection connection = new FakeHttpURLConnection(new BreakingInputStream(partialBody), true);

        byte[] result = newDispatcher(3000).releaseConnection(connection, 500, false);

        assertArrayEquals(partialBody, result);
        assertTrue(connection.disconnected);
    }

    // HELPERS

    private CollectDispatcher newDispatcher(int readTimeout) {
        Logger logger = new Logger(LogLevel.VERBOSE, new PrintStream(log, true));
        return new CollectDispatcher(server.url("/event").toString(), TestLibraryContext.newInstance(logger),
                readTimeout);
    }

    private static Result dispatch(CollectDispatcher collect) throws CollectDispatchException {
        final Result result = new Result();
        DispatchCallback callback = (success, info, error) -> {
            result.success = success;
            result.info = info;
            result.error = error;
        };
        collect.dispatch(new Udo(), callback);
        assertNotNull("callback wasn't called", result.info);
        return result;
    }

    /**
     * The dispatch failed after an HTTP response was received, rather than because of a connection failure.
     */
    private static void assertFailedRequest(Result result) {
        assertNotNull(result.error);
        assertTrue(result.error, result.error.startsWith(FailedRequestException.class.getName()));
        assertNotNull(result.info.get(DataManager.InfoKey.RESPONSE_HEADERS));
    }

    private void assertAtMostOneConnectionOpen(String burst) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (socketFactory.openConnections() > 1 && System.nanoTime() < deadline) {
            //noinspection BusyWait: polling for socket closes done by MockWebServer's threads; there's no event to wait on
            Thread.sleep(20);
        }
        assertTrue("after the '" + burst + "' burst, " + socketFactory.openConnections()
                        + " connections are still open",
                socketFactory.openConnections() <= 1);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull("server didn't receive the request", request);
        return request;
    }

    private static MockResponse.Builder successResponse() {
        return new MockResponse.Builder().code(200).chunkedBody("", Integer.MAX_VALUE);
    }

    private static MockResponse failureResponse(int bodySize) {
        return new MockResponse.Builder().code(500).chunkedBody(body(bodySize), Integer.MAX_VALUE).build();
    }

    private static String body(int size) {
        StringBuilder body = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            body.append('x');
        }
        return body.toString();
    }

    private String logged() {
        return new String(log.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class Result {
        boolean success;
        Map<String, Object> info;
        String error;
    }

    /**
     * A minimal {@code HttpURLConnection} fake for driving {@link CollectDispatcher#releaseConnection} directly,
     * without a real connection. {@code bodyIsError} picks whether the body comes back from
     * {@link #getInputStream()} (as for a successful response) or {@link #getErrorStream()} (as for a failed one).
     */
    private static final class FakeHttpURLConnection extends HttpURLConnection {

        final InputStream body;
        private final boolean bodyIsError;
        boolean disconnected;

        FakeHttpURLConnection(InputStream body, boolean bodyIsError) throws Exception {
            super(new URL("http://localhost/"));
            this.body = body;
            this.bodyIsError = bodyIsError;
        }

        @Override
        public InputStream getInputStream() {
            return bodyIsError ? null : body;
        }

        @Override
        public InputStream getErrorStream() {
            return bodyIsError ? body : null;
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }
    }

    /**
     * A body stream that counts the bytes read out of it and records whether it was closed.
     */
    private static final class CountingInputStream extends ByteArrayInputStream {

        int bytesRead;
        boolean closed;

        CountingInputStream(int size) {
            super(new byte[size]);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            // Short reads, like a socket, so a read loop that overshoots its limit is caught.
            int read = super.read(buffer, offset, Math.min(length, 1000));
            if (read > 0) {
                bytesRead += read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /**
     * A body stream that hands out a fixed chunk of data, in short reads like a socket, then fails as if the
     * underlying connection broke.
     */
    private static final class BreakingInputStream extends InputStream {

        private final byte[] data;
        private int position;

        BreakingInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (position >= data.length) {
                throw new IOException("simulated read failure");
            }
            int read = Math.min(length, data.length - position);
            System.arraycopy(data, position, buffer, offset, read);
            position += read;
            return read;
        }
    }

    /**
     * Creates server sockets that count the connections they accept and how many of those are gone: closed by
     * the server, or closed by the client as seen by the server reading the end of the stream or failing to read
     * or write.
     *
     * The client side counts too because MockWebServer doesn't close a socket whose read or write fails: when the
     * client gives up on a delayed response, the server's late write makes the client reset the connection, and
     * the server then leaves its end open until it shuts down.
     */
    private static final class CountingServerSocketFactory extends ServerSocketFactory {

        private final AtomicInteger accepted = new AtomicInteger();
        private final AtomicInteger closed = new AtomicInteger();

        int openConnections() {
            return accepted.get() - closed.get();
        }

        @Override
        public ServerSocket createServerSocket() throws IOException {
            return new CountingServerSocket();
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            ServerSocket serverSocket = new CountingServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog) throws IOException {
            ServerSocket serverSocket = new CountingServerSocket();
            serverSocket.bind(new InetSocketAddress(port), backlog);
            return serverSocket;
        }

        @Override
        public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException {
            ServerSocket serverSocket = new CountingServerSocket();
            serverSocket.bind(new InetSocketAddress(address, port), backlog);
            return serverSocket;
        }

        private final class CountingServerSocket extends ServerSocket {

            CountingServerSocket() throws IOException {
                super();
            }

            @Override
            public Socket accept() throws IOException {
                if (isClosed()) {
                    throw new SocketException("Socket is closed");
                }
                Socket socket = new CountingSocket();
                implAccept(socket);
                accepted.incrementAndGet();
                return socket;
            }
        }

        private final class CountingSocket extends Socket {

            private final AtomicBoolean counted = new AtomicBoolean();

            @Override
            public synchronized void close() throws IOException {
                try {
                    super.close();
                } finally {
                    countClosed();
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FilterInputStream(super.getInputStream()) {
                    @Override
                    public int read() throws IOException {
                        try {
                            return countEnd(super.read());
                        } catch (IOException e) {
                            countClosed();
                            throw e;
                        }
                    }

                    @Override
                    public int read(byte[] buffer, int offset, int length) throws IOException {
                        try {
                            return countEnd(super.read(buffer, offset, length));
                        } catch (IOException e) {
                            countClosed();
                            throw e;
                        }
                    }
                };
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return new FilterOutputStream(super.getOutputStream()) {
                    @Override
                    public void write(int b) throws IOException {
                        try {
                            super.write(b);
                        } catch (IOException e) {
                            countClosed();
                            throw e;
                        }
                    }

                    @Override
                    public void write(byte[] buffer, int offset, int length) throws IOException {
                        try {
                            out.write(buffer, offset, length);
                        } catch (IOException e) {
                            countClosed();
                            throw e;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        try {
                            super.flush();
                        } catch (IOException e) {
                            countClosed();
                            throw e;
                        }
                    }
                };
            }

            private int countEnd(int read) {
                if (read == -1) {
                    countClosed();
                }
                return read;
            }

            private void countClosed() {
                if (counted.compareAndSet(false, true)) {
                    closed.incrementAndGet();
                }
            }
        }
    }
}
