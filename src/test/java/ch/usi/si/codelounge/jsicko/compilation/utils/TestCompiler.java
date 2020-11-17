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

package ch.usi.si.codelounge.jsicko.compilation.utils;

import ch.usi.si.codelounge.jsicko.plugin.JSickoContractCompiler;

import javax.tools.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public abstract class TestCompiler {

    static class CompilationErrorsDiagnosticListener implements DiagnosticListener<JavaFileObject>, CompilationResults {

        private List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        private List<Diagnostic<? extends JavaFileObject>> notes = new ArrayList<>();

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind().equals(Diagnostic.Kind.ERROR)) {
                this.errors.add(diagnostic);
            } else if (diagnostic.getKind().equals(Diagnostic.Kind.NOTE)) {
                this.notes.add(diagnostic);
            }
        }

        public List<Diagnostic<? extends JavaFileObject>> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<Diagnostic<? extends JavaFileObject>> getNotes() {
            return Collections.unmodifiableList(notes);
        }
    }

    public static CompilationResults compile(String qualifiedClassName, String fileName) throws IOException {
        StringWriter output = new StringWriter();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(
                compiler.getStandardFileManager(null, null, null));
        List<SimpleSourceFile> compilationUnits
                = Collections.singletonList(new SimpleSourceFile(qualifiedClassName, fileName));
        List<String> arguments = new ArrayList<>();
        arguments.addAll(asList("-classpath", System.getProperty("java.class.path"),
                "-Xplugin:" + JSickoContractCompiler.NAME));
        var diagnosticListener = new CompilationErrorsDiagnosticListener();
        JavaCompiler.CompilationTask task
                = compiler.getTask(output, fileManager, diagnosticListener, arguments, null,
                compilationUnits);
        task.call();

        return diagnosticListener;
    }
}