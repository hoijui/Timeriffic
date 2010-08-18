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

package com.alfray.timeriffic.storage;

import java.util.ArrayList;
import java.util.List;

import com.alfray.timeriffic.annotations.Null;
import com.alfray.timeriffic.serial.SerialReader;
import com.alfray.timeriffic.serial.SerialWriter;

//-----------------------------------------------

/**
 * A profile is a list of actions grouped together.
 * <p/>
 * <em>Important</em>: Serializing a profile <b>does not</b> serialize
 * its actions! It's up to the {@link Storage} class to save and restore
 * the actions of the a given profile. This is done for convenience and
 * to let the {@link Storage} class decide on the optimum file format.
 */
public class Profile extends Line {

    private String mTitle;
    private boolean mEnabled;
    private final List<Action> mActions = new ArrayList<Action>();

    public Profile(@Null String data) {
        super(data);
        if (data == null) {
            mTitle = "";
            mEnabled = true;
        }
    }

    public Profile(String title, boolean isEnabled,
            @Null List<Action> actions) {
        super(null);
        mTitle = title;
        mEnabled = isEnabled;
        if (actions != null) mActions.addAll(actions);
    }

    public String getTitle() {
        return mTitle;
    }

    public Profile setTitle(String title) {
        mTitle = title;
        return this;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public Profile setEnabled(boolean enabled) {
        mEnabled = enabled;
        return this;
    }

    public List<Action> getActions() {
        return mActions;
    }

    public Profile addAction(String data) {
        Action a = new Action(data);
        mActions.add(a);
        return this;
    }

    @Override
    protected void decode(String data) {
        SerialReader sr = new SerialReader(data);
        mEnabled = sr.getBool("enable");
        mTitle = sr.getString("title");
    }

    @Override
    protected String encode() {
        SerialWriter sw = new SerialWriter();
        sw.addBool("enable", mEnabled);
        sw.addString("title", mTitle);
        return sw.encodeAsString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Profile)) return false;
        Profile rhs = (Profile) obj;
        if (mEnabled != rhs.mEnabled) return false;
        if (!mActions.equals(rhs.mActions)) return false;
        return mTitle == rhs.mTitle || (mTitle != null && mTitle.equals(rhs.mTitle));
    }

    @Override
    public int hashCode() {
        long h = mEnabled ? 1 : 0;
        for (Action a : mActions) {
            h = 31*h +a.hashCode();
        }
        if (mTitle != null) h = 31*h + mTitle.hashCode();

        int i = (int)(h & 0x0FFFFFFFF) ^ (int)((h >> 32) & 0x0FFFFFFFF);
        return i;
    }
}


