package com.tealium;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.tealium.Tealium.DispatchCallback;

import static org.junit.Assert.*;

/**
 * Test logic related to Tealium
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class TealiumTests {

    @Test
    public void testTealiumInit() throws Exception {
        new Tealium.Builder("tealiummobile", "main").build();
    }

    @Test
    public void testTealiumInitWithLogLevel() throws Exception {
        new Tealium.Builder("tealiummobile", "main")
                .setLogLevel(LogLevel.WARNINGS)
                .build();

    }

    @Test
    public void testTrackCallContainsCorrectData() throws InterruptedException {

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

        final CountDownLatch barrier = new CountDownLatch(3);

        // Keys and values should have been checked by other class tests - just double checking keys here
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

        DispatchCallback callBack = new DispatchCallback() {
            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                Udo payload = (Udo) info.get("payload");

                assertTrue(udoContainsKeysFromUdo(payload, expectedKeysImmutable));
                assertFalse(payload.containsKey("tealium_visitor_id"));
                assertFalse(payload.containsKey("tealium_vid"));

                barrier.countDown();
            }
        };

        Tealium tealium = new Tealium.Builder("tealiummobile", "demo")
                .setEnvironment("env")
                .setDatasource("datasource")
                .setPersistentData(persistentUdoFake)
                .build();

        tealium.track("test", null, callBack);
        barrier.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testTrackCallPayloadAndPersistentDataAreNotSame() throws InterruptedException {

        final CountDownLatch barrier = new CountDownLatch(3);
        Udo event = new Udo();
        event.put("event_data", "event_value");

        final Tealium tealium = new Tealium.Builder("tealiummobile", "demo")
                .setEnvironment("env")
                .setDatasource("datasource")
                .build();

        DispatchCallback callBack = new DispatchCallback() {
            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                Udo payload = (Udo) info.get("payload");

                Udo persistentData = tealium.getDataManager().getPersistentData();
                assertNotSame(payload, persistentData);
                assertFalse(persistentData.containsKey("event_data"));

                barrier.countDown();
            }
        };


        tealium.track("test", event, callBack);
        barrier.await(1, TimeUnit.SECONDS);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

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
