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

package ch.usi.si.codelounge.jsicko.tutorials.stack;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.tutorials.stack.impl.BadStack;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simple tests for the bad Stack.
 */
public class BadStackTest {

    @Test
    public void popOnEmptyStack() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        assertThrows(Contract.PreconditionViolation.class,foo::pop);
    }

    @Test
    public void pushTest() throws Throwable {
        BadStack<String> foo = new BadStack<String>();
        foo.push("elem");
    }

    @Test
    public void baseTest() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        foo.push("elem");
        foo.top();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        foo.push("elem1");
        assertThrows(Contract.PostconditionViolation.class, () -> foo.push("elem2"));
    }

    @Test
    public void clearTest() throws Throwable {
        var baseCollection = IntStream.range(0,10).boxed().collect(Collectors.toList());
        BadStack<Integer> foo = new BadStack<>(baseCollection);
        foo.clear();
    }


}
