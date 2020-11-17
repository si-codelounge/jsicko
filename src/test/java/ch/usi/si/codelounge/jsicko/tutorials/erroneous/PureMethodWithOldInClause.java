/*
 * Copyright (C) 2020 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
 *
 * This file is part of jSicko - Java SImple Contract checKer.
 *
 *  jSicko is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * jSicko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jSicko.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package ch.usi.si.codelounge.jsicko.tutorials.erroneous;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.Contract.old;

public class PureMethodWithOldInClause implements Contract {

    private boolean _property = false;
    private static boolean _staticProperty = false;

    @Pure
    @Ensures("postcondition")
    public final void pureMethod() {
        this.setProperty(!this.getProperty());
        return;
    }

    @Pure
    public boolean postcondition() {
        return this.getProperty() != old(this).getProperty();
    }

    boolean getProperty() {
        return _property;
    }

    void setProperty(boolean value) {
        this._property = value;
    }

    @Pure
    @Ensures("static_postcondition")
    public static void pureStaticMethod() {
        _staticProperty = !_staticProperty;
        return;
    }

    @Pure
    public static boolean static_postcondition() {
        return _staticProperty != old(_staticProperty);
    }


}
