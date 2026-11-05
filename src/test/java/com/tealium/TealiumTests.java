package com.tealium;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.jr.ob.JSON;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tealium.Tealium.DispatchCallback;

import static org.junit.Assert.*;

/**
 * Test logic related to Tealium
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TealiumTests {

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

    @Test
    public void testTealiumInit() {
        new Tealium.Builder("tealiummobile", "main").build();
    }

    @Test
    public void testTealiumInitWithLogLevel() {
        new Tealium.Builder("tealiummobile", "main")
                .setLogLevel(LogLevel.WARNINGS)
                .build();

    }

    @Test
    public void testTrackPayloadContainsStandardKeysAndNoVisitorId() throws InterruptedException {
        server.enqueue(new MockResponse.Builder().code(200).build());

        // create a fake persistent data that just returns the default data
        PersistentUdo persistentUdoFake = new PersistentUdo(null) {
            @Override
            public Udo readOrCreateUdo(Udo defaultData) {
                return defaultData;
            }

            @Override
            public void writeData(Udo data) {

            }
        };

        final CountDownLatch barrier = new CountDownLatch(1);

        // Keys and values should have been checked by other class tests - just double-checking keys here
        DispatchCallback callBack = getDispatchCallback(barrier);

        Tealium tealium = buildTealium(persistentUdoFake);

        tealium.track("test", null, callBack);
        assertTrue(barrier.await(5, TimeUnit.SECONDS));
    }

    private DispatchCallback getDispatchCallback(CountDownLatch barrier) {
        Udo expectedKeys = new Udo();
        expectedKeys.put("event_name", "value");
        expectedKeys.put("tealium_account", "value");
        expectedKeys.put("tealium_environment", "value");
        expectedKeys.put("tealium_datasource", "value");
        expectedKeys.put("tealium_event", "value");
        expectedKeys.put("tealium_event_type", "value");
        expectedKeys.put("tealium_library_name", "value");
        expectedKeys.put("tealium_library_version", "value");
        expectedKeys.put("tealium_profile", "value");
        expectedKeys.put("tealium_random", "value");
        expectedKeys.put("tealium_session_id", "value");
        expectedKeys.put("tealium_timestamp_epoch", "value");

        final Udo expectedKeysImmutable = new Udo(expectedKeys);

        return (success, info, error) -> {
            Udo payload = (Udo) info.get("payload");

            assertTrue(udoContainsKeysFromUdo(payload, expectedKeysImmutable));
            assertFalse(payload.containsKey("tealium_visitor_id"));
            assertFalse(payload.containsKey("tealium_vid"));

            barrier.countDown();
        };
    }

    @Test
    public void testTrackDoesNotAddEventDataToPersistentData() throws InterruptedException {
        server.enqueue(new MockResponse.Builder().code(200).build());

        final CountDownLatch barrier = new CountDownLatch(1);
        Udo event = new Udo();
        event.put("event_data", "event_value");

        final Tealium tealium = buildTealium(TestUtils.dummyPersistentUdo());

        DispatchCallback callBack = (success, info, error) -> {
            Udo payload = (Udo) info.get("payload");

            Udo persistentData = tealium.getDataManager().getPersistentData();
            assertNotSame(payload, persistentData);
            assertFalse(persistentData.containsKey("event_data"));

            barrier.countDown();
        };


        tealium.track("test", event, callBack);
        assertTrue(barrier.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testTrackSendsMergedPayloadToServer() throws Exception {
        server.enqueue(new MockResponse.Builder().code(200).build());

        // fake persistent data that returns the default data plus a marker key, so we can confirm it
        // ends up in the dispatched payload alongside the event data and account/profile.
        PersistentUdo persistentUdoFake = new PersistentUdo(null) {
            @Override
            public Udo readOrCreateUdo(Udo defaultData) {
                defaultData.put("custom_persistent_key", "custom_persistent_value");
                return defaultData;
            }

            @Override
            public void writeData(Udo data) {

            }
        };

        Tealium tealium = buildTealium(persistentUdoFake);

        Udo eventData = new Udo();
        eventData.put("event_data", "event_value");

        final CountDownLatch barrier = new CountDownLatch(1);
        tealium.track("test", eventData, (success, info, error) -> barrier.countDown());

        assertTrue(barrier.await(5, TimeUnit.SECONDS));

        RecordedRequest recordedRequest = server.takeRequest();
        Map<String, Object> bodyMap = JSON.std.mapFrom(recordedRequest.getBody().utf8());

        assertEquals("event_value", bodyMap.get("event_data"));
        assertEquals("tealiummobile", bodyMap.get("tealium_account"));
        assertEquals("demo", bodyMap.get("tealium_profile"));
        assertEquals("custom_persistent_value", bodyMap.get("custom_persistent_key"));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    @SuppressWarnings("deprecation")
    private Tealium buildTealium(PersistentUdo persistentData) {
        CollectDispatcher collectDispatcher = new CollectDispatcher(server.url("/event").toString(),
                TestLibraryContext.newInstance(), 3000);

        return new Tealium.Builder("tealiummobile", "demo")
                .setEnvironment("env")
                .setDatasource("datasource")
                .setCollectDispatcher(collectDispatcher)
                .setPersistentData(persistentData)
                .build();
    }

    boolean udoContainsKeysFromUdo(Udo sourceUdo, Udo subsetUdo) {

        Set<String> sourceKeys = sourceUdo.keySet();
        Set<String> subSetKeys = subsetUdo.keySet();

        for (String k : subSetKeys) {
            if (!sourceKeys.contains(k)) {
                System.out.println("Source map does not contain key from subSetMap: " + k);
                return false;
            }

        }

        return true;
    }

}
