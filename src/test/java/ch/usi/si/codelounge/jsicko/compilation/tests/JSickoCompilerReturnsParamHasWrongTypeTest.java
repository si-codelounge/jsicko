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

package ch.usi.si.codelounge.jsicko.compilation.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JSickoCompilerReturnsParamHasWrongTypeTest extends JSickoAbstractCompilerErrorTest {

    @Override
    protected String getQualifiedClassName() {
        return "ch.usi.si.codelounge.jsicko.compilation.tests.ReturnsParamHasWrongType";
    }

    @Override
    protected String getFileName() {
        return "ReturnsParamHasWrongType.java";
    }

    @Override
    protected int getExpectedErrorCount() {
        return 1;
    }

    @Override
    protected String getExpectedDiagnosticCode() {
        return "jsicko.err.wrong.param.type";
    }

    @Override
    protected long getExpectedDiagnosticLineNumber() {
        return 33l;
    }

    @Override
    protected long getExpectedDiagnosticColumnNumber() {
        return 23l;
    }

    @Override
    protected String[] getExpectedDiagnosticArguments() {
        return new String[] { "returns", "double", "int", "clause returns_something(int returns) in ReturnsParamHasWrongType#voidMethod()" };
    }

}
