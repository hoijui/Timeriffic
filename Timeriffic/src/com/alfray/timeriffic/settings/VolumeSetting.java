/*
 * Project: Timeriffic
 * Copyright (C) 2010 ralfoide gmail com,
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

package com.alfray.timeriffic.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.actions.PrefPercent;
import com.alfray.timeriffic.actions.PrefPercentDialog.Accessor;
import com.alfray.timeriffic.profiles.Columns;

//-----------------------------------------------

public class VolumeSetting implements ISetting {

    private static final boolean DEBUG = true;
    private static final String TAG = "VolumeSetting";
    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;
    private final int mStream;

    /** android.provider.Settings.NOTIFICATION_USE_RING_VOLUME, available starting with API 3
     *  but it's hidden from the SDK. The Settings.java comment says eventually this setting
     *  will go away later once there are "profile" support, whatever that is. */
    private static final String NOTIF_RING_VOL_KEY = "notifications_use_ring_volume";
    /** Notification vol and ring volumes are synched. */
    private static final int NOTIF_RING_VOL_SYNCED = 1;
    /** Notification vol and ring volumes are not synched. */
    private static final int NOTIF_RING_VOL_NOT_SYNCHED = 0;
    /** No support for notification and ring volume sync. */
    private static final int NOTIF_RING_VOL_UNSUPPORTED = -1;

    private static SparseArray<StreamInfo> sStreamInfo = new SparseArray<StreamInfo>(6);

    static {
        sStreamInfo.put(AudioManager.STREAM_RING,
                new StreamInfo(Columns.ACTION_RING_VOLUME,
                        R.id.ringerVolButton,
                        R.string.editaction_volume,
                        R.string.timedaction_ringer_int));

        sStreamInfo.put(AudioManager.STREAM_NOTIFICATION,
                new StreamInfo(Columns.ACTION_NOTIF_VOLUME,
                        R.id.notifVolButton,
                        R.string.editaction_notif_volume,
                        R.string.timedaction_notif_int));

        sStreamInfo.put(AudioManager.STREAM_MUSIC,
                new StreamInfo(Columns.ACTION_MEDIA_VOLUME,
                        R.id.mediaVolButton,
                        R.string.editaction_media_volume,
                        R.string.timedaction_media_int));

        sStreamInfo.put(AudioManager.STREAM_ALARM,
                new StreamInfo(Columns.ACTION_ALARM_VOLUME,
                        R.id.alarmVolButton,
                        R.string.editaction_alarm_volume,
                        R.string.timedaction_alarm_int));

        sStreamInfo.put(AudioManager.STREAM_SYSTEM,
                new StreamInfo(Columns.ACTION_SYSTEM_VOLUME,
                        R.id.systemVolButton,
                        R.string.editaction_system_volume,
                        R.string.timedaction_system_vol_int));

        sStreamInfo.put(AudioManager.STREAM_VOICE_CALL,
                new StreamInfo(Columns.ACTION_VOICE_CALL_VOLUME,
                        R.id.voiceCallVolButton,
                        R.string.editaction_voice_call_volume,
                        R.string.timedaction_voice_call_vol_int));
        }

    public VolumeSetting(int stream) {
        mStream = stream;
    }

    @Override
    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            if (sStreamInfo.get(mStream) == null) {
                mIsSupported = false;
            } else {
                AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                mIsSupported = manager != null;
            }
            mCheckSupported = false;
        }
        return mIsSupported;
    }

    @Override
    public Object createUi(final Activity activity, String[] currentActions) {
        StreamInfo info = sStreamInfo.get(mStream);
        if (info == null) return null; // should not happen

        PrefPercent p = new PrefPercent(activity,
                info.getButtonResId(),
                currentActions,
                info.getActionPrefix(),
                activity.getString(info.getDialogTitleResId()),
                0,
                new Accessor() {
                    @Override
                    public void changePercent(int percent) {
                        change(activity, percent);
                    }

                    @Override
                    public int getPercent() {
                        return getVolume(activity, mStream);
                    }

                    @Override
                    public int getCustomChoiceLabel() {
                        if (mStream == AudioManager.STREAM_NOTIFICATION &&
                                canSyncNotificationRingVol(activity)) {
                            return R.string.editaction_notif_ring_sync;
                        }
                        return 0;
                    }

                    @Override
                    public int getCustomChoiceButtonLabel() {
                        if (mStream == AudioManager.STREAM_NOTIFICATION) {
                            return R.string.actionlabel_notif_ring_sync;
                        }

                        return 0;
                    }

                    @Override
                    public char getCustomChoiceValue() {
                        if (mStream == AudioManager.STREAM_NOTIFICATION) {
                            return Columns.ACTION_NOTIF_RING_VOL_SYNC;
                        }
                        return 0;
                    }
                });
        return p;
    }

    @Override
    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefPercent) {
            ((PrefPercent) settingUi).collectResult(outActions);
        }
    }

    @Override
    public String getActionLabel(Context context, String action) {
        if (mStream == AudioManager.STREAM_NOTIFICATION) {
            char v = action.charAt(1);
            if (v == Columns.ACTION_NOTIF_RING_VOL_SYNC) {
                return context.getString(R.string.timedaction_notif_ring_sync);
            }
        }
        try {
            StreamInfo info = sStreamInfo.get(mStream);
            if (info == null) return null; // should not happen

            int value = Integer.parseInt(action.substring(1));
            return context.getString(info.getActionLabelResId(), value);
        } catch (NumberFormatException e) {
            if (DEBUG) Log.d(TAG, "Invalid volume number for action " + action);
        }
        return null;
    }

    @Override
    public void performAction(Context context, String action) {
        if (mStream == AudioManager.STREAM_NOTIFICATION) {
            char v = action.charAt(1);
            if (v == Columns.ACTION_NOTIF_RING_VOL_SYNC) {
                changeNotifRingVolSync(context, true);
                return;
            }
        }
        try {
            int value = Integer.parseInt(action.substring(1));

            if (mStream == AudioManager.STREAM_NOTIFICATION) {
                changeNotifRingVolSync(context, false);
            }

            change(context, value);
        } catch (NumberFormatException e) {
            if (DEBUG) Log.d(TAG, "Perform action failed for " + action);
        }
    }

    // -----

    private int getVolume(Context context, int stream) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (manager == null) {
            if (DEBUG) Log.w(TAG, "getVolume: AUDIO_SERVICE missing!");
            return 50;
        }

        int vol = manager.getStreamVolume(stream);
        int max = manager.getStreamMaxVolume(stream);

        return (vol * 100 / max);
    }

    private void change(Context context, int percent) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (manager == null) {
            if (DEBUG) Log.w(TAG, "changeVolume: AUDIO_SERVICE missing!");
            return;
        }

        if (DEBUG) Log.d(TAG, String.format("changeVolume: stream=%d, vol=%d%%", mStream, percent));

        int max = manager.getStreamMaxVolume(mStream);
        int vol = (max * percent) / 100;

        broadcastVolumeUpdate(context, vol);
        manager.setStreamVolume(mStream, vol, 0 /*flags*/);
    }

    /**
     * Notify ring-guard app types that the volume change was automated
     * and intentional.
     *
     * @see http://code.google.com/p/autosettings/issues/detail?id=4
     * @see http://www.openintents.org/en/node/380
     * @param stream One of AudioManager.STREAM_xyz
     * @param volume The new volume level or -1 for a ringer/mute change
     */
    private void broadcastVolumeUpdate(Context context, int volume) {
        try {
            Intent intent = new Intent("org.openintents.audio.action_volume_update");
            intent.putExtra("org.openintents.audio.extra_stream_type", mStream);
            intent.putExtra("org.openintents.audio.extra_volume_index", volume);
            //--Log.d(TAG, "Notify RingGuard: " + intent.toString() + intent.getExtras().toString());
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private void changeNotifRingVolSync(Context context, boolean sync) {
        ContentResolver resolver = context.getContentResolver();
        Settings.System.putInt(resolver,
                               NOTIF_RING_VOL_KEY,
                               sync ? NOTIF_RING_VOL_SYNCED : NOTIF_RING_VOL_NOT_SYNCHED);
    }

    /**
     * Returns one of {@link #NOTIF_RING_VOL_SYNCED}, {@link #NOTIF_RING_VOL_NOT_SYNCHED} or
     * {@link #NOTIF_RING_VOL_UNSUPPORTED}.
     */
    private int getSyncNotifRingVol(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, NOTIF_RING_VOL_KEY, NOTIF_RING_VOL_UNSUPPORTED);
    }

    private boolean canSyncNotificationRingVol(Context context) {
        return getSyncNotifRingVol(context) != NOTIF_RING_VOL_UNSUPPORTED;
    }

    private static class StreamInfo {
        private final char mActionPrefix;
        private final int mButtonResId;
        private final int mDialogTitleResId;
        private final int mActionLabelResId;

        public StreamInfo(char actionPrefix,
                int buttonResId,
                int dialogTitleResId,
                int actionLabelResId) {
                    mActionPrefix = actionPrefix;
                    mButtonResId = buttonResId;
                    mDialogTitleResId = dialogTitleResId;
                    mActionLabelResId = actionLabelResId;
        }

        public char getActionPrefix() {
            return mActionPrefix;
        }

        public int getActionLabelResId() {
            return mActionLabelResId;
        }

        public int getButtonResId() {
            return mButtonResId;
        }

        public int getDialogTitleResId() {
            return mDialogTitleResId;
        }
    }
}


