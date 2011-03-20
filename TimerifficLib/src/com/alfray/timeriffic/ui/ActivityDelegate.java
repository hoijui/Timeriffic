/*
 * Project: Timeriffic
 * Copyright (C) 2011 ralfoide gmail com,
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

package com.alfray.timeriffic.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity delegate.
 */
public abstract class ActivityDelegate<T extends IActivityDelegate<?>> extends Activity {

    private T mDelegate;

    abstract public T createDelegate();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDelegate = createDelegate();
        mDelegate.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDelegate.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDelegate.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mDelegate.onStop();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDelegate.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDelegate.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mDelegate.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        return mDelegate.onCreateDialog(id);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean b = mDelegate.onContextItemSelected(item);
        if (!b) b = super.onContextItemSelected(item);
        return b;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mDelegate.onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean b = mDelegate.onOptionsItemSelected(item);
        if (!b) b = super.onOptionsItemSelected(item);
        return b;
    }
}
