/*
 * Project: Timeriffic
 * Copyright (C) 2011 ralfoide gmail com,
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

import com.rdrrlabs.timeriffic.core.profiles1.ProfilesUiImpl;
import com.rdrrlabs.timeriffic.ui.ActivityDelegate;

/**
 * Activity redirector which is only present for backward compatibility.
 *
 * IMPORTANT: this MUST remain as com.rdrrlabs.timeriffic.profiles.ProfilesUi1
 * otherwise legacy home shortcuts will break.
 */
public class ProfilesUi1 extends ActivityDelegate<ProfilesUiImpl> {

    @Override
    public ProfilesUiImpl createDelegate() {
        return new ProfilesUiImpl(this);
    }
}
