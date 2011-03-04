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

package com.alfray.timeriffic.core.utils;

import android.content.Context;
import android.media.AudioManager;
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
    public static final String TAG = SettingsHelper.class.getSimpleName();

    private final Context mContext;

    public SettingsHelper(Context context) {
        mContext = context;
    }

    public boolean canControlAudio() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return manager != null;
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
                    // normal may or may not vibrate, cf setting above
                    // (for RingGuard intent, need to keep volume unchanged)
                    VolumeChange.changeRinger(
                            mContext,
                            AudioManager.RINGER_MODE_NORMAL);
                    break;

                case MUTE:
                    if (vib != null && vib == VibrateRingerMode.VIBRATE) {
                        VolumeChange.changeRinger(
                                mContext,
                                AudioManager.RINGER_MODE_VIBRATE);
                    } else {
                        // this turns off the vibrate, which unfortunately doesn't respect
                        // the case where vibrate should not be changed when going silent.
                        // TODO read the system pref for the default "vibrate" mode and use
                        // when vib==null.
                        VolumeChange.changeRinger(
                                mContext,
                                AudioManager.RINGER_MODE_SILENT);
                    }
                    break;
            }
        }
    }
}
