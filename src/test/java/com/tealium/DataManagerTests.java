package com.tealium;

import com.tealium.DataManager.Key;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test logic related to DataManager
 *
 * Jason Koo, Chad Hartman, Karen Tamayo, Merritt Tidwell, Chris Anderberg
 */
public class DataManagerTests {

    @Test
    public void testInit() throws Exception {
        TestLibraryContext testCtx = TestLibraryContext.newInstance();
        new DataManager(testCtx, TestUtils.dummyPersistentUdo());
    }

    @Test
    public void testRandom() throws Exception {
        TestLibraryContext testCtx = TestLibraryContext.newInstance();
        DataManager data = new DataManager(testCtx, TestUtils.dummyPersistentUdo());
        ArrayList<String> randomArray = new ArrayList<String>();
        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("getRandom");
        method.setAccessible(true);
        for (int i = 0; i < 100; i++) {
            String random = (String) method.invoke(data);
            if (!randomArray.contains(random)) {
                randomArray.add(random);
                // check length
                if (!random.matches("^[0-9]{16}$")){
                    fail("Random value is not 16 digits");
                }
            } else {
                fail("Random value has been repeated");
            }
        }
    }

    @Test
    public void testGetPersistentData() throws Exception {

        LibraryContext ctx = TestLibraryContext.newInstance();

        DataManager data = new DataManager(ctx, TestUtils.dummyPersistentUdo());
        Udo map = new Udo();

        String key = "testKey";
        String value = "testValue";

        map.put(key, value);

        // Write to disk first
        data.addPersistentData(map);

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("getPersistentData");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Udo savedData = (Udo) method.invoke(data);

        assertTrue(savedData.containsKey(key));
        assertTrue(savedData.containsValue(value));
    }

    /*
     * Test that new data doesn't contain visitor id or vid
     * Test that new data contains expected variables
     */
    @Test
    public void testNewPersistentData() throws PersistentDataAccessException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {

        String account = "account";
        String profile = "profile";
        String env = "env";
        String datasource = "datasource";
        LibraryContext ctx = new LibraryContext(account, profile, env, datasource, new Logger(LogLevel.VERBOSE));

        DataManager data = new DataManager(ctx, TestUtils.dummyPersistentUdo());

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("createNewPersistentData");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Udo persistentData = (Udo) method.invoke(data);

        Udo expectedData = new Udo();
        expectedData.put("tealium_account", account);
        expectedData.put("tealium_profile", profile);
        expectedData.put("tealium_environment", env);
        expectedData.put("tealium_library_name", "java");
        expectedData.put("tealium_library_version", LibraryContext.version);

        assertTrue(udoContainsUdo(persistentData, expectedData));
        assertFalse(persistentData.containsKey("tealium_vid"));
        assertFalse(persistentData.containsKey("tealium_visitor_id"));
    }

    /*
     * Test that new data doesn't contain visitor id or vid
     */
    @Test
    public void testNewPersistentDataDoesNotContainVisitorId() throws PersistentDataAccessException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String account = "account";
        String profile = "profile";
        String env = "env";
        String datasource = "datasource";
        LibraryContext ctx = new LibraryContext(account, profile, env, datasource, new Logger(LogLevel.VERBOSE));

        DataManager data = new DataManager(ctx, TestUtils.dummyPersistentUdo());

        Class<? extends DataManager> myclass = data.getClass();
        Method method = myclass.getDeclaredMethod("createNewPersistentData");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> persistentData = (Map<String, Object>) method.invoke(data);

        assertFalse(persistentData.containsKey(Key.TEALIUM_VISITOR_ID));
        assertFalse(persistentData.containsKey("tealium_vid"));
    }

    // test that two consecutive session id resets don't result in the same new session id
    @Test
    public void testResetSessionId() throws InterruptedException, PersistentDataAccessException {

        LibraryContext ctx = TestLibraryContext.newInstance();

        DataManager data = new DataManager(ctx, TestUtils.dummyPersistentUdo());

        String sessionId = data.resetSessionId();
        Thread.sleep(100);
        String sessionId2 = data.resetSessionId();

        assertFalse(sessionId.equals(sessionId2));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /*
     * Determine if the set of key/value pairs in one map is a subset of the key/value pairs of another map
     */
    boolean udoContainsUdo(Udo sourceMap, Udo subSetMap) {

        Set<String> sourceKeys = sourceMap.keySet();
        Set<String> subSetKeys = subSetMap.keySet();

        for (String k : subSetKeys) {
            if (!sourceKeys.contains(k)) {
                System.out.println("Source map does not contain key from subSetMap: " + k);
                return false;
            }

            Object sourceValue = sourceMap.get(k);
            Object subSetValue = subSetMap.get(k);
            if (!sourceValue.equals(subSetValue)) {
                System.out.println("Source map key-value: " + k + ":" + sourceValue.toString()
                        + " does not equal subSet's: " + k + ":" + subSetValue.toString());
                return false;
            }
        }

        return true;
    }

}