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

import ch.usi.si.codelounge.jsicko.compilation.utils.CompilationResults;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.util.JCDiagnostic;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.function.Supplier;

@TestInstance(Lifecycle.PER_CLASS)
public class JSickoCompilerMissingClauseTest extends JSickoAbstractCompilerErrorTest {

    private CompilationResults results;
    private Supplier<JCDiagnostic> firstErrorDiagnostic = () -> ((ClientCodeWrapper.DiagnosticSourceUnwrapper) results.getErrors().get(0)).d;

    @Override
    protected String getQualifiedClassName() {
        return "ch.usi.si.codelounge.jsicko.compilation.tests.MissingClauseMethod";
    }

    @Override
    protected String getFileName() {
        return "MissingClauseMethod.java";
    }

    @Override
    protected int getExpectedErrorCount() {
        return 1;
    }

    @Override
    protected String getExpectedDiagnosticCode() {
        return "jsicko.err.missing.clause";
    }

    @Override
    protected long getExpectedDiagnosticLineNumber() {
        return 33l;
    }

    @Override
    protected long getExpectedDiagnosticColumnNumber() {
        return 15;
    }

    @Override
    protected String[] getExpectedDiagnosticArguments() {
        return new String[] { "clause clause_typo in MissingClauseMethod#someMethod()" };
    }

}
