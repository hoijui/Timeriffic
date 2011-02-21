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

package com.alfray.timeriffic.core.profiles1;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.core.actions.TimedActionUtils;
import com.alfray.timeriffic.ui.EditActionUI;
import com.alfray.timeriffic.ui.EditProfileUI;
import com.alfray.timeriffic.ui.ProfilesUI1;
import com.alfray.timeriffic.ui.ProfilesUI1.ColIndexes;

/**
 * A base holder class that keeps tracks of the current cursor
 * and the common widgets of the two derived holders.
 */
public abstract class BaseHolder {

    public static final String TAG = BaseHolder.class.getSimpleName();

    /**
     * The text view that holds the title or description as well
     * as the "check box".
     */
    private final TextView mDescription;

    protected final ProfilesUI1 mActivity;

    public BaseHolder(ProfilesUI1 activity, View view) {
        mActivity = activity;
        mDescription = view != null ? (TextView) view.findViewById(R.id.description) : null;
    }

    protected void setUiData(String description, Drawable state) {
        if (description != null) mDescription.setText(description);
        if (state != null) mDescription.setCompoundDrawablesWithIntrinsicBounds(
                state /*left*/, null /*top*/, null /*right*/, null /*bottom*/);
    }

    public abstract void setUiData();
    public abstract void onItemSelected();
    public abstract void onCreateContextMenu(ContextMenu menu);
    public abstract void onContextMenuSelected(MenuItem item);


    // --- profile actions ---

    private void startEditActivity(Class<?> activity, String extra_id, long extra_value) {
        Intent intent = new Intent(mActivity, activity);
        intent.putExtra(extra_id, extra_value);

        mActivity.startActivityForResult(intent, ProfilesUI1.DATA_CHANGED);
    }

    protected void deleteProfile(Cursor cursor) {
        ColIndexes colIndexes = mActivity.getColIndexes();
        final long row_id = cursor.getLong(colIndexes.mIdColIndex);
        String title = cursor.getString(colIndexes.mDescColIndex);

        mActivity.showTempDialog(row_id, title, ProfilesUI1.DIALOG_DELETE_PROFILE);
    }

    protected void insertNewProfile(Cursor beforeCursor) {
        long prof_index = 0;
        if (beforeCursor != null) {
            ColIndexes colIndexes = mActivity.getColIndexes();
            prof_index = beforeCursor.getLong(colIndexes.mProfIdColIndex) >> Columns.PROFILE_SHIFT;
        }

        ProfilesDB profDb = mActivity.getProfilesDb();
        prof_index = profDb.insertProfile(prof_index,
                        mActivity.getString(R.string.insertprofile_new_profile_title),
                        true /*isEnabled*/);

        startEditActivity(EditProfileUI.class,
                EditProfileUI.EXTRA_PROFILE_ID, prof_index << Columns.PROFILE_SHIFT);
    }

    protected void editProfile(Cursor cursor) {
        ColIndexes colIndexes = mActivity.getColIndexes();
        long prof_id = cursor.getLong(colIndexes.mProfIdColIndex);

        startEditActivity(EditProfileUI.class, EditProfileUI.EXTRA_PROFILE_ID, prof_id);
    }

    // --- timed actions ----


    protected void deleteTimedAction(Cursor cursor) {
        ColIndexes colIndexes = mActivity.getColIndexes();
        final long row_id = cursor.getLong(colIndexes.mIdColIndex);
        String description = cursor.getString(colIndexes.mDescColIndex);

        mActivity.showTempDialog(row_id, description, ProfilesUI1.DIALOG_DELETE_ACTION);
    }

    protected void insertNewAction(Cursor beforeCursor) {
        long prof_index = 0;
        long action_index = 0;
        if (beforeCursor != null) {
            ColIndexes colIndexes = mActivity.getColIndexes();
            prof_index = beforeCursor.getLong(colIndexes.mProfIdColIndex);
            action_index = prof_index & Columns.ACTION_MASK;
            prof_index = prof_index >> Columns.PROFILE_SHIFT;
        }

        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(System.currentTimeMillis());
        int hourMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);

        int day = TimedActionUtils.calendarDayToActionDay(c);

        ProfilesDB profDb = mActivity.getProfilesDb();
        action_index = profDb.insertTimedAction(
                prof_index,
                action_index,
                hourMin,    // hourMin
                day,        // days
                "",         // actions
                0           // nextMs
                );

        long action_id = (prof_index << Columns.PROFILE_SHIFT) + action_index;

        startEditActivity(EditActionUI.class, EditActionUI.EXTRA_ACTION_ID, action_id);
    }

    protected void editAction(Cursor cursor) {
        try {
            ColIndexes colIndexes = mActivity.getColIndexes();
            long action_id = cursor.getLong(colIndexes.mProfIdColIndex);

            startEditActivity(EditActionUI.class, EditActionUI.EXTRA_ACTION_ID, action_id);
        } catch (Throwable t) {
            Log.e(TAG, "editAction", t);
        }
    }

}
