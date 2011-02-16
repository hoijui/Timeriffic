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

import java.util.Random;

import android.app.Application;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alfray.timeriffic.prefs.PrefsStorage;


public class TimerifficApp extends Application {

    private static final String TAG = TimerifficApp.class.getSimpleName();

    private boolean mFirstStart = true;
    private Runnable mDataListener;

    private PrefsStorage mPrefsStorage = new PrefsStorage("prefs");

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefsStorage.beginReadAsync(getApplicationContext());
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

    public PrefsStorage getPrefsStorage() {
        return mPrefsStorage;
    }

    public String getDeviceId() {
        mPrefsStorage.endReadAsync();
        String id = mPrefsStorage.getString("did", null);

        if (id == null) {
            try {
                TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    id = tm.getDeviceId();
                    if (id != null) {
                        char c[] = new char[id.length() + 1];
                        c[0] = 'i';
                        for (int i = 1; i < c.length; i++) {
                            char c1 = c[i];
                            if (c1 >= '0' && c1 <= '9') c1 = (char) ('9' - c1);
                            else if (c1 >= 'a' && c1 <= 'z') c1 = (char) ('z' - c1 + 'a');
                            else if (c1 >= 'A' && c1 <= 'Z') c1 = (char) ('Z' - c1 + 'A');
                            c[i] = c1;
                        }
                        id = "i" + id;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }

        if (id == null) {
            Random r = new Random();
            char c[] = new char[64+1];
            c[0] = 'r';
            for (int i = 1; i < c.length; i++) {
                char c1 = (char) ('0' + r.nextInt(16));
                if (c1 > '9') c1 = (char) (c1 + 'a' - '9');
                c[i] = c1;
            }
            id = new String(c);

            mPrefsStorage.putString("did", id);
            mPrefsStorage.flushSync(this.getApplicationContext());
        }

        return id;
    }

    public String getIssueId() {
        mPrefsStorage.endReadAsync();
        String id = mPrefsStorage.getString("iid", null);

        if (id == null) {
            // generate a random code with 8 unique symbols out of 34
            // (0-9 + A-Z). We avoid letter O and I which look like 0 and 1.

            Random r = new Random();
            char c[] = new char[8];
            long used = 0;
            // Mark O and I (the letters) as used, to avoid using them.
            used |= (1 << (10 + 'O' - 'A'));
            used |= (1 << (10 + 'I' - 'A'));
            // Avoid repeating the same symbol twice in a row
            int last = -1;
            for (int i = 0; i < c.length; i++) {
                int j = 0;
                // get a new unused letter
                do {
                    j = r.nextInt(10+26);
                } while (j == last || (used & (1 << j)) != 0);
                last = j;
                if (j < 10) {
                    c[i] = (char) ('0' + j);
                } else {
                    c[i] = (char) ('A' + j - 10);
                }
            }
            id = new String(c);

            mPrefsStorage.putString("iid", id);
            mPrefsStorage.flushSync(this.getApplicationContext());
        }

        return id;
    }
}
