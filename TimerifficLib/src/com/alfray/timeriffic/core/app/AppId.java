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

package com.alfray.timeriffic.core.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.alfray.timeriffic.core.prefs.PrefsStorage;


public class AppId {

    private static final int ID_LEN = 8;
    private static final String TAG = AppId.class.getSimpleName();

    /**
     * Returns an id specific to this device or instance of the app.
     * The id is based on an actual device id (e.g. IMEI hashed into a partial
     * SHA1) if we can get it or a random code.
     */
    public static String getIssueId(Context context, PrefsStorage storage) {
        String id = storage.getString("issue_id", null);

        if (id == null) {
            // Let's see if we can get a real device id. It should be
            // typically 14 or more chars long.
            id = getDeviceId(context);
            boolean sane = (id != null && id.length() >= 14);
            if (sane) {
                byte[] id_bytes = id.getBytes();
                // it's not a real id if all the bytes are the same
                sane = false;
                for (int i = 1; i < id_bytes.length; i++) {
                    if (id_bytes[i] != id_bytes[0]) {
                        sane = true;
                        break;
                    }
                }
                if (sane) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        md.update(id_bytes);
                        byte[] result = md.digest();

                        // Encode up to 8 characters using the 34-symbol alphabet
                        // as defined below. The SHA-1 digest is 160 bits, so
                        // the result has 40 bytes. We'll use the 8 first bytes
                        // modulo 34.
                        char c[] = new char[ID_LEN];

                        // Mark O and I (the letters) as used, to avoid using them.
                        int index_i = 10 + 'I' - 'A';
                        int index_o = 10 + 'O' - 'A';

                        for (int i = 0; i < c.length; i++) {
                            int j = (result[i] + 128) % 34;
                            if (j >= index_i) j++;
                            if (j >= index_o) j++;

                            if (j < 10) {
                                c[i] = (char) ('0' + j);
                            } else {
                                c[i] = (char) ('A' + j - 10);
                            }
                        }
                        id = new String(c);

                    } catch (NoSuchAlgorithmException e) {
                        Log.w(TAG, e);
                        sane = false;
                    }
                }
            }
            if (!sane) id = null;
        }

        if (id == null) {
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

    /** Returns the real device id from the telephony service, or null. */
    private static String getDeviceId(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                return tm.getDeviceId();
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        return null;
    }
}
