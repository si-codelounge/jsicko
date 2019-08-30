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
import ch.usi.si.codelounge.jsicko.tutorials.stack.impl.ListStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListStackTest {

    @Test
    public void popOnEmptyStack() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        assertThrows(Contract.PreconditionViolation.class,foo::pop);
    }

    @Test
    public void pushTest() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        foo.push("elem");
    }

    @Test
    public void elementAtTest() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        foo.push("elem");
        assertThrows(Contract.PreconditionViolation.class,() -> foo.elementAt(2));
    }

    @Test
    public void baseTest() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        foo.push("elem");
        foo.top();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        foo.push("elem1");
        foo.push("elem2");
        foo.push("elem3");
        foo.top();
        foo.pop();
        foo.top();
        foo.pop();
        foo.push("elem4");
        foo.top();
        foo.pop();
        foo.top();
        foo.pop();
    }

    @Test
    public void clearTest() throws Throwable {
        ListStack<String> foo = new ListStack<>();
        for (int i = 0; i < 10; i++)
            foo.push(String.valueOf(i));
        foo.clear();
    }

}
