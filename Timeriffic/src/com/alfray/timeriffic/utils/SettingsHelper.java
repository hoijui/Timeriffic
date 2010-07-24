/*
 * Project: Timeriffic
 * Copyright (C) 2008 ralfoide gmail com,
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

package com.alfray.timeriffic.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.google.code.apndroid.ApplicationConstants;

/**
 * Helper class that changes settings.
 * <p/>
 * Methods here directly correspond to something available in the UI.
 * Currently the different cases are:
 * <ul>
 * <li> Ringer: normal, silent..
 * <li> Ringer Vibrate: on, off.
 * <li> Ringer volume: percent.
 * <li> Wifi: on/off.
 * <li> Brightness: percent (disabled due to API)
 * </ul>
 */
public class SettingsHelper {

    private static final boolean DEBUG = true;
    public static final String TAG = "TFC-SettingsH";

    private final Context mContext;

    public SettingsHelper(Context context) {
        mContext = context;
    }

    public boolean canControlWifi() {
        WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        return manager != null;
    }

    public boolean canControlBrigthness() {
        return true;
    }

    public boolean canControlAirplaneMode() {
        return true;
    }

    public boolean canControlAudio() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return manager != null;
    }

    public boolean canControlBluetooth() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) return false;

        if (!checkMinApiLevel(5)) return false;

        // Is a bluetooth adapter actually available?
        try {
            Class<?> btaClass = Class.forName("android.bluetooth.BluetoothAdapter");

            Method getter = btaClass.getMethod("getDefaultAdapter");
            Object result = getter.invoke(null);
            return result != null;

        } catch (Exception e) {
            if (DEBUG) Log.d(TAG, "Missing BTA API");
        }

        return false;
    }

    public boolean canControlApnDroid() {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
        ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

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

    public enum RingerMode {
        /** Normal ringer: actually rings. */
        RING,
        /** Muted ringed. */
        MUTE;

        public char getActionLetter() {
            return (this == RING) ? 'R' : 'M';
        }

        /** Capitalizes the string */
        public String toUiString(Context context) {
            return (this == RING) ?
                context.getString(R.string.ringermode_ring) :
                context.getString(R.string.ringermode_mute);
        }
    }

    public enum VibrateRingerMode {
        /** Vibrate is on (Ringer & Notification) */
        VIBRATE,
        /** Vibrate is off, both ringer & notif */
        NO_VIBRATE_ALL,
        /** Ringer vibrate is off but notif is on */
        NO_RINGER_VIBRATE,
        /** Ringer vibrate is on but notif is off */
        NO_NOTIF_VIBRATE;

        public char getActionLetter() {
            if (this == NO_VIBRATE_ALL) return 'N';
            if (this == NO_RINGER_VIBRATE) return 'R';
            if (this == NO_NOTIF_VIBRATE) return 'O';
            assert this == VIBRATE;
            return 'V';
        }

        /** Capitalizes the string */
        public String toUiString(Context context) {
            if (this == NO_VIBRATE_ALL) {
                return context.getString(R.string.vibrateringermode_no_vibrate);
            }
            if (this == NO_RINGER_VIBRATE) {
                return context.getString(R.string.vibrateringermode_no_ringer_vibrate);
            }
            if (this == NO_NOTIF_VIBRATE) {
                return context.getString(R.string.vibrateringermode_no_notif_vibrate);
            }
            assert this == VIBRATE;
            return context.getString(R.string.vibrateringermode_vibrate);
        }
    }

    // --- ringer: vibrate & volume ---

    /**
     * Notify ring-guard app types that the volume change was automated
     * and intentional.
     *
     * @see http://code.google.com/p/autosettings/issues/detail?id=4
     * @see http://www.openintents.org/en/node/380
     * @param stream One of AudioManager.STREAM_xyz
     * @param volume The new volume level or -1 for a ringer/mute change
     */
    private void broadcastVolumeUpdate(int stream, int volume, int ringMode) {
        try {
            Intent intent = new Intent("org.openintents.audio.action_volume_update");
            intent.putExtra("org.openintents.audio.extra_stream_type", stream);

            if (volume != -1) {
                intent.putExtra("org.openintents.audio.extra_volume_index", volume);
            }
            if (ringMode != -1) {
                intent.putExtra("org.openintents.audio.extra_ringer_mode", ringMode);
            }
            Log.d(TAG, "Notify RingGuard: " + intent.toString() + intent.getExtras().toString());
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void changeRingerVibrate(RingerMode ringer, VibrateRingerMode vib) {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (manager == null) {
            Log.w(TAG, "changeRingerMode: AUDIO_SERVICE missing!");
            return;
        }

        if (DEBUG) Log.d(TAG, String.format("changeRingerVibrate: %s + %s",
                        ringer != null ? ringer.toString() : "ringer-null",
                        vib != null ? vib.toString() : "vib-null"));

        if (vib != null) {
            switch(vib) {
                case VIBRATE:
                    // set both ringer & notification vibrate modes to on
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_RINGER,
                            AudioManager.VIBRATE_SETTING_ON);
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_NOTIFICATION,
                            AudioManager.VIBRATE_SETTING_ON);
                    break;
                case NO_VIBRATE_ALL:
                    // set both ringer & notification vibrate modes to off
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_RINGER,
                            AudioManager.VIBRATE_SETTING_OFF);
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_NOTIFICATION,
                            AudioManager.VIBRATE_SETTING_OFF);
                    break;
                case NO_RINGER_VIBRATE:
                    // ringer vibrate off, notification vibrate on
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_RINGER,
                            AudioManager.VIBRATE_SETTING_OFF);
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_NOTIFICATION,
                            AudioManager.VIBRATE_SETTING_ON);
                    break;
                case NO_NOTIF_VIBRATE:
                    // ringer vibrate on, notification vibrate off
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_RINGER,
                            AudioManager.VIBRATE_SETTING_ON);
                    manager.setVibrateSetting(
                            AudioManager.VIBRATE_TYPE_NOTIFICATION,
                            AudioManager.VIBRATE_SETTING_OFF);
                    break;
            }
        }

        if (ringer != null) {
            switch (ringer) {
                case RING:
                    broadcastVolumeUpdate(AudioManager.STREAM_RING,
                            -1, AudioManager.RINGER_MODE_NORMAL);

                    // normal may or may not vibrate, cf setting above
                    manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    break;
                case MUTE:

                    if (vib != null && vib == VibrateRingerMode.VIBRATE) {
                        broadcastVolumeUpdate(AudioManager.STREAM_RING,
                                0, AudioManager.RINGER_MODE_VIBRATE);
                        manager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    } else {
                        // this turns of the vibrate, which unfortunately doesn't respect
                        // the case where vibrate should not be changed when going silent.
                        // TODO read the system pref for the default "vibrate" mode and use
                        // when vib==null.
                        broadcastVolumeUpdate(AudioManager.STREAM_RING,
                                0, AudioManager.RINGER_MODE_SILENT);
                        manager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                    break;
            }
        }
    }

    private void changeVolume(int stream, int percent) {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (manager == null) {
            if (DEBUG) Log.w(TAG, "changeVolume: AUDIO_SERVICE missing!");
            return;
        }

        if (DEBUG) Log.d(TAG, String.format("changeVolume: stream=%d, vol=%d%%", stream, percent));

        int max = manager.getStreamMaxVolume(stream);
        int vol = (max * percent) / 100;

        broadcastVolumeUpdate(stream, vol, -1);
        manager.setStreamVolume(stream, vol, 0 /*flags*/);
    }

    private int getVolume(int stream) {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (manager == null) {
            if (DEBUG) Log.w(TAG, "getVolume: AUDIO_SERVICE missing!");
            return 50;
        }

        int vol = manager.getStreamVolume(stream);
        int max = manager.getStreamMaxVolume(stream);

        return (vol * 100 / max);
    }

    public void changeRingerVolume(int percent) {
        changeVolume(AudioManager.STREAM_RING, percent);
    }

    public void changeNotificationVolume(int percent) {
        changeVolume(AudioManager.STREAM_NOTIFICATION, percent);
    }

    public void changeMediaVolume(int percent) {
        changeVolume(AudioManager.STREAM_MUSIC, percent);
    }

    public void changeAlarmVolume(int percent) {
        changeVolume(AudioManager.STREAM_ALARM, percent);
    }

    public int getRingerVolume() {
        return getVolume(AudioManager.STREAM_RING);
    }

    public int getNotificationVolume() {
        return getVolume(AudioManager.STREAM_NOTIFICATION);
    }

    public int getMediaVolume() {
        return getVolume(AudioManager.STREAM_MUSIC);
    }

    public int getAlarmVolume() {
        return getVolume(AudioManager.STREAM_ALARM);
    }

    // --- Global Brightness --

    /**
     * @param percent The new value in 0..100 range (will get mapped to adequate OS values)
     * @param persistent True if the setting should be made persistent, e.g. written to system pref.
     *  If false, only the current hardware value is changed.
     */
    public void changeBrightness(int percent, boolean persistent) {
        if (canControlBrigthness()) {
            // Reference:
            // http://android.git.kernel.org/?p=platform/packages/apps/Settings.git;a=blob;f=src/com/android/settings/BrightnessPreference.java
            // The source indicates
            // - Backlight range is 0..255
            // - Must not set to 0 (user would see nothing) so they use 10 as minimum
            // - All constants are in android.os.Power which is hidden in the SDK.
            // - To get value: Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            // - To set value: Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, v);

            Log.d(TAG, "changeBrightness: " + Integer.toString(percent));

            Intent i = new Intent(mContext, ChangeBrightnessActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(ChangeBrightnessActivity.INTENT_SET_BRIGHTNESS, percent / 100.0f);
            mContext.startActivity(i);
        }
    }

    /**
     * Returns screen brightness in range 0..100%.
     * <p/>
     * See comments in {@link #changeBrightness(int)}. The real range is 0..255,
     * maps it 0..100.
     */
    public int getCurrentBrightness() {
        return (int) (100 * ChangeBrightnessActivity.getCurrentBrightness(mContext));
    }

    // --- Wifi ---

    public void changeWifi(boolean enabled) {
        // This requires two permissions:
        //     android.permission.ACCESS_WIFI_STATE
        // and android.permission.CHANGE_WIFI_STATE

        if (canControlWifi()) {
            WifiManager manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

            if (manager == null) {
                if (DEBUG) Log.w(TAG, "changeWifi: WIFI_SERVICE missing!");
                return;
            }

            if (DEBUG) Log.d(TAG, "changeWifi: " + (enabled ? "on" : "off"));

            manager.setWifiEnabled(enabled);
        }
    }

    // --- Airplane mode ---

    /** Changes the airplane mode */
    public void changeAirplaneMode(boolean turnOn) {
        // Reference: settings source is in the cupcake gitweb tree at
        //   packages/apps/Settings/src/com/android/settings/AirplaneModeEnabler.java
        // http://android.git.kernel.org/?p=platform/packages/apps/Settings.git;a=blob;f=src/com/android/settings/AirplaneModeEnabler.java;h=f105712260fd7b2d7804460dd180d1d6cea01afa;hb=HEAD

        if (canControlAirplaneMode()) {
            // Change the system setting
            Settings.System.putInt(
                            mContext.getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON,
                            turnOn ? 1 : 0);

            // Post the intent
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", turnOn);
            mContext.sendBroadcast(intent);

            if (DEBUG) Log.d(TAG, "changeAirplaneMode: " + (turnOn ? "on" : "off"));
        }
    }

    // --- Bluetooh ---

    public void changeBluetooh(boolean enabled) {
        // This requires permission android.permission.BLUETOOTH_ADMIN

        if (canControlBluetooth()) {
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

    // --- APN Droid ---

    public void changeApnDroid(boolean enabled) {
        if (canControlApnDroid()) {
            if (DEBUG) Log.d(TAG, "changeApnDroid: " + (enabled ? "on" : "off"));
            Intent intent = new Intent(ApplicationConstants.CHANGE_STATUS_REQUEST);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ApplicationConstants.TARGET_APN_STATE,
                    enabled ? ApplicationConstants.State.ON :
                        ApplicationConstants.State.OFF);
            mContext.startActivity(intent);
        }
    }
}
