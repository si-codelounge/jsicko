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

public abstract class JSickoAbstractCompilerSuccessTest {

    private CompilationResults results;
    private Supplier<JCDiagnostic> firstErrorDiagnostic = () -> ((ClientCodeWrapper.DiagnosticSourceUnwrapper) results.getErrors().get(0)).d;

    @BeforeAll
    public void compile() throws IOException {
        this.results = TestCompiler.compile(this.getQualifiedClassName(), this.getFileName());
    }

    @Test
    public void errorCount() {
        Assertions.assertEquals(0, results.getErrors().size(), "Compilation of MissingClauseParameter.java should have no errors (actual size " + results.getErrors().size() + ")");
    }

    /*
     * Test parameters
     */
    protected abstract String getQualifiedClassName();
    protected abstract String getFileName();

}
