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

import junit.framework.TestCase;

import com.alfray.timeriffic.serial.SerialReader.DecodeError;

/**
 * Test for {@link SerialReader}.
 * Extracted from NerdkillAndroid.
 */
public class SerialReaderTest extends TestCase {

    private SerialWriter m;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m = new SerialWriter();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNoData_Null() throws Exception {
        makeError(null);
    }

    public void testNoData_Empty() throws Exception {
        makeError("");
    }

    public void testNoData_NotHexa() throws Exception {
        makeError("@#@#$ V$#$ #@");
    }

    public void testNoData_InvalidStr1() throws Exception {
        makeError("0 0");
    }

    public void testNoData_InvalidStr2() throws Exception {
        makeError("0 0 0 0 0 0");
    }

    public void testNoData_MissingEOF() throws Exception {
        makeError("3 33F170F2");
    }

    public void testNoData_WrongEOF() throws Exception {
        makeError("3 33F170F2 1234");
    }

    public void testNoData_WrongCRC() throws Exception {
        makeError("3 1234 E0F");
    }

    public void testNoData_GoodMsg_Direct() throws Exception {
        assertNotNull(new SerialReader("3 33F170F2 E0F"));
    }

    public void testNoData_GoodWriterAsString() throws Exception {
        String s = m.encodeAsString();
        assertNotNull(s);

        SerialReader sr = new SerialReader(s);
        assertNotNull(sr);
        assertEquals(0, sr.size());
    }

    public void testNoData_GoodWriterAsArray() throws Exception {
        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(0, sr.size());
    }

    public void testGetInt() throws Exception {
        m.addInt("foo",  42);
        m.addInt("max", Integer.MAX_VALUE);
        m.addInt("min", Integer.MIN_VALUE);
        m.addInt("0",   0);
        m.addInt("-1",  -1);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(5, sr.size());

        assertTrue(sr.hasName("foo"));
        assertTrue(sr.hasName("max"));
        assertTrue(sr.hasName("min"));
        assertTrue(sr.hasName("0"));
        assertTrue(sr.hasName("-1"));

        assertEquals(0, sr.getInt("0"));
        assertEquals(-1, sr.getInt("-1"));
        assertEquals(42, sr.getInt("foo"));
        assertEquals(Integer.MAX_VALUE, sr.getInt("max"));
        assertEquals(Integer.MIN_VALUE, sr.getInt("min"));

        assertFalse(sr.hasName("unknown key"));
        assertEquals(0, sr.getInt("unknown key"));

        castException(sr, long.class, "foo");
        castException(sr, float.class, "foo");
        castException(sr, double.class, "foo");
        castException(sr, boolean.class, "foo");
        castException(sr, String.class, "foo");
        castException(sr, SerialReader.class, "foo");
    }

    public void testGetLong() throws Exception {
        m.addLong("foo",  42L);
        m.addLong("max", Long.MAX_VALUE);
        m.addLong("min", Long.MIN_VALUE);
        m.addLong("0",   0L);
        m.addLong("-1",  -1L);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(5, sr.size());

        assertTrue(sr.hasName("foo"));
        assertTrue(sr.hasName("max"));
        assertTrue(sr.hasName("min"));
        assertTrue(sr.hasName("0"));
        assertTrue(sr.hasName("-1"));

        assertEquals(0L, sr.getLong("0"));
        assertEquals(-1L, sr.getLong("-1"));
        assertEquals(42, sr.getLong("foo"));
        assertEquals(Long.MAX_VALUE, sr.getLong("max"));
        assertEquals(Long.MIN_VALUE, sr.getLong("min"));

        assertEquals(0L, sr.getLong("unknown key"));

        castException(sr, int.class, "foo");
        castException(sr, float.class, "foo");
        castException(sr, double.class, "foo");
        castException(sr, boolean.class, "foo");
        castException(sr, String.class, "foo");
        castException(sr, SerialReader.class, "foo");
    }

    public void testGetFloat() throws Exception {
        m.addFloat("foo" , 42.0f);
        m.addFloat("max" , Float.MAX_VALUE);
        m.addFloat("min" , Float.MIN_VALUE);
        m.addFloat("-inf", Float.NEGATIVE_INFINITY);
        m.addFloat("+inf", Float.POSITIVE_INFINITY);
        m.addFloat("nan" , Float.NaN);
        m.addFloat("0", 0.0f);
        m.addFloat("-1", -1.0f);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(8, sr.size());

        assertTrue(sr.hasName("foo"));
        assertTrue(sr.hasName("max"));
        assertTrue(sr.hasName("min"));
        assertTrue(sr.hasName("-inf"));
        assertTrue(sr.hasName("+inf"));
        assertTrue(sr.hasName("nan"));
        assertTrue(sr.hasName("0"));
        assertTrue(sr.hasName("-1"));

        assertEquals(42.0f, sr.getFloat("foo"));
        assertEquals(Float.MAX_VALUE, sr.getFloat("max"));
        assertEquals(Float.MIN_VALUE, sr.getFloat("min"));
        assertEquals(Float.NEGATIVE_INFINITY, sr.getFloat("-inf"));
        assertEquals(Float.POSITIVE_INFINITY, sr.getFloat("+inf"));
        assertEquals(Float.NaN, sr.getFloat("nan"));
        assertEquals(0.0f, sr.getFloat("0"));
        assertEquals(-1.0f, sr.getFloat("-1"));

        assertEquals(0.0f, sr.getFloat("unknown key"));

        castException(sr, int.class, "foo");
        castException(sr, long.class, "foo");
        castException(sr, double.class, "foo");
        castException(sr, boolean.class, "foo");
        castException(sr, String.class, "foo");
        castException(sr, SerialReader.class, "foo");
    }

    public void testGetDouble() throws Exception {
        m.addDouble("foo" , 42.0);
        m.addDouble("max" , Double.MAX_VALUE);
        m.addDouble("min" , Double.MIN_VALUE);
        m.addDouble("-inf", Double.NEGATIVE_INFINITY);
        m.addDouble("+inf", Double.POSITIVE_INFINITY);
        m.addDouble("nan" , Double.NaN);
        m.addDouble("0", 0.0);
        m.addDouble("-1", -1.0);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(8, sr.size());

        assertTrue(sr.hasName("foo"));
        assertTrue(sr.hasName("max"));
        assertTrue(sr.hasName("min"));
        assertTrue(sr.hasName("-inf"));
        assertTrue(sr.hasName("+inf"));
        assertTrue(sr.hasName("nan"));
        assertTrue(sr.hasName("0"));
        assertTrue(sr.hasName("-1"));

        assertEquals(42.0, sr.getDouble("foo"));
        assertEquals(Double.MAX_VALUE, sr.getDouble("max"));
        assertEquals(Double.MIN_VALUE, sr.getDouble("min"));
        assertEquals(Double.NEGATIVE_INFINITY, sr.getDouble("-inf"));
        assertEquals(Double.POSITIVE_INFINITY, sr.getDouble("+inf"));
        assertEquals(Double.NaN, sr.getDouble("nan"));
        assertEquals(0.0, sr.getDouble("0"));
        assertEquals(-1.0, sr.getDouble("-1"));

        assertEquals(0.0, sr.getDouble("unknown key"));

        castException(sr, int.class, "foo");
        castException(sr, long.class, "foo");
        castException(sr, float.class, "foo");
        castException(sr, boolean.class, "foo");
        castException(sr, String.class, "foo");
        castException(sr, SerialReader.class, "foo");
    }

    public void testGetBool() throws Exception {
        m.addBool("nil"  , false);
        m.addBool("t"    , true);
        m.addBool("false", Boolean.FALSE);
        m.addBool("true" , Boolean.TRUE);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(4, sr.size());

        assertTrue(sr.hasName("nil"));
        assertTrue(sr.hasName("t"));
        assertTrue(sr.hasName("false"));
        assertTrue(sr.hasName("true"));

        assertEquals(false, sr.getBool("nil"));
        assertEquals(false, sr.getBool("false"));
        assertEquals(true , sr.getBool("t"));
        assertEquals(true , sr.getBool("true"));

        assertEquals(false, sr.getBool("unknown key"));

        castException(sr, int.class, "nil");
        castException(sr, long.class, "nil");
        castException(sr, float.class, "nil");
        castException(sr, double.class, "nil");
        castException(sr, String.class, "nil");
        castException(sr, SerialReader.class, "nil");
    }

    public void testGetString() throws Exception {
        m.addString("nil"  , "nil");
        m.addString("t"    , "t");
        m.addString("false", "false");
        m.addString("true" , "true");
        m.addString(""     , "");

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(5, sr.size());

        assertTrue(sr.hasName("nil"));
        assertTrue(sr.hasName("t"));
        assertTrue(sr.hasName("false"));
        assertTrue(sr.hasName("true"));
        assertTrue(sr.hasName(""));

        assertEquals("nil"  , sr.getString("nil"));
        assertEquals("t"    , sr.getString("t"));
        assertEquals("false", sr.getString("false"));
        assertEquals("true" , sr.getString("true"));
        assertEquals(""     , sr.getString(""));

        assertNull(sr.getString("unknown key"));

        castException(sr, int.class, "nil");
        castException(sr, long.class, "nil");
        castException(sr, float.class, "nil");
        castException(sr, double.class, "nil");
        castException(sr, boolean.class, "nil");
        castException(sr, SerialReader.class, "nil");
    }

    public void testGetSerial() throws Exception {
        SerialWriter sw1 = new SerialWriter();
        sw1.addInt("sw1_42", 42);

        SerialWriter sw2 = new SerialWriter();
        sw2.addString("sw2_str", "str");
        sw2.addInt("sw2_42", 42);

        m.addSerial("s1", sw1);
        m.addSerial("s2", sw2);

        int[] a = m.encodeAsArray();
        assertNotNull(a);

        SerialReader sr = new SerialReader(a);
        assertNotNull(sr);
        assertEquals(2, sr.size());

        assertTrue(sr.hasName("s1"));
        assertTrue(sr.hasName("s2"));
        assertFalse(sr.hasName("sw1_42"));
        assertFalse(sr.hasName("sw2_42"));

        SerialReader sr1 = sr.getSerial("s1");
        SerialReader sr2 = sr.getSerial("s2");
        assertNotNull(sr1);
        assertNotNull(sr2);
        assertNull(sr.getSerial("unknown key"));

        castException(sr, int.class, "s1");
        castException(sr, long.class, "s1");
        castException(sr, float.class, "s1");
        castException(sr, double.class, "s1");
        castException(sr, boolean.class, "s1");
        castException(sr, String.class, "s1");

        assertEquals(1, sr1.size());
        assertEquals(42, sr1.getInt("sw1_42"));
        assertFalse(sr1.hasName("sw2_42"));

        assertEquals(2, sr2.size());
        assertEquals(42, sr2.getInt("sw2_42"));
        assertEquals("str", sr2.getString("sw2_str"));
        assertFalse(sr2.hasName("sw1_42"));
    }

    //----

    private void makeError(String msg) {
        try {
            new SerialReader(msg);
        } catch (DecodeError e) {
            // success
            return;
        }

        fail("Expected DecodeError");
    }

    /** type: one of Integer, Boolean, etc.. */
    private void castException(SerialReader sr, Class<?> type, String name) {
        try {
            if (type == int.class || type == Integer.class) {
                sr.getInt(name);

            } else if (type == long.class || type == Long.class) {
                sr.getLong(name);

            } else if (type == float.class || type == Float.class) {
                sr.getFloat(name);

            } else if (type == double.class || type == Double.class) {
                sr.getDouble(name);

            } else if (type == boolean.class || type == Boolean.class) {
                sr.getBool(name);

            } else if (type == String.class) {
                sr.getString(name);

            } else if (type == SerialReader.class) {
                sr.getSerial(name);
            }

        } catch (ClassCastException e) {
            // success
            return;
        }

        fail("Expected DecodeError");
    }

}
