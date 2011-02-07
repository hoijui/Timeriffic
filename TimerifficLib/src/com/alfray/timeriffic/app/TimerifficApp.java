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

import android.app.Application;

import com.alfray.timeriffic.prefs.PrefsStorage;


public class TimerifficApp extends Application {

    @SuppressWarnings("unused")
    private static final String TAG = TimerifficApp.class.getSimpleName();

    private boolean mFirstStart = true;
    private Runnable mDataListener;

    @SuppressWarnings("unused")
    private PrefsStorage mPrefsStorage;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean isFirstStart() {
        return mFirstStart;
    }

    public void setFirstStart(boolean firstStart) {
        mFirstStart = firstStart;
    }

    //---------------------

    public void setDataListener(Runnable listener) {
        mDataListener = listener;
    }

    public void invokeDataListener() {
        if (mDataListener != null) mDataListener.run();
    }
}
