/*
 * Project: Timeriffic
 * Copyright (C) 2009 ralfoide gmail com,
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


package com.alfray.timeriffic.actions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.content.Context;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.profiles.Columns;
import com.alfray.timeriffic.utils.SettingsHelper;
import com.alfray.timeriffic.utils.SettingsHelper.RingerMode;
import com.alfray.timeriffic.utils.SettingsHelper.VibrateRingerMode;


public class TimedActionUtils {

    static private final int[] CALENDAR_DAYS = {
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
    };

    static private final String[] DAYS_NAMES_EN = {
        "Mon",
        "Tue",
        "Wed",
        "Thu",
        "Fri",
        "Sat",
        "Sun"
    };

    static protected String[] sDaysNames = null;

    static public int calendarDayToActionDay(Calendar c) {
        int day = c.get(Calendar.DAY_OF_WEEK);
        for (int i = Columns.MONDAY_BIT_INDEX; i <= Columns.SUNDAY_BIT_INDEX; i++) {
            if (day == CALENDAR_DAYS[i]) {
                day = 1<<i;
                break;
            }
        }
        return day;
    }

    static public String computeDescription(Context context, int hourMin, int days, String actions) {

        String desc_time = computeTime(context, hourMin);
        String desc_days = computeDays(context, days);
        String desc_actions = computeActions(context, actions);

        String description = String.format("%s %s, %s", desc_time, desc_days, desc_actions);
        return description;
    }

    private static String computeTime(Context context, int hourMin) {
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, hourMin / 60);
        int min = hourMin % 60;
        c.set(Calendar.MINUTE, min);
        String desc_time = null;
        if (min != 0) {
            desc_time = context.getString(R.string.timedaction_time_with_minutes, c);
        } else {
            desc_time = context.getString(R.string.timedaction_time_0_minute, c);
        }
        return desc_time;
    }

    static public String[] getDaysNames() {
        if (sDaysNames != null) return sDaysNames;

        sDaysNames = new String[CALENDAR_DAYS.length];

        Calendar loc = new GregorianCalendar(Locale.getDefault());

        loc.setTimeInMillis(System.currentTimeMillis());
        for (int i = 0; i < CALENDAR_DAYS.length; i++) {
            loc.set(Calendar.DAY_OF_WEEK, CALENDAR_DAYS[i]);
            String s = String.format("%ta", loc);

            // In Android 1.5, "%ta" doesn't seem to work properly so
            // we just hardcode them.
            if (s == null || s.length() == 0 || s.matches("[0-9]")) {
                s = DAYS_NAMES_EN[i];
            }
            sDaysNames[i] = s;
        }

        return sDaysNames;
    }

    static private String computeDays(Context context, int days) {
        int start = -2;
        int count = 0;
        StringBuilder desc_days = new StringBuilder();

        String[] dayNames = getDaysNames();

        for (int i = Columns.MONDAY_BIT_INDEX; i <= Columns.SUNDAY_BIT_INDEX; i++) {
            if ((days & (1<<i)) != 0 ) {

                if (start == i-1) {
                    // continue range
                    start = i;
                    count++;
                } else {
                    // start new range
                    if (desc_days.length() > 0) desc_days.append(", ");
                    desc_days.append(dayNames[i]);
                    start = i;
                    count = 0;
                }
            } else {
                if (start >= 0 && count > 0) {
                    // close range
                    desc_days.append(" - ");
                    desc_days.append(dayNames[start]);
                }
                start = -2;
                count = 0;
            }
        }
        if (start >= 0 && count > 0) {
            // close range
            desc_days.append(" - ");
            desc_days.append(dayNames[start]);
        }
        if (desc_days.length() == 0) {
            desc_days.append(context.getString(R.string.timedaction_nodays_never));
        }

        return desc_days.toString();
    }

    static public String computeActions(Context context, String actions) {

        SettingsHelper sh = new SettingsHelper(context);
        ArrayList<String> actions_names = new ArrayList<String>();

        if (actions != null) {
            for (String action : actions.split(",")) {
                int value = -1;
                if (action.length() > 1) {
                    char code = action.charAt(0);
                    char v = action.charAt(1);

                    switch(code) {
                    case Columns.ACTION_RINGER:
                        for (RingerMode mode : RingerMode.values()) {
                            if (mode.getActionLetter() == v) {
                                actions_names.add(mode.toUiString(context));   // ringer name
                                break;
                            }
                        }
                        break;
                    case Columns.ACTION_VIBRATE:
                        for (VibrateRingerMode mode : VibrateRingerMode.values()) {
                            if (mode.getActionLetter() == v) {
                                actions_names.add(mode.toUiString(context));   // vibrate name
                                break;
                            }
                        }
                        break;
                    default:
                        try {
                            value = Integer.parseInt(action.substring(1));

                            switch(code) {
                            case Columns.ACTION_BRIGHTNESS:
                                if (sh.canControlBrigthness()) {
                                    actions_names.add(
                                            context.getString(
                                                    R.string.timedaction_brightness_int,
                                                    value));
                                }
                                break;
                            case Columns.ACTION_RING_VOLUME:
                                actions_names.add(
                                        context.getString(
                                                R.string.timedaction_ringer_int,
                                                value));
                                break;
                            case Columns.ACTION_NOTIF_VOLUME:
                                actions_names.add(
                                        context.getString(
                                                R.string.timedaction_notif_int,
                                                value));
                                break;
                            case Columns.ACTION_MEDIA_VOLUME:
                                actions_names.add(
                                        context.getString(
                                                R.string.timedaction_media_int,
                                                value));
                                break;
                            case Columns.ACTION_ALARM_VOLUME:
                                actions_names.add(
                                        context.getString(
                                                R.string.timedaction_alarm_int,
                                                value));
                                break;
                            case Columns.ACTION_BLUETOOTH:
                                if (sh.canControlWifi()) {
                                    actions_names.add(
                                            context.getString(
                                                    value > 0 ? R.string.timedaction_bluetooth_on :
                                                                R.string.timedaction_bluetooth_off));
                                }
                                break;
                            case Columns.ACTION_WIFI:
                                if (sh.canControlWifi()) {
                                    actions_names.add(
                                            context.getString(
                                                    value > 0 ? R.string.timedaction_wifi_on :
                                                                R.string.timedaction_wifi_off));
                                }
                                break;
                            case Columns.ACTION_AIRPLANE:
                                if (sh.canControlAirplaneMode()) {
                                    actions_names.add(
                                            context.getString(
                                                    value > 0 ? R.string.timedaction_airplane_on :
                                                                R.string.timedaction_airplane_off));
                                }
                                break;
                            case Columns.ACTION_APN_DROID:
                                if (sh.canControlApnDroid()) {
                                    actions_names.add(
                                            context.getString(
                                                    value > 0 ? R.string.timedaction_apndroid_on :
                                                                R.string.timedaction_apndroid_off));
                                }
                                break;
                            }

                        } catch (NumberFormatException e) {
                            // pass
                        }
                    }
                }
            }
        }

        StringBuilder desc_actions = new StringBuilder();

        if (actions_names.size() == 0) {
            desc_actions.append(context.getString(R.string.timedaction_no_action));
        } else {
            for (String name : actions_names) {
                if (desc_actions.length() > 0) desc_actions.append(", ");
                desc_actions.append(name);
            }
        }

        return desc_actions.toString();
    }
}
