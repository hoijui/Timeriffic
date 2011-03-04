/*
 * Project: Timeriffic
 * Copyright (C) 2011 ralfoide gmail com,
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

package com.alfray.timeriffic.core.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

//-----------------------------------------------

public class ApplyVolumeReceiver extends BroadcastReceiver {
    public static final String TAG = ApplyVolumeReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (getResultCode() == Activity.RESULT_CANCELED) {
            Log.d(TAG, "VolumeReceiver was canceled (and ignored)");
        }

        applyVolumeIntent(context, intent);
    }

    public static void applyVolumeIntent(Context context, Intent intent) {
        try {
            if (intent == null) {
                Log.d(TAG, "null intent");
                return;
            }

            int stream    = intent.getIntExtra(VolumeChange.EXTRA_OI_STREAM, -1);
            int vol       = intent.getIntExtra(VolumeChange.EXTRA_OI_VOLUME, -1);
            int ringMode  = intent.getIntExtra(VolumeChange.EXTRA_OI_RING_MODE, -1);

            if (stream >= 0 && vol >= 0) {
                changeStreamVolume(context, stream, vol);
            }

            if (ringMode >= 0) {
                changeRingMode(context, ringMode);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private static void changeStreamVolume(Context context, int stream, int vol) {
        //-- if (DEBUG) Log.d(TAG, String.format("applyVolume: stream=%d, vol=%d%%", stream, vol));

        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager != null) {
            //-- if (DEBUG) Log.d(TAG, String.format("current=%d%%", manager.getStreamVolume(stream)));

            manager.setStreamVolume(stream, vol, 0 /*flags*/);

            try {
                Thread.sleep(1 /*ms*/);
            } catch (InterruptedException e) {
                // ignore
            }

            int actual = manager.getStreamVolume(stream);
            if (actual == vol) {
                if (DEBUG) Log.d(TAG,
                        String.format("Vol change OK, stream %d, vol %d", stream, vol));
            } else {
                if (DEBUG) Log.d(TAG,
                        String.format("Vol change FAIL, stream %d, vol %d, actual %d", stream, vol, actual));
            }
        } else {
            Log.d(TAG, "No audio manager found");
        }
    }

    private static void changeRingMode(Context context, int ringMode) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (manager != null) {
            manager.setRingerMode(ringMode);
            if (DEBUG) Log.d(TAG, String.format("Ring mode set to %d", ringMode));
        } else {
            Log.d(TAG, "No audio manager found");
        }
    }
}


