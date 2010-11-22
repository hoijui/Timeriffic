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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.actions.PrefToggle;
import com.alfray.timeriffic.profiles.Columns;
import com.google.code.apndroid.ApplicationConstants;

//-----------------------------------------------

public class ApnDroidSetting implements ISetting {

    private static final boolean DEBUG = true;
    public static final String TAG = ApnDroidSetting.class.getSimpleName();

    @Override
    public boolean isSupported(Context context) {
        // We don't want to cache the state here -- each time we create the
        // UI we want to check whether the app is installed. That's because
        // the instance has an app-lifetime scope and it's entirely possible
        // for the user to start the app, notice apndroid is missing, install
        // it and come back. The alternative is to listen for app (un)installs
        // but I rather not do that.

        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
        ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    @Override
    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        R.id.apndroidButton,
                        currentActions,
                        Columns.ACTION_APN_DROID,
                        activity.getString(R.string.editaction_apndroid),
                        new String[] {
                            activity.getString(R.string.timedaction_apndroid_on),
                            activity.getString(R.string.timedaction_apndroid_off)
                        } );
        p.setEnabled(isSupported(activity), activity.getString(R.string.setting_not_installed));
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
            return context.getString(value > 0 ? R.string.timedaction_apndroid_on :
                                                 R.string.timedaction_apndroid_off);
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

    private void change(Context context, boolean enabled) {
        if (DEBUG) Log.d(TAG, "changeApnDroid: " + (enabled ? "on" : "off"));
        try {
            Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ApplicationConstants.TARGET_APN_STATE,
                    enabled ? ApplicationConstants.State.ON :
                        ApplicationConstants.State.OFF);
            context.startActivity(intent);
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Change failed", e);
        }
    }

}


