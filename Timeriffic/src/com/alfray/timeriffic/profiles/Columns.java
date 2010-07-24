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

package com.alfray.timeriffic.profiles;

import android.provider.BaseColumns;

//-----------------------------------------------

/**
 * Column names for the profiles/timed_actions table.
 */
public class Columns implements BaseColumns {

    /** The type of this row.
     * Enum: {@link #TYPE_IS_PROFILE} or {@link #TYPE_IS_TIMED_ACTION}.
     * <p/>
     * Note: 0 is not a valid value. Makes it easy to identify a non-initialized row.
     * <p/>
     * Type: INTEGER
     */
    public static final String TYPE = "type";

    /** type TYPE_IS_PROFILE: the row is a profile definition */
    public static final int TYPE_IS_PROFILE = 1;
    /** type TYPE_IS_TIMED_ACTION: the row is a timed action definition */
    public static final int TYPE_IS_TIMED_ACTION = 2;

    // --- fields common to both a profile definition and a timed action

    /** Description:
     * - Profile: user title.
     * - Time action: pre-computed summary description.
     * <p/>
     * Type: TEXT
     */
    public static final String DESCRIPTION = "descrip";

    /** Is Enabled:
     * - Profile: user-selected enabled toggle.
     *   0=disabled, 1=enabled
     * - Timed action: is executing (pre-computed value).
     *   0=default, 1=last, 2=next
     * <p/>
     * Type: INTEGER
     */
    public static final String IS_ENABLED = "enable";

    public static final int PROFILE_ENABLED = 0;
    public static final int PROFILE_DISABLED = 1;
    public static final int ACTION_MARK_DEFAULT = 0;
    public static final int ACTION_MARK_PREV = 1;
    public static final int ACTION_MARK_NEXT = 2;


    // --- fields for a profile definition



    // --- fields for a timed action

    /** Profile ID = profile_index << PROFILE_SHIFT + action_index.
     * <p/>
     * - Profile: The base number of the profile << {@link #PROFILE_SHIFT}
     *            e.g. PROF << 16.
     * - Timed action: The profile's profile_id + index of the action,
     *            e.g. PROF << 16 + INDEX_ACTION.
     * <p/>
     * Allocation rules:
     * - Profile's index start at 1, not 0. So first profile_id is 1<<16.
     * - Action index start at 1, so 1<<16+0 is a profile but 1<<16+1 is an action.
     * - Max 1<<16-1 actions per profile.
     * - On delete, don't compact numbers.
     * - On insert before or after, check if the number is available.
     *   - On insert, if not available, need to move items to make space.
     * - To avoid having to move, leave gaps:
     *   - Make initial first index at profile 256*capacity.
     *   - When inserting at the end, leave a 256 gap between profiles or actions.
     *   - When inserting between 2 existing entries, pick middle point.
     * <p/>
     * Type: INTEGER
     */
    public static final String PROFILE_ID = "prof_id";

    public static final int PROFILE_SHIFT = 16;
    public static final int ACTION_MASK = (1<<PROFILE_SHIFT)-1;
    public static final int PROFILE_GAP = 256;
    public static final int TIMED_ACTION_GAP = 256;


    /** Hour-Min Time, computed as hour*60+min in a day (from 0 to 23*60+59)
     * <p/>
     * Type: INTEGER
     */
    public static final String HOUR_MIN = "hourMin";

    /** Day(s) of the week.
     * This is a bitfield: {@link #MONDAY} thru {@link #SUNDAY} at
     * bit indexes {@link #MONDAY_BIT_INDEX} thru {@link #SUNDAY_BIT_INDEX}.
     * <p/>
     * Type: INTEGER
     */
    public static final String DAYS = "days";

    /** The first day of the bit field: monday is bit 0. */
    public static final int MONDAY_BIT_INDEX = 0;
    /** The last day of the bit field: sunday is bit 6. */
    public static final int SUNDAY_BIT_INDEX = 6;

    public static final int MONDAY    = 1 << MONDAY_BIT_INDEX;
    public static final int TUESDAY   = 1 << 1;
    public static final int WEDNESDAY = 1 << 2;
    public static final int THURSDAY  = 1 << 3;
    public static final int FRIDAY    = 1 << 4;
    public static final int SATURDAY  = 1 << 5;
    public static final int SUNDAY    = 1 << SUNDAY_BIT_INDEX;



    /** Actions to execute.
     * This is an encoded string:
     * - action letter
     * - digits for parameter (optional)
     * - comma (except for last).
     * Example: "M0,V1,R50"
     * <p/>
     * Type: STRING
     */
    public static final String ACTIONS = "actions";

    /** Ringer: R)ring, M)muted */
    public static final char ACTION_RINGER      = 'R';
    /** Vibrate Ringer: V)ibrate, N)o vibrate all, R)no ringer vibrate, O)no notif vibrate */
    public static final char ACTION_VIBRATE     = 'V';
    /** Ringer volume. Integer: 0..100 */
    public static final char ACTION_RING_VOLUME = 'G';
    /** Notification volume. Integer: 0..100 */
    public static final char ACTION_NOTIF_VOLUME = 'N';
    /** Media volume. Integer: 0..100 */
    public static final char ACTION_MEDIA_VOLUME = 'M';
    /** Alarm volume. Integer: 0..100 */
    public static final char ACTION_ALARM_VOLUME = 'L';
    /** Screen Brightness. Integer: 0..100 */
    public static final char ACTION_BRIGHTNESS  = 'B';
    /** Wifi. Boolean: 0..1 */
    public static final char ACTION_WIFI        = 'W';
    /** AirplaneMode. Boolean: 0..1 */
    public static final char ACTION_AIRPLANE    = 'A';
    /** Bluetooth. Boolean: 0..1 */
    public static final char ACTION_BLUETOOTH   = 'U';
    /** APN Droid. Boolean: 0..1 */
    public static final char ACTION_APN_DROID   = 'D';

    /**
     * The precomputed System.currentTimeMillis timestamp of the next event for this action.
     * Type: INTEGER (long)
     */
    public static final String NEXT_MS = "next_ms";

    /** The default sort order for this table, _ID ASC */
    public static final String DEFAULT_SORT_ORDER = PROFILE_ID + " ASC";
}
