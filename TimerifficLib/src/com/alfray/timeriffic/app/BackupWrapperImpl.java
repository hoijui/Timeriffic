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
