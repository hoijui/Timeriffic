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

package com.alfray.timeriffic.actions;

import com.alfray.timeriffic.R;

import android.app.Activity;
import android.view.ContextMenu;
import android.view.MenuItem;

//-----------------------------------------------

public abstract class PrefBase {

    private final Activity mActivity;

    protected final static int ID_DOT_UNCHANGED = R.drawable.dot_gray;
    protected final static int ID_DOT_STATE_ON  = R.drawable.dot_green;
    protected final static int ID_DOT_STATE_OFF = R.drawable.dot_red;
    protected final static int ID_DOT_PERCENT   = R.drawable.dot_purple;
    protected final static int ID_DOT_EXTRA     = R.drawable.dot_purple;

    public PrefBase(Activity activity) {
        mActivity = activity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    protected String getActionValue(String[] actions, char prefix) {
        if (actions == null) return null;

        for (String action : actions) {
            if (action.length() > 1 && action.charAt(0) == prefix) {
                return action.substring(1);
            }
        }

        return null;
    }

    protected void appendAction(StringBuilder actions, char prefix, String value) {
        if (actions.length() > 0) actions.append(",");
        actions.append(prefix);
        actions.append(value);
    }

    public abstract void setEnabled(boolean enable, String disabledMessage);

    public abstract boolean isEnabled();

    public abstract void requestFocus();

    public abstract void onCreateContextMenu(ContextMenu menu);

    public abstract void onContextItemSelected(MenuItem item);
}


