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
import ch.usi.si.codelounge.jsicko.tutorials.stack.impl.GoodStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoodStackTest {

    @Test
    public void popOnEmptyStack() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        assertThrows(Contract.PreconditionViolation.class,foo::pop);
    }

    @Test
    public void pushTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
    }

    @Test
    public void elementAtTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
        assertThrows(Contract.PreconditionViolation.class,() -> foo.elementAt(2));
    }

    @Test
    public void baseTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
        foo.top();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
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
        GoodStack<String> foo = new GoodStack<>();
        for (int i = 0; i < 10; i++)
            foo.push(String.valueOf(i));
        foo.clear();
    }

    @Test
    public void removeTest()  {
        GoodStack<String> foo = new GoodStack<>();
        for (int i = 0; i < 10; i++)
            foo.push(String.valueOf(i));
        foo.remove(3);
    }

    @Test
    public void exceptionalRemoveTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        for (int i = 0; i < 10; i++)
            foo.push(String.valueOf(i));
        assertThrows(Contract.PostconditionViolation.class,() -> foo.remove(-1));
        assertThrows(Contract.PostconditionViolation.class,() -> foo.remove(11));
    }

    @Test
    public void testIndexOf() {
        GoodStack<Integer> s = new GoodStack<>();
        s.push(3);
        s.indexOf(9);  // to trigger the bug, look for an element that is not present
    }

}
