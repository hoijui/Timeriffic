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

package com.alfray.timeriffic.core.settings;

import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.Log;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.PrefPercent;
import com.alfray.timeriffic.core.actions.PrefPercentDialog.Accessor;
import com.alfray.timeriffic.core.profiles1.Columns;
import com.alfray.timeriffic.ui.ChangeBrightnessUI;

//-----------------------------------------------

public class BrightnessSetting implements ISetting {

    public static final String TAG = BrightnessSetting.class.getSimpleName();

    private boolean mCheckAutoSupported = true;
    private boolean mIsAutoSupported = false;

    /** android.provider.Settings.SCREEN_BRIGHTNESS_MODE, available starting with API 8. */
    private static final String AUTO_BRIGHT_KEY = Settings.System.SCREEN_BRIGHTNESS_MODE;
    /** Auto-brightness is supported and in "manual" mode. */
    public static final int AUTO_BRIGHT_MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    /** Auto-brightness is supported and in automatic mode. */
    public static final int AUTO_BRIGHT_AUTO = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    /** Auto-brightness is not supported. */
    public static final int AUTO_BRIGHT_UNSUPPORTED = -1;

    public boolean isSupported(Context context) {
        return true;
    }

    public boolean isAutoBrightnessSupported(Context context) {
        if (!mCheckAutoSupported) return mIsAutoSupported;

        int mode = AUTO_BRIGHT_UNSUPPORTED;
        try {
            mode = getAutoBrightness(context);
        } catch (Throwable e) {
            // There's no good reason for this to crash but it's been
            // shown to fail when trying to perform it at boot. So in this
            // case return false but do not mark it as checked so that we
            // can retry later.
            return false;
        }

        try {
            if (mode == AUTO_BRIGHT_UNSUPPORTED) return false;

            SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (manager != null) {
                List<Sensor> list = manager.getSensorList(Sensor.TYPE_LIGHT);
                mIsAutoSupported = list != null && list.size() > 0;
            }
        } finally {
            mCheckAutoSupported = false;
        }

        return mIsAutoSupported;
    }

    public Object createUi(final Activity activity, String[] currentActions) {
        PrefPercent p = new PrefPercent(activity,
                        R.id.brightnessButton,
                        currentActions,
                        Columns.ACTION_BRIGHTNESS,
                        activity.getString(R.string.editaction_brightness),
                        R.drawable.ic_menu_view_brightness,
                        new Accessor() {
                            public void changePercent(int percent) {
                                // disable the immediate slider feedback, it flickers too much and is very slow.
                            }

                            public int getPercent() {
                                return getCurrentBrightness(activity);
                            }

                            public int getCustomChoiceLabel() {
                                if (isAutoBrightnessSupported(activity)) {
                                    return R.string.timedaction_auto_brightness;
                                }
                                return 0;
                            }

                            public int getCustomChoiceButtonLabel() {
                                return R.string.timedaction_automatic;
                            }

                            public char getCustomChoiceValue() {
                                return Columns.ACTION_BRIGHTNESS_AUTO;
                            }
                        });
        p.setEnabled(isSupported(activity), activity.getString(R.string.setting_not_supported));
        return p;
    }

    public void collectUiResults(Object settingUi, StringBuilder outActions) {
        if (settingUi instanceof PrefPercent) {
            ((PrefPercent) settingUi).collectResult(outActions);
        }
    }

    public String getActionLabel(Context context, String action) {
        if (action.length() < 2) return null;
        char v = action.charAt(1);
        if (isAutoBrightnessSupported(context) && v == Columns.ACTION_BRIGHTNESS_AUTO) {
            return context.getString(R.string.timedaction_auto_brightness);
        } else {
            try {
                int value = Integer.parseInt(action.substring(1));

                return context.getString(
                            R.string.timedaction_brightness_int,
                            value);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return null;
    }

    public boolean performAction(Context context, String action) {
        if (action.length() < 2) return true;
        char v = action.charAt(1);
        try {
            if (v == Columns.ACTION_BRIGHTNESS_AUTO) {
                changeAutoBrightness(context, true);
            } else {
                int value = Integer.parseInt(action.substring(1));
                changeAutoBrightness(context, false);
                changeBrightness(context, value);
            }
        } catch (NumberFormatException e) {
            // pass

        } catch (Throwable e) {
            // Shouldn't happen. Offer to retry later.
            return false;
        }

        return true;
    }

    private void changeAutoBrightness(Context context, boolean auto) {
        ContentResolver resolver = context.getContentResolver();
        Settings.System.putInt(resolver,
                               AUTO_BRIGHT_KEY,
                               auto ? AUTO_BRIGHT_AUTO : AUTO_BRIGHT_MANUAL);
    }

    /**
     * Returns one of {@link #AUTO_BRIGHT_MANUAL}, {@link #AUTO_BRIGHT_AUTO} or
     * {@link #AUTO_BRIGHT_UNSUPPORTED}.
     */
    private int getAutoBrightness(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, AUTO_BRIGHT_KEY, AUTO_BRIGHT_UNSUPPORTED);
    }

    /**
     * @param percent The new value in 0..100 range (will get mapped to adequate OS values)
     */
    private void changeBrightness(Context context, int percent) {
        // Reference:
        // http://android.git.kernel.org/?p=platform/packages/apps/Settings.git;a=blob;f=src/com/android/settings/BrightnessPreference.java
        // The source indicates
        // - Backlight range is 0..255
        // - Must not set to 0 (user would see nothing) so they use 10 as minimum
        // - All constants are in android.os.Power which is hidden in the SDK.
        // - To get value: Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        // - To set value: Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, v);


        int actual = getCurrentBrightness(context);
        if (actual == percent) {
            Log.d(TAG, "NOT changed, already " + Integer.toString(percent));
            return;
        }

        Log.d(TAG, "SET to " + Integer.toString(percent));

        Intent i = new Intent(context, ChangeBrightnessUI.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(ChangeBrightnessUI.INTENT_SET_BRIGHTNESS, percent / 100.0f);
        context.startActivity(i);
    }

    /**
     * Returns screen brightness in range 0..100%.
     * <p/>
     * See comments in {@link #changeBrightness(Context, int)}. The real range is 0..255,
     * maps it 0..100.
     */
    private int getCurrentBrightness(Context context) {
        return (int) (100 * ChangeBrightnessUI.getCurrentBrightness(context));
    }
}


