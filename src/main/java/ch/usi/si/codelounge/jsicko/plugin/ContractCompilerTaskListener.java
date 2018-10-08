package ch.usi.si.codelounge.jsicko.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;

import java.util.ArrayDeque;

public class ContractCompilerTaskListener implements TaskListener {

    private final JavacTask task;

    public ContractCompilerTaskListener(JavacTask task) {
        this.task = task;
    }


    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() != TaskEvent.Kind.ENTER) {
            return;
        }
        e.getCompilationUnit().accept(new ContractCompilerTreeScanner((BasicJavacTask) task), new ArrayDeque<>());
    }

}
