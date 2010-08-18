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

import com.alfray.timeriffic.annotations.Null;
import com.alfray.timeriffic.serial.SerialReader;
import com.alfray.timeriffic.serial.SerialWriter;

//-----------------------------------------------

public class Action extends Line {

    private int mHourMin;
    private int mDays;
    private String mActions;

    public Action(@Null String data) {
        super(data);
        if (data == null) {
            mActions = "";
        }
    }

    public Action(String actions, int hourMin, int days) {
        super(null);
        mActions = actions;
        mHourMin = hourMin;
        mDays = days;
    }

    public int getHourMin() {
        return mHourMin;
    }

    public Action setHourMin(int hourMin) {
        if (hourMin != mHourMin) {
            markDirty();
            mHourMin = hourMin;
        }
        return this;
    }

    public int getDays() {
        return mDays;
    }

    public Action setDays(int days) {
        if (days != mDays) {
            markDirty();
            mDays = days;
        }
        return this;
    }

    public String getActions() {
        return mActions;
    }

    public Action setActions(String actions) {
        if (mActions == null || !mActions.equals(actions)) {
            markDirty();
            mActions = actions;
        }
        return this;
    }

    @Override
    protected void decode(String data) {
        SerialReader sr = new SerialReader(data);
        mHourMin = sr.getInt("hourMin");
        mDays = sr.getInt("days");
        mActions = sr.getString("actions");
    }

    @Override
    protected String encode() {
        SerialWriter sw = new SerialWriter();
        sw.addInt("hourMin", mHourMin);
        sw.addInt("days", mDays);
        sw.addString("actions", mActions);
        return sw.encodeAsString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Action)) return false;
        Action rhs = (Action) obj;
        if (mDays != rhs.mDays) return false;
        if (mHourMin != rhs.mHourMin) return false;
        return (mActions == rhs.mActions || (mActions != null && mActions.equals(rhs.mActions)));
    }

    @Override
    public int hashCode() {
        long h = mDays + 31 * mHourMin;
        if (mActions != null) h = 31*h + mActions.hashCode();
        int i = (int)(h & 0x0FFFFFFFF) ^ (int)((h >> 32) & 0x0FFFFFFFF);
        return i;
    }

}


