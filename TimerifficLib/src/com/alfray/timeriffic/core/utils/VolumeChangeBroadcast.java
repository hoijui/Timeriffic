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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String EXTRA_NOTIF_SYNC = "com.rdrrlabs.audio.ring_notif_sync";

    /** android.provider.Settings.NOTIFICATION_USE_RING_VOLUME, available starting with API 3
     *  but it's hidden from the SDK. The Settings.java comment says eventually this setting
     *  will go away later once there are "profile" support, whatever that is. */
    public static final String NOTIF_RING_VOL_KEY = "notifications_use_ring_volume";
    /** Notification vol and ring volumes are synched. */
    public static final int NOTIF_RING_VOL_SYNCED = 1;
    /** Notification vol and ring volumes are not synched. */
    public static final int NOTIF_RING_VOL_NOT_SYNCHED = 0;
    /** No support for notification and ring volume sync. */
    public static final int NOTIF_RING_VOL_UNSUPPORTED = -1;

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
            int ringMode,
            int notifSync) {
        try {
            Intent intent = new Intent(INTENT_OI_VOL_UPDATE);
            if (volume != -1) {
                intent.putExtra(EXTRA_OI_STREAM, stream);
                intent.putExtra(EXTRA_OI_VOLUME, volume);
            }
            if (ringMode != -1) {
                intent.putExtra(EXTRA_OI_RING_MODE, ringMode);

                // RingGuard will ignore the ringMode change if we don't
                // also provide a stream/volume information.
                // For mute, specify a volume of 0.
                // For ring, reuse the same current volume.
                if (volume == -1) {
                    // simulate an indempotent volume change
                    AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (manager != null) {
                        int vol = manager.getStreamVolume(AudioManager.STREAM_RING);
                        intent.putExtra(EXTRA_OI_STREAM, AudioManager.STREAM_RING);
                        intent.putExtra(EXTRA_OI_VOLUME,
                                ringMode == AudioManager.RINGER_MODE_NORMAL ? vol : 0);
                    }
                }
            }
            if (notifSync != -1) {
                intent.putExtra(EXTRA_NOTIF_SYNC, notifSync);
            }

            ApplyVolumeReceiver receiver = new ApplyVolumeReceiver();
            context.registerReceiver(receiver, new IntentFilter());

            if (DEBUG) Log.d(TAG, String.format("Broadcast: %s %s",
                    intent.toString(), intent.getExtras().toString()));

            context.sendOrderedBroadcast(intent,
                    null, //receiverPermission
                    receiver,
                    null, //scheduler
                    Activity.RESULT_OK, //initialCode
                    null, //initialData
                    intent.getExtras() //initialExtras
                    );

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private static class ApplyVolumeReceiver extends BroadcastReceiver {
        public static final String TAG = ApplyVolumeReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {

            context.unregisterReceiver(this);

            if (intent == null) {
                Log.d(TAG, "null intent");
                return;
            }

            if (getResultCode() == Activity.RESULT_CANCELED) {
                Log.d(TAG, "VolumeReceiver was canceled");
            }

            int stream = intent.getIntExtra(EXTRA_OI_STREAM, -1);
            int vol = intent.getIntExtra(EXTRA_OI_VOLUME, -1);
            int ringMode = intent.getIntExtra(EXTRA_OI_RING_MODE, -1);
            int notifSync = intent.getIntExtra(EXTRA_NOTIF_SYNC, -1);

            if (stream >= 0 && vol >= 0) {
                changeStreamVolume(context, stream, vol);
            }

            if (notifSync >= 0) {
                changeNotifRingVolSync(context, notifSync > 0);
            }

            if (ringMode >= 0) {
                changeRingMode(context, ringMode);
            }
        }

        private void changeStreamVolume(Context context, int stream, int vol) {
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

        private void changeNotifRingVolSync(Context context, boolean sync) {
            ContentResolver resolver = context.getContentResolver();
            Settings.System.putInt(resolver,
                                   NOTIF_RING_VOL_KEY,
                                   sync ? NOTIF_RING_VOL_SYNCED : NOTIF_RING_VOL_NOT_SYNCHED);
        }

        private void changeRingMode(Context context, int ringMode) {
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


