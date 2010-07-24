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

package com.alfray.timeriffic.prefs;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Oldv1PrefsValues {

    public static final String KEY_START_HOUR = "start_hour";
    public static final String KEY_END_HOUR = "end_hour";

    public static final int VERSION = 0;

    private SharedPreferences mPrefs;

    public Oldv1PrefsValues(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public SharedPreferences getPrefs() {
        return mPrefs;
    }

    public boolean isServiceEnabled() {
        return mPrefs.getBoolean("enable_serv", true);
    }

    /**
     * Sets the dismiss_intro boolean value.
     * @return true if value was successfully changed if the prefs
     */
    public boolean setServiceEnabled(boolean checked) {
        return mPrefs.edit().putBoolean("enable_serv", checked).commit();
    }

    /** Returns the start hour-min or -1 if not present. */
    public int startHourMin() {
        try {
            return mPrefs.getInt(KEY_START_HOUR, -1);
        } catch (ClassCastException e) {
            // The field used to be a String, so it could fail here
            String s = mPrefs.getString(KEY_START_HOUR, null);
            return s == null ? -1 : parseHoursMin(s);
        }
    }

    /** Returns the stop hour-min or -1 if not present. */
    public int stopHourMin() {
        try {
            return mPrefs.getInt(KEY_END_HOUR, -1);
        } catch (ClassCastException e) {
            // The field used to be a String, so it could fail here
            String s = mPrefs.getString(KEY_END_HOUR, null);
            return s == null ? -1 : parseHoursMin(s);
        }
    }

    private int parseHoursMin(String text) {
        int hours = 0;
        int minutes = 0;

        String[] numbers = text.trim().split(":");
        if (numbers.length >= 1) hours = parseNumber(numbers[0], 23);
        if (numbers.length >= 2) minutes = parseNumber(numbers[1], 59);

        return hours*60 + minutes;
    }

    private static int parseNumber(String string, int maxValue) {
        try {
            int n = Integer.parseInt(string);
            if (n < 0) return 0;
            if (n > maxValue) return maxValue;
            return n;
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    public boolean startMute() {
        return mPrefs.getBoolean("start_mute", true);
    }

    public boolean startVibrate() {
        return mPrefs.getBoolean("start_vibrate", true);
    }

    public boolean stopMute() {
        return mPrefs.getBoolean("end_mute", false);
    }

    public boolean stopVibrate() {
        return mPrefs.getBoolean("end_vibrate", false);
    }
}
