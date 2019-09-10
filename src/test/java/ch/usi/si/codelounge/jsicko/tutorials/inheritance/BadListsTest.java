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

import static ch.usi.si.codelounge.jsicko.tutorials.inheritance.BadLists.BadList1;
import static org.junit.jupiter.api.Assertions.*;

public class BadListsTest {

    @Test
    public void badList1AddTest() throws Throwable {
            BadList1<Integer> list = new BadList1<>();
            list.add(3);
        Executable textFixture = () -> list.add(4);
        var exception = assertThrows(Contract.PostconditionViolation.class, textFixture);
        assertTrue(exception.getMessage().contains("element_at_old_size_position"));
    }



}
