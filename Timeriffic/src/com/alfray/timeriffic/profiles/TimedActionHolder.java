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
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.profiles.ProfilesUI.ColIndexes;

/**
 * The holder for a timed action row.
 */
public class TimedActionHolder extends BaseHolder {

    private static boolean DEBUG = false;
    public static String TAG = "TFC-TAHolder";

    public TimedActionHolder(ProfilesUI activity, View view) {
        super(activity, view);
    }

    @Override
    public void setUiData() {
        ColIndexes colIndexes = mActivity.getColIndexes();
        Cursor cursor = mActivity.getCursor();
        super.setUiData(cursor.getString(colIndexes.mDescColIndex),
                        getDotColor(cursor.getInt(colIndexes.mEnableColIndex)));
    }

    private Drawable getDotColor(int actionMark) {
        switch (actionMark) {
        case Columns.ACTION_MARK_PREV:
            return mActivity.getGreenDot();
        case Columns.ACTION_MARK_NEXT:
            return mActivity.getPurpleDot();
        default:
            return mActivity.getGrayDot();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        menu.setHeaderTitle(R.string.timedactioncontextmenu_title);

        menu.add(0, R.string.insert_action, 0, R.string.insert_action);
        menu.add(0, R.string.delete, 0, R.string.delete);
        menu.add(0, R.string.edit, 0, R.string.edit);
    }

    @Override
    public void onItemSelected() {
        // trigger edit
        if (DEBUG) Log.d(TAG, "action - edit");
        editAction(mActivity.getCursor());
    }

    @Override
    public void onContextMenuSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.string.insert_action:
            if (DEBUG) Log.d(TAG, "action - insert_action");
            insertNewAction(mActivity.getCursor());
            break;
        case R.string.delete:
            if (DEBUG) Log.d(TAG, "action - delete");
            deleteTimedAction(mActivity.getCursor());
            break;
        case R.string.edit:
            if (DEBUG) Log.d(TAG, "action - edit");
            editAction(mActivity.getCursor());
            break;
        default:
            break;
        }
    }
}
