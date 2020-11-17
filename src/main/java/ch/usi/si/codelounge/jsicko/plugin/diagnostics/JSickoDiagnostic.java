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

package ch.usi.si.codelounge.jsicko.plugin.diagnostics;

import ch.usi.si.codelounge.jsicko.plugin.ConditionClause;
import ch.usi.si.codelounge.jsicko.plugin.ContractConditionEnum;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.function.Function;

/**
 * Holder for JSicko Diagnostic classes and
 * utility methods.
 *
 * A diagnostic is a note, warning, or error
 * emitted by JSicko during instrumentation.
 */
public abstract class JSickoDiagnostic {

    /**
     * A JSicko error.
     *
     * It holds a standard javac
     * diagnostic error that can be emitted / logged
     * during compilation.
     */
    public static class JSickoError extends JSickoDiagnostic {

        private final JCDiagnostic.Error jcError;

        JSickoError(String key, Object... args) {
            this.jcError = new JCDiagnostic.Error("jsicko", key, args);
        }

        public JCDiagnostic.Error jcError() {
            return this.jcError;
        }

    }

    /**
     * A JSicko warning.
     *
     * It holds a standard javac diagnostic warning that can be
     * emitted / logged during compilation.
     */
    public static class JSickoWarning extends JSickoDiagnostic {
        private final JCDiagnostic.Warning jcWarning;

        JSickoWarning(String key, Object... args) {
            this.jcWarning = new JCDiagnostic.Warning("jsicko", key, args);
        }

        public JCDiagnostic.Warning jcWarning() {
            return this.jcWarning;
        }

    }


    /**
     * A JSicko note.
     *
     * Used to emit code of instrumented methods, classes, and
     * in general logs of what happens during JSicko execution.
     * It holds a standard javac diagnostic note that can be
     * emitted / logged during compilation.
     */
    public static class JSickoNote extends JSickoDiagnostic {
        private final JCDiagnostic.Note jcNote;

        JSickoNote(String key, Object... args) {
            this.jcNote = new JCDiagnostic.Note("jsicko", key, args);
        }

        public JCDiagnostic.Note jcNote() {
            return this.jcNote;
        }

    }

    private static Function<Boolean,String> staticRep = (Boolean isStatic) -> isStatic ? "static": "non-static";

    /**
     * Generates a JSicko missing clause error.
     *
     * A missing clause error represent the usage of a clause in a precondition
     * or postcondition that has no corresponding method.
     *
     * @param clause the missing clause.
     * @return a JSickoError representing the diagnostic to be emitted.
     */
    public static JSickoError MissingClause(ConditionClause clause) {
        return new JSickoError("missing.clause", clause.getClauseRep());
    }

    public static JSickoError MissingParamName(Name paramName, ConditionClause clause) {
        return new JSickoError("missing.param.name", paramName.toString(), clause.getResolvedClauseRepWithNames());
    }

    public static JSickoError WrongParamType(Name paramName, Type expectedType, Type clauseType, ConditionClause clause) {
        return new JSickoError("wrong.param.type", paramName.toString(), String.valueOf(expectedType), String.valueOf(clauseType), clause.getResolvedClauseRepWithNames());
    }

    public static JSickoError ReturnsOnVoidMethod(ConditionClause clause) {
        return new JSickoError("returns.on.void.method", clause.getResolvedClauseRepWithNames());
    }

    public static JSickoError ReturnsOnPrecondition(ConditionClause clause) {
        return new JSickoError("returns.on.precondition", clause.getResolvedClauseRepWithNames());
    }

    public static JSickoError RaisesOnPrecondition(ConditionClause clause) {
        return new JSickoError("raises.on.precondition", clause.getResolvedClauseRepWithNames());
    }

    public static JSickoError InvariantHasNonZeroArity(JCTree.JCMethodDecl methodDeclMarkedAsInvariant) {
        return new JSickoError("invariant.non.zero.arity", methodDeclMarkedAsInvariant.getName().toString());
    }

    public static JSickoError InvariantIsStatic(JCTree.JCMethodDecl methodDeclMarkedAsInvariant) {
        return new JSickoError("invariant.is.static", methodDeclMarkedAsInvariant.getName().toString());
    }

    public static JSickoError InvariantIsNotBoolean(JCTree.JCMethodDecl methodDeclMarkedAsInvariant) {
        return new JSickoError("invariant.is.not.boolean", methodDeclMarkedAsInvariant.getName().toString(), String.valueOf(methodDeclMarkedAsInvariant.getReturnType()));
    }

    public static JSickoError ClauseIsNotBoolean(ConditionClause clause, Symbol.MethodSymbol methodSymbolMarkedAsClause) {
        return new JSickoError("clause.is.not.boolean", String.valueOf(clause), methodSymbolMarkedAsClause.name.toString(), String.valueOf(methodSymbolMarkedAsClause.getReturnType()));
    }

    public static JSickoError IncompatibleClause(ConditionClause clause, boolean isMethodStatic, Name methodName) {
        return new JSickoError("incompatible.clause", staticRep.apply(clause.isClauseMethodStatic()), String.valueOf(clause), staticRep.apply(isMethodStatic), String.valueOf(methodName));
    }

    public static JSickoNote InstrumentedMethodNote(JCTree.JCMethodDecl jcMethodDecl) {
        return new JSickoNote("instrumented.method", jcMethodDecl.sym.toString(), jcMethodDecl.toString());
    }

    public static JSickoNote InstrumentedClassNote(JCTree.JCClassDecl jcClassDecl) {
        return new JSickoNote("instrumented.class", jcClassDecl.sym.toString(), jcClassDecl.toString());
    }

    public static JSickoNote ContractInterfacesNote(Symbol.ClassSymbol sym, List<Type> contracts) {
        return new JSickoNote("contract.interfaces", sym.toString(), contracts.map(t -> t.toString()).toString(", "));
    }

    public static JSickoNote ConditionCheckNote(Symbol.MethodSymbol sym, ContractConditionEnum conditionType, List<List<ConditionClause>> groupedClauses) {
        return new JSickoNote("condition.checks", sym.toString(), conditionType.toString().toLowerCase(), groupedClauses.map(l -> l.map(c -> c.getMethodName()).toString(", ")).toString("; "));
    }

    public static JSickoNote OverriddenOldMethodNote(JCTree.JCMethodDecl overriddenOldMethod) {
        return new JSickoNote("overridden.old.method", overriddenOldMethod.toString());
    }


}
