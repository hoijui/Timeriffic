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

import android.content.ContentResolver;

/**
 * Gives access to {@link ContentResolver#getMasterSyncAutomatically()}
 * which is only available starting with API 5.
 *
 * {@link SyncSetting} uses this. When trying to load in API < 5, the class
 * will fail to load with a VerifyError exception since the sync method does
 * not exists.
 */
public class SyncHelper {

    /**
     * This will fail to load with a VerifyError exception if the
     * API to read the master sync doesn't exists (Android API Level 5).
     *
     * This requires permission android.permission.READ_SYNC_SETTINGS
     * @see ContentResolver#getMasterSyncAutomatically()
     */
    public static boolean getMasterSyncAutomatically() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    /**
     * This will fail to load with a VerifyError exception if the
     * API to set the master sync doesn't exists (Android API Level 5).
     *
     * This requires permission android.permission.WRITE_SYNC_SETTINGS
     * @see ContentResolver#setMasterSyncAutomatically(boolean)
     */
    public static void setMasterSyncAutomatically(boolean sync) {
        ContentResolver.setMasterSyncAutomatically(sync);
    }
}
