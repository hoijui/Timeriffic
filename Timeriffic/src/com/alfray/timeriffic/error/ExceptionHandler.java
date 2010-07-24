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

package com.alfray.timeriffic.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.prefs.PrefsValues;

public class ExceptionHandler {

    /** Exception Notification ID. 'ExcH' as an int. */
    private static final int EXCEPTION_NOTIF_ID = 'E' << 24 + 'x' << 16 + 'c' << 8 + 'H';

    public static final String TAG = "TFC-Exception";

    public static final String SEP_START = "{[ ";
    public static final String SEP_END = "} /*end*/ \n";

    private Context mAppContext;
    private Handler mHandler;

    private static DateFormat sDateFormat;

    static {
        sDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
    }

    // -----

    public static int getNumExceptionsInLog(Context context) {
        try {
            PrefsValues pv = new PrefsValues(context);
            String curr = pv.getLastExceptions();

            if (curr == null) {
                return 0;
            }

            int count = -1;
            int pos = -1;
            do {
                count++;
                pos = curr.indexOf(SEP_END, pos + 1);
            } while (pos >= 0);

            return count;

        } catch (Exception e) {
            Log.d(TAG, "getNumExceptionsInLog failed", e);
        }

        return 0;
    }


    // -----

    public ExceptionHandler(Context context) {
        // Context is never supposed to be null
        if (context == null) return;

        // We only set our handler if there's no current handler or it is
        // not our -- we don't override our own handler.
        UncaughtExceptionHandler h = Thread.currentThread().getUncaughtExceptionHandler();
        if (h == null || !(h instanceof Handler)) {
            mAppContext = context.getApplicationContext();
            mHandler = new Handler(h);
            Thread.currentThread().setUncaughtExceptionHandler(mHandler);

        }
    }

    public void detach() {
        if (mAppContext != null) {
            Thread.currentThread().setUncaughtExceptionHandler(mHandler.getOldHanlder());
            mHandler = null;
            mAppContext = null;
        }
    }

    private class Handler implements Thread.UncaughtExceptionHandler {

        private final UncaughtExceptionHandler mOldHanlder;

        public Handler(UncaughtExceptionHandler oldHanlder) {
            mOldHanlder = oldHanlder;
        }

        public UncaughtExceptionHandler getOldHanlder() {
            return mOldHanlder;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {

            try {
                PrefsValues pv = new PrefsValues(mAppContext);
                addToLog(pv, e);

                createNotification();

            } catch (Throwable t2) {
                // ignore, or we'll get into an infinite loop
            }

            try {
                // chain the calls to any previous handler that is not one of ours
                UncaughtExceptionHandler h = mOldHanlder;
                while (h != null && h instanceof Handler) {
                    h = ((Handler) h).getOldHanlder();
                }
                if (h != null) {
                    mOldHanlder.uncaughtException(t, e);
                } else {
                    // If we couldn't find any old handler, log the error
                    // to the console if this is an emulator
                    if ("sdk".equals(Build.MODEL)) {
                        Log.e(TAG, "Exception caught in Timeriffic", e);
                    }
                }
            } catch (Throwable t3) {
                // ignore
            }
        }
    };

    public synchronized static void addToLog(PrefsValues pv, Throwable e) {
        // get a trace of the exception
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();

        // store the exception
        String currEx = pv.getLastExceptions();
        if (currEx == null) currEx = "";

        // trim the string if it gets too big.
        if (currEx.length() > 4096) {
            int pos = currEx.indexOf(SEP_END);
            int p = pos + SEP_END.length();
            if (pos > 0) {
                if (p < currEx.length()) {
                    currEx = currEx.substring(p);
                } else {
                    currEx = "";
                }
            }
        }

        String d = sDateFormat.format(new Date(System.currentTimeMillis()));

        currEx += SEP_START + d + " ]\n";
        currEx += sw.toString() + "\n";
        currEx += SEP_END;
        pv.setLastExceptions(currEx);
    }

    private void createNotification() {
        NotificationManager ns =
            (NotificationManager) mAppContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notif = new Notification(
                R.drawable.app_icon, // icon
                "Timeriffic Crashed!",      // tickerText
                System.currentTimeMillis()  // when
                );
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        notif.defaults = Notification.DEFAULT_ALL;

        Intent i = new Intent(mAppContext, ErrorReporterUI.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(mAppContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        notif.setLatestEventInfo(mAppContext,
                "Oh no! Timeriffic Crashed!",               // contentTitle
                "Please click here to report this error.",  // contentText
                pi                                          // contentIntent
                );

        ns.notify(EXCEPTION_NOTIF_ID, notif);
    }
}
