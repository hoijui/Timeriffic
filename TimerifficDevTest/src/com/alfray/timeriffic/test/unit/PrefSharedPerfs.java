/*
 * (c) ralfoide gmail com, 2009
 * Project: TimerifficTest
 * License GPLv3
 */

/**
 *
 */
package com.alfray.timeriffic.test.unit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

public class PrefSharedPerfs extends AndroidTestCase {

    private static final String TAG = PrefSharedPerfs.class.getSimpleName();
    private SharedPreferences p;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        p = getContext().getSharedPreferences("test_prefs_perfs", Context.MODE_WORLD_READABLE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSharedPrefsPerfs_1_write() throws Exception {
        // clear any old data
        assertTrue(p.edit().clear().commit());
        System.gc();
        System.gc();

        long t1 = SystemClock.currentThreadTimeMillis();
        
        // Write a bunch of things, committing them every single time since that's how
        // we use the PrefsValues.
        for (int i = 0; i < 100; i++) {
            Editor e = p.edit();
            String key = "integer-value-" + Integer.toString(i);
            e.putInt(key, i);
            assertTrue(e.commit());
        }

        long t2 = SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < 100; i++) {
            Editor e = p.edit();
            String key = "string-value-" + Integer.toString(i);
            e.putString(key, key);
            assertTrue(e.commit());
        }

        long t3 = SystemClock.currentThreadTimeMillis();
        String key = "append-to-same-string";
        for (int i = 0; i < 100; i++) {
            String v = p.getString(key, "");
            Editor e = p.edit();
            String s = " append-to-same-string" + Integer.toString(i);
            e.putString(key, v + s);
            assertTrue(e.commit());
        }
        
        long t4 = SystemClock.currentThreadTimeMillis();
        
        Log.d(TAG, String.format("SharedPrefs Prefs: WRITE=int %d, str1 %d, str2 %d, total %d ms",
                t2-t1, t3-t2, t4-t3, t4-t1));
    }

    public void testSharedPrefsPerfs_2_read() throws Exception {
        System.gc();
        System.gc();

        long t1 = SystemClock.currentThreadTimeMillis();

        // Read the stuff from step 1
        
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
        
        Log.d(TAG, String.format("SharedPrefs Prefs: READ=int %d, str1 %d, str2 %d, total %d ms",
                t2-t1, t3-t2, t4-t3, t4-t1));
    }
}
