/*
 * Project: Timeriffic
 * Copyright (C) 2011 rdrr labs gmail com,
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.alfray.timeriffic.R;
import com.rdrrlabs.timeriffic.app.UpdateReceiver;
import com.rdrrlabs.timeriffic.app.UpdateService;
import com.rdrrlabs.timeriffic.core.error.ExceptionHandler;
import com.rdrrlabs.timeriffic.core.prefs.PrefsValues;
import com.rdrrlabs.timeriffic.core.utils.VolumeChange;

public class UpdateServiceImpl {

    public static final String TAG = UpdateServiceImpl.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String EXTRA_RELEASE_WL = "releaseWL";
    private static final String EXTRA_OLD_INTENT = "old_intent";
    private static final String EXTRA_RETRY_ACTIONS = "retry_actions";

    /** Failed Actions Notification ID. 'FaiL' as an int. */
    private static final int FAILED_ACTION_NOTIF_ID = 'F' << 24 + 'a' << 16 + 'i' << 8 + 'L';

    private WakeLock sWakeLock = null;

    /**
     * Starts the service from the {@link UpdateReceiver}.
     * This is invoked from the {@link UpdateReceiver} so code should be
     * at its minimum. No logging or DB access here.
     *
     * @param intent Original {@link UpdateReceiver}'s intent. *Could* be null.
     * @param wakeLock WakeLock created by {@link UpdateReceiver}. Could be null.
     */
    public void startFromReceiver(Context context, Intent intent, WakeLock wakeLock) {
        Intent i = new Intent(context, UpdateService.class);
        if (intent != null) {
            i.putExtra(EXTRA_OLD_INTENT, intent);
        }

        if (wakeLock != null) {
            i.putExtra(EXTRA_RELEASE_WL, true);

            // if there's a current wake lock, release it first
            synchronized (UpdateServiceImpl.class) {
                WakeLock oldWL = sWakeLock;
                if (oldWL != wakeLock) {
                    sWakeLock  = wakeLock;
                    if (oldWL != null) oldWL.release();
                }
            }
        }

        context.startService(i);
    }

    public void createRetryNotification(
            Context context,
            PrefsValues prefs,
            String actions,
            String details) {
        if (DEBUG) Log.d(TAG, "create retry notif: " + actions);

        try {
            NotificationManager ns =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (ns == null) return;

            Intent i = new Intent(context, UpdateServiceImpl.class);
            i.putExtra(EXTRA_RETRY_ACTIONS, actions);

            PendingIntent pi = PendingIntent.getService(
                    context, 0 /*requestCode*/, i, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notif = new Notification(
                R.drawable.app_icon,            // icon
                "Timeriffic actions failed",   // status bar tickerText
                System.currentTimeMillis()      // when to show it
                );
            notif.flags |= Notification.FLAG_AUTO_CANCEL;
            notif.defaults = Notification.DEFAULT_ALL;

            notif.setLatestEventInfo(context,
                "Timeriffic actions failed",           // contentTitle
                "Click here to retry: " + details,          // contentText
                pi                                          // contentIntent
                );

            ns.notify(FAILED_ACTION_NOTIF_ID, notif);
        } catch (Throwable t) {
            Log.e(TAG, "Notification crashed", t);
            ExceptionHandler.addToLog(prefs, t);
        }
    }


    //----

    public void onStart(Context context, Intent intent, int startId) {
        try {
            String actions = intent.getStringExtra(EXTRA_RETRY_ACTIONS);
            if (actions != null) {
                retryActions(context, actions);
                return;
            }

            Intent i = intent.getParcelableExtra(EXTRA_OLD_INTENT);
            if (i == null) {
                // Not supposed to happen.
                String msg = "Missing old_intent in UpdateService.onStart";
                PrefsValues prefs = new PrefsValues(context);
                ApplySettings as = new ApplySettings(context, prefs);
                as.addToDebugLog(msg);
                Log.e(TAG, msg);
            } else {
                applyUpdate(context, i);
                return;
            }

        } finally {
            if (intent.getBooleanExtra(EXTRA_RELEASE_WL, false)) {
                releaseWakeLock();
            }
        }
    }

    public void onDestroy(Context context) {
        VolumeChange.unregisterVolumeReceiver(context);
        releaseWakeLock();
    }

    // ---

    private void releaseWakeLock() {
        synchronized (UpdateServiceImpl.class) {
            WakeLock oldWL = sWakeLock;
            if (oldWL != null) {
                sWakeLock  = null;
                oldWL.release();
            }
        }
    }

    private void applyUpdate(Context context, Intent intent) {
        PrefsValues prefs = new PrefsValues(context);
        ApplySettings as = new ApplySettings(context, prefs);

        String action = intent.getAction();

        int displayToast = intent.getIntExtra(UpdateReceiver.EXTRA_TOAST_NEXT_EVENT, UpdateReceiver.TOAST_NONE);
        boolean fromUI = UpdateReceiver.ACTION_UI_CHECK.equals(action);

        // We *only* apply settings if we recognize the action as being:
        // - Profiles UI > check now
        // - a previous alarm with Apply State
        // - boot completed
        // In all other cases (e.g. time/timezone changed), we'll recompute the
        // next alarm but we won't enforce settings.
        boolean applyState = fromUI ||
                UpdateReceiver.ACTION_APPLY_STATE.equals(action) ||
                Intent.ACTION_BOOT_COMPLETED.equals(action);

        // Log all actions except for "TIME_SET" which can happen too
        // frequently on some carriers and then pollutes the log.
        if (!Intent.ACTION_TIME_CHANGED.equals(action)) {
            String logAction = action.replace("android.intent.action.", "");
            logAction = logAction.replace("com.rdrrlabs.intent.action.", "");

            String debug = String.format("UpdateService %s%s",
                    applyState ? "*Apply* " : "",
                    logAction
                    );
            as.addToDebugLog(debug);
            Log.d(TAG, debug);
        }

        if (!prefs.isServiceEnabled()) {
            String debug = "Checking disabled";
            as.addToDebugLog(debug);
            Log.d(TAG, debug);

            if (displayToast == UpdateReceiver.TOAST_ALWAYS) {
                showToast(context, prefs,
                        R.string.globalstatus_disabled,
                        Toast.LENGTH_LONG);
            }

            return;
        }

        as.apply(applyState, displayToast);
    }

    private void retryActions(Context context, String actions) {
        PrefsValues prefs = new PrefsValues(context);
        ApplySettings as = new ApplySettings(context, prefs);

        if (!prefs.isServiceEnabled()) {
            String debug = "[Retry] Checking disabled";
            as.addToDebugLog(debug);
            Log.d(TAG, debug);

            // Since this comes from user intervention clicking on a
            // notifcation, it's OK to display a status via a toast UI.
            showToast(context, prefs,
                    R.string.globalstatus_disabled,
                    Toast.LENGTH_LONG);
            return;
        }

        as.retryActions(actions);
    }

    private void showToast(Context context, PrefsValues pv, int id, int duration) {
        try {
            Toast.makeText(context, id, duration).show();
        } catch (Throwable t) {
            Log.e(TAG, "Toast.show crashed", t);
            ExceptionHandler.addToLog(pv, t);
        }
    }

}
