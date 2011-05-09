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

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.PrefToggle;
import com.alfray.timeriffic.core.profiles1.Columns;

//-----------------------------------------------

public class BluetoothSetting implements ISetting {

    private static final boolean DEBUG = true;
    public static final String TAG = BluetoothSetting.class.getSimpleName();

    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;

    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            try {
                AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (manager == null) return false;

                if (!checkMinApiLevel(5)) return false;

                // Is a bluetooth adapter actually available?
                Class<?> btaClass = Class.forName("android.bluetooth.BluetoothAdapter");

                Method getter = btaClass.getMethod("getDefaultAdapter");
                Object result = getter.invoke(null);
                mIsSupported = result != null;

                if (!mIsSupported) {
                    String fp = Build.FINGERPRINT;
                    if (fp != null &&
                            fp.startsWith("generic/sdk/generic/:") &&
                            fp.endsWith(":eng/test-keys")) {
                        // This looks like an emulator that has no BT emulation.
                        // Just enable it anyway.
                        mIsSupported = true;
                    }
                }

            } catch (Exception e) {
                Log.d(TAG, "Missing BTA API");
            } finally {
                mCheckSupported = false;
            }
        }
        return mIsSupported;
    }

    public Object createUi(Activity activity, String[] currentActions) {
        PrefToggle p = new PrefToggle(activity,
                        R.id.bluetoothButton,
                        currentActions,
                        Columns.ACTION_BLUETOOTH,
                        activity.getString(R.string.editaction_bluetooth));
        p.setEnabled(isSupported(activity), activity.getString(R.string.setting_not_supported));
        return p;
    }

    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefToggle) {
            ((PrefToggle) settingUi).collectResult(outActions);
        }
    }

    public String getActionLabel(Context context, String action) {
        try {
            int value = Integer.parseInt(action.substring(1));
            return context.getString(value > 0 ? R.string.timedaction_bluetooth_on :
                                                 R.string.timedaction_bluetooth_off);

        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    public boolean performAction(Context context, String action) {
        try {
            Object t = context.getSystemService(Context.TELEPHONY_SERVICE);
            if (t instanceof TelephonyManager) {
                if (((TelephonyManager) t).getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    // There's an ongoing call or a ringing one.
                    // Either way, not a good time to switch bluetooth on or off.
                    return false;
                }
            }

            int value = Integer.parseInt(action.substring(1));
            change(value > 0);
        } catch (Throwable e) {
            if (DEBUG) Log.e(TAG, "Perform action failed for " + action, e);
        }

        return true;
    }

    // ----

    private boolean checkMinApiLevel(int minApiLevel) {
        // Build.SDK_INT is only in API 4 and we're still compatible with API 3
        try {
            int n = Integer.parseInt(Build.VERSION.SDK);
            return n >= minApiLevel;
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse Build.VERSION.SDK=" + Build.VERSION.SDK, e);
        }
        return false;
    }

    private void change(boolean enabled) {
        // This requires permission android.permission.BLUETOOTH_ADMIN

        try {
            Class<?> btaClass = Class.forName("android.bluetooth.BluetoothAdapter");

            Method getter = btaClass.getMethod("getDefaultAdapter");
            Object bt = getter.invoke(null);

            if (bt == null) {
                if (DEBUG) Log.w(TAG, "changeBluetooh: BluetoothAdapter null!");
                return;
            }

            if (DEBUG) Log.d(TAG, "changeBluetooh: " + (enabled ? "on" : "off"));

            if (enabled) {
                bt.getClass().getMethod("enable").invoke(bt);
            } else {
                bt.getClass().getMethod("disable").invoke(bt);
            }

        } catch (Exception e) {
            if (DEBUG) Log.d(TAG, "Missing BTA API");
        }
    }
}


