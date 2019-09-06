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

import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class JSickoContractCompilerState {

    private final JavacUtils javac;

    private boolean _currentClassHasContract = false;

    private Optional<JCTree.JCClassDecl> _currentClassDecl = Optional.empty();
    private Optional<JCTree.JCMethodDecl> _currentMethodDecl = Optional.empty();
    private List<ConditionClause> _classInvariants = List.of();

    private Optional<JCTree.JCVariableDecl> _currentMethodReturnVarDecl = Optional.empty();
    private Optional<JCTree.JCVariableDecl> _currentMethodRaisesVarDecl = Optional.empty();

    private Optional<JCTree.JCVariableDecl> _optionalOldValuesTableField = Optional.empty();
    private Optional<JCTree.JCVariableDecl> _optionalStaticOldValuesTableField = Optional.empty();
    private Optional<JCTree.JCMethodDecl> _overriddenOldMethod = Optional.empty();
    private Optional<CompilationUnitTree> _currentCompilationUnitTree = Optional.empty();

    JSickoContractCompilerState(BasicJavacTask task) {
        this.javac = new JavacUtils(task);
    }

    public boolean currentClassHasContract() {
        return _currentClassHasContract;
    }

    void enterCompilationUnit(CompilationUnitTree compilationUnitTree) {
        this._currentCompilationUnitTree = Optional.of(compilationUnitTree);
    }

    void exitCompilationUnit() {
        this._currentCompilationUnitTree = Optional.empty();
    }

    Optional<CompilationUnitTree> currentCompilationUnitTree() {
        return this._currentCompilationUnitTree;
    }

    boolean isInCompilationUnit() {
        return this._currentCompilationUnitTree.isPresent();
    }

    void enterClassDecl(JCTree.JCClassDecl classDecl) {
        this._currentClassDecl = Optional.of(classDecl);
        this._classInvariants = ConditionClause.createInvariants(javac.findInvariants(classDecl), javac);
        List<Type> contracts = retrieveContractTypes(classDecl.sym.type);
        this.logNote(classDecl.pos(),
                "Contract interfaces for class " + classDecl.getSimpleName() + ": " + contracts);

        this._currentClassHasContract = !classDecl.sym.type.isInterface() && !contracts.isEmpty();
    }

    void exitClassDecl() {
        this._currentClassDecl = Optional.empty();
        this._optionalOldValuesTableField = Optional.empty();
        this._optionalStaticOldValuesTableField = Optional.empty();
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
        this._overriddenOldMethod = Optional.empty();
        this._classInvariants = List.of();
        this._currentClassHasContract = false;
    }

    Optional<JCTree.JCClassDecl> currentClassDecl() {
        return this._currentClassDecl;
    }

    public void ifClassDeclPresent(Consumer<? super JCTree.JCClassDecl> consumer) {
        this._currentClassDecl.ifPresentOrElse(consumer,() -> new IllegalStateException("No current class declaration in jSicko compiler state"));
    }

    public <U> U mapAndGetOnClassDecl(Function<? super JCTree.JCClassDecl, ? extends U> mapper) {
        return this._currentClassDecl.map(mapper).orElseThrow(() -> new IllegalStateException("No current class declaration in jSicko compiler state"));
    }

    boolean isOverriddenOldMethod(JCTree.JCMethodDecl otherMethodDecl) {
        return otherMethodDecl.equals(this._overriddenOldMethod.get());
    }

    void enterMethodDecl(JCTree.JCMethodDecl methodDecl) {
        this._currentMethodDecl = Optional.of(methodDecl);
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
    }

    void exitMethodDecl() {
        if (this._currentClassHasContract || this._currentMethodRaisesVarDecl.isPresent()) {
            this.logNote(this._currentMethodDecl.get().pos(),
                    "Code of Instrumented " + this._currentMethodDecl.get().sym + " method: " + this._currentMethodDecl.get().toString());
        }
        this._currentMethodDecl = Optional.empty();
        this._currentMethodReturnVarDecl = Optional.empty();
        this._currentMethodRaisesVarDecl = Optional.empty();
    }

    List<Symbol> findOverriddenMethodsOfCurrentMethod() {
        var overriddenMethods = javac.findOverriddenMethods(this._currentClassDecl.get(), this._currentMethodDecl.get());
        var currentMethodSymbol = this._currentMethodDecl.get().sym;
        if (!overriddenMethods.contains(currentMethodSymbol))
            overriddenMethods.add(currentMethodSymbol);
        return overriddenMethods;
    }

    boolean currentMethodShouldBeInstrumented() {
        return mapAndGetOnMethodDecl((JCTree.JCMethodDecl methodDecl) -> this.currentClassHasContract() &&
                this.isInCompilationUnit() &&
                !this.isOverriddenOldMethod(methodDecl) &&
                methodDecl.getModifiers().getFlags().contains(Modifier.PUBLIC) &&
                !methodDecl.getModifiers().getFlags().contains(Modifier.ABSTRACT));
    }

    public <U> U mapAndGetOnMethodDecl(Function<? super JCTree.JCMethodDecl, ? extends U> mapper) {
        return this._currentMethodDecl.map(mapper).orElseThrow(() -> new IllegalStateException("No current method declaration in jSicko compiler state"));
    }

    public void ifMethodDeclPresent(Consumer<? super JCTree.JCMethodDecl> consumer) {
        this._currentMethodDecl.ifPresentOrElse(consumer,() -> new IllegalStateException("No current method declaration in jSicko compiler state"));
    }

    Optional<JCTree.JCVariableDecl> optionalOldValuesTableField() {
        return this._optionalOldValuesTableField;
    }

    JCTree.JCVariableDecl oldValuesTableFieldDeclByMethodType() {
        return (this._currentMethodDecl.get().sym.isStatic() ?
                this._optionalStaticOldValuesTableField.get() :
                this._optionalOldValuesTableField.get());
    }


    public void overrideOldMethod(JCTree.JCMethodDecl overriddenOldMethod) {
        var classDecl = this._currentClassDecl.get();
        classDecl.defs = classDecl.defs.prepend(overriddenOldMethod);
        classDecl.sym.members().enter(overriddenOldMethod.sym);
        this._overriddenOldMethod = Optional.of(overriddenOldMethod);
        this.logNote(null, "Code of overridden old method " + overriddenOldMethod);
    }

    public Optional<JCTree.JCMethodDecl> overriddenOldMethod() {
        return this._overriddenOldMethod;
    }


    public void appendOldValuesTableField(JCTree.JCVariableDecl varDef, boolean isTheStaticOne) {
        if (isTheStaticOne)
            this._optionalStaticOldValuesTableField = Optional.of(varDef);
        else
            this._optionalOldValuesTableField = Optional.of(varDef);
        this._currentClassDecl.get().defs = this._currentClassDecl.get().defs.prepend(varDef);
        this._currentClassDecl.get().sym.members().enter(varDef.sym);
    }

    public Optional<JCTree.JCVariableDecl> currentMethodRaisesVarDecl() {
        return this._currentMethodRaisesVarDecl;
    }

    public void setCurrentMethodReturnVarDecl(JCTree.JCVariableDecl varDef) {
        this._currentMethodReturnVarDecl = Optional.of(varDef);
    }

    public void setCurrentMethodRaisesVarDecl(JCTree.JCVariableDecl varDef) {
        this._currentMethodRaisesVarDecl = Optional.of(varDef);
    }

    public List<ConditionClause> classInvariants() {
        return this._classInvariants;
    }


    void logNote(JCDiagnostic.DiagnosticPosition pos, String message) {
        javac.logNote(this._currentCompilationUnitTree.get().getSourceFile(),
                pos,
                message);
    }

    void logError(JCDiagnostic.DiagnosticPosition pos, String message) {
        javac.logError(this._currentCompilationUnitTree.get().getSourceFile(),
                pos,
                message);
    }

    public Optional<JCTree.JCVariableDecl> currentMethodReturnVarDecl() {
        return this._currentMethodReturnVarDecl;
    }

    public Optional<JCTree.JCMethodDecl> currentMethodDecl() {
        return this._currentMethodDecl;
    }

    public boolean isCurrentMethodDecl(Tree tree) {
        return tree.equals(this._currentMethodDecl.get());
    }

    private List<Type> retrieveContractTypes(Type t) {
        var contractTypeName = javac.nameFromString("ch.usi.si.codelounge.jsicko.Contract");
        var closure = javac.typeClosure(t);

        var contractType = closure.stream().filter((Type closureElem) -> {
            var rawType = javac.typeErasure(closureElem);
            return rawType.asElement().getQualifiedName().equals(contractTypeName);
        }).map((Type likelyContractType) -> javac.typeErasure(likelyContractType)).findFirst();

        if (contractType.isPresent()) {
            return closure.stream().filter((Type closureElem) -> {
                var rawType = javac.typeErasure(closureElem);
                return rawType.isInterface() && javac.isTypeAssignable(rawType, contractType.get());
            }).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

}
