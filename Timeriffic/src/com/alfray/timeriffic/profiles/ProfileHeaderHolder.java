/*
 * Project: Timeriffic
 * Copyright (C) 2009 ralfoide gmail com,
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

package com.alfray.timeriffic.profiles;

import android.database.Cursor;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.profiles.ProfilesUI.ColIndexes;

/**
 * The holder for a profile header row.
 */
public class ProfileHeaderHolder extends BaseHolder {

    private static boolean DEBUG = false;
    public static String TAG = "TFC-PHHolder";

    public ProfileHeaderHolder(ProfilesUI activity, View view) {
        super(activity, view);
    }

    @Override
    public void setUiData() {
        ColIndexes colIndexes = mActivity.getColIndexes();
        Cursor cursor = mActivity.getCursor();
        super.setUiData(cursor.getString(colIndexes.mDescColIndex),
                        cursor.getInt(colIndexes.mEnableColIndex) != 0 ?
                                mActivity.getCheckOn() :
                                mActivity.getCheckOff());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        menu.setHeaderTitle(R.string.profilecontextmenu_title);

        menu.add(0, R.string.insert_profile, 0, R.string.insert_profile);
        menu.add(0, R.string.insert_action, 0, R.string.insert_action);
        menu.add(0, R.string.delete, 0, R.string.delete);
        menu.add(0, R.string.rename, 0, R.string.rename);
    }

    @Override
    public void onItemSelected() {
        Cursor cursor = mActivity.getCursor();
        if (cursor == null) return;

        ColIndexes colIndexes = mActivity.getColIndexes();

        boolean enabled = cursor.getInt(colIndexes.mEnableColIndex) != 0;
        enabled = !enabled;

        ProfilesDB profDb = mActivity.getProfilesDb();
        profDb.updateProfile(
                cursor.getLong(colIndexes.mProfIdColIndex),
                null, // name
                enabled);

        // update ui
        cursor.requery();
        setUiData(null,
                  enabled ? mActivity.getCheckOn() : mActivity.getCheckOff());
    }

    @Override
    public void onContextMenuSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.string.insert_profile:
            if (DEBUG) Log.d(TAG, "profile - insert_profile");
            insertNewProfile(mActivity.getCursor());
            break;
        case R.string.insert_action:
            if (DEBUG) Log.d(TAG, "profile - insert_action");
            insertNewAction(mActivity.getCursor());
            break;
        case R.string.delete:
            if (DEBUG) Log.d(TAG, "profile - delete");
            deleteProfile(mActivity.getCursor());
            break;
        case R.string.rename:
            if (DEBUG) Log.d(TAG, "profile - rename");
            editProfile(mActivity.getCursor());
            break;
        default:
            break;
        }
    }

}
