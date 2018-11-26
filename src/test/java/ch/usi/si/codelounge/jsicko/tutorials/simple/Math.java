/*
 * Copyright (C) 2018 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
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

package ch.usi.si.codelounge.jsicko.tutorials.simple;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.Contract.old;

public abstract class Math implements Contract {

    @Requires("non_negative_arg")
    @Ensures({"returns_approximately_equal_to_square_of_arg"})
    public static double sqrt(double arg) {
        return java.lang.Math.sqrt(arg);
    }

    @Requires("non_negative_arg")
    @Ensures("returns_approximately_equal_to_square_of_arg")
    public static double badsqrt(double arg) {
        return java.lang.Math.pow(arg,0.48);
    }

    @Pure
    private static boolean non_negative_arg(double arg) {
        return arg >= 0;
    }

    @Pure
    private static boolean returns_approximately_equal_to_square_of_arg(double returns, double arg) {
        return java.lang.Math.abs((returns * returns) - arg) < 0.001;
    }

}
