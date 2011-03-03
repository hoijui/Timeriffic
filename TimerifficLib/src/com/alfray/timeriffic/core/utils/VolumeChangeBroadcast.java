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

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;

//-----------------------------------------------

/**
 * Notify ring-guard app types that the volume change was automated
 * and intentional, and then performs the actual action.
 * <p/>
 * See http://code.google.com/p/autosettings/issues/detail?id=4  </br>
 * See http://www.openintents.org/en/node/380
 */
public class VolumeChangeBroadcast {

    public static final String TAG = VolumeChangeBroadcast.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String INTENT_OI_VOL_UPDATE = "org.openintents.audio.action_volume_update";
    private static final String EXTRA_OI_VOLUME = "org.openintents.audio.extra_volume_index";
    private static final String EXTRA_OI_STREAM = "org.openintents.audio.extra_stream_type";
    private static final String EXTRA_OI_RING_MODE = "org.openintents.audio.extra_ringer_mode";

    /** Static instance of the volume receiver. */
    private static ApplyVolumeReceiver sVolumeReceiver;

    /**
     * Notify ring-guard app types that the volume change was automated
     * and intentional, and then performs the actual action.
     * <p/>
     * See http://code.google.com/p/autosettings/issues/detail?id=4  </br>
     * See http://www.openintents.org/en/node/380
     *
     * @param context App context
     * @param volume The new volume level or -1 for a ringer/mute change
     */
    public static void broadcast(Context context,
            int stream,
            int volume,
            int ringMode) {
        try {
            Intent intent = new Intent(INTENT_OI_VOL_UPDATE);
            if (volume != -1) {
                intent.putExtra(EXTRA_OI_STREAM, stream);
                intent.putExtra(EXTRA_OI_VOLUME, volume);
            }
            if (ringMode != -1) {
                // Note: RingGuard will ignore the ringMode change if we don't
                // also provide a stream/volume information. It's up to the caller
                // to pass in the stream/volume too.
                intent.putExtra(EXTRA_OI_RING_MODE, ringMode);
                if (volume == -1) {
                    Log.w(TAG, "Warning: ringmode will be ignored if there's no stream/volume");
                }
            }

            if (DEBUG) Log.d(TAG, String.format("Broadcast: %s %s",
                    intent.toString(), intent.getExtras().toString()));

            List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(intent, 0 /*flags*/);
            if (receivers == null || receivers.isEmpty()) {
                Log.d(TAG, "No vol_update receivers found. Doing direct call.");
                ApplyVolumeReceiver.applyVolumeIntent(context, intent);
                return;
            }

            synchronized (VolumeChangeBroadcast.class) {
                if (sVolumeReceiver == null) {
                    sVolumeReceiver = new ApplyVolumeReceiver();
                    context.getApplicationContext().registerReceiver(sVolumeReceiver, new IntentFilter());
                }
            }

            context.sendOrderedBroadcast(intent,
                    null, //receiverPermission
                    sVolumeReceiver,
                    null, //scheduler
                    Activity.RESULT_OK, //initialCode
                    null, //initialData
                    intent.getExtras() //initialExtras
                    );

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void unregisterVolumeReceiver(Context context) {
        synchronized (VolumeChangeBroadcast.class) {
            if (sVolumeReceiver != null) {
                try {
                    context.getApplicationContext().unregisterReceiver(sVolumeReceiver);
                } catch (Exception e) {
                    Log.w(TAG, e);
                } finally {
                    sVolumeReceiver = null;
                }
            }
        }
    }

    public static class ApplyVolumeReceiver extends BroadcastReceiver {
        public static final String TAG = ApplyVolumeReceiver.class.getSimpleName();

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

                int stream    = intent.getIntExtra(EXTRA_OI_STREAM, -1);
                int vol       = intent.getIntExtra(EXTRA_OI_VOLUME, -1);
                int ringMode  = intent.getIntExtra(EXTRA_OI_RING_MODE, -1);

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
}


