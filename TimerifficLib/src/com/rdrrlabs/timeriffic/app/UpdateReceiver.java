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

package com.rdrrlabs.timeriffic.app;

import com.rdrrlabs.timeriffic.core.app.Core;
import com.rdrrlabs.timeriffic.core.error.ExceptionHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class UpdateReceiver extends BroadcastReceiver {

    public final static String TAG = UpdateReceiver.class.getSimpleName();
    private final static boolean DEBUG = false;

    /** Name of intent to broadcast to activate this receiver when doing
     *  alarm-based apply-state. */
    public final static String ACTION_APPLY_STATE = "com.rdrrlabs.intent.action.APPLY_STATE";

    /** Name of intent to broadcast to activate this receiver when triggering
     *  a check from the UI. */
    public final static String ACTION_UI_CHECK = "com.rdrrlabs.intent.action.UI_CHECK";

    /** Name of an extra int: how we should display a toast for next event. */
    public final static String EXTRA_TOAST_NEXT_EVENT = "toast-next";

    public final static int TOAST_NONE = 0;
    public final static int TOAST_IF_CHANGED = 1;
    public final static int TOAST_ALWAYS = 2;

    /**
     * Starts the {@link UpdateService}.
     * Code should be at its minimum. No logging or DB access here.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        ExceptionHandler handler = new ExceptionHandler(context);
        WakeLock wl = null;
        try {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerifficReceiver");
                wl.acquire();
            } catch (Exception e) {
                // Hmm wake lock failed... not sure why. Continue anyway.
                Log.w(TAG, "WakeLock.acquire failed");
            }

            if (DEBUG) Log.d(TAG, "UpdateService requested");

            Core core = TimerifficApp.getInstance(context).getCore();
            core.getUpdateService().startFromReceiver(context, intent, wl);
            wl = null;

        } finally {
            if (wl != null) {
                try {
                    wl.release();
                } catch (Exception e) {
                    // ignore
                }
            }
            handler.detach();
        }
    }
}
