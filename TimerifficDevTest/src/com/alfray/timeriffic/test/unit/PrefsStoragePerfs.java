/*
 * (c) ralfoide gmail com, 2009
 * Project: TimerifficTest
 * License GPLv3
 */

/**
 *
 */
package com.alfray.timeriffic.test.unit;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

import com.alfray.timeriffic.prefs.PrefsStorage;

public class PrefsStoragePerfs extends AndroidTestCase {

    private static final String TAG = PrefsStoragePerfs.class.getSimpleName();
    private PrefsStorage p;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = new PrefsStorage("test_prefs_perfs.sprefs");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPrefsStoragePerfs_1_write() throws Exception {
        System.gc();
        System.gc();

        long t1 = SystemClock.currentThreadTimeMillis();
        
        // Write a bunch of things, committing them every single time since that's how
        // we use the *OLD* PrefsValues.
        for (int i = 0; i < 100; i++) {
            String key = "integer-value-" + Integer.toString(i);
            p.putInt(key, i);
            assertTrue(p.flushSync(getContext()));
        }

        long t2 = SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < 100; i++) {
            String key = "string-value-" + Integer.toString(i);
            p.putString(key, key);
            assertTrue(p.flushSync(getContext()));
        }

        long t3 = SystemClock.currentThreadTimeMillis();
        String key = "append-to-same-string";
        for (int i = 0; i < 100; i++) {
            String v = p.getString(key, "");
            String s = " append-to-same-string" + Integer.toString(i);
            p.putString(key, v + s);
            assertTrue(p.flushSync(getContext()));
        }
        
        long t4 = SystemClock.currentThreadTimeMillis();
        
        Log.d(TAG, String.format("PrefsStorage Prefs: WRITE=int %d, str1 %d, str2 %d, total %d ms",
                t2-t1, t3-t2, t4-t3, t4-t1));
    }

    public void testPrefsStoragePerfs_2_write() throws Exception {
        System.gc();
        System.gc();

        long t1 = SystemClock.currentThreadTimeMillis();
        
        // Write a bunch of things, however this time doesn't commit till the end
        // which is how the *NEW* PrefsValues will be used.
        
        for (int i = 0; i < 100; i++) {
            String key = "integer-value-" + Integer.toString(i);
            p.putInt(key, i);
        }

        long t2 = SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < 100; i++) {
            String key = "string-value-" + Integer.toString(i);
            p.putString(key, key);
        }

        long t3 = SystemClock.currentThreadTimeMillis();
        String key = "append-to-same-string";
        for (int i = 0; i < 100; i++) {
            String v = p.getString(key, "");
            String s = " append-to-same-string" + Integer.toString(i);
            p.putString(key, v + s);
        }
        
        long t4 = SystemClock.currentThreadTimeMillis();

        assertTrue(p.flushSync(getContext()));

        long t5 = SystemClock.currentThreadTimeMillis();

        Log.d(TAG, String.format("PrefsStorage Prefs: WRITE=int %d, str1 %d, str2 %d, flush %d, total %d ms",
                t2-t1, t3-t2, t4-t3, t5-t4, t5-t1));
    }
    
    public void testPrefsStoragePerfs_3_read() throws Exception {
        System.gc();
        System.gc();

        long t0 = SystemClock.currentThreadTimeMillis();
        
        p.beginReadAsync(getContext());
        p.endReadAsync();

        long t1 = SystemClock.currentThreadTimeMillis();

        // Read the stuff from step 2
        
        for (int i = 0; i < 100; i++) {
            String key = "integer-value-" + Integer.toString(i);
            int v = p.getInt(key, -1);
            assertTrue(v >= 0);
        }

        long t2 = SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < 100; i++) {
            String key = "string-value-" + Integer.toString(i);
            String s = p.getString(key, null);
            assertTrue(s != null);
        }

        long t3 = SystemClock.currentThreadTimeMillis();
        String key = "append-to-same-string";
        String s = p.getString(key, null);
        assertTrue(s != null);
        
        long t4 = SystemClock.currentThreadTimeMillis();
        
        Log.d(TAG, String.format("PrefsStorage Prefs: READ=read %d, int %d, str1 %d, str2 %d, total %d ms",
                t1-t0, t2-t1, t3-t2, t4-t3, t4-t0));
    }
}
