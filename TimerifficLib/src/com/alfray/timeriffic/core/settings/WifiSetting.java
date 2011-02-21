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

package com.alfray.timeriffic.core.settings;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.PrefToggle;
import com.alfray.timeriffic.core.profiles1.Columns;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

//-----------------------------------------------

public class WifiSetting implements ISetting {

    private static final boolean DEBUG = true;
    public static final String TAG = WifiSetting.class.getSimpleName();

    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;

    @Override
    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            mIsSupported = manager != null;
            mCheckSupported = false;
        }
        return mIsSupported;
    }

    @Override
    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        R.id.wifiButton,
                        currentActions,
                        Columns.ACTION_WIFI,
                        activity.getString(R.string.editaction_wifi));
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
            return context.getString(value > 0 ? R.string.timedaction_wifi_on :
                                                 R.string.timedaction_wifi_off);
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
        } catch (Throwable e) {
            if (DEBUG) Log.e(TAG, "Perform action failed for " + action, e);
        }

        return true;
    }

    private void change(Context context, boolean enabled) {
        // This requires two permissions:
        //     android.permission.ACCESS_WIFI_STATE
        // and android.permission.CHANGE_WIFI_STATE

        if (DEBUG) Log.d(TAG, "changeWifi: " + (enabled ? "on" : "off"));

        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            if (manager == null) {
                if (DEBUG) Log.w(TAG, "changeWifi: WIFI_SERVICE missing!");
                return;
            }
            manager.setWifiEnabled(enabled);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Change failed", e);
        }
    }

}


