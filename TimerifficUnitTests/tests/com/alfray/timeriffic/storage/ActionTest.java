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
        

        assertEquals("", a.getActions());
        assertEquals(0, a.getHourMin());
        assertEquals(0, a.getDays());
    }

    @Test
    public void testGetHourMin() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetDays() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetActions() {
        fail("Not yet implemented");
    }

}


