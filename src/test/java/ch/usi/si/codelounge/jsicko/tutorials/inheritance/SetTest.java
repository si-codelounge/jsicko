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

package ch.usi.si.codelounge.jsicko.tutorials.inheritance;

import ch.usi.si.codelounge.jsicko.Contract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SetTest {

    @Test
    public void addTest() throws Throwable {
        Set<Integer> intSet = new Set<>();
        intSet.add(3);
        intSet.add(3);
    }

    @Test
    public void addNullExceptionalTest() throws Throwable {
        Set<Integer> intSet = new Set<>();
        Executable testFixture = () -> intSet.add(null);
        assertThrows(NullPointerException.class, testFixture);
    }

    @Test
    public void removeTest() throws Throwable {
        Set<Integer> intSet = new Set<>();
        intSet.add(3);
        intSet.remove(3);
        intSet.remove(4);
        intSet.remove(3);
        intSet.remove(3);
    }

}
