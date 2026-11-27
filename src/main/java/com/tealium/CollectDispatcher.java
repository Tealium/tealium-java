package com.tealium;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tealium.Tealium.DispatchCallback;
import com.tealium.DataManager.InfoKey;


/**
 * Tealium Collect Dispatch service for delivering tracked data to Tealium's
 * Collect Service
 *
 * @author Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
final class CollectDispatcher {

    public final static String DEFAULT_URL = "https://collect.tealiumiq.com/event";

    /** The most bytes of a failed response's body that are read before the stream is closed. */
    static final int MAX_FAILURE_BODY_BYTES = 8192;

    private final String endpoint;
    private final Logger logger;
    private final int timeout;
    private final Integer connectTimeout;

    // =========================================================================
    // PUBLIC
    // =========================================================================

    /**
     * Constructor for creating an instance of the Tealium Collect Dispatch
     * service, with no connect timeout set (so the JVM's
     * {@code -Dsun.net.client.defaultConnectTimeout} system property, or the operating system's own limit,
     * applies instead).
     *
     * @param endpoint
     *            The target url to send track dispatches to. Should NOT include
     *            any ending query strings or "?" suffix.
     * @param context
     *            Library context providing the logger.
     * @param timeout
     *            Read timeout for the collect call, in milliseconds. Applied to each read of the response
     *            individually, not to the request as a whole.
     */
    public CollectDispatcher(String endpoint, LibraryContext context, int timeout) {
        this(endpoint, context, timeout, null);
    }

    /**
     * Constructor for creating an instance of the Tealium Collect Dispatch
     * service.
     *
     * @param endpoint
     *            The target url to send track dispatches to. Should NOT include
     *            any ending query strings or "?" suffix.
     * @param context
     *            Library context providing the logger.
     * @param timeout
     *            Read timeout for the collect call, in milliseconds. Applied to each read of the response
     *            individually, not to the request as a whole.
     * @param connectTimeout
     *            Connect timeout for the collect call, in milliseconds, or null to leave it unset so the JVM's
     *            {@code -Dsun.net.client.defaultConnectTimeout} system property, or the operating system's own
     *            limit, applies instead.
     */
    public CollectDispatcher(String endpoint, LibraryContext context, int timeout, Integer connectTimeout) {
        super();
        this.endpoint = endpoint;
        this.logger = context.getLogger();
        this.timeout = timeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Encodes the data as JSON and POSTs it to the endpoint.
     *
     * @param data
     *            All key-values to be sent with dispatch.
     * @param callback
     *            Optional callback notified of the dispatch result; may be
     *            null.
     * @throws CollectDispatchException
     *             If the data can't be serialized to JSON or the connection
     *             can't be created. The callback is notified before throwing.
     *             Failures while sending, a non-2xx response or an "x-error"
     *             response are reported only through the callback.
     * @see DispatchCallback
     */
    public void dispatch(Udo data, DispatchCallback callback) throws CollectDispatchException {
        String payloadJson = encodePayload(data, callback);
        HttpURLConnection connection = this.getConnection(data, callback);
        this.sendPayload(data, callback, payloadJson, connection);
    }

    /**
     * Encode payload data into JSON, call the callback if it fails
     *
     * @param data The data to encode
     * @param callback The callback to notify on failure, may be null
     * @return The encoded data as a JSON string
     * @throws CollectDispatchException if the data can't be serialized to JSON
     */
    private static String encodePayload(Udo data, DispatchCallback callback) throws CollectDispatchException {
        String payloadJson;

        try {
            payloadJson = data.toJson();
        } catch (UdoSerializationException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because of data serialization error", e);

            callCallback(callback,
                    false,
                    null,
                    null,
                    data,
                    err.toString());
            throw err;
        }

        return payloadJson;
    }

    /**
     * Create and return a new HttpURLConnection for making a collect call, calling the callback if the connection
     * can't be created for some reason.
     *
     * @param data The data being dispatched, passed to the callback on failure
     * @param callback The callback to notify on failure, may be null
     * @return The HttpURLConnection instance to be used to send data
     * @throws CollectDispatchException if the endpoint url is malformed or the connection can't be opened or
     *                                  configured
     */
    private HttpURLConnection getConnection(Udo data, DispatchCallback callback) throws CollectDispatchException {
        HttpURLConnection connection;

        try {
            URL url = new URL(this.endpoint);

            connection = (HttpURLConnection) url.openConnection();

            configureConnection(connection);

        } catch (MalformedURLException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection url is malformed", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    data,
                    err.toString());
            throw err;
        } catch (ProtocolException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection does not support specified http protocol", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    data,
                    err.toString());
            throw err;
        } catch (IOException e) {
            CollectDispatchException err =
                    new CollectDispatchException("Dispatch failed because connection could not be created", e);

            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    data,
                    err.toString());
            throw err;
        }

        return connection;
    }

    /**
     * Test seam: applies this dispatcher's request configuration (headers, method, read timeout, and connect
     * timeout if set) to a connection. Package-private so tests can drive it with a mock
     * {@code HttpURLConnection} without making a real request.
     *
     * @param connection The connection to configure
     * @throws ProtocolException if the connection doesn't support the "POST" request method
     */
    void configureConnection(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // A 3xx is reported as a failure; in streaming mode the JDK couldn't follow it anyway.
        connection.setInstanceFollowRedirects(false);
        connection.setReadTimeout(this.timeout);

        if (this.connectTimeout != null) {
            connection.setConnectTimeout(this.connectTimeout);
        }
    }

    /**
     * Take the payload string and HttpURLConnection and make the collect call,
     * then call the callback with the result. Failures are reported only
     * through the callback, never thrown.
     *
     * @param data The data being dispatched, passed to the callback
     * @param callback The callback to notify of the result, may be null
     * @param payloadJson The JSON-encoded data to send as the request body
     * @param connection The connection returned by {@link #getConnection(Udo, DispatchCallback)}
     */
    private void sendPayload(Udo data, DispatchCallback callback, String payloadJson, HttpURLConnection connection) {
        // Send data and get response
        try {
            Map<String, List<String>> headers;

            int responseCode;

            byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);

            // send the data
            try {
                // Streaming stops the JDK from silently resending the POST when the response read fails.
                connection.setFixedLengthStreamingMode(payload.length);
                connection.connect();
                OutputStream os = connection.getOutputStream();
                os.write(payload);
                os.close();
            } catch (IOException e) {
                connection.disconnect();
                throw new FailedConnectionException("Could not open connection with server: " + e, e);
            }

            // get result
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                connection.disconnect();
                throw new FailedConnectionException("Could not get response from server: " + e, e);
            }

            // check the response for serverside issues, including any complaints about the payload;
            // an "x-error" header takes precedence over the status code
            String responseError = connection.getHeaderField("x-error");
            headers = connection.getHeaderFields();

            boolean success = responseError == null && responseCode >= 200 && responseCode <= 299;

            byte[] body = releaseConnection(connection, responseCode, success);

            if (!success) {
                throw new FailedRequestException(responseCode, responseError, body, headers);
            }

            // call the dispatch callback
            callCallback(callback,
                    true,
                    this.endpoint,
                    headers,
                    data,
                    null);
        } catch (FailedRequestException e) {
            this.logger.log(e.getMessage(), LogLevel.ERRORS);
            callCallback(callback,
                    false,
                    this.endpoint,
                    e.headers,
                    data,
                    e.toString());
        } catch (FailedConnectionException e) {
            this.logger.log(e, LogLevel.ERRORS);
            callCallback(callback,
                    false,
                    this.endpoint,
                    null,
                    data,
                    e.toString());
        }
    }

    /**
     * Test seam: reads the response body so the JDK can reuse the connection, then closes it. A successful
     * response's body is read to the end without being buffered, and this returns null. A failed response's
     * body is buffered and returned, up to {@value #MAX_FAILURE_BODY_BYTES} bytes, including whatever was read
     * before a read failure. On close, the JDK keeps the connection if the rest of the body has already
     * arrived, drains up to 512 KB of it on a background thread, or closes the socket. If reading fails, the
     * connection is disconnected; that doesn't change the dispatch's result.
     *
     * @param connection The connection the response was received on
     * @param responseCode The response's status code
     * @param success Whether the dispatch succeeded
     * @return The failed response's body bytes (at most {@value #MAX_FAILURE_BODY_BYTES}), or null for a
     *         successful response or when there's no body stream
     */
    byte[] releaseConnection(HttpURLConnection connection, int responseCode, boolean success) {
        ByteArrayOutputStream failureBody = success ? null : new ByteArrayOutputStream();

        try {
            InputStream body = responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
            if (body == null) {
                return null;
            }

            int limit = success ? Integer.MAX_VALUE : MAX_FAILURE_BODY_BYTES;
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while (total < limit && (read = body.read(buffer, 0, Math.min(buffer.length, limit - total))) != -1) {
                total += read;
                if (failureBody != null) {
                    failureBody.write(buffer, 0, read);
                }
            }
            body.close();
        } catch (IOException e) {
            connection.disconnect();
            this.logger.log("Could not read the response body: " + e, LogLevel.VERBOSE);
        }

        return failureBody == null ? null : failureBody.toByteArray();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Build the info map from the results of the collect call and pass it to the callback.
     *
     * @param callback The callback to call, does nothing if null
     * @param success true if successful, false otherwise
     * @param encodedUrl The url endpoint, null if the request was never sent
     * @param headerFields Response headers, null if no response was received
     * @param data data sent to endpoint
     * @param errorMessage message of what went wrong, null if nothing went wrong
     */
    private static void callCallback(Tealium.DispatchCallback callback,
                              boolean success,
                              String encodedUrl,
                              Map<String, List<String>> headerFields,
                              Udo data,
                              String errorMessage) {

        if (callback == null) {
            return;
        }

        // General
        Map<String, Object> info = new HashMap<>();
        info.put(InfoKey.DISPATCH_SERVICE, "collect");

        if (encodedUrl != null) {
            info.put(InfoKey.ENCODED_URL, encodedUrl);
        }

        if (data != null) {
            info.put(InfoKey.PAYLOAD, data);
        }

        // Headers
        if (headerFields != null) {
            Map<String, Object> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> k : headerFields.entrySet()) {
                for (String v : k.getValue()) {
                    headers.put(k.getKey(), v);
                }
            }
            info.put(DataManager.InfoKey.RESPONSE_HEADERS, headers);
        }

        callback.dispatchComplete(success, info, errorMessage);

    }
}
