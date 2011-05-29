/*
 * Project: Timeriffic
 * Copyright (C) 2011 rdrr labs gmail com,
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

package com.rdrrlabs.timeriffic.core.app;

/**
 * Holder for app-wide implementation-specific instances.
 * <p/>
 * Think of it has the kind of global instances one would typically
 * define using dependency injection. Tests could override this, etc.
 */
public class Core {

    protected UpdateServiceImpl mUpdateServiceImpl;
    protected CoreStrings mCoreStrings;

    /** Base constructor with defaults. */
    public Core() {
        mUpdateServiceImpl = new UpdateServiceImpl();
        mCoreStrings = new CoreStrings();
    }

    /** Derived constructor with overrides. */
    public Core(CoreStrings strings) {
        mUpdateServiceImpl = new UpdateServiceImpl();
        mCoreStrings = strings;
    }

    public UpdateServiceImpl getUpdateService() {
        return mUpdateServiceImpl;
    }

    public CoreStrings getCoreStrings() {
        return mCoreStrings;
    }
}
