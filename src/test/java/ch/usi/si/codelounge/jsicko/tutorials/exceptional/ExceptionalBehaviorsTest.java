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

package ch.usi.si.codelounge.jsicko.tutorials.exceptional;

import ch.usi.si.codelounge.jsicko.Contract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionalBehaviorsTest {

    @Test
    public void throwsUndeclaredUncheckedExceptionTest() throws Throwable {
        Executable textFixture = () -> ExceptionalBehaviors.throwsUndeclaredUncheckedException();
        assertThrows(UnsupportedOperationException.class, textFixture);
    }

    @Test
    public void throwsDeclaredUncheckedExceptionTest() throws Throwable {
        Executable textFixture = () -> ExceptionalBehaviors.throwsDeclaredUncheckedException(-3);
        assertThrows(IllegalArgumentException.class, textFixture);
    }

    @Test
    public void throwsDeclaredUncheckedExceptionNormalBehaviorTest() {
        ExceptionalBehaviors.throwsDeclaredUncheckedException(0);
    }

    @Test
    public void throwsDeclaredCheckedExceptionTest() {
        Executable textFixture = () -> ExceptionalBehaviors.throwsDeclaredCheckedException();
        assertThrows(IOException.class, textFixture);
    }

    @Test
    public void randomlyThrowsTwoDeclaredCheckedExceptionsTest() {
        Executable textFixture = () -> ExceptionalBehaviors.randomlyThrowsTwoDeclaredCheckedExceptions();
        for (int i = 0; i < 100; i++) {
            assertThrows(Exception.class, textFixture);
        }
    }

    @Test
    public void throwsThrowableTest() {
        Executable textFixture = () -> ExceptionalBehaviors.throwsThrowable();
        assertThrows(Throwable.class, textFixture);
    }

    @Test
    public void throwsSomeErrorTest() {
        Executable textFixture = () -> ExceptionalBehaviors.throwsSomeError();
        assertThrows(Error.class, textFixture);
    }

    @Test
    public void throwsSomeOtherCheckedExceptionTest() {
        Executable textFixture = () -> ExceptionalBehaviors.throwsSomeOtherCheckedException();
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void doesntThrowUndeclaredUncheckedExceptionTest() {
        Executable textFixture = () -> ExceptionalBehaviors.doesntThrowUndeclaredUncheckedException();
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void doesntThrowDeclaredUncheckedExceptionNormalBehaviorWhenPositiveTest() {
        Executable textFixture = () -> ExceptionalBehaviors.doesntThrowDeclaredUncheckedException(1);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void doesntThrowDeclaredUncheckedExceptionNormalBehaviorWhenNegativeTest() {
        Executable textFixture = () -> ExceptionalBehaviors.doesntThrowDeclaredUncheckedException(-1);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

    @Test
    public void doesntThrowDeclaredCheckedExceptionTest() {
        Executable textFixture = () -> ExceptionalBehaviors.doesntThrowDeclaredCheckedException();
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }

}
