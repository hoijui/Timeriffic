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

/**
 * Test for {@link SerialWriter}.
 * Extracted from NerdkillAndroid.
 */
public class SerialWriterTest extends TestCase {

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

    public void testDupKey() throws Exception {
        try {
            m.addInt("foo", 42);
            m.addBool("foo", false);
        } catch (SerialWriter.DuplicateKey e) {
            // that's expected
            return;
        }

        fail("Expected SerialWriter.DuplicateKey, exception not thrown");
    }

    public void testCloseTwice() throws Exception {
        // closing twice in a row is ok
        m.encodeAsString();
        m.encodeAsString();
    }

    public void testCloseThenAdd() throws Exception {
        try {
            m.addInt("foo", 42);
            m.encodeAsString();   // this will close
            m.addBool("foo2", false); // this should fail
        } catch (SerialWriter.CantAddData e) {
            // that's expected
            return;
        }

        fail("Expected CantAddData, exception not thrown");
    }

    public void testAddNothing() throws Exception {
        String s = m.encodeAsString();
        assertEquals("3 33F170F2 E0F ", s);
    }

    public void testAddInt0() throws Exception {
        m.addInt("foo", 0);
        String s = m.encodeAsString();
        assertEquals("6 1 18CC6 0 1CAC734D E0F ", s);
    }

    public void testAddInt() throws Exception {
        m.addInt("foo", 0x12345678);
        String s = m.encodeAsString();
        assertEquals("6 1 18CC6 12345678 92852B83 E0F ", s);
    }

    public void testAddLong() throws Exception {
        m.addLong("foo", 0x12345678ABCDABCDL);
        String s = m.encodeAsString();
        assertEquals("7 2 18CC6 12345678 ABCDABCD 90B02923 E0F ", s);
    }

    public void testAddBool() throws Exception {
        m.addBool("foo", false);
        m.addBool("abc", true);
        String s = m.encodeAsString();
        assertEquals("9 3 18CC6 0 3 17862 1 D2E397B7 E0F ", s);
    }

    public void testAddFloat() throws Exception {
        m.addFloat("foo", 3.141592654f);
        String s = m.encodeAsString();
        assertEquals("6 4 18CC6 40490FDB 2961C824 E0F ", s);
    }

    public void testAddDouble() throws Exception {
        m.addDouble("foo", Math.PI);
        String s = m.encodeAsString();
        assertEquals("7 5 18CC6 400921FB 54442D18 5074D704 E0F ", s);
    }

    public void testAddString0() throws Exception {
        m.addString("foo", "");
        String s = m.encodeAsString();
        assertEquals("6 6 18CC6 0 68349AC2 E0F ", s);
    }

    public void testAddString4() throws Exception {
        m.addString("foo", "ABCD");
        String s = m.encodeAsString();
        assertEquals("8 6 18CC6 4 410042 430044 21185502 E0F ", s);
    }

    public void testAddString5() throws Exception {
        m.addString("foo", "ABCDE");
        String s = m.encodeAsString();
        assertEquals("9 6 18CC6 5 410042 430044 450000 D9BA407 E0F ", s);
    }

    public void testAddSerial0() throws Exception {
        SerialWriter s1 = new SerialWriter();

        m.addSerial("foo", s1);
        String s = m.encodeAsString();
        assertEquals("8 7 18CC6 3 33F170F2 E0F 6BEDBE94 E0F ", s);
    }

    public void testAddSerial1() throws Exception {
        SerialWriter s1 = new SerialWriter();
        s1.addInt("foo", 0x043);

        m.addSerial("foo", s1);
        String s = m.encodeAsString();
        assertEquals("B 7 18CC6 6 1 18CC6 43 950D849E E0F 3AA8CE38 E0F ", s);
    }

}
