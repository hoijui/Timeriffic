/*
 * Project: Timeriffic
 * Copyright (C) 2010 ralfoide gmail com,
 * based on a contribution by:
 *   http://code.google.com/p/autosettings/issues/detail?id=73
 *   Copyright (C) 2010 timendum gmail com,
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

package com.alfray.timeriffic.core.settings;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.PrefToggle;
import com.alfray.timeriffic.core.profiles1.Columns;

//-----------------------------------------------

public class SyncSetting implements ISetting {

    public static final String TAG = SyncSetting.class.getSimpleName();
    private static final boolean DEBUG = true;

    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;

    @Override
    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            try {
                // This will fail to load with a VerifyError exception if the
                // API to set the master sync doesn't exists (Android API >= 5).
                // Also it requires permission android.permission.READ_SYNC_SETTINGS
                SyncHelper.getMasterSyncAutomatically();
                mIsSupported = true;
            } catch (Throwable t) {
                // We expect VerifyError when the API isn't supported.
                Log.d(TAG, "Auto-Sync not supported", t);
            } finally {
                mCheckSupported = false;
            }
        }
        return mIsSupported;
    }

    @Override
    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        -1 /*button id*/,
                        currentActions,
                        Columns.ACTION_SYNC,
                        activity.getString(R.string.editaction_sync));
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
            return context.getString(value > 0 ? R.string.timedaction_sync_on :
                                                 R.string.timedaction_sync_off);
        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    @Override
    public boolean performAction(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            change(context, value > 0);
        } catch (NumberFormatException e) {
            if (DEBUG) Log.d(TAG, "Perform action failed for " + action);
        }

        return true;
    }

    private void change(Context context, boolean enabled) {

        // This requires permission android.permission.WRITE_SYNC_SETTINGS

        if (DEBUG) Log.d(TAG, "changeSync: " + (enabled ? "on" : "off"));

        try {
            if (mIsSupported) {
                SyncHelper.setMasterSyncAutomatically(enabled);
            }
        } catch (Throwable t) {
            if (DEBUG) {
                Log.e(TAG, "Change failed", t);
            }
        }
    }

}


