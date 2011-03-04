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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.util.Log;

//-----------------------------------------------

/**
 * Notify ring-guard app types that the volume change was automated
 * and intentional, and then performs the actual action.
 * <p/>
 * See http://code.google.com/p/autosettings/issues/detail?id=4  </br>
 * See http://www.openintents.org/en/node/380
 */
public class VolumeChange {

    public static final String TAG = VolumeChange.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String INTENT_OI_VOL_UPDATE = "org.openintents.audio.action_volume_update";
    public static final String EXTRA_OI_VOLUME = "org.openintents.audio.extra_volume_index";
    public static final String EXTRA_OI_STREAM = "org.openintents.audio.extra_stream_type";
    public static final String EXTRA_OI_RING_MODE = "org.openintents.audio.extra_ringer_mode";

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
    public static void changeVolume(Context context,
            int stream,
            int volume) {
        broadcast(context, stream, volume, -1 /*ring*/);
    }

    /**
     * Notify ring-guard app types that the ringer change was automated
     * and intentional, and then performs the actual action.
     * <p/>
     * See http://code.google.com/p/autosettings/issues/detail?id=4  </br>
     * See http://www.openintents.org/en/node/380
     *
     * @param context App context
     * @param volume The new volume level or -1 for a ringer/mute change
     */
    public static void changeRinger(Context context, int ringMode) {
        broadcast(context, -1 /*stream*/, -1 /*vol*/, ringMode);
    }

    private static void broadcast(Context context,
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
                intent.putExtra(EXTRA_OI_RING_MODE, ringMode);
            }

            List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(intent, 0 /*flags*/);
            if (receivers == null || receivers.isEmpty()) {
                Log.d(TAG, "No vol_update receivers found. Doing direct call.");
                ApplyVolumeReceiver.applyVolumeIntent(context, intent);
                return;
            }

            // If we get here, we detected something is listening to
            // the ringguard intent.

            if (ringMode != -1 && volume == -1 && stream == -1) {
                //  Note: RingGuard will ignore the ringMode change if we don't
                // also provide a stream/volume information. It's up to the caller
                // to pass in the stream/volume too.
                AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                stream = AudioManager.STREAM_RING;
                volume = ringMode == AudioManager.RINGER_MODE_NORMAL ?
                            manager.getStreamVolume(stream) :
                                0;
                intent.putExtra(EXTRA_OI_STREAM, stream);
                intent.putExtra(EXTRA_OI_VOLUME, volume);
            }

            synchronized (VolumeChange.class) {
                if (sVolumeReceiver == null) {
                    sVolumeReceiver = new ApplyVolumeReceiver();
                    context.getApplicationContext().registerReceiver(sVolumeReceiver, new IntentFilter());
                }
            }

            if (DEBUG) Log.d(TAG, String.format("Broadcast: %s %s",
                    intent.toString(), intent.getExtras().toString()));

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
        synchronized (VolumeChange.class) {
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
}


