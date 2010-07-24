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

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.alfray.timeriffic.R;
import com.alfray.timeriffic.profiles.ProfilesUI.ColIndexes;


/**
 * A custom {@link CursorAdapter} that can provide the two views we
 * need: the profile header and the timed action entry.
 * <p/>
 * For each new view, the tag is set to either {@link ProfileHeaderHolder}
 * or {@link TimedActionHolder}, a subclass of {@link BaseHolder}.
 * <p/>
 * When a view is reused, it's tag is reused with a new cursor by using
 * {@link BaseHolder#setUiData(Cursor)}. This also updates the view
 * with the data from the cursor.
 * <p/>
 * When a view is recycled/reclaimed, it's tag is cleared by the
 * {@link ProfileRecyclerListener}.
 */
class ProfileCursorAdapter extends CursorAdapter {

    /** View type is a profile header. */
    private final static int TYPE_PROFILE = 0;
    /** View type is a timed action item. */
    private final static int TYPE_TIMED_ACTION = 1;

    private final LayoutInflater mLayoutInflater;
    private final ColIndexes mColIndexes;
    private final ProfilesUI mActivity;

    /**
     * Creates a new {@link ProfileCursorAdapter} for that cursor
     * and context.
     */
    public ProfileCursorAdapter(ProfilesUI activity,
            ColIndexes colIndexes,
            LayoutInflater layoutInflater) {
        super(activity, activity.getCursor());
        mActivity = activity;
        mColIndexes = colIndexes;
        mLayoutInflater = layoutInflater;
    }

    /**
     * All items are always enabled in this view.
     */
    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    /**
     * All items are always enabled in this view.
     */
    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    /**
     * This adapter can serve 2 view types.
     */
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * View types served are either {@link #TYPE_PROFILE} or
     * {@link #TYPE_TIMED_ACTION}. This is based on the value of
     * {@link Columns#TYPE} in the cursor.
     */
    @Override
    public int getItemViewType(int position) {
        Cursor c = (Cursor) getItem(position);
        int type = c.getInt(mColIndexes.mTypeColIndex);
        if (type == Columns.TYPE_IS_PROFILE)
            return TYPE_PROFILE;
        if (type == Columns.TYPE_IS_TIMED_ACTION)
            return TYPE_TIMED_ACTION;

        return IGNORE_ITEM_VIEW_TYPE;
    }

    // ---

    /**
     * Depending on the value of {@link Columns#TYPE} in the cursor,
     * this inflates either a profile_header or a timed_action resource.
     * <p/>
     * It then associates the tag with a new {@link ProfileHeaderHolder}
     * or {@link TimedActionHolder} and initializes the holder using
     * {@link BaseHolder#setUiData(Cursor)}.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View v = null;
        BaseHolder h = null;

        int type = cursor.getInt(mColIndexes.mTypeColIndex);
        if (type == Columns.TYPE_IS_PROFILE) {
            v = mLayoutInflater.inflate(R.layout.profile_header, null);
            h = new ProfileHeaderHolder(mActivity, v);
        } else if (type == Columns.TYPE_IS_TIMED_ACTION) {
            v = mLayoutInflater.inflate(R.layout.timed_action, null);
            h = new TimedActionHolder(mActivity, v);
        }
        if (v != null) {
            v.setTag(h);
            h.setUiData();
        }
        return v;
    }

    /**
     * To recycle a view, we just re-associate its tag using
     * {@link BaseHolder#setUiData(Cursor)}.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        int type = cursor.getInt(mColIndexes.mTypeColIndex);
        if (type == Columns.TYPE_IS_PROFILE ||
                type == Columns.TYPE_IS_TIMED_ACTION) {
            BaseHolder h = (BaseHolder) view.getTag();
            h.setUiData();
        }
    }
}

