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
import ch.usi.si.codelounge.jsicko.plugin.diagnostics.JSickoDiagnostic;
import com.google.common.collect.Streams;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.Attr;
import  com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

public final class JavacUtils {

    private final Types types;
    private final Symtab symtab;
    private final Names symbolsTable;
    private final TreeMaker factory;
    private final TransTypes transTypes;
    private final Attr attr;

    private final Log log;
    private final JCDiagnostic.Factory diagnosticFactory;
    private final Type _throwableType;
    private final Type _exceptionType;
    private final Type _runtimeExceptionType;
    private final Type _objectType;

    private final Symbol javaUtilCollectionIteratorMethodSymbol;

    private final ModuleSymbol _unnamedModule;
    private final ModuleSymbol _javaBaseModule;

    public JavacUtils(BasicJavacTask task) {
        this.symbolsTable = Names.instance(task.getContext());
        this.types = Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.symtab = Symtab.instance(task.getContext());
        this.log = Log.instance(task.getContext());
        this.diagnosticFactory = JCDiagnostic.Factory.instance(task.getContext());

        this.transTypes = TransTypes.instance(task.getContext());
        this.attr = Attr.instance(task.getContext());

        var javaUtilCollectioniteratorMethodSymbol = retrieveMemberFromClassByName(symtab.java_base, Collection.class.getCanonicalName(), "iterator");

        this._unnamedModule = symtab.unnamedModule;
        this._javaBaseModule = symtab.java_base;

        if (!javaUtilCollectioniteratorMethodSymbol.isPresent()) {
            throw new IllegalStateException("JSicko Fatal Error: missing java.util.Collection symbol.");
        }

        this.javaUtilCollectionIteratorMethodSymbol = javaUtilCollectioniteratorMethodSymbol.get();
        this._throwableType = retrieveClassSymbol(symtab.java_base, Throwable.class.getCanonicalName()).get().type;
        this._exceptionType = retrieveClassSymbol(symtab.java_base, Exception.class.getCanonicalName()).get().type;
        this._runtimeExceptionType = retrieveClassSymbol(symtab.java_base, RuntimeException.class.getCanonicalName()).get().type;
        this._objectType = retrieveClassSymbol(symtab.java_base, Object.class.getCanonicalName()).get().type;
    }

    public TreeMaker getFactory() {
        return this.factory;
    }

    public JCExpression Expression(ModuleSymbol moduleSymbol, String classQualifiedName) {
        Type classType = this.typeErasure(this.retrieveType(moduleSymbol, classQualifiedName));
        return factory.Type(classType);
    }

    public JCExpression Expression(ModuleSymbol moduleSymbol, String classQualifiedName, String methodName) {
        Type classType = this.retrieveType(moduleSymbol, classQualifiedName);
        Symbol method = this.retrieveMemberFromClassByName(moduleSymbol, classQualifiedName, methodName).get();
        return factory.Select(factory.Type(classType), method);
    }

    public JCExpression Erroneous() {
        return factory.Erroneous();
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

    public ModuleSymbol unnamedModule() {
        return this._unnamedModule;
    }

    public ModuleSymbol javaBaseModule() {
        return this._javaBaseModule;
    }

    public JCStatement MethodCall(ModuleSymbol moduleSymbol, JCExpression baseExpression, Name methodName, List<JCExpression> args) {
        return factory.Exec(MethodInvocation(moduleSymbol, baseExpression, methodName, args));
    }

    public JCStatement MethodCall(ModuleSymbol moduleSymbol, JCExpression baseExpression, Name methodName) {
        return MethodCall(moduleSymbol, baseExpression, methodName, List.nil());
    }

    public JCMethodInvocation MethodInvocation(ModuleSymbol moduleSymbol, JCExpression baseExpression, Name methodName, List<JCExpression> args) {
        MethodSymbol sym = (MethodSymbol) this.retrieveMemberFromClassByName(moduleSymbol,
                baseExpression.type.tsym.getQualifiedName().toString(),
                methodName.toString(),
                args.map(a -> a.type)
        ).get();

        var selector = factory.Select(baseExpression, sym);
        var apply = factory.Apply(List.nil(), selector, args);
        if (sym.isVarArgs()) {
            var paramType =  sym.params.head.type;
            apply.varargsElement = ((Type.ArrayType) paramType).getComponentType();
        }
        return apply.setType(selector.type.getReturnType());
    }

    public JCMethodInvocation MethodInvocation(ModuleSymbol moduleSymbol, JCExpression baseExpression, Name methodName) {
        return MethodInvocation(moduleSymbol, baseExpression, methodName, List.nil());
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
        if (t.equals(symtab.intType) || t.equals(symtab.charType) || t.equals(symtab.byteType) || t.equals(symtab.longType)) {
            return factory.Literal(t.getTag(), 0);
        }
        if (t.equals(symtab.booleanType)) {
            return factory.Literal(t.getTag(), 0);
        }
        if (t.equals(symtab.doubleType)) {
            return factory.Literal(t.getTag(), 0.0d);
        }
        if (t.equals(symtab.floatType)) {
            return factory.Literal(t.getTag(), 0.0f);
        }

        return nullLiteral();
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
        var typeSymbol = new TypeVariableSymbol(0, symbolsTable.fromString("X"), null, owner);
        var typeVar = new Type.TypeVar(typeSymbol,symtab.objectType,symtab.botType);
        typeSymbol.type = typeVar;
        return typeVar;
    }

    public JCExpression oldValuesTableTypeExpression() {
        var mapTypeIdent = this.retrieveType(symtab.noModule, OldValuesTable.class.getCanonicalName());
        return factory.Type(mapTypeIdent);
    }

    public JCExpression Type(Type tpe) {
        return factory.Type(tpe);
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

    public Type objectType() { return _objectType; }

    public Type runtimeExceptionType() {
        return _runtimeExceptionType;
    }

    public void logError(JavaFileObject fileObject, JCDiagnostic.DiagnosticPosition pos, JSickoDiagnostic.JSickoError jsickoError) {
        var diagnosticError = diagnosticFactory.error(JCDiagnostic.DiagnosticFlag.MANDATORY,
                new DiagnosticSource(fileObject,log),
                pos,
                jsickoError.jcError());
        log.report(diagnosticError);
    }

    public void logWarning(JCDiagnostic.DiagnosticPosition pos, JSickoDiagnostic.JSickoWarning warning) {
        log.mandatoryWarning(pos, warning.jcWarning());
    }

    public void logNote(JavaFileObject fileObject, JCDiagnostic.DiagnosticPosition pos, JSickoDiagnostic.JSickoNote note) {
        var sourcedDiagnosticNote = diagnosticFactory.create(null, EnumSet.of(JCDiagnostic.DiagnosticFlag.MANDATORY),
                new DiagnosticSource(fileObject,log),
                pos,
                note.jcNote());
        log.report(sourcedDiagnosticNote);
    }

    public Optional<Symbol> retrieveMemberFromClassByName(ModuleSymbol moduleSymbol, String qualifiedClassName, String methodName) {
        Optional<ClassSymbol> mapClassSymbol = retrieveClassSymbol(moduleSymbol, qualifiedClassName);
        var local = mapClassSymbol.map(s -> s.members().findFirst(symbolsTable.fromString(methodName)));
        if (!local.isPresent() && mapClassSymbol.isPresent() && mapClassSymbol.get().isInner()) {
            var inner = mapClassSymbol.get().owner.members().findFirst(symbolsTable.fromString(methodName));
            return Optional.of(inner);
        } else
        return local;
    }

    private Optional<Symbol> retrieveMemberFromClassByName(ModuleSymbol moduleSymbol, String qualifiedClassName, String methodName, List<Type> types) {
        Optional<ClassSymbol> mapClassSymbol = retrieveClassSymbol(moduleSymbol, qualifiedClassName);

        var local = mapClassSymbol.map(s -> s.members().findFirst(symbolsTable.fromString(methodName),
            t -> {
                if (!(t instanceof MethodSymbol))
                    return false;

                var ms = (MethodSymbol)t;

                // TODO: REFINE
                if (ms.isVarArgs())
                    return true;

                if (ms.params.length() != types.length())
                    return false;

                var msts = ms.params.map(x -> x.type).stream();
                var pts = types.stream();

                var matches = Streams.zip(msts,pts, (t1, t2) -> {
                   return this.types.isAssignable(t2,t1);
                });

                return matches.allMatch(b -> b.booleanValue());
            }
        )).or(() -> mapClassSymbol.map(s -> s.members().findFirst(symbolsTable.fromString(methodName))));

        if (!local.isPresent() && mapClassSymbol.isPresent() && mapClassSymbol.get().isInner()) {
            var inner = mapClassSymbol.get().owner.members().findFirst(symbolsTable.fromString(methodName));
            return Optional.of(inner);
        } else
            return local;
    }

    private Optional<ClassSymbol> retrieveClassSymbol(ModuleSymbol moduleSymbol, String qualifiedClassName) {
        var symbols = symtab.getClassesForName(this.Name(qualifiedClassName)).iterator();
        if (symbols.hasNext()) {
            var symbol = symbols.next();
            return Optional.of(symbol);
        } else {
            var inBase = symtab.enterClass(moduleSymbol, symbolsTable.fromString(qualifiedClassName));
            return Optional.of(inBase);
        }
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
        for (JCExpression throwClause : throwsList) {
            if (!this.types.isAssignable(throwClause.type, this._runtimeExceptionType) &&
                    !this.types.isAssignable(throwClause.type, this._exceptionType))
                return _throwableType;
            else if (!this.types.isAssignable(throwClause.type, this._runtimeExceptionType)) {
                return _exceptionType;
            }
        }
        return _runtimeExceptionType;
    }

    public Type retrieveType(ModuleSymbol moduleSymbol, String canonicalName) {
        return retrieveClassSymbol(moduleSymbol, canonicalName).get().type;
    }

    public Symbol retrieveConstructor(ModuleSymbol moduleSymbol, String canonicalName) {
        return retrieveClassSymbol(moduleSymbol, canonicalName).get().members().findFirst(symbolsTable.fromString("<init>"));
    }

    public Symbol retrieveConstructor(ModuleSymbol moduleSymbol, String canonicalName, Type... argTypes) {
        return retrieveClassSymbol(moduleSymbol, canonicalName).get().members().findFirst(symbolsTable.fromString("<init>"),
                f -> {
                    if (!(f instanceof MethodSymbol))
                        return false;
                    var symbolParams = ((MethodSymbol) f).params();
                    if (argTypes.length != symbolParams.length())
                        return false;
                    for (int i = 0; i < symbolParams.length(); i++) {
                        var symbolParam = symbolParams.get(i);
                        var paramToCheck = argTypes[i];
                        if (!symbolParam.type.asElement().equals(paramToCheck.asElement())) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    public Symbol retrieveEmptyConstructor(ModuleSymbol moduleSymbol, String canonicalName) {
        return retrieveClassSymbol(moduleSymbol, canonicalName).get().members().findFirst(symbolsTable.fromString("<init>"),
                f -> f instanceof MethodSymbol && ((MethodSymbol)f).params().length() == 0);
    }


    public void setOperator(JCUnary unaryOp) {
        unaryOp.operator = new OperatorSymbol.OperatorSymbol(this.Name("!"), symtab.booleanType, 257, symtab.noSymbol);
        unaryOp.type = booleanType();
    }

    public Type booleanType() {
        return symtab.booleanType;
    }

    public Type voidType() {
        return symtab.voidType;
    }

    public Type botType() {
        return symtab.botType;
    }

    public void setOperator(JCBinary binary) {
        Type.MethodType opType = new Type.MethodType(
                List.of(stringType(), stringType()), stringType(), List.nil(), symtab.methodClass);
        binary.operator = new OperatorSymbol.OperatorSymbol(this.Name("+"), opType, 256, symtab.noSymbol);
        binary.type = stringType();
    }

    public void visitLambda(JCLambda lambda) {
        var t = (VarSymbol) retrieveMemberFromClassByName(this.unnamedModule(), "ch.usi.si.codelounge.jsicko.plugin.utils.ConditionChecker","dummy").get();
        lambda.type = t.type;
        lambda.target = t.type;
    }

    public Type zeroType(Type t) {
        return t.isPrimitive() ?
                t
                : botType();
    }

    public JCExpression falseLiteral() {
        return zeroValue(booleanType());
    }

    public boolean isTypeAssignable(VarSymbol a, VarSymbol b) {
        var aType = a.type.isPrimitive() ? a.type : a.erasure(this.types);
        var bType = b.type.isPrimitive() ? b.type : b.erasure(this.types);
        return this.types.isAssignable(aType, bType);
    }

}
