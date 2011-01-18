/*
 * Project:  Timeriffic
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

package com.alfray.timeriffic.serial;

import android.util.SparseArray;

/**
 * Encode keys used by {@link SerialWriter}.
 * <p/>
 * Keys are transformed in a unique integer.
 * In case of collision, {@link DuplicateKey} is thrown.
 */
public class SerialKey {

    public static class DuplicateKey extends RuntimeException {
        private static final long serialVersionUID = -1735763023714511003L;
        public DuplicateKey(String message) { super(message); }
    }

    private SparseArray<String> mUsedKeys = null;

    /**
     * Encode a key name into an integer.
     * <p/>
     * There's a risk of collision when adding keys: different names can map to
     * the same encoded integer. If this is the case and the key is different
     * then a {@link DuplicateKey} is thrown. It is ok to register twice the
     * same key name, which will result in the same value being returned.
     * 
     * @param name The name of the key. Must not be empty nor null.
     * @return The integer associated with that key name.
     * @throws DuplicateKey if the key collides with a different one.
     */
    public int encodeNewKey(String name) {
        if (mUsedKeys == null) mUsedKeys = new SparseArray<String>();

        int key = encodeKey(name);
        int index = mUsedKeys.indexOfKey(key);
        
        if (index >= 0) {
            String previous = mUsedKeys.valueAt(index);
            if (!name.equals(previous)) {
                throw new DuplicateKey(
                    String.format("Key name collision: '%1$s' has the same hash than previously used '%2$s'",
                            name, previous));
            }
        } else {
            mUsedKeys.put(key, name);
        }

        return key;
    }

    /**
     * Encode a key name into an integer and makes sure the key name is unique
     * and has never be registered before.
     * 
     * @param name The name of the key. Must not be empty nor null.
     * @return The integer associated with that key name.
     * @throws DuplicateKey if the key had already been registered.
     */
    public int encodeUniqueKey(String name) {
        if (mUsedKeys == null) mUsedKeys = new SparseArray<String>();

        int key = encodeKey(name);
        int index = mUsedKeys.indexOfKey(key);
        
        if (index >= 0) {
            String previous = mUsedKeys.valueAt(index);
            throw new DuplicateKey(
                String.format("Key name collision: '%1$s' has the same hash than previously used '%2$s'",
                        name, previous));
        }

        mUsedKeys.put(key, name);
        return key;
    }

    /**
     * Encode a key name into an integer.
     * <p/>
     * Unlike {@link #encodeNewKey(String)}, this does not check whether the key has already
     * been used.
     * 
     * @param name The name of the key. Must not be empty nor null.
     * @return The integer associated with that key name.
     */
    public int encodeKey(String name) {
        int key = name.hashCode();
        return key;
    }
}
