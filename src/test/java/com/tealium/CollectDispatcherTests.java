package com.tealium;

import com.tealium.Tealium.DispatchCallback;
import org.junit.Test;
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

        DispatchCallback callBack = new DispatchCallback() {

            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {
                System.out.println(error);
                assertTrue(success);
                barrier.countDown();
            }

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

        DispatchCallback callBack = new DispatchCallback() {

            @Override
            public void dispatchComplete(boolean success, Map<String, Object> info, String error) {

                assertFalse(success);
                barrier.countDown();
            }

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
}
