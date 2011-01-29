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

package com.alfray.timeriffic.app;

import android.app.backup.BackupManager;
import android.content.Context;
import android.util.Log;

/**
 * Wrapper around the {@link BackupManager} API, which is only available
 * starting with Froyo (Android API 8).
 *
 * The actual work is done in the class BackupWrapperImpl, which uses the
 * API methods, and thus will fail to load with a VerifyError exception
 * on older Android versions. The wrapper defers to the impl class if
 * it loaded, and otherwise just drops all the calls.
 */
public class BackupWrapper {

    private static final String TAG = BackupWrapper.class.getSimpleName();
    private static final boolean DEBUG = true;

    private final BackupWrapperImpl mImpl;

    public BackupWrapper(Context context) {
        BackupWrapperImpl b = null;
        try {
            // Try to load the actual implementation. This may fail.
            b = new BackupWrapperImpl(context);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "BackupWrapperImpl failed to load", e);
        }
        mImpl = b;
    }

    public void dataChanged() {
        if (mImpl != null) {
            mImpl.dataChanged();
            if (DEBUG) Log.d(TAG, "Backup dataChanged");
        }
    }
}
