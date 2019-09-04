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
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JavacUtils {

    private final Types types;
    private final Symtab symtab;
    private final Names symbolsTable;
    private final TreeMaker factory;
    private final Enter enter;
    private final MemberEnter memberEnter;
    private final Log log;
    private final JCDiagnostic.Factory diagnosticFactory;

    private final Symbol javaUtilCollectionIteratorMethodSymbol;

    public JavacUtils(BasicJavacTask task) {
        this.symbolsTable = Names.instance(task.getContext());
        this.types = Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.enter = Enter.instance(task.getContext());
        this.memberEnter = MemberEnter.instance(task.getContext());
        this.symtab = Symtab.instance(task.getContext());
        this.log = Log.instance(task.getContext());
        this.diagnosticFactory = JCDiagnostic.Factory.instance(task.getContext());
        this.javaUtilCollectionIteratorMethodSymbol = retrieveMemberFromClassByName("java.util.Collection", "iterator");
    }

    public JCTree.JCExpression constructExpression(String... identifiers) {
        var nameList = Arrays.stream(identifiers).map((String string) -> symbolsTable.fromString(string));

        var expr = nameList.reduce(null, (JCTree.JCExpression firstElem, Name name) -> {
            if (firstElem == null)
                return factory.Ident(name);
            else
                return factory.Select(firstElem, name);
        }, (JCTree.JCExpression firstElem, JCTree.JCExpression name) -> name);

        return expr;
    }

    public TreeMaker getFactory() {
        return this.factory;
    }

    public JCTree.JCExpression constructExpression(String qualifiedName) {
        String[] splitQualifiedName = qualifiedName.split("\\.");
        return constructExpression(splitQualifiedName);
    }

    public List<Type> typeClosure(Type t) {
        return this.types.closure(t);
    }

    public Type typeErasure(Type t) {
        return t.asElement().erasure(types);
    }

    public Name nameFromString(String name) {
        return this.symbolsTable.fromString(name);
    }

    public boolean isTypeAssignable(Type t, Type s) {
        return types.isAssignable(t, s);
    }

    public java.util.List<Symbol> findOverriddenMethods(JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl methodDecl) {
        var methodSymbol = methodDecl.sym;
        var thisTypeClosure = this.typeClosure(classDecl.sym.type);

        return thisTypeClosure.stream().flatMap((Type contractType) -> {
            Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                return methodSymbol.getQualifiedName().equals(contractElement.name) &&
                        methodSymbol.overrides(contractElement, contractType.tsym, types, true, true);
            });
            return contractOverriddenSymbols;
        }).collect(Collectors.toList());
    }

    public boolean hasVoidReturnType(MethodTree methodTree) {
        var methodReturnType = methodTree.getReturnType();
        return (methodReturnType instanceof JCTree.JCPrimitiveTypeTree) && ((JCTree.JCPrimitiveTypeTree) methodReturnType).typetag.equals(TypeTag.VOID);
    }

    public JCTree.JCLiteral zeroValue(Type t) {
        return t.isPrimitive() ?
                factory.Literal(t.getTag(),0)
                : factory.Literal(TypeTag.BOT,null);
    }

    public java.util.List<Symbol.MethodSymbol> findInvariants(JCTree.JCClassDecl classDecl) {
        var thisTypeClosure = this.typeClosure(classDecl.sym.type);

        return thisTypeClosure.stream().flatMap((Type contractType) -> {
            Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                return (contractElement instanceof Symbol.MethodSymbol) && contractElement.getAnnotation(Contract.Invariant.class) != null;
            });
            return contractOverriddenSymbols.map((Symbol s) -> (Symbol.MethodSymbol) s);
        }).collect(Collectors.toList());
    }

    public boolean isSuperOrThisConstructorCall(JCTree.JCStatement head) {
        return head instanceof JCTree.JCExpressionStatement &&
                ((JCTree.JCExpressionStatement)head).expr instanceof JCTree.JCMethodInvocation &&
                ((JCTree.JCMethodInvocation)((JCTree.JCExpressionStatement)head).expr).meth instanceof JCTree.JCIdent &&
                (((JCTree.JCIdent) ((JCTree.JCMethodInvocation)((JCTree.JCExpressionStatement)head).expr).meth).name.equals(symbolsTable._super) ||
                ((JCTree.JCIdent) ((JCTree.JCMethodInvocation)((JCTree.JCExpressionStatement)head).expr).meth).name.equals(symbolsTable._this));
    }


    public Type oldValuesTableClassType() {
        Symbol.ClassSymbol mapClassSymbol = symtab.getClassesForName(this.nameFromString("ch.usi.si.codelounge.jsicko.plugin.OldValuesTable")).iterator().next();

        return new Type.ClassType(
                Type.noType,
                List.nil(),
                mapClassSymbol, TypeMetadata.EMPTY);

    }

    public Symbol oldMethodType() {
        Symbol.ClassSymbol contractClassSymbol = symtab.getClassesForName(this.nameFromString("ch.usi.si.codelounge.jsicko.Contract")).iterator().next();
        var member = contractClassSymbol.members().getSymbolsByName(symbolsTable.fromString("old"));
        return member.iterator().next();
    }

    public Type.TypeVar freshObjectTypeVar(Symbol owner) {
        var typeVar = new Type.TypeVar(symbolsTable.fromString("X"),owner,symtab.botType);
        typeVar.bound = symtab.objectType;
        return typeVar;
    }

    public JCTree.JCExpression oldValuesTableTypeExpression() {
        var mapTypeIdent = this.constructExpression("ch.usi.si.codelounge.jsicko.plugin.OldValuesTable");
        return mapTypeIdent;
    }

    public Type stringType() {
        return symtab.stringType;
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
        Symbol.ClassSymbol mapClassSymbol = symtab.getClassesForName(this.nameFromString(qualifiedClassName)).iterator().next();
        var iteratorSymbol = mapClassSymbol.members().findFirst(symbolsTable.fromString(methodName));
        return iteratorSymbol;
    }

    public Symbol getJavaUtilCollectionIteratorMethodSymbol() {
        return this.javaUtilCollectionIteratorMethodSymbol;
    }

}
