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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class PhoneStateService extends Service {

    private MyPhoneStateListener mPSListener;

    @Override
    public IBinder onBind(Intent intent) {
        // pass
        return null;
    }

    @Override
    public void onCreate() {

        Context context = getApplicationContext();
        TelephonyManager telephony =
            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (mPSListener == null) {
            mPSListener = new MyPhoneStateListener();
        }
        telephony.listen(mPSListener, PhoneStateListener.LISTEN_CALL_STATE);

        super.onCreate();
    }

    @Override
    public void onDestroy() {

        if (mPSListener != null) {
            Context context = getApplicationContext();
            TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(mPSListener, PhoneStateListener.LISTEN_NONE);
            mPSListener = null;
        }

        super.onDestroy();
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
        }
    }

}
