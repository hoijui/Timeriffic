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

import com.alfray.timeriffic.serial.SerialReader;
import com.alfray.timeriffic.serial.SerialWriter;

//-----------------------------------------------

public class Action extends Line {

    private int mHourMin;
    private int mDays;
    private String mActions;

    public Action(String data) {
        super(data);
        if (data == null) {
            mActions = "";
        }
    }

    public int getHourMin() {
        return mHourMin;
    }

    public void setHourMin(int hourMin) {
        if (hourMin != mHourMin) {
            markDirty();
            mHourMin = hourMin;
        }
    }

    public int getDays() {
        return mDays;
    }

    public void setDays(int days) {
        if (days != mDays) {
            markDirty();
            mDays = days;
        }
    }

    public String getActions() {
        return mActions;
    }

    public void setActions(String actions) {
        if (mActions == null || !mActions.equals(actions)) {
            markDirty();
            mActions = actions;
        }
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

}


