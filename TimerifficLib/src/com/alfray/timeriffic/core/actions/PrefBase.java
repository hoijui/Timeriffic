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

package com.alfray.timeriffic.core.actions;

import com.alfray.timeriffic.R;

import android.app.Activity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;

//-----------------------------------------------

public abstract class PrefBase {

    public static final String TAG = PrefBase.class.getSimpleName();

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

    // ---

    private final static int[] sExtraButtonsResId = {
            R.id.button0,
            R.id.button1,
            R.id.button2,
            R.id.button3,
    };

    public Button findButtonById(int buttonResId) {
        if (buttonResId == -1) {
            // This is a dynamically allocated button.
            // Use the first one that is free.

            for (int id : sExtraButtonsResId) {
                Button b = (Button) getActivity().findViewById(id);
                if (b != null && b.getTag() == null) {
                    b.setTag(this);
                    b.setEnabled(true);
                    b.setVisibility(View.VISIBLE);
                    ViewParent p = b.getParent();
                    if (p instanceof View) {
                        ((View) p).setVisibility(View.VISIBLE);
                    }
                    return b;
                }
            }

            Log.e(TAG, "No free button slot for " + this.getClass().getSimpleName());
            throw new RuntimeException("No free button slot for " + this.getClass().getSimpleName());
        }

        return (Button) getActivity().findViewById(buttonResId);
    }
}


