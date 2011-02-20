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

import com.alfray.timeriffic.actions.PrefPercent;
import com.alfray.timeriffic.actions.PrefToggle;

import android.app.Activity;
import android.content.Context;

//-----------------------------------------------

public interface ISetting {

    /**
     * Return true if the setting is supported.
     * <p/>
     * Implementations may want to cache the value, depending on how
     * expensive it is to evaluate, knowing that this setting will
     * be long lived.
     */
    boolean isSupported(Context context);

    /**
     * Create the UI to edit the setting.
     * The UI object is generally something like {@link PrefPercent}
     * or {@link PrefToggle}.
     */
    Object createUi(Activity activity, String[] currentActions);

    /**
     * Collects the actions to perform based on the choices made
     * by the user in the UI object from {@link #createUi(Activity, String[])}.
     * The action is a string that is happened to <code>outActions</code>.
     */
    void collectUiResults(Object settingUi, StringBuilder outActions);

    /**
     * Returns a human-readable description of the given action.
     */
    String getActionLabel(Context context, String action);

    /**
     * Performs the given action.
     * <p/>
     * Returns true if the action was (supposedly) performed.
     * <p/>
     * Must only return false when it is obvious that the action failed or
     * that it cannot/must not be carried now, in which case the caller might
     * want to try to reschedule it later.
     */
    boolean performAction(Context context, String action);
}


