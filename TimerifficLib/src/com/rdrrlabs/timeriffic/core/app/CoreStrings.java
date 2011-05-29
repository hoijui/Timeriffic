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
 * Holder for app-wide encoded strings.
 */
public class CoreStrings {

    public enum Strings {
        ERR_UI_MAILTO,
        ERR_UI_DOMTO
    }

    CoreStrings() {
    }

    public String get(Strings code) {
        return "undefined";
    }

    public String format(Strings code, Object...args) {
        return String.format(get(code), args);
    }

}
