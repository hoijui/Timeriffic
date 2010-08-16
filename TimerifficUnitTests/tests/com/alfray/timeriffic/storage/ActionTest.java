/*
 * (c) ralfoide gmail com, 2008
 * Project: TimerifficUnitTests
 * License TBD
 */

package com.alfray.timeriffic.storage;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alfray.timeriffic.serial.SerialReader;

//-----------------------------------------------

public class ActionTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test(expected=SerialReader.DecodeError.class)
    public void testAction_bogusData() {
        new Action("bogus data");
    }

    @Test
    public void testAction_null() {
        // This creates a new object with no previous data state.
        Action a = new Action(null);

        assertEquals("", a.getActions());
        assertEquals(0, a.getHourMin());
        assertEquals(0, a.getDays());
    }

    @Test
    public void testAction_values() {
        Action a = new Action("P0 Q1 T2", 1234, 5);

        assertEquals("P0 Q1 T2", a.getActions());
        assertEquals(1234, a.getHourMin());
        assertEquals(5, a.getDays());
    }

    @Test
    public void testAction_set() {
        Action a = new Action("P0 Q1 T2", 1234, 5);

        assertEquals("P0 Q1 T2", a.getActions());
        assertEquals(1234, a.getHourMin());
        assertEquals(5, a.getDays());

        assertEquals(a, a.setActions("A0 B1"));
        assertEquals(a, a.setDays(4));
        assertEquals(a, a.setHourMin(1234));

        assertEquals("A0 B1", a.getActions());
        assertEquals(4, a.getDays());
        assertEquals(1234, a.getHourMin());
    }

    @Test
    public void testAction_create_getData_recreate() {
        Action a = new Action("A0 B1", 1234, 4);

        assertEquals("A0 B1", a.getActions());
        assertEquals(4, a.getDays());
        assertEquals(1234, a.getHourMin());

        String data = a.getData();
        assertNotNull(data);
        assertTrue(data.length() > 0);

        Action b = new Action(data);
        assertEquals("A0 B1", b.getActions());
        assertEquals(4, b.getDays());
        assertEquals(1234, b.getHourMin());
    }

    @Test
    public void testEquals() {
        Action a1 = new Action("P0 Q1 T2", 1234, 5);
        Action a2 = new Action("P0 Q1 T2", 1234, 5);
        Action b = new Action("A0 B1", 1234, 4);

        assertNotSame(a2, a1);
        assertEquals(a2, a1);
        assertFalse(a2.equals(b));
    }

    @Test
    public void testHashCode() {
        Action a1 = new Action("P0 Q1 T2", 1234, 5);
        Action a2 = new Action("P0 Q1 T2", 1234, 5);
        Action b = new Action("A0 B1", 1234, 4);

        assertEquals(a2.hashCode(), a1.hashCode());
        assertFalse(a2.hashCode() == b.hashCode());
    }
}


