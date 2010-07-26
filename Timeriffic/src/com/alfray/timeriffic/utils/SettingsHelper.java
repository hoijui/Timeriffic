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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.alfray.timeriffic.R;

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

    /** android.provider.Settings.NOTIFICATION_USE_RING_VOLUME, available starting with API 5
     *  but it's hidden from the SDK. The Settings.java comment says eventually this setting
     *  will go away later once there are "profile" support, whatever that is. */
    private static final String NOTIF_RING_VOL_KEY = "notifications_use_ring_volume";
    /** Notification vol and ring volumes are synched. */
    public static final int NOTIF_RING_VOL_SYNCED = 1;
    /** Notification vol and ring volumes are not synched. */
    public static final int NOTIF_RING_VOL_NOT_SYNCHED = 0;
    /** No support for notification and ring volume sync. */
    public static final int NOTIF_RING_VOL_UNSUPPORTED = -1;

    private final Context mContext;

    public SettingsHelper(Context context) {
        mContext = context;
    }

    public boolean canControlAudio() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return manager != null;
    }

    public boolean canSyncNotificationRingVol() {
        return checkMinApiLevel(5) &&
            getSyncNotifRingVol() != NOTIF_RING_VOL_UNSUPPORTED;
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

    public void changeNotifRingVolSync(boolean sync) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver,
                               NOTIF_RING_VOL_KEY,
                               sync ? NOTIF_RING_VOL_SYNCED : NOTIF_RING_VOL_NOT_SYNCHED);
    }

    /**
     * Returns one of {@link #NOTIF_RING_VOL_SYNCED}, {@link #NOTIF_RING_VOL_NOT_SYNCHED} or
     * {@link #NOTIF_RING_VOL_UNSUPPORTED}.
     */
    public int getSyncNotifRingVol() {
        final ContentResolver resolver = mContext.getContentResolver();
        return Settings.System.getInt(resolver, NOTIF_RING_VOL_KEY, NOTIF_RING_VOL_UNSUPPORTED);
    }


}
