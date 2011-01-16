/*
 * (c) ralfoide gmail com, 2010
 * Project: Timeriffic
 * License TBD
 */

package com.alfray.timeriffic.app;

import android.app.backup.BackupManager;
import android.content.Context;

/**
 * Wrapper around the {@link BackupManager} API, only available with
 * Froyo (Android API level 8).
 * <p/>
 * This class should not be used directly. Instead, use {@link BackupWrapper}
 * which will delegate calls to this one if it can be loaded (that is if the
 * backup API is available.)
 */
/* package */ class BackupWrapperImpl {

    private BackupManager mManager;

    public BackupWrapperImpl(Context context) {
        mManager = new BackupManager(context);
    }

    public void dataChanged() {
        if (mManager != null) {
            mManager.dataChanged();
        }
    }

}
