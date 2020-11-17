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

package ch.usi.si.codelounge.jsicko.compilation.tests;

import ch.usi.si.codelounge.jsicko.Contract;

public class InvariantIsStatic {

    static class Implementation implements Contract {

        private int value = 0;

        /**
         * Check that the return value of method getValue is always positive.
         *
         * @param returns
         * @return
         */
        @Pure
        public boolean negative_return_value(int returns) {
            return returns < 0;
        }

        /**
         * Check getValue() return value is less than three.
         *
         * @return
         */
        @Pure
        public boolean lower_than_three_return_value(int returns) {
            return returns < 3;
        }

        @Invariant
        @Pure
        public static boolean static_invariant() {
            return true;
        }


        @Ensures({"!negative_return_value", "lower_than_three_return_value"})
        public int getValue() {
            return this.value;
        }

    }


}
