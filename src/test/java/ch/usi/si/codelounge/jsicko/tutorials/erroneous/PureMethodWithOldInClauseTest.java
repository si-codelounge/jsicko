/*
 * Copyright (C) 2020 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
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

package ch.usi.si.codelounge.jsicko.tutorials.erroneous;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PureMethodWithOldInClauseTest {

    @Test
    public void throwsRuntimeExceptionWhenInvokingPureMethodWithOldInClause() throws Throwable {
        var instance = new PureMethodWithOldInClause();
        Executable textFixture = () -> instance.pureMethod();
        var exception = assertThrows(RuntimeException.class, textFixture);
        assertEquals("[jsicko] values table does not contain key: clause uses old(.) in a pure method", exception.getMessage(), "Message is unexpected");
    }

    @Test
    public void throwsRuntimeExceptionWhenInvokingStaticPureMethodWithOldInClause() throws Throwable {
        Executable textFixture = () -> PureMethodWithOldInClause.pureStaticMethod();
        var exception = assertThrows(RuntimeException.class, textFixture);
        assertEquals("[jsicko] values table does not contain key _staticProperty: clause uses old(.) in a pure method", exception.getMessage(), "Message is unexpected");
    }


}
