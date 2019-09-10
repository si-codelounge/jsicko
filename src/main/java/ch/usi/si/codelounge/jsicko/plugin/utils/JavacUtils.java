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

package ch.usi.si.codelounge.jsicko.plugin.utils;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.plugin.OldValuesTable;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;

public final class JavacUtils {

    private final Types types;
    private final Symtab symtab;
    private final Names symbolsTable;
    private final TreeMaker factory;
    private final Log log;
    private final JCDiagnostic.Factory diagnosticFactory;
    private final Type _throwableType;
    private final Type _exceptionType;
    private final Type _runtimeExceptionType;

    private final Symbol javaUtilCollectionIteratorMethodSymbol;

    public JavacUtils(BasicJavacTask task) {
        this.symbolsTable = Names.instance(task.getContext());
        this.types = Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.symtab = Symtab.instance(task.getContext());
        this.log = Log.instance(task.getContext());
        this.diagnosticFactory = JCDiagnostic.Factory.instance(task.getContext());
        this.javaUtilCollectionIteratorMethodSymbol = retrieveMemberFromClassByName(Collection.class.getCanonicalName(), "iterator");
        this._throwableType = retrieveClassSymbol(Throwable.class.getCanonicalName()).type;
        this._exceptionType = retrieveClassSymbol(Exception.class.getCanonicalName()).type;
        this._runtimeExceptionType = retrieveClassSymbol(RuntimeException.class.getCanonicalName()).type;
    }

    public JCExpression Expression(String... identifiers) {
        var nameList = Arrays.stream(identifiers).map((String string) -> symbolsTable.fromString(string));

        var expr = nameList.reduce(null, (JCExpression firstElem, Name name) -> {
            if (firstElem == null)
                return factory.Ident(name);
            else
                return factory.Select(firstElem, name);
        }, (JCExpression firstElem, JCExpression name) -> name);

        return expr;
    }

    public TreeMaker getFactory() {
        return this.factory;
    }

    public JCExpression Expression(String qualifiedName) {
        String[] splitQualifiedName = qualifiedName.split("\\.");
        return Expression(splitQualifiedName);
    }

    public List<Type> typeClosure(Type t) {
        return this.types.closure(t);
    }

    public Type typeErasure(Type t) {
        return t.asElement().erasure(types);
    }

    public Name Name(String name) {
        return this.symbolsTable.fromString(name);
    }

    public JCStatement MethodCall(JCExpression baseExpression, Name methodName, List<JCExpression> args) {
        return factory.Exec(MethodInvocation(baseExpression, methodName, args));
    }

    public JCStatement MethodCall(JCExpression baseExpression, Name methodName) {
        return MethodCall(baseExpression, methodName, List.nil());
    }

    public JCMethodInvocation MethodInvocation(JCExpression baseExpression, Name methodName, List<JCExpression> args) {
        var selector = factory.Select(baseExpression, methodName);
        return factory.Apply(List.nil(), selector, args);
    }

    public JCMethodInvocation MethodInvocation(JCExpression baseExpression, Name methodName) {
        return MethodInvocation(baseExpression, methodName, List.nil());
    }


    public boolean isTypeAssignable(Type t, Type s) {
        return types.isAssignable(t, s);
    }

    public List<Symbol> findOverriddenMethods(JCClassDecl classDecl, JCMethodDecl methodDecl) {
        var methodSymbol = methodDecl.sym;
        var thisTypeClosure = this.typeClosure(classDecl.sym.type);

        return thisTypeClosure.stream().flatMap((Type contractType) -> {
            Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> methodSymbol.getQualifiedName().equals(contractElement.name) &&
                    methodSymbol.overrides(contractElement, contractType.tsym, types, true, true));
            return contractOverriddenSymbols;
        }).collect(List.collector());
    }

    public boolean hasVoidReturnType(JCMethodDecl methodDecl) {
        var methodReturnType = methodDecl.getReturnType();
        return (methodReturnType instanceof JCPrimitiveTypeTree) && ((JCPrimitiveTypeTree) methodReturnType).typetag.equals(TypeTag.VOID);
    }

    public JCLiteral zeroValue(Type t) {
        return t.isPrimitive() ?
                factory.Literal(t.getTag(),0)
                : nullLiteral();
    }

    public JCLiteral nullLiteral() {
        return factory.Literal(TypeTag.BOT,null);
    }

    public List<MethodSymbol> findInvariants(JCClassDecl classDecl) {
        var thisTypeClosure = this.typeClosure(classDecl.sym.type);

        return thisTypeClosure.stream().flatMap((Type contractType) -> {
            Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                return (contractElement instanceof MethodSymbol) && contractElement.getAnnotation(Contract.Invariant.class) != null;
            });
            return contractOverriddenSymbols.map((Symbol s) -> (MethodSymbol) s);
        }).collect(List.collector());
    }

    public boolean isSuperOrThisConstructorCall(JCStatement head) {
        return head instanceof JCExpressionStatement &&
                ((JCExpressionStatement)head).expr instanceof JCMethodInvocation &&
                ((JCMethodInvocation)((JCExpressionStatement)head).expr).meth instanceof JCIdent &&
                (((JCIdent) ((JCMethodInvocation)((JCExpressionStatement)head).expr).meth).name.equals(symbolsTable._super) ||
                ((JCIdent) ((JCMethodInvocation)((JCExpressionStatement)head).expr).meth).name.equals(symbolsTable._this));
    }


    public Type oldValuesTableClassType() {
        ClassSymbol mapClassSymbol = symtab.getClassesForName(this.Name(OldValuesTable.class.getCanonicalName())).iterator().next();

        return new Type.ClassType(
                Type.noType,
                List.nil(),
                mapClassSymbol, TypeMetadata.EMPTY);

    }

    public Type preconditionCheckerType() {
        ClassSymbol mapClassSymbol = symtab.getClassesForName(this.Name(ConditionChecker.class.getCanonicalName())).iterator().next();

        return new Type.ClassType(
                Type.noType,
                List.nil(),
                mapClassSymbol, TypeMetadata.EMPTY);
    }

    public Type.TypeVar freshObjectTypeVar(Symbol owner) {
        var typeVar = new Type.TypeVar(symbolsTable.fromString("X"),owner,symtab.botType);
        typeVar.bound = symtab.objectType;
        return typeVar;
    }

    public JCExpression oldValuesTableTypeExpression() {
        var mapTypeIdent = this.Expression(OldValuesTable.class.getCanonicalName());
        return mapTypeIdent;
    }

    public Type stringType() {
        return symtab.stringType;
    }

    public Type throwableType() {
        return _throwableType;
    }

    public Type exceptionType() {
        return _exceptionType;
    }

    public Type runtimeExceptionType() {
        return _runtimeExceptionType;
    }

    public void logError(JavaFileObject fileObject, JCDiagnostic.DiagnosticPosition pos, String message) {
        var diagnosticError = diagnosticFactory.error(JCDiagnostic.DiagnosticFlag.MANDATORY,
                new DiagnosticSource(fileObject,log), pos, new JCDiagnostic.Error("compiler", "proc.messager", message));
        log.report(diagnosticError);
    }

    public void logWarning(JCDiagnostic.DiagnosticPosition pos, String message) {
        log.mandatoryWarning(pos, new JCDiagnostic.Warning("compiler", "proc.messager", message));
    }

    public void logNote(JavaFileObject fileObject, JCDiagnostic.DiagnosticPosition pos, String message) {
        var sourcedDiagnosticNote = diagnosticFactory.create(null, EnumSet.of(JCDiagnostic.DiagnosticFlag.MANDATORY), new DiagnosticSource(fileObject,log), pos, new JCDiagnostic.Note("compiler", "proc.messager", "[jSicko] " + message));
        log.report(sourcedDiagnosticNote);
    }

    private Symbol retrieveMemberFromClassByName(String qualifiedClassName, String methodName) {
        ClassSymbol mapClassSymbol = retrieveClassSymbol(qualifiedClassName);
        return mapClassSymbol.members().findFirst(symbolsTable.fromString(methodName));
    }

    private ClassSymbol retrieveClassSymbol(String qualifiedClassName) {
        return symtab.getClassesForName(this.Name(qualifiedClassName)).iterator().next();
    }

    public Symbol getJavaUtilCollectionIteratorMethodSymbol() {
        return this.javaUtilCollectionIteratorMethodSymbol;
    }

    /**
     * Given a list of exception types from a method throws declarations, derives the most
     * general exception type thrown that can be safely rethrown according to Java's
     * improved type checking analysis of rethrown exceptions.
     *
     * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html">Official Oracle documentation.</a>
     *
     * @param throwsList a list of expressions corresponding to a throws clause in a method declaration.
     * @return one type among {@link java.lang.Throwable}, {@link java.lang.Exception}, and {@link java.lang.RuntimeException}.
     */
    public Type deriveMostGeneralExceptionTypeThrown(List<JCExpression> throwsList) {
        for(JCExpression throwClause: throwsList) {
            if (!this.types.isAssignable(throwClause.type,this._runtimeExceptionType) &&
                    !this.types.isAssignable(throwClause.type,this._exceptionType))
                return _throwableType;
            else if (!this.types.isAssignable(throwClause.type,this._runtimeExceptionType)) {
                return _exceptionType;
            }
        }
        return _runtimeExceptionType;
    }
}
