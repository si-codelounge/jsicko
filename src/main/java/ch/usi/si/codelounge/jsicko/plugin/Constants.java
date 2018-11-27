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

package ch.usi.si.codelounge.jsicko.plugin;

/**
 * Holds some constants needed for code generation.
 */
public final class Constants {

    private Constants() {}

    /**
     * The synthetic local variable for the variable holding the return value
     * in an instrumented method.
     *
     * This variable is essential check post-conditions for query/
     * hybrid command/query methods.
     */
    public static final String RETURNS_SYNTHETIC_IDENTIFIER_STRING = "$returns";

    /**
     * A special identifier to be used in clause methods to refer to the value returned
     * by a method.
     */
    public static final String RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING = "returns";

    /**
     * The name of the synthetic instance field used to store the old values table
     * for instance method calls.
     */
    public static final String OLD_FIELD_IDENTIFIER_STRING = "$oldValuesTable";

    /**
     * The name of the synthetic static field used to store the old values table
     * for static method calls.
     */
    public static final String STATIC_OLD_FIELD_IDENTIFIER_STRING = "$staticOldValuesTable";

    /**
     * The name of the method called to retrieve old values in instance method calls.
     *
     * Calls to the static old method of Contract are rewritten to this method in the
     * case of instance method calls.
     *
     * @see ch.usi.si.codelounge.jsicko.Contract#old(Object)
     * @see ch.usi.si.codelounge.jsicko.Contract#instanceOld(String, Object)
     */
    public static final String INSTANCE_OLD_METHOD_IDENTIFIER_STRING = "instanceOld";

    /**
     * The name of the method called to retrieve old values in static method calls.
     *
     * Calls to the static old method of Contract are rewritten to this method in the
     * case of static method calls.
     *
     * @see ch.usi.si.codelounge.jsicko.Contract#old(Object) s
     * @see ch.usi.si.codelounge.jsicko.Contract#staticOld(Class, String, Object)
     */
    public static final String STATIC_OLD_METHOD_IDENTIFIER_STRING = "staticOld";
}
