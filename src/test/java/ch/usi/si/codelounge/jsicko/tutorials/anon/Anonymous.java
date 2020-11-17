/*
 * Copyright (C) 2019 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
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

package ch.usi.si.codelounge.jsicko.tutorials.anon;

import ch.usi.si.codelounge.jsicko.Contract;

public class Anonymous implements Contract {

    @Invariant
    boolean inv() {
        return true;
    }

    @Pure
    static boolean returns_reminder(int returns, int p) {
        return returns == p % p;
    }

    @Pure boolean p_get0( int p) {
        return p >= 0;
    }
    @Pure  boolean p_let0( int p) {
        return p <= 0;
    }

    @Ensures("returns_reminder")
    public static int bar(int p) {
            return p % p;
        }

        @Pure
        public int overloaded(Object... bar) {
            return 0;
        }

        @Requires({"p_get0","p_let0"})
        public  void overloaded(int p) {
            overloaded(1,2,3,4);
            return;
        }

    public Anonymous anonymous = new Anonymous() {

        @Ensures("returns_reminder")
         public int barr(int p) {
            return p - p;
        };

    };

}
