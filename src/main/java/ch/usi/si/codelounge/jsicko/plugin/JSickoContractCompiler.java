package ch.usi.si.codelounge.jsicko.plugin;

import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;

/**
 * The main entry point for jSicko, that is, the implementation of the javac compiler.
 */
public class JSickoContractCompiler implements Plugin {

    private final String NAME;

    /**
     * Constructs the contract compiler.
     */
    public JSickoContractCompiler() {
        this.NAME = JSickoContractCompiler.class.getSimpleName();
    }

    /**
     * Returns the name of the plugin.
     * @return the name of the plugin.
     */
    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public void init(JavacTask task, String... args) {
        Context context = ((BasicJavacTask) task).getContext();
        ContractCompilerTaskListener contractCollectorTaskListener = new ContractCompilerTaskListener(task);
        task.addTaskListener(contractCollectorTaskListener);
    }

}
