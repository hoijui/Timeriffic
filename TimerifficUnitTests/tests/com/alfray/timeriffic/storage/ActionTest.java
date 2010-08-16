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

    @Test
    public void testAction_null() {
        // This creates a new object with no previous data state.
        Action a = new Action(null);

        assertEquals("", a.getActions());
        assertEquals(0, a.getHourMin());
        assertEquals(0, a.getDays());
    }

    @Test(expected=SerialReader.DecodeError.class)
    public void testAction_bogusData() {
        new Action("bogus data");
    }

    @Test
    public void testAction_create_getData_recreate() {
        // This creates a new object with no previous data state.
        Action a = new Action(null);
        a.setActions("A0 B1");
        a.setDays(4);
        a.setHourMin(1234);

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
}


