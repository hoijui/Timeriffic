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

package com.rdrrlabs.timeriffic.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public interface IActivityDelegate<T> {

    public void onCreate(Bundle savedInstanceState);
    public void onResume();
    public void onPause();
    public void onStop();
    public void onRestoreInstanceState(Bundle savedInstanceState);
    public void onSaveInstanceState(Bundle outState);
    public void onDestroy();
    public void onActivityResult(int requestCode, int resultCode, Intent data);
    public Dialog onCreateDialog(int id);
    public boolean onContextItemSelected(MenuItem item);
    public void onCreateOptionsMenu(Menu menu);
    public boolean onOptionsItemSelected(MenuItem item);
}
