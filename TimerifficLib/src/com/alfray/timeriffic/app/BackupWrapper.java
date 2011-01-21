/*
 * (c) ralfoide gmail com, 2010
 * Project: Timeriffic
 * License TBD
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
