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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListTest {

    @Test
    public void addTest() throws Throwable {
            List<Integer> list = new List<>();
            list.add(3);
    }

    @Test
    public void addNullTest() throws Throwable {
        List<Integer> list = new List<>();
        list.add(null);
    }

    @Test
    public void removeTest() throws Throwable {
        List<Integer> list = new List<>();
        list.add(3);
        list.add(3);
        list.add(3);
        list.remove(3);
        list.remove(4);
        list.remove(3);
        list.remove(3);
    }

    Executable copyFromTestFixture1 = () -> {
        AbstractCollection<Integer> list = new List<>();
        AbstractCollection<Integer> other = new List<>();
        other.add(3);
        other.add(5);
        list.copyFrom(other);
    };

    @Test
    public void copyTestViolation1() throws Throwable {
        assertThrows(Contract.PreconditionViolation.class, copyFromTestFixture1);
    }

    @Test
    public void copyTestViolation2() throws Throwable {
        var exception = assertThrows(Contract.PreconditionViolation.class, copyFromTestFixture1);
        assertTrue(exception.getMessage().contains("other_equal_size"));
    }

    @Test
    public void copyTestViolation3() throws Throwable {
        var exception = assertThrows(Contract.PreconditionViolation.class, copyFromTestFixture1);
        assertTrue(exception.getMessage().contains("greater_size_than_other"));
    }

    @Test
    public void copyTestViolation4() throws Throwable {
        var exception = assertThrows(Contract.PreconditionViolation.class, copyFromTestFixture1);
        assertTrue(exception.getMessage().contains("isEmpty"));
    }

}
