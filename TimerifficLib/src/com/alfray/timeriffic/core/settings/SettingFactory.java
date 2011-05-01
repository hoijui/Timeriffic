/*
 * Project: Timeriffic
 * Copyright (C) 2010 rdrr labs gmail com,
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

import com.alfray.timeriffic.core.profiles1.Columns;

//-----------------------------------------------

public class SettingFactory {

    public static final String TAG = SettingFactory.class.getSimpleName();

    private static final SettingFactory sInstance = new SettingFactory();
    private ISettingsFactory2 mSettingsFactory2;

    /** A synchronized map of existing loaded settings. */
    private final Map<Character, ISetting> mSettings =
        Collections.synchronizedMap(new HashMap<Character, ISetting>());

    public static SettingFactory getInstance() {
        return sInstance;
    }

    private SettingFactory() {
    }

    public void registerFactory2(ISettingsFactory2 factory2) {
        mSettingsFactory2 = factory2;
    }

    /** Unloads the setting if it's loaded. */
    public void forgetSetting(char code) {
        mSettings.remove(code);
    }

    /**
     * Returns an {@link ISetting}. Never returns null.
     */
    public ISetting getSetting(char code) {
        ISetting s = mSettings.get(code);
        if (s != null) return s;

        switch(code) {
            case Columns.ACTION_RINGER:
                s = new RingerSetting();
                break;
            case Columns.ACTION_VIBRATE:
                s = new VibrateSetting();
                break;
            case Columns.ACTION_RING_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_RING);
                break;
            case Columns.ACTION_NOTIF_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_NOTIFICATION);
                break;
            case Columns.ACTION_MEDIA_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_MUSIC);
                break;
            case Columns.ACTION_ALARM_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_ALARM);
                break;
            case Columns.ACTION_SYSTEM_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_SYSTEM);
                break;
            case Columns.ACTION_VOICE_CALL_VOLUME:
                s = new VolumeSetting(AudioManager.STREAM_VOICE_CALL);
                break;
            case Columns.ACTION_BRIGHTNESS:
                s = new BrightnessSetting();
                break;
            case Columns.ACTION_WIFI:
                s = new WifiSetting();
                break;
            case Columns.ACTION_AIRPLANE:
                s = new AirplaneSetting();
                break;
            case Columns.ACTION_BLUETOOTH:
                s = new BluetoothSetting();
                break;
            case Columns.ACTION_APN_DROID:
                s = new ApnDroidSetting();
                break;
            case Columns.ACTION_DATA:
                s = new DataSetting();
                break;
        }

        if (s == null && mSettingsFactory2 != null) {
            s = mSettingsFactory2.getSetting(code);
        }

        if (s == null) {
            s = new NullSetting();
        }
        assert s != null;

        mSettings.put(code, s);
        return s;
    }

    private static class NullSetting implements ISetting {
        @Override
        public boolean isSupported(Context context) {
            return false;
        }

        @Override
        public Object createUi(Activity activity, String[] currentActions) {
            return null;
        }

        @Override
        public void collectUiResults(Object settingUi, StringBuilder outActions) {
            // pass
        }


        @Override
        public String getActionLabel(Context context, String action) {
            return null;
        }

        @Override
        public boolean performAction(Context context, String action) {
            return true;
        }

    }
}


