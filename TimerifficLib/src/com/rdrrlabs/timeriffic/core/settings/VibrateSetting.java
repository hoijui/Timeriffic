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

package com.rdrrlabs.timeriffic.core.settings;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

//-----------------------------------------------

public class VibrateSetting implements ISetting {

    public static final String TAG = VibrateSetting.class.getSimpleName();

    private boolean mCheckSupported = true;
    private boolean mIsSupported = false;

    public boolean isSupported(Context context) {
        if (mCheckSupported) {
            AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mIsSupported = manager != null;
            mCheckSupported = false;
        }
        return mIsSupported;
    }

    public Object createUi(Activity activity, String[] currentActions) {
        return null;
    }

    public void collectUiResults(Object settingUi, StringBuilder outActions) {
    }

    public String getActionLabel(Context context, String action) {
        return null;
    }

    public boolean performAction(Context context, String action) {
        return true;
    }

}


