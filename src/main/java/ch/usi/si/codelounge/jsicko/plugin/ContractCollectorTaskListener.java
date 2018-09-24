package ch.usi.si.codelounge.jsicko.plugin;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContractCollectorTaskListener implements TaskListener {

    private final JavacTask task;

    public ContractCollectorTaskListener(JavacTask task) {
        this.task = task;
    }


    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() != TaskEvent.Kind.ENTER) {
            return;
        }
        e.getCompilationUnit().accept(new ContractCollectorTreeScanner((BasicJavacTask) task), null);
    }

}
