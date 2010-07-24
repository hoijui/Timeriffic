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

package com.alfray.timeriffic.app;

import com.alfray.timeriffic.error.ExceptionHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class UpdateReceiver extends BroadcastReceiver {

    private final static boolean DEBUG = true;
    public final static String TAG = "TFC-UpdReceiver";

    /** Name of intent to broadcast to activate this receiver when doing
     *  alarm-based apply-state. */
    public final static String ACTION_APPLY_STATE = "com.alfray.intent.action.APPLY_STATE";

    /** Name of intent to broadcast to activate this receiver when triggering
     *  a check from the UI. */
    public final static String ACTION_UI_CHECK = "com.alfray.intent.action.UI_CHECK";

    /** Name of an extra int: how we should display a toast for next event. */
    public final static String EXTRA_TOAST_NEXT_EVENT = "toast-next";

    public final static int TOAST_NONE = 0;
    public final static int TOAST_IF_CHANGED = 1;
    public final static int TOAST_ALWAYS = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        ExceptionHandler handler = new ExceptionHandler(context);
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerifficReceiver");
            wl.acquire();
            UpdateService.update(context, intent, wl);
            if (DEBUG) Log.d(TAG, "UpdateService requested");
        } finally {
            handler.detach();
        }
    }
}
