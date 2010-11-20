/*
 * Project: NerdkillAndroid
 * Copyright (C) 2010 ralfoide gmail com,
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alfray.timeriffic.test.functional;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.app.IntroActivity;
import com.alfray.timeriffic.app.TimerifficApp;
import com.alfray.timeriffic.error.ErrorReporterUI;
import com.alfray.timeriffic.prefs.PrefsActivity;
import com.alfray.timeriffic.prefs.PrefsValues;
import com.alfray.timeriffic.profiles.ProfilesUI;

//-----------------------------------------------

public class ProfilesUiTest extends ActivityInstrumentationTestCase2<ProfilesUI> {

    private static final String TAG = "ProfilesUiTest";

    private TimerifficApp mApplication;
    private PrefsValues mPV;

    public ProfilesUiTest() {
        super(ProfilesUI.class.getPackage().getName(), ProfilesUI.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startApplication(false /* show auto-intro */);
    }

    @Override
    protected void tearDown() throws Exception {

        if (mApplication != null) {
            mApplication.onTerminate();
            mApplication = null;
        }

        super.tearDown();
    }

    private TimerifficApp startApplication(boolean showAutoIntro) {

        if (mApplication != null) {
            Log.d(TAG, String.format("Reuse application: %s=%s",
                    mApplication.getClass().getSimpleName(),
                    mApplication.toString()));
        }

        if (mApplication == null) {
            Context ac = getInstrumentation().getTargetContext().getApplicationContext();

            if (ac instanceof TimerifficApp) {
                mApplication = (TimerifficApp) ac;
                Log.d(TAG, String.format("Reuse context application: %s=%s",
                        mApplication.getClass().getSimpleName(),
                        mApplication.toString()));
            }
        }


        try {
            if (mApplication == null) {
                mApplication = (TimerifficApp) Instrumentation.newApplication(
                                TimerifficApp.class,
                                getInstrumentation().getTargetContext());
                Log.d(TAG, String.format("Start application: %s=%s",
                        mApplication.getClass().getSimpleName(),
                        mApplication.toString()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Application", e);
        }

        assertNotNull(mApplication);

        // always reset is-first-start
        mApplication.setFirstStart(true);

        // disable auto-start of IntroActivity, activated as needed below.
        mPV = new PrefsValues(mApplication);
        mPV.setIntroDismissed(!showAutoIntro);

        return mApplication;
    }

    // ------------

    /** Starts the activity from cold, with no auto-intro. */
    public void testCold_NoIntro() throws Exception {

        ActivityMonitor monitor = new ActivityMonitor(
                IntroActivity.class.getName(),
                null, // result
                true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        ProfilesUI a = getActivity();
        assertNotNull(a);

        getInstrumentation().waitForIdleSync();
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);
        assertEquals(0, monitor.getHits());
    }

    /** Starts the activity from cold, with auto-intro. */
    public void testCold_WithIntro() throws Exception {

        startApplication(true /* show auto-intro */);

        ActivityMonitor monitor = new ActivityMonitor(
                IntroActivity.class.getName(),
                null, // result
                true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        ProfilesUI a = getActivity();
        assertNotNull(a);

        getInstrumentation().waitForIdleSync();
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);
        assertEquals(1, monitor.getHits());
    }

    /** Starting the application changes its isFirstStart flag. */
    public void testIntro_IsFirstStart() throws Exception {

        TimerifficApp app = startApplication(false);
        app.setFirstStart(true);

        // onCreate will check app.isFirstStart and set it to false
        ProfilesUI a = getActivity();
        assertNotNull(a);

        assertTrue(app.isFirstStart());
    }

    /** Menu>About/Help starts the IntroActivity. */
    public void testMenu_Help() throws Exception {
        ProfilesUI a = getActivity();
        assertNotNull(a);

        ActivityMonitor monitor = new ActivityMonitor(
                        IntroActivity.class.getName(),
                        null, // result
                        true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        boolean ok = getInstrumentation().invokeMenuActionSync(a, R.string.about, 0);
        assertTrue(ok);
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);

        assertEquals(1, monitor.getHits());
    }

    /** Menu>Settings starts the PrefsActivity. */
    public void testMenu_Settings() throws Exception {
        ProfilesUI a = getActivity();
        assertNotNull(a);

        ActivityMonitor monitor = new ActivityMonitor(
                        PrefsActivity.class.getName(),
                        null, // result
                        true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        boolean ok = getInstrumentation().invokeMenuActionSync(a, R.string.settings, 0);
        assertTrue(ok);
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);
        assertEquals(1, monitor.getHits());
    }

    /** Menu>About starts the IntroActivity. */
    public void testMenu_ShowIntro() throws Exception {
        ActivityMonitor monitor = new ActivityMonitor(
                        IntroActivity.class.getName(),
                        null, // result
                        true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        getInstrumentation().addMonitor(monitor);

        ProfilesUI a = getActivity();
        assertNotNull(a);

        boolean ok = getInstrumentation().invokeMenuActionSync(a, R.string.about, 0);
        assertTrue(ok);
        getInstrumentation().waitForIdleSync();
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);
        assertEquals(1, monitor.getHits());
    }

    /** Menu>ErroReport starts the ErrorReportActivity. */
    public void testMenu_ErrorReport() throws Exception {
        ActivityMonitor monitor = new ActivityMonitor(
                        ErrorReporterUI.class.getName(),
                        null, // result
                        true); // block
        getInstrumentation().addMonitor(monitor);
        assertEquals(0, monitor.getHits());

        getInstrumentation().addMonitor(monitor);

        ProfilesUI a = getActivity();
        assertNotNull(a);

        boolean ok = getInstrumentation().invokeMenuActionSync(a, R.string.report_error, 0);
        assertTrue(ok);
        getInstrumentation().waitForIdleSync();
        getInstrumentation().waitForMonitorWithTimeout(monitor, 1000);
        assertEquals(1, monitor.getHits());
    }
}


