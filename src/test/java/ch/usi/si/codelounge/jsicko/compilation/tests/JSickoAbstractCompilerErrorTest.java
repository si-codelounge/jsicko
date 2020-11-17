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

package ch.usi.si.codelounge.jsicko.compilation.tests;

import ch.usi.si.codelounge.jsicko.compilation.utils.CompilationResults;
import ch.usi.si.codelounge.jsicko.compilation.utils.TestCompiler;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.util.JCDiagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.function.Supplier;

public abstract class JSickoAbstractCompilerErrorTest {

    private CompilationResults results;
    private Supplier<JCDiagnostic> firstErrorDiagnostic = () -> ((ClientCodeWrapper.DiagnosticSourceUnwrapper) results.getErrors().get(0)).d;

    @BeforeAll
    public void compile() throws IOException {
        this.results = TestCompiler.compile(this.getQualifiedClassName(), this.getFileName());
    }

    @Test
    public void errorCount() {
        Assertions.assertEquals(this.getExpectedErrorCount(), results.getErrors().size(), "Compilation of MissingClauseParameter.java should have " + this.getExpectedErrorCount() +  " errors (actual size " + results.getErrors().size() + ")");
    }

    @Test
    public void errorType() {
        Assertions.assertTrue(results.getErrors().get(0) instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper,
                "Unexpected error type " + results.getErrors().get(0).getClass () + "; should be , ClientCodeWrapper.DiagnosticSourceUnwrapper");
    }

    @Test
    public void diagnosticKind() {
        Assertions.assertSame(Diagnostic.Kind.ERROR, this.firstErrorDiagnostic.get().getKind(), "Unexpected diagnostic kind " + this.firstErrorDiagnostic.get().getKind() + "; should be Diagnostic.Kind.ERROR");
    }

    @Test
    public void diagnosticCode() {
        Assertions.assertEquals(this.getExpectedDiagnosticCode(), this.firstErrorDiagnostic.get().getCode(),  "Unexpected diagnostic code " + this.firstErrorDiagnostic.get().getCode() + "; should be \"jsicko.err.invariant.non.zero.arity\"");
    }

    @Test
    public void diagnosticPosition() {
        var lineNumber = this.firstErrorDiagnostic.get().getLineNumber();
        var columnNumber = this.firstErrorDiagnostic.get().getColumnNumber();
        Assertions.assertEquals(this.getExpectedDiagnosticLineNumber(), lineNumber ,  "Wrong diagnostic line number: " + lineNumber);
        Assertions.assertEquals(this.getExpectedDiagnosticColumnNumber(), columnNumber ,  "Wrong diagnostic column number: " + columnNumber);
    }

    @Test
    public void diagnosticArguments() {
        Assertions.assertArrayEquals(this.getExpectedDiagnosticArguments(), this.firstErrorDiagnostic.get().getArgs(), "Wrong arguments");
    }

    /*
     * Test parameters
     */
    protected abstract String getQualifiedClassName();
    protected abstract String getFileName();
    protected abstract int getExpectedErrorCount();
    protected abstract String getExpectedDiagnosticCode();
    protected abstract long getExpectedDiagnosticLineNumber();
    protected abstract long getExpectedDiagnosticColumnNumber();
    protected abstract String[] getExpectedDiagnosticArguments();

}
