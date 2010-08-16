/*
 * (c) ralfoide gmail com, 2008
 * Project: TimerifficUnitTests
 * License TBD
 */

package com.alfray.timeriffic.storage;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alfray.timeriffic.serial.SerialReader;

//-----------------------------------------------

public class ProfileTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test(expected=SerialReader.DecodeError.class)
    public void testProfile_bogusData() {
        new Profile("bogus data");
    }

    @Test
    public void testProfile_null() {
        // This creates a new object with no previous data state.
        Profile p = new Profile(null);

        assertEquals("", p.getTitle());
        assertEquals(true, p.isEnabled());
        assertArrayEquals(new Action[0], p.getActions().toArray());
    }

    @Test
    public void testProfile_values() {
        ArrayList<Action> list = new ArrayList<Action>();
        list.add(new Action("A0", 900, 1));
        list.add(new Action("A1 B2 Na", 800, 2));
        list.add(new Action("A2 Qv", 710, 3));

        Profile p = new Profile("My Profile", false, list);

        assertEquals("My Profile", p.getTitle());
        assertEquals(false, p.isEnabled());
        assertArrayEquals(
            new Action[] {
                new Action("A0", 900, 1),
                new Action("A1 B2 Na", 800, 2),
                new Action("A2 Qv", 710, 3)},
            p.getActions().toArray());
    }

    @Test
    public void testProfile_create_getData_recreate() {
        ArrayList<Action> list = new ArrayList<Action>();
        list.add(new Action("A0", 900, 1));
        list.add(new Action("A1 B2 Na", 800, 2));
        list.add(new Action("A2 Qv", 710, 3));

        Profile p = new Profile("My Profile", false, list);

        assertEquals("My Profile", p.getTitle());
        assertEquals(false, p.isEnabled());
        assertArrayEquals(
            new Action[] {
                new Action("A0", 900, 1),
                new Action("A1 B2 Na", 800, 2),
                new Action("A2 Qv", 710, 3)},
            p.getActions().toArray());

        String data = p.getData();
        assertNotNull(data);
        assertTrue(data.length() > 0);

        Profile q = new Profile(data);

        assertEquals("My Profile", q.getTitle());
        assertEquals(false, q.isEnabled());
        // Important: serializing the profile does NOT save the actions.
        // This is an implementation detail of the Storage class.
        assertArrayEquals(new Action[0], q.getActions().toArray());
    }

    @Test
    public void testEquals() {
        ArrayList<Action> list = new ArrayList<Action>();
        list.add(new Action("A0", 900, 1));
        list.add(new Action("A1 B2 Na", 800, 2));
        list.add(new Action("A2 Qv", 710, 3));
        Profile p1 = new Profile("My Profile", false, list);
        Profile p2 = new Profile("My Profile", false, list);
        Profile p3 = new Profile(null, true, null);

        assertNotSame(p2, p1);
        assertEquals(p2, p1);
        assertFalse(p2.equals(p3));
    }

    @Test
    public void testHashCode() {
        ArrayList<Action> list = new ArrayList<Action>();
        list.add(new Action("A0", 900, 1));
        list.add(new Action("A1 B2 Na", 800, 2));
        list.add(new Action("A2 Qv", 710, 3));
        Profile p1 = new Profile("My Profile", false, list);
        Profile p2 = new Profile("My Profile", false, list);
        Profile p3 = new Profile(null, true, null);

        assertEquals(p2.hashCode(), p1.hashCode());
        assertFalse(p2.hashCode() == p3.hashCode());
    }
}


