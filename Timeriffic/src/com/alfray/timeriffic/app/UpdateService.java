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

package com.alfray.timeriffic.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.error.ExceptionHandler;
import com.alfray.timeriffic.prefs.PrefsValues;

public class UpdateService extends Service {

    public static final String TAG = "TFC-UpdServ";
    private static final boolean DEBUG = true;

    private static final String EXTRA_RELEASE_WL = "releaseWL";
    private static final String EXTRA_OLD_INTENT = "old_intent";

    private static WakeLock sWakeLock = null;

    @Override
    public IBinder onBind(Intent intent) {
        // pass
        return null;
    }

    /**
     * Starts the service.
     * This is invoked from the {@link UpdateReceiver}.
     */
    public static void update(Context context, Intent intent, WakeLock wakeLock) {

        Intent i = new Intent(context, UpdateService.class);
        i.putExtra(EXTRA_OLD_INTENT, intent);

        if (wakeLock != null) {
            i.putExtra(EXTRA_RELEASE_WL, true);

            // if there's a current wake lock, release it first
            synchronized (UpdateService.class) {
                WakeLock oldWL = sWakeLock;
                if (oldWL != wakeLock) {
                    sWakeLock  = wakeLock;
                    if (oldWL != null) oldWL.release();
                }
            }
        }

        context.startService(i);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (DEBUG) Log.d(TAG, "Start service");
        ExceptionHandler handler = new ExceptionHandler(this);
        try {
            try {
                super.onStart(intent, startId);

                Intent i = intent.getParcelableExtra(EXTRA_OLD_INTENT);
                if (i == null) {
                    // Not supposed to happen.
                    String msg = "Missing old_intent in UpdateService.onStart";
                    PrefsValues prefs = new PrefsValues(this);
                    ApplySettings as = new ApplySettings(this, prefs);
                    as.addToDebugLog(msg);
                    Log.e(TAG, msg);
                } else {
                    applyUpdate(i);
                }

            } finally {
                if (intent.getBooleanExtra(EXTRA_RELEASE_WL, false)) {
                    releaseWakeLock();
                }
            }

        } finally {
            handler.detach();
        }
        if (DEBUG) Log.d(TAG, "Stopping service");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        super.onDestroy();
    }

    // ---

    private void releaseWakeLock() {
        synchronized (UpdateService.class) {
            WakeLock oldWL = sWakeLock;
            if (oldWL != null) {
                sWakeLock  = null;
                oldWL.release();
            }
        }
    }

    private void applyUpdate(Intent intent) {
        PrefsValues prefs = new PrefsValues(this);
        ApplySettings as = new ApplySettings(this, prefs);

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

        String logAction = action.replace("android.intent.action.", "");
        logAction = logAction.replace("com.alfray.intent.action.", "");

        String debug = String.format("UpdateService %s%s",
                applyState ? "*Apply* " : "",
                logAction
                );
        as.addToDebugLog(debug);
        Log.d(TAG, debug);

        if (!prefs.isServiceEnabled()) {
            debug = "Checking disabled";
            as.addToDebugLog(debug);
            Log.d(TAG, debug);

            if (displayToast == UpdateReceiver.TOAST_ALWAYS) {
                showToast(this, prefs,
                        R.string.globalstatus_disabled,
                        Toast.LENGTH_LONG);
            }

            return;
        }

        as.apply(applyState, displayToast);
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
