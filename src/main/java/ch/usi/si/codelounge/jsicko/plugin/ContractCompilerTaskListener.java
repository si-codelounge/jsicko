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

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.JavacMessages;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.ResourceBundle;

public class ContractCompilerTaskListener implements TaskListener {

    private final JavacTask task;

    public ContractCompilerTaskListener(JavacTask task) {
        this.task = task;
        var context = ((BasicJavacTask) task).getContext();
        var messages = JavacMessages.instance(context);
        var jsickoBundle = ResourceBundle.getBundle("jsicko");
        var resourceBundleHelper = new JavacMessages.ResourceBundleHelper() {
            @Override
            public ResourceBundle getResourceBundle(Locale locale) {
                return jsickoBundle;
            }
        };
        messages.add(resourceBundleHelper);

    }

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            e.getCompilationUnit().accept(new ContractCompilerTreeScanner((BasicJavacTask) task), new ArrayDeque<>());
        }

    }

}
