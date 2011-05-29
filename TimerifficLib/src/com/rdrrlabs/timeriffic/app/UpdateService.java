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

package com.rdrrlabs.timeriffic.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.rdrrlabs.timeriffic.core.app.Core;
import com.rdrrlabs.timeriffic.core.error.ExceptionHandler;

public class UpdateService extends Service {

    public static final String TAG = UpdateService.class.getSimpleName();
    private static final boolean DEBUG = false;

    @Override
    public IBinder onBind(Intent intent) {
        // pass
        return null;
    }


    //----

    @Override
    public void onStart(Intent intent, int startId) {
        if (DEBUG) Log.d(TAG, "Start service");
        ExceptionHandler handler = new ExceptionHandler(this);
        try {
            super.onStart(intent, startId);

            Core core = TimerifficApp.getInstance(this).getCore();
            core.getUpdateService().onStart(this, intent, startId);

        } finally {
            handler.detach();
            if (DEBUG) Log.d(TAG, "Stopping service");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Core core = TimerifficApp.getInstance(this).getCore();
        core.getUpdateService().onDestroy(this);

        super.onDestroy();
    }
}
