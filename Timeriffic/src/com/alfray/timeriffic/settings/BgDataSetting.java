/*
 * Project: Timeriffic
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

package com.alfray.timeriffic.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.content.Intent;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.actions.PrefToggle;
import com.alfray.timeriffic.profiles.Columns;

//-----------------------------------------------

public class BgDataSetting implements ISetting {


    private static final boolean DEBUG = true;
    private static final String TAG = "BgDataSetting";

    private boolean mCheckSupported = true;
    private Method mSetBgDataMethod;

    @Override
    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            // Check whether we can access ConnectivityManager#setBackgroundDataSetting
            try {
                ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (conMan != null) {
                    mSetBgDataMethod = conMan.getClass().getMethod("setBackgroundDataSetting", new Class<?>[] { boolean.class });
                }
            } catch (Exception e) {

            } finally {
                mCheckSupported = false;
            }
        }

        return mSetBgDataMethod != null;
    }

    @Override
    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        R.id.apndroidButton,
                        currentActions,
                        Columns.ACTION_BG_DATA,
                        activity.getString(R.string.editaction_airplane));
        p.setEnabled(isSupported(activity), activity.getString(R.string.setting_not_supported));
        return p;
    }

    @Override
    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefToggle) {
            ((PrefToggle) settingUi).collectResult(outActions);
        }
    }

    @Override
    public String getActionLabel(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            return context.getString(value > 0 ? R.string.timedaction_airplane_on :
                                                 R.string.timedaction_airplane_off);
        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    @Override
    public void performAction(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            change(context, value > 0);
        } catch (NumberFormatException e) {
            if (DEBUG) Log.d(TAG, "Perform action failed for " + action);
        }
    }

    /** Changes the background data mode */
    private void change(Context context, boolean turnOn) {

        if (DEBUG) Log.d(TAG, "changeBgDataMode: " + (turnOn ? "on" : "off"));

//        try {
//            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//            mSetBgDataMethod.invoke(conMan, new Object[] { turnOn });
//        } catch (Exception e) {
//            if (DEBUG) Log.e(TAG, "Change failed", e);
//        }

        try {
            // Change the system setting

            Context tc = new TestContext(context);

            final Uri CONTENT_URI = Uri.parse("content://" + Settings.AUTHORITY + "/secure");
            ContentResolver r = context.getContentResolver();

            Field field = ContentResolver.class.getDeclaredField("mContext");
            field.setAccessible(true);
            Object old_context = field.get(r);
            TestContext new_context = new TestContext((Context) old_context);
            field.set(r, new_context);

            String name = Settings.Secure.BACKGROUND_DATA;
            String value = Integer.toString(turnOn ? 1 : 0);
            Settings_NameValueTable_putString(r, CONTENT_URI, name, value);

            try {
                ConnectivityManager conMan =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                boolean isTurnedOn = conMan.getBackgroundDataSetting();
                if (isTurnedOn != turnOn) Log.e(TAG, "Change failed. Current value is " + Boolean.toString(isTurnedOn));
              } catch (Exception e) {
              }

            // Post the intent
            Intent broadcast = new Intent(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED);
            context.sendBroadcast(broadcast);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Change failed", e);
        }
    }

    protected static boolean Settings_NameValueTable_putString(ContentResolver resolver, Uri uri,
                    String name, String value) {
        // The database will take care of replacing duplicates.
        try {
            ContentValues values = new ContentValues();
            values.put(Settings.NameValueTable.NAME, name);
            values.put(Settings.NameValueTable.VALUE, value);
            resolver.insert(uri, values);
            return true;
        } catch (SQLException e) {
            Log.w(TAG, "Can't set key " + name + " in " + uri, e);
            return false;
        }
    }

    public static class TestContext extends ContextWrapper {

        public TestContext(Context base) {
            super(base);
        }

        @Override
        public int checkCallingOrSelfPermission(String permission) {
            return super.checkCallingOrSelfPermission(permission);
        }

    }


}


