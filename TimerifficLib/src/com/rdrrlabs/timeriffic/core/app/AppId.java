/*
 * Project: Timeriffic
 * Copyright (C) 2008 ralfoide gmail com,
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

package com.rdrrlabs.timeriffic.core.app;

import java.util.Random;

import android.content.Context;

import com.rdrrlabs.timeriffic.core.prefs.PrefsStorage;


public class AppId {

    private static final int ID_LEN = 8;
    public static final String TAG = AppId.class.getSimpleName();

    /**
     * Returns an id specific to this instance of the app.
     * The id is based on a random code generated once and stored in prefs.
     */
    public static String getIssueId(Context context, PrefsStorage storage) {
        storage.endReadAsync();
        String id = storage.getString("issue_id", null);

        if (id == null) {
            // Generate a new installation-specific ID.

            // We not longer use the device id from telephony. First
            // because it's not relevant for non-telephone devices and
            // second because there's just too many issues with it that
            // it's not even funny. See this blog post for the details:
            // http://android-developers.blogspot.com/2011/03/identifying-app-installations.html


            // Generate a random code with 8 unique symbols out of 34
            // (0-9 + A-Z). We avoid letter O and I which look like 0 and 1.
            // We avoid repeating the same symbol twice in a row so
            // the number of combinations is n*(n-1)*(n-1)*..*(n-1)
            // or c = n * (n-1)^(k-1)
            // k=6, n=34 => c=    1,330,603,362 ... 1 million is a bit low.
            // k=8, n=34 => c=1,449,027,061,218 ... 1 trillion will do it.

            Random r = new Random();
            char c[] = new char[ID_LEN];

            // Mark O and I (the letters) as used, to avoid using them.
            int index_i = 10 + 'I' - 'A';
            int index_o = 10 + 'O' - 'A';

            for (int i = 0; i < c.length; i++) {
                int j = r.nextInt(10+26-2);
                if (j >= index_i) j++;
                if (j >= index_o) j++;

                if (j < 10) {
                    c[i] = (char) ('0' + j);
                } else {
                    c[i] = (char) ('A' + j - 10);
                }
            }
            id = new String(c);
        }

        if (id != null) {
            storage.putString("issue_id", id);
            storage.flushSync(context.getApplicationContext());
        }

        return id;
    }
}
