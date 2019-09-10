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

package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.Modifier;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the state of the contract compiler.
 *
 * The state of the contract compiler includes the currently instrumented contract class, methods, etc.
 */
class JSickoContractCompilerState {

    private final JavacUtils javac;

    private boolean _currentClassHasContract = false;

    private Optional<JCClassDecl> _currentClassDecl = Optional.empty();
    private Optional<JCMethodDecl> _currentMethodDecl = Optional.empty();
    private List<ConditionClause> _classInvariants = List.nil();

    private Optional<JCVariableDecl> _currentMethodReturnVarDecl = Optional.empty();
    private Optional<JCVariableDecl> _currentMethodRaisesVarDecl = Optional.empty();

    private Optional<JCVariableDecl> _optionalOldValuesTableField = Optional.empty();
    private Optional<JCVariableDecl> _optionalStaticOldValuesTableField = Optional.empty();
    private Optional<JCMethodDecl> _overriddenOldMethod = Optional.empty();
    private Optional<JCCompilationUnit> _currentCompilationUnitTree = Optional.empty();

    /**
     * Constructs a new state object.
     * @param task the base javac task.
     */
    JSickoContractCompilerState(BasicJavacTask task) {
        this.javac = new JavacUtils(task);
    }

    /**
     * Returns <code>true</code> iff the current class has a jSicko contract,
     * and thus it's being compiled.
     *
     * The value is cached for every class visited.
     *
     * @return <code>true</code> iff the current class has a jSicko contract.
     */
    public boolean currentClassHasContract() {
        return _currentClassHasContract;
    }

    /**
     * Enter a new compilation unit.
     * @param compilationUnitTree the current compilation unit.
     */
    void enterCompilationUnit(JCCompilationUnit compilationUnitTree) {
        this._currentCompilationUnitTree = Optional.of(compilationUnitTree);
    }

    /**
     * Exits a compilation unit.
     */
    void exitCompilationUnit() {
        this._currentCompilationUnitTree = Optional.empty();
    }

    /**
     * The current compilation unit, if present.
     * @return an optional compilation unit.
     */
    Optional<JCCompilationUnit> currentCompilationUnitTree() {
        return this._currentCompilationUnitTree;
    }

    /**
     * Checks if the state is currently visiting a compilation unit.
     * @return <code>true</code> iff the current compilation unit is present.
     */
    boolean isInCompilationUnit() {
        return this._currentCompilationUnitTree.isPresent();
    }

    /**
     * Enters a new class declaration.
     * @param classDecl the class declaration to enter.
     */
    void enterClassDecl(JCClassDecl classDecl) {
        this._currentClassDecl = Optional.of(classDecl);
        this._classInvariants = ConditionClause.createInvariants(javac.findInvariants(classDecl), javac);
        List<Type> contracts = retrieveContractTypes(classDecl.sym.type);
        this.logNote(classDecl.pos(),
                "Contract interfaces for class " + classDecl.getSimpleName() + ": " + contracts);

        this._currentClassHasContract = !classDecl.sym.type.isInterface() && !contracts.isEmpty();
    }

    /**
     * Exits the current class declaration.
     */
    void exitClassDecl() {
        this._currentClassDecl = Optional.empty();
        this._optionalOldValuesTableField = Optional.empty();
        this._optionalStaticOldValuesTableField = Optional.empty();
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
        this._overriddenOldMethod = Optional.empty();
        this._classInvariants = List.nil();
        this._currentClassHasContract = false;
    }

    /**
     * Returns the current class declaration, if present.
     * @return the currently visited class declaration, if present.
     */
    Optional<JCClassDecl> currentClassDecl() {
        return this._currentClassDecl;
    }

    /**
     * Executes some code iff the state is visiting a class declaration.
     *
     * @param consumer some code that consumes a class declaration.
     * @throws IllegalStateException iff the class declaration is absent.
     */
    public void ifClassDeclPresent(Consumer<? super JCClassDecl> consumer) {
        this._currentClassDecl.ifPresentOrElse(consumer,() -> new IllegalStateException("No current class declaration in jSicko compiler state"));
    }

    /**
     * Executes a function over the current class declaration.
     *
     * @param mapper a function from a class declaration to some type U.
     * @param <U> the return type of this method and the mapper function.
     * @return the result of <code>mapper</code> over the current class declaration.
     * @throws IllegalStateException iff the class declaration is absent.
     */
    public <U> U mapAndGetOnClassDecl(Function<? super JCClassDecl, ? extends U> mapper) {
        return this._currentClassDecl.map(mapper).orElseThrow(() -> new IllegalStateException("No current class declaration in jSicko compiler state"));
    }

    /**
     * Checks if a given method is the overridden old method.
     * @param otherMethodDecl a method delcaration.
     * @return <code>true</code> iff the passed method is the overridden old method.
     */
    boolean isOverriddenOldMethod(JCMethodDecl otherMethodDecl) {
        return otherMethodDecl.equals(this._overriddenOldMethod.get());
    }

    /**
     * Enters a method declaration.
     * @param methodDecl a method declaration.
     */
    void enterMethodDecl(JCMethodDecl methodDecl) {
        this._currentMethodDecl = Optional.of(methodDecl);
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
    }

    /**
     * Exits a method declaration.
     */
    void exitMethodDecl() {
        if (this._currentClassHasContract || this._currentMethodRaisesVarDecl.isPresent()) {
            this.logNote(this._currentMethodDecl.get().pos(),
                    "Code of Instrumented " + this._currentMethodDecl.get().sym + " method: " + this._currentMethodDecl.get().toString());
        }
        this._currentMethodDecl = Optional.empty();
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
    }

    /**
     * Finds all the overridden methods for current method.
     * @return the list of methods that have been overridden by the current method declaration, starting from the one on top of the hierarchy.
     */
    List<Symbol> findOverriddenMethodsOfCurrentMethod() {
        var overriddenMethods = javac.findOverriddenMethods(this._currentClassDecl.get(), this._currentMethodDecl.get()).reverse();
        var currentMethodSymbol = this._currentMethodDecl.get().sym;
        if (!overriddenMethods.contains(currentMethodSymbol))
            overriddenMethods.append(currentMethodSymbol);
        return overriddenMethods;
    }

    /**
     * Checks all the criteria that determine if the currently visited method should be instrumented.
     *
     * @return <code>true</code> iff the method should be instrumented.
     */
    boolean currentMethodShouldBeInstrumented() {
        return mapAndGetOnMethodDecl((JCMethodDecl methodDecl) -> this.currentClassHasContract() &&
                this.isInCompilationUnit() &&
                !this.isOverriddenOldMethod(methodDecl) &&
                methodDecl.getModifiers().getFlags().contains(Modifier.PUBLIC) &&
                !methodDecl.getModifiers().getFlags().contains(Modifier.ABSTRACT));
    }

    /**
     * Executes a function over the current method declaration.
     *
     * @param mapper a function from a method declaration to some type U.
     * @param <U> the return type of this method and the mapper function.
     * @return the result of <code>mapper</code> over the current method declaration.
     * @throws IllegalStateException iff the method declaration is absent.
     */
    public <U> U mapAndGetOnMethodDecl(Function<? super JCMethodDecl, ? extends U> mapper) {
        return this._currentMethodDecl.map(mapper).orElseThrow(() -> new IllegalStateException("No current method declaration in jSicko compiler state"));
    }

    /**
     * Executes some code consuming the current method declaration.
     *
     * @param consumer a consumer of the current method declaration.
     * @throws IllegalStateException iff the method declaration is absent.
     */
    public void ifMethodDeclPresent(Consumer<? super JCMethodDecl> consumer) {
        this._currentMethodDecl.ifPresentOrElse(consumer,() -> new IllegalStateException("No current method declaration in jSicko compiler state"));
    }

    /**
     * Returns the current method declaration, if present.
     * @return the current method declaration, if present.
     */
    public Optional<JCVariableDecl> currentMethodReturnVarDecl() {
        return this._currentMethodReturnVarDecl;
    }

    /**
     * Returns the old values table field, if present.
     * @return the old values table field declaration, if present.
     */
    Optional<JCVariableDecl> optionalOldValuesTableField() {
        return this._optionalOldValuesTableField;
    }


    /**
     * Returns the old values table instance field or the static one, depending on the currently instrumented method.
     * @return the requested old values table field declaration.
     */
    JCVariableDecl oldValuesTableFieldDeclByMethodType() {
        return (this._currentMethodDecl.get().sym.isStatic() ?
                this._optionalStaticOldValuesTableField.get() :
                this._optionalOldValuesTableField.get());
    }

    /**
     * Overrides the old method.
     * @param overriddenOldMethod the method that overrides the default old declaration.
     */
    void overrideOldMethod(JCMethodDecl overriddenOldMethod) {
        var classDecl = this._currentClassDecl.get();
        classDecl.defs = classDecl.defs.prepend(overriddenOldMethod);
        classDecl.sym.members().enter(overriddenOldMethod.sym);
        this._overriddenOldMethod = Optional.of(overriddenOldMethod);
        this.logNote(null, "Code of overridden old method " + overriddenOldMethod);
    }

    /**
     * Appends the old values table field to the currently visited class
     * @param varDef the field to append.
     * @param isTheStaticOne iff the passed one is the static one.
     */
    void appendOldValuesTableField(JCVariableDecl varDef, boolean isTheStaticOne) {
        if (isTheStaticOne)
            this._optionalStaticOldValuesTableField = Optional.of(varDef);
        else
            this._optionalOldValuesTableField = Optional.of(varDef);
        this._currentClassDecl.get().defs = this._currentClassDecl.get().defs.prepend(varDef);
        this._currentClassDecl.get().sym.members().enter(varDef.sym);
    }

    /**
     * Returns the raises synthetic variable of the currently visited method.
     * @return the declaration of the raises variable.
     */
    Optional<JCVariableDecl> currentMethodRaisesVarDecl() {
        return this._currentMethodRaisesVarDecl;
    }

    /**
     * Sets the returns synthetic variable of the currently visited method.
     * @param varDef the returns variable declaration.
     */
    void setCurrentMethodReturnVarDecl(JCVariableDecl varDef) {
        this._currentMethodReturnVarDecl = Optional.of(varDef);
    }

    /**
     * Sets the raises synthetic variable of the currently visited method.
     * @param varDef the raises variable declaration.
     */
    void setCurrentMethodRaisesVarDecl(JCVariableDecl varDef) {
        this._currentMethodRaisesVarDecl = Optional.of(varDef);
    }

    /**
     * Returns the invariants in the currently visited class.
     * @return the list of invariant clauses in the currently visited class.
     */
    List<ConditionClause> classInvariants() {
        return this._classInvariants;
    }

    /**
     * Logs a compiler note in the source file of the currently visited compilation unit.
     * @param pos the position of the note.
     * @param message the message to log.
     */
    void logNote(JCDiagnostic.DiagnosticPosition pos, String message) {
        javac.logNote(this._currentCompilationUnitTree.get().getSourceFile(),
                pos,
                message);
    }

    /**
     * Logs a compiler error in the source file of the currently visited compilation unit
     * @param pos the position of the note.
     * @param message the error message to log.
     */
    void logError(JCDiagnostic.DiagnosticPosition pos, String message) {
        javac.logError(this._currentCompilationUnitTree.get().getSourceFile(),
                pos,
                message);
    }

    /**
     * Checks if the passed tree is the currently visited method.
     * @param tree a tree.
     * @return <code>true</code> iff the passed tree is the currently visited method declaration.
     */
    public boolean isCurrentMethodDecl(Tree tree) {
        return tree.equals(this._currentMethodDecl.get());
    }

    /**
     * Retrieves the contract types for a given type t.
     *
     * The jSicko contract types for a type t are the inherited/extended types that extend Contract.
     * @param t any type.
     * @return a list of types that extend Contract and that are (indirectly) inherited by t.
     */
    private List<Type> retrieveContractTypes(Type t) {
        var contractTypeName = javac.Name(Contract.class.getCanonicalName());
        var closure = javac.typeClosure(t);

        var contractType = closure.stream().filter((Type closureElem) -> {
            var rawType = javac.typeErasure(closureElem);
            return rawType.asElement().getQualifiedName().equals(contractTypeName);
        }).map((Type likelyContractType) -> javac.typeErasure(likelyContractType)).findFirst();

        if (contractType.isPresent()) {
            return closure.stream().filter((Type closureElem) -> {
                var rawType = javac.typeErasure(closureElem);
                return rawType.isInterface() && javac.isTypeAssignable(rawType, contractType.get());
            }).collect(List.collector());
        } else {
            return List.nil();
        }
    }
}
