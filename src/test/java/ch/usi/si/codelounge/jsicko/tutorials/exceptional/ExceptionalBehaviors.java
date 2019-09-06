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

import javax.print.PrintException;
import java.io.IOException;

import static ch.usi.si.codelounge.jsicko.ContractUtils.implies;

/**
 * A collection of toy methods with exceptional behaviors.
 *
 * Covers checked exceptions, unchecked exceptions, generic throwables,
 * and a few interesting corner cases.
 */
public abstract class ExceptionalBehaviors implements Contract {

    private ExceptionalBehaviors() {}

    @Pure
    public static boolean throws_unsupported_operation_exception(Throwable raises) {
        return raises instanceof UnsupportedOperationException;
    }

    @Pure
    public static boolean throws_illegal_argument_exception(Throwable raises) {
        return raises instanceof IllegalArgumentException;
    }

    @Pure
    public static boolean throws_io_exception(Throwable raises) {
        return raises instanceof IOException;
    }

    @Pure
    public static boolean nondeterministically_throws_checked_exceptions(Throwable raises) {
        return raises instanceof IOException ||
                raises instanceof PrintException;
    }

    @Pure
    public static boolean illegal_argument_only_when_negative(Throwable raises, int param) {
        return implies(param < 0,
                () -> throws_illegal_argument_exception(raises),
                () -> raises == null);
    }

    @Pure
    public static boolean throws_some_throwable(Throwable raises) {
        return raises != null && !(raises instanceof Exception);
    }

    @Pure
    public static boolean throws_some_error(Throwable raises) {
        return raises != null && raises instanceof Error;
    }

    @Ensures("throws_unsupported_operation_exception")
    public static void throwsUndeclaredUncheckedException() {
        throw new UnsupportedOperationException();
    }


    @Ensures("illegal_argument_only_when_negative")
    public static void throwsDeclaredUncheckedException(int param) throws IllegalArgumentException {
        if (param < 0)
            throw new IllegalArgumentException();
    }

    @Ensures("throws_io_exception")
    public static void throwsDeclaredCheckedException() throws IOException {
        throw new IOException("A IO Error");
    }

    @Ensures("nondeterministically_throws_checked_exceptions")
    public static void randomlyThrowsTwoDeclaredCheckedExceptions() throws IOException, PrintException  {
        java.lang.Throwable foo = null;
        try {
            if (Math.random() > 0.5)
                throw new IOException("A IO Error");
            else
                throw new PrintException("A print exception");
        } catch (java.lang.Exception bar) {
            foo = bar;
            throw bar;
        }
    }

    @Ensures("throws_some_throwable")
    public static void throwsThrowable() throws Throwable {
        throw new Throwable("A throwable object");
    }

    @Ensures("throws_some_error")
    public static void throwsSomeError() throws StackOverflowError {
        throw new StackOverflowError("A very bad virtual machine error");
    }

    /*
     * Wrong implementations (do not respect postconditions)
     */

    @Ensures("throws_io_exception")
    public static void throwsSomeOtherCheckedException() throws Exception {
        throw new PrintException("A Print Exception");
    }


    @Ensures("throws_unsupported_operation_exception")
    public static void doesntThrowUndeclaredUncheckedException() {
        return;
    }


    @Ensures("illegal_argument_only_when_negative")
    public static void doesntThrowDeclaredUncheckedException(int param) throws IllegalArgumentException {
        if (param > 0)
            throw new IllegalArgumentException();
    }

    @Ensures("throws_io_exception")
    public static void doesntThrowDeclaredCheckedException() throws IOException {
        return;
    }

}
