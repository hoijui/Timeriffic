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

package com.rdrrlabs.timeriffic.core.app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.alfray.timeriffic.R;
import com.rdrrlabs.timeriffic.app.TimerifficApp;
import com.rdrrlabs.timeriffic.app.UpdateReceiver;
import com.rdrrlabs.timeriffic.core.actions.TimedActionUtils;
import com.rdrrlabs.timeriffic.core.error.ExceptionHandler;
import com.rdrrlabs.timeriffic.core.prefs.PrefsValues;
import com.rdrrlabs.timeriffic.core.profiles1.Columns;
import com.rdrrlabs.timeriffic.core.profiles1.ProfilesDB;
import com.rdrrlabs.timeriffic.core.profiles1.ProfilesDB.ActionInfo;
import com.rdrrlabs.timeriffic.core.settings.ISetting;
import com.rdrrlabs.timeriffic.core.settings.SettingFactory;
import com.rdrrlabs.timeriffic.core.utils.SettingsHelper;
import com.rdrrlabs.timeriffic.core.utils.SettingsHelper.RingerMode;
import com.rdrrlabs.timeriffic.core.utils.SettingsHelper.VibrateRingerMode;


public class ApplySettings {

    private final static boolean DEBUG = true;
    public final static String TAG = ApplySettings.class.getSimpleName();

    private final Context mContext;
    private final PrefsValues mPrefs;
    private final SimpleDateFormat mUiDateFormat;
    private final SimpleDateFormat mDebugDateFormat;

    public ApplySettings(Context context, PrefsValues prefs) {
        mContext = context;
        mPrefs = prefs;
        mDebugDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
        String format = null;
        SimpleDateFormat dt = null;
        try {
            format = context.getString(R.string.globalstatus_nextlast_date_time);
            dt  = new SimpleDateFormat(format);
        } catch (Exception e) {
            Log.e(TAG, "Invalid R.string.globalstatus_nextlast_date_time: " +
                    (format == null ? "null" : format));
        }
        mUiDateFormat = dt == null ? mDebugDateFormat : dt;
    }

    private void showToast(String s, int duration) {
        try {
            Toast.makeText(mContext, s, duration).show();
        } catch (Throwable t) {
            ExceptionHandler.addToLog(mPrefs, t);
            Log.w(TAG, "Toast.show crashed", t);
        }
    }

    public void apply(boolean applyState, int displayToast) {
        if (DEBUG) Log.d(TAG, "Checking enabled");

        checkProfiles(applyState, displayToast);
        notifyDataChanged();
    }

    public void retryActions(String actions) {
        if (DEBUG) Log.d(TAG, "Retry actions: " + actions);

        SettingsHelper settings = new SettingsHelper(mContext);
        if (performAction(settings, actions, null /*failedActions*/)) {
            showToast("Timeriffic retried the actions", Toast.LENGTH_LONG);
            if (DEBUG) Log.d(TAG, "Retry succeed");
        } else {
            showToast("Timeriffic found no action to retry", Toast.LENGTH_LONG);
            if (DEBUG) Log.d(TAG, "Retry no-op");
        }
    }

    private void checkProfiles(boolean applyState, int displayToast) {
        ProfilesDB profilesDb = new ProfilesDB();
        try {
            profilesDb.onCreate(mContext);

            profilesDb.removeAllActionExecFlags();

            // Only do something if at least one profile is enabled.
            long[] prof_indexes = profilesDb.getEnabledProfiles();
            if (prof_indexes != null && prof_indexes.length != 0) {

                Calendar c = new GregorianCalendar();
                c.setTimeInMillis(System.currentTimeMillis());
                int hourMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
                int day = TimedActionUtils.calendarDayToActionDay(c);

                if (applyState) {
                    ActionInfo[] actions =
                        profilesDb.getWeekActivableActions(hourMin, day, prof_indexes);

                    if (actions != null && actions.length > 0) {
                        performActions(actions);
                        profilesDb.markActionsEnabled(actions, Columns.ACTION_MARK_PREV);
                    }
                }

                // Compute next event and set an alarm for it
                ActionInfo[] nextActions = new ActionInfo[] { null };
                int nextEventMin = profilesDb.getWeekNextEvent(hourMin, day, prof_indexes, nextActions);
                if (nextEventMin > 0) {
                    scheduleAlarm(c, nextEventMin, nextActions[0], displayToast);
                    if (applyState) {
                        profilesDb.markActionsEnabled(nextActions, Columns.ACTION_MARK_NEXT);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "checkProfiles failed", e);
            ExceptionHandler.addToLog(mPrefs, e);

        } finally {
            profilesDb.onDestroy();
        }
    }

    private void performActions(ActionInfo[] actions) {

        String logActions = null;
        String lastAction = null;
        SettingsHelper settings = null;
        SparseArray<String> failedActions = new SparseArray<String>();

        for (ActionInfo info : actions) {
            try {
                if (settings == null) settings = new SettingsHelper(mContext);

                if (performAction(settings, info.mActions, failedActions)) {
                    lastAction = info.mActions;
                    if (logActions == null) {
                        logActions = lastAction;
                    } else {
                        logActions += " | " + lastAction;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to apply setting", e);
                ExceptionHandler.addToLog(mPrefs, e);
            }
        }

        if (lastAction != null) {
            // Format the timestamp of the last action to be "now"
            String time = mUiDateFormat.format(new Date(System.currentTimeMillis()));

            // Format the action description
            String a = TimedActionUtils.computeLabels(mContext, lastAction);

            synchronized (mPrefs.editLock()) {
                Editor e = mPrefs.startEdit();
                try {
                    mPrefs.editStatusLastTS(e, time);
                    mPrefs.editStatusNextAction(e, a);

                } finally {
                    mPrefs.endEdit(e, TAG);
                }
            }

            addToDebugLog(logActions);
        }

        if (failedActions.size() > 0) {
            notifyFailedActions(failedActions);
        }
    }

    private boolean performAction(
            SettingsHelper settings,
            String actions,
            SparseArray<String> failedActions) {
        if (actions == null) return false;
        boolean didSomething = false;

        RingerMode ringerMode = null;
        VibrateRingerMode vibRingerMode = null;

        for (String action : actions.split(",")) {
            if (action.length() > 1) {
                char code = action.charAt(0);
                char v = action.charAt(1);

                switch(code) {
                case Columns.ACTION_RINGER:
                    for (RingerMode mode : RingerMode.values()) {
                        if (mode.getActionLetter() == v) {
                            ringerMode = mode;
                            break;
                        }
                    }
                    break;
                case Columns.ACTION_VIBRATE:
                    for (VibrateRingerMode mode : VibrateRingerMode.values()) {
                        if (mode.getActionLetter() == v) {
                            vibRingerMode = mode;
                            break;
                        }
                    }
                    break;

                default:
                    ISetting s = SettingFactory.getInstance().getSetting(code);
                    if (s != null && s.isSupported(mContext)) {
                        if (!s.performAction(mContext, action)) {
                            // Record failed action
                            if (failedActions != null) {
                                failedActions.put(code, action);
                            }
                        } else {
                            didSomething = true;
                        }
                    }
                    break;
                }
            }
        }

        if (ringerMode != null || vibRingerMode != null) {
            didSomething = true;
            settings.changeRingerVibrate(ringerMode, vibRingerMode);
        }

        return didSomething;
    }

    /** Notify UI to update */
    private void notifyDataChanged() {
        TimerifficApp app = TimerifficApp.getInstance(mContext);
        if (app != null) {
            app.invokeDataListener();
        }
    }

    /**
     * Schedule an alarm to happen at nextEventMin minutes from now.
     *
     * @param now The time that was used at the beginning of the update.
     * @param nextEventMin The number of minutes ( > 0) after "now" where to set the alarm.
     * @param nextActions
     * @param displayToast One of {@link UpdateReceiver#TOAST_NONE},
     *             {@link UpdateReceiver#TOAST_IF_CHANGED} or {@link UpdateReceiver#TOAST_ALWAYS}
     */
    private void scheduleAlarm(
            Calendar now,
            int nextEventMin,
            ActionInfo nextActions,
            int displayToast) {
        AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(mContext, UpdateReceiver.class);
        intent.setAction(UpdateReceiver.ACTION_APPLY_STATE);
        PendingIntent op = PendingIntent.getBroadcast(
                        mContext,
                        0 /*requestCode*/,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.add(Calendar.MINUTE, nextEventMin);

        long timeMs = now.getTimeInMillis();

        manager.set(AlarmManager.RTC_WAKEUP, timeMs, op);

        boolean shouldDisplayToast = displayToast != UpdateReceiver.TOAST_NONE;
        if (displayToast == UpdateReceiver.TOAST_IF_CHANGED) {
            shouldDisplayToast = timeMs != mPrefs.getLastScheduledAlarm();
        }


        SimpleDateFormat sdf = null;
        String format = null;
        try {
            format = mContext.getString(R.string.toast_next_alarm_date_time);
            sdf = new SimpleDateFormat(format);
        } catch (Exception e) {
            Log.e(TAG, "Invalid R.string.toast_next_alarm_date_time: " +
                    (format == null ? "null" : format));
        }
        if (sdf == null) sdf = mDebugDateFormat;

        sdf.setCalendar(now);
        String s2 = sdf.format(now.getTime());

        synchronized (mPrefs.editLock()) {
            Editor e = mPrefs.startEdit();
            try {
                mPrefs.editLastScheduledAlarm(e, timeMs);
                mPrefs.editStatusNextTS(e, s2);
                mPrefs.editStatusNextAction(e,
                        TimedActionUtils.computeLabels(mContext, nextActions.mActions));
            } finally {
                mPrefs.endEdit(e, TAG);
            }
        }

        s2 = mContext.getString(R.string.toast_next_change_at_datetime, s2);

        if (shouldDisplayToast) showToast(s2, Toast.LENGTH_LONG);
        if (DEBUG) Log.d(TAG, s2);
        addToDebugLog(s2);
    }


    protected static final String SEP_START = " [ ";
    protected static final String SEP_END = " ]\n";

    /** Add debug log for now. */
    public /* package */ void addToDebugLog(String message) {
        String time = mDebugDateFormat.format(new Date(System.currentTimeMillis()));
        addToDebugLog(time, message);
    }

    /** Add time:action specific log. */
    protected synchronized void addToDebugLog(String time, String logActions) {

        logActions = time + SEP_START + logActions + SEP_END;
        int len = logActions.length();

        // We try to keep only 4k in the buffer
        int max = 4096;

        String a = null;

        if (logActions.length() < max) {
            a = mPrefs.getLastActions();
            if (a != null) {
                int lena = a.length();
                if (lena + len > max) {
                    int extra = lena + len - max;
                    int pos = -1;
                    int p = -1;
                    do {
                        pos = a.indexOf(SEP_END, pos + 1);
                        p = pos + SEP_END.length();
                    } while (pos >= 0 && p < extra);

                    if (pos < 0 || p >= lena) {
                        a = null;
                    } else {
                        a = a.substring(p);
                    }
                }
            }
        }

        if (a == null) {
            mPrefs.setLastActions(logActions);
        } else {
            a += logActions;
            mPrefs.setLastActions(a);
        }

    }

    public static synchronized int getNumActionsInLog(Context context) {
        try {
            PrefsValues pv = new PrefsValues(context);
            String curr = pv.getLastActions();

            if (curr == null) {
                return 0;
            }

            int count = -1;
            int pos = -1;
            do {
                count++;
                pos = curr.indexOf(" ]", pos + 1);
            } while (pos >= 0);

            return count;

        } catch (Exception e) {
            Log.w(TAG, "getNumActionsInLog failed", e);
            ExceptionHandler.addToLog(new PrefsValues(context), e);
        }

        return 0;
    }


    /**
     * Send a notification indicating the given actions have failed.
     */
    private void notifyFailedActions(SparseArray<String> failedActions) {
        StringBuilder sb = new StringBuilder();
        for (int n = failedActions.size(), i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append(failedActions.valueAt(i));
        }
        String actions = sb.toString();
        String details = TimedActionUtils.computeLabels(mContext, actions);

        Core core = TimerifficApp.getInstance(mContext).getCore();
        core.getUpdateService().createRetryNotification(mContext, mPrefs, actions, details);
    }

}
