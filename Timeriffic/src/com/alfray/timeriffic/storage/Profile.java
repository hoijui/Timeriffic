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

import com.alfray.timeriffic.serial.SerialReader;
import com.alfray.timeriffic.serial.SerialWriter;

//-----------------------------------------------

public class Profile extends Line {

    private String mTitle;
    private boolean mEnabled;
    private final List<Action> mActions = new ArrayList<Action>();

    public Profile(String data) {
        super(data);
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public List<Action> getActions() {
        return mActions;
    }

    public void addAction(String data) {
        Action a = new Action(data);
        mActions.add(a);
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
}


