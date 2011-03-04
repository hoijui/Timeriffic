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

import java.io.IOException;
import java.lang.reflect.Method;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alfray.timeriffic.core.app.BackupWrapper;
import com.alfray.timeriffic.core.profiles1.ProfilesDB;

/**
 * Backup agent to backup/restore both preferences and main database.
 * Used only with Froyo (Android API level 8) and above.
 * <p/>
 * This class is never referenced directly except by a class name reference
 * in the &lt;application&gt; tag of the manifest.
 * <p/>
 * Implementation detail: since {@link TimerifficBackupAgent} depends
 * on the BackupAgent class, it is not available on platform < API 8.
 * This means any direct access to this class must be avoided. E.g. to
 * get the lock, callers must use the {@link BackupWrapper} instead.
 * <p/>
 * TODO exclude from Proguard
 */
public class TimerifficBackupAgent extends BackupAgentHelper {

    /*
     * References for understanding this:
     *
     * - BackupAgent:
     *   http://d.android.com/reference/android/app/backup/BackupAgent.html
     *
     * - FileHelperExampleAgent.java in the BackupRestore sample for
     *   the Froyo/2.2 Samples.
     */

    public static final String TAG = TimerifficBackupAgent.class.getSimpleName();

    private static final String KEY_DEFAULT_SHARED_PREFS = "default_shared_prefs";
    private static final String KEY_PROFILES_DB = "profiles_db";

    @Override
    public void onCreate() {
        super.onCreate();

        // --- shared prefs backup ---

        // The sharedPreferencesBackupHelper wants the name of the shared pref,
        // however the "default" name is not given by any public API. Try
        // to retrieve it via reflection. This may fail if the method changes
        // in future Android APIs.

        String sharedPrefsName = null;
        try {
            Method m = PreferenceManager.class.getMethod(
                    "getDefaultSharedPreferencesName",
                    new Class<?>[] { Context.class } );
            Object v = m.invoke(null /*receiver*/, (Context) this);
            if (v instanceof String) {
                sharedPrefsName = (String) v;
            }
        } catch (Exception e) {
            // ignore
        }

        if (sharedPrefsName == null) {
            // In case the API call fails, we implement it naively ourselves
            // like it used to be done in Android 1.6 (API Level 4)
            sharedPrefsName = this.getPackageName() + "_preferences";
        }

        if (sharedPrefsName != null) {
            SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, sharedPrefsName);
            addHelper(KEY_DEFAULT_SHARED_PREFS, helper);
            Log.d(TAG, "Register backup 1 for " + sharedPrefsName);
        }

        // --- profiles db backup ---

        // Now add the backup helper for the profile database.
        // The FileBackupHelper defaults to a file under getFilesDir()
        // so we need to trim the full database path.

        String filesPath = getFilesDir().getAbsolutePath();
        String dbPath = ProfilesDB.getDatabaseFile(this).getAbsolutePath();

        if (filesPath != null && dbPath != null) {
            dbPath = relativePath(filesPath, dbPath);

            FileBackupHelper helper = new FileBackupHelper(this, dbPath);
            addHelper(KEY_PROFILES_DB, helper);
            Log.d(TAG, "Register backup 2 for " + dbPath);
        }

    }

    @Override
    public void onBackup(
            ParcelFileDescriptor oldState,
            BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the helper performs the backup operation
        synchronized (BackupWrapper.getBackupLock()) {
            super.onBackup(oldState, data, newState);
            Log.d(TAG, "onBackup");
        }
    }

    @Override
    public void onRestore(
            BackupDataInput data,
            int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the helper restores the file from
        // the data provided here.
        synchronized (BackupWrapper.getBackupLock()) {
            super.onRestore(data, appVersionCode, newState);
            Log.d(TAG, "onRestore");
        }
    }

    /**
     * Computes a relative path from source to dest.
     * e.g. if source is A/B and dest is A/C/D, the relative path is ../C/D
     * <p/>
     * Source and dest are expected to be absolute unix-like, e.g. /a/b/c
     */
    private String relativePath(String source, String dest) {
        // Implementation: we're working with unix-like stuff that is absolute
        // so /a/b/c => { "", "a", "b", "c" }
        //
        // In our use-case we can't have source==dest, so we don't handle it.

        String[] s = source.split("/");
        String[] d = dest.split("/");

        // Find common root part and ignore it.
        int common = 0;
        while (common < s.length &&
                common < d.length &&
                s[common].equals(d[common])) {
            common++;
        }

        // Now we need as many ".." as dirs left in source to go back to the
        // common root.
        String result = "";
        for (int i = common; i < s.length; i++) {
            if (result.length() > 0) result += "/";
            result += "..";
        }

        // Finally add whatever is not a common root from the dest
        for (int i = common; i < d.length; i++) {
            if (result.length() > 0) result += "/";
            result += d[i];
        }


        return result;
    }

}
