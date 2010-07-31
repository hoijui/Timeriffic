/*
 * (c) ralfoide gmail com, 2009
 * Project: TimerifficTest
 * License GPLv3
 */

/**
 *
 */
package com.alfray.timeriffic.test.unit;

import java.util.Locale;

import com.alfray.timeriffic.actions.TimedActionUtils;

import junit.framework.TestCase;

public class TimedActionUtilsTest extends TestCase {

    private static class MockTimedActionUtils extends TimedActionUtils {

        public String[] _getDaysNames() {
            TimedActionUtils.sDaysNames = null;
            return TimedActionUtils.getDaysNames();
        }

    }

    private MockTimedActionUtils m;
    private Locale mDefaultLocale;

    @Override
    protected void setUp() throws Exception {
        mDefaultLocale = Locale.getDefault();
        m = new MockTimedActionUtils();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        Locale.setDefault(mDefaultLocale);
        m = null;
        super.tearDown();
    }

    public void testInitDaysNames_en() throws Exception {

        Locale.setDefault(new Locale("en"));

        String[] days = m._getDaysNames();
        assertNotNull(days);
        assertEquals(7, days.length);

        String[] expected = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

        for (int i = 0; i < 7; i++) {
            assertEquals(expected[i], days[i]);
        }
    }

    public void testInitDaysNames_fr() throws Exception {

        Locale.setDefault(new Locale("fr"));

        String[] days = m._getDaysNames();
        assertNotNull(days);
        assertEquals(7, days.length);

        String[] expected = { "lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim." };

        for (int i = 0; i < 7; i++) {
            assertEquals(expected[i], days[i]);
        }
    }
}
