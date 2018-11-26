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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CollectionsTest {

    @Test
    public void collectionsSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Collections.sort(list);
    }

    @Test
    public void mutableSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Collections.mutableSort(list);
    }

    @Test
    public void badMutableSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Executable textFixture = () -> Collections.badMutableSort(list);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void collectionsBadSortTest() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Executable textFixture = () -> Collections.badSort(list);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void collectionsBadSort2Test() throws Throwable {
        List<Integer> list = new ArrayList<>(List.of(3,2,1));
        Executable textFixture = () -> Collections.badSort2(list);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

}
