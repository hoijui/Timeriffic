/*
 * Project: NerdkillAndroid
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

import java.util.Iterator;

import android.util.SparseArray;


/**
 * Encoder/decoder for typed data.
 * Extracted from NerdkillAndroid.
 *
 * See {@link SerialWriter} for implementations details.
 */
public class SerialReader implements Iterable<SerialReader.Entry> {

    public class Entry {
        private final int mKey;
        private final Object mValue;

        public Entry(int key, Object value) {
            mKey = key;
            mValue = value;
        }
        
        public int getKey() {
            return mKey;
        }
        
        public Object getValue() {
            return mValue;
        }
    }

    public static final String TAG = SerialReader.class.getSimpleName();

    public static class DecodeError extends RuntimeException {
        private static final long serialVersionUID = -8603565615795418588L;
        public DecodeError(String message) { super(message); }
    }

    static final int TYPE_INT = SerialWriter.TYPE_INT;
    static final int TYPE_LONG = SerialWriter.TYPE_LONG;
    static final int TYPE_BOOL = SerialWriter.TYPE_BOOL;
    static final int TYPE_FLOAT = SerialWriter.TYPE_FLOAT;
    static final int TYPE_DOUBLE = SerialWriter.TYPE_DOUBLE;
    static final int TYPE_STRING = SerialWriter.TYPE_STRING;
    static final int TYPE_SERIAL = SerialWriter.TYPE_SERIAL;
    static final int EOF = SerialWriter.EOF;

    private final SparseArray<Object> mData = new SparseArray<Object>();
    private final SerialKey mKeyer = new SerialKey();

    public SerialReader(String data) {
        decodeString(data);
    }

    public SerialReader(int[] data, int offset, int length) {
        decodeArray(data, offset, length);
    }

    public SerialReader(int[] data) {
        this(data, 0, data.length);
    }

    /**
     * A shortcut around {@link SerialReader#SerialReader(int[])} useful for unit tests.
     */
    public SerialReader(SerialWriter sw) {
        this(sw.encodeAsArray());
    }

    public int size() {
        return mData.size();
    }

    public boolean hasName(String name) {
        int id = mKeyer.encodeKey(name);
        return mData.indexOfKey(id) >= 0;
    }

    public int getInt(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return 0;
        if (d instanceof Integer) return ((Integer) d).intValue();
        throw new ClassCastException("Int expected, got " + d.getClass().getSimpleName());
    }

    public long getLong(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return 0;
        if (d instanceof Long) return ((Long) d).longValue();
        throw new ClassCastException("Long expected, got " + d.getClass().getSimpleName());
    }

    public boolean getBool(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return false;
        if (d instanceof Boolean) return ((Boolean) d).booleanValue();
        throw new ClassCastException("Bool expected, got " + d.getClass().getSimpleName());
    }

    public float getFloat(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return 0;
        if (d instanceof Float) return ((Float) d).floatValue();
        throw new ClassCastException("Float expected, got " + d.getClass().getSimpleName());
    }

    public double getDouble(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return 0;
        if (d instanceof Double) return ((Double) d).doubleValue();
        throw new ClassCastException("Double expected, got " + d.getClass().getSimpleName());
    }

    public String getString(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return null;
        if (d instanceof String) return (String) d;
        throw new ClassCastException("String expected, got " + d.getClass().getSimpleName());
    }

    public SerialReader getSerial(String name) {
        int id = mKeyer.encodeKey(name);
        Object d = mData.get(id);
        if (d == null) return null;
        if (d instanceof SerialReader) return (SerialReader) d;
        throw new ClassCastException("SerialReader expected, got " + d.getClass().getSimpleName());
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
            final int n = mData.size();
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < n;
            }

            @Override
            public Entry next() {
                Entry e = new Entry(mData.keyAt(index), mData.valueAt(index));
                index++;
                return e;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException(
                        "Remove is not supported by " + 
                        SerialReader.class.getSimpleName());
            }
        };
    }

    //---

    private static class IntArray {
        public int[] a = new int[16];;
        public int n = 0;

        public void add(long i) {
            if (n == a.length) {
                int[] newArray = new int[2 * a.length];
                System.arraycopy(a, 0, newArray, 0, n);
                a = newArray;
            }

            int j = (int) (i & 0x0FFFFFFFFL);
            a[n++] = j;
        }
    }

    private void decodeString(String data) {
        IntArray a = new IntArray();

        if (data == null || data.length() == 0) {
            throw new DecodeError("No data to decode.");
        }

        // Read a bunch of hexa numbers separated by non-hex chars
        char[] cs = data.toCharArray();
        int cn = cs.length;

        int size = 0;
        long i = -1;
        for (int k = 0; k < cn; k++) {
            char c = cs[k];
            if (c >= '0' && c <= '9') {
                if (i == -1) i = 0;
                i = (i << 4) + c - '0';

            } else if (c >= 'a' && c <= 'f') {
                if (i == -1) i = 0;
                i = (i << 4) + c - 'a' + 0x0A;

            } else if (c >= 'A' && c <= 'F') {
                if (i == -1) i = 0;
                i = (i << 4) + c - 'A' + 0x0A;

            } else if (i >= 0) {
                a.add(i);
                i = -1;

                // The first int indicates how many ints we needed to read.
                // There's no need to try to read any more than that, we wouldn't
                // use it anyway.
                if (a.n == 1) {
                    size = a.a[0];
                } else if (a.n >= size) {
                    break;
                }
            }
        }
        if (i >= 0) a.add(i);

        decodeArray(a.a, 0, a.n);
    }

    private void decodeArray(int[] data, int offset, int length) {
        int size = data.length > offset ? data[offset] : 0;
        int end = size + offset;

        // An empty message has at least 3 ints.
        if (size < 3 || end > data.length || offset + length > data.length) {
            throw new DecodeError("Message too short. Not enough data found.");
        }

        if (data[end - 1] != EOF) {
            throw new DecodeError("Missing EOF.");
        }

        // Compute the CRC without the 2-int footer
        int crc = SerialWriter.computeCrc(data, offset, size - 2);
        if (data[end - 2] != crc) {
            throw new DecodeError("Invalid CRC.");
        }

        // Nothing to do if this is an empty message.
        if (size == 3) return;

        // disregard the ending CRC and EOF now
        end -= 2;

        // skip the size int
        offset++;

        while (offset < end) {
            // The smallest type needs 3 ints: type, id, 1 int
            if (offset + 3 > end) {
                throw new DecodeError("Not enough data to decode short primitive.");
            }
            int type = data[offset++];
            int id = data[offset++];
            int i1, i2;

            switch(type) {
            case TYPE_INT:
                i1 = data[offset++];
                mData.put(id, new Integer(i1));
                break;

            case TYPE_BOOL:
                i1 = data[offset++];
                boolean b = i1 != 0;
                mData.put(id, new Boolean(b));
                break;

            case TYPE_FLOAT:
                i1 = data[offset++];
                float f = Float.intBitsToFloat(i1);
                mData.put(id, new Float(f));
                break;

            case TYPE_LONG:
            case TYPE_DOUBLE:
                if (offset + 2 > end) throw new DecodeError("Not enough data to decode long primitive.");
                i1 = data[offset++];
                i2 = data[offset++];

                long L = ((long)i1) << 32;
                long L2 = (long)i2;
                L |= L2 & 0x0FFFFFFFFL;

                if (type == TYPE_LONG) {
                    mData.put(id, new Long(L));
                } else {
                    double d = Double.longBitsToDouble(L);
                    mData.put(id, new Double(d));
                }
                break;

            case TYPE_STRING:
                int char_size = data[offset++];
                int m = (char_size + 1) / 2;
                if (offset + m > end) throw new DecodeError("Not enough data to decode string type.");

                char[] cs = new char[char_size];

                for (int i = 0; i < char_size; ) {
                    i1 = data[offset++];

                    char c = (char) ((i1 >> 16) & 0x0FFFF);
                    cs[i++] = c;

                    if (i < char_size) {
                        c = (char) (i1 & 0x0FFFF);
                        cs[i++] = c;
                    }
                }

                String s = new String(cs);
                mData.put(id, s);

                break;

            case TYPE_SERIAL:
                // The next int must be the header of the serial data, which conveniently
                // starts with its own size.
                int sub_size = data[offset];

                if (offset + sub_size > end) {
                    throw new DecodeError("Not enough data to decode sub-serial type.");
                } else if (data[offset + sub_size - 1] != EOF) {
                    throw new DecodeError("Missing EOF in sub-serial type.");
                }

                SerialReader sr = new SerialReader(data, offset, sub_size);
                mData.put(id, sr);
                offset += sub_size;
                break;
            }
        }
    }

}
