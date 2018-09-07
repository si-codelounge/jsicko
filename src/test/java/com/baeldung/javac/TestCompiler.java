package com.baeldung.javac;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.Diagnostic;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class TestCompiler {

    private final DiagnosticListener<JavaFileObject> systemOutDiagnosticListener = new DiagnosticListener<JavaFileObject> () {
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            System.out.println(diagnostic.getMessage(null));
        }
    };

    public byte[] compile(String qualifiedClassName, String testSource) {
        StringWriter output = new StringWriter();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(
                null,
                null,
                null
        ));
        List<SimpleSourceFile> compilationUnits = singletonList(new SimpleSourceFile(qualifiedClassName, testSource));
        List<String> arguments = new ArrayList<>();
        arguments.addAll(asList("-classpath", System.getProperty("java.class.path"),
                                "-Xplugin:" + SampleJavacPlugin.NAME));
        JavaCompiler.CompilationTask task = compiler.getTask(output,
                fileManager,
                systemOutDiagnosticListener,
                arguments,
                null,
                compilationUnits);

        var compilerResult = task.call();
        if (!compilerResult)
            throw new RuntimeException("Compilation failed");

        return fileManager.getCompiled().iterator().next().getCompiledBinaries();
    }
}
