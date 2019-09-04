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

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree.*;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ContractCompilerTreeScanner extends TreeScanner<Void, Deque<Tree>> {

    private final JavacUtils javac;

    private boolean hasContract;

    private Optional<JCVariableDecl> currentMethodReturnVarDecl = Optional.empty();
    private Optional<JCVariableDecl> optionalOldValuesTableField = Optional.empty();
    private Optional<JCVariableDecl> optionalStaticOldValuesTableField = Optional.empty();
    private Optional<JCMethodDecl> overriddenOldMethod = Optional.empty();
    private Optional<CompilationUnitTree> currentCompilationUnitTree = Optional.empty();
    private Optional<JCClassDecl> currentClassDecl = Optional.empty();
    private Optional<MethodTree> currentMethodTree = Optional.empty();
    private List<ConditionClause> classInvariants = List.of();

    public ContractCompilerTreeScanner(BasicJavacTask task) {
        this.javac = new JavacUtils(task);
        this.hasContract = false;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Deque<Tree> relevantScope) {
        this.currentCompilationUnitTree = Optional.of(node);
        return super.visitCompilationUnit(node, relevantScope);
    }

    @Override
    public Void visitClass(ClassTree classTree, Deque<Tree> relevantScope) {

        this.hasContract = false;

        this.currentCompilationUnitTree.ifPresent((CompilationUnitTree currentCompilationUnitTree) -> {

            var classDecl = (JCClassDecl) classTree;

            List<Type> contracts = retrieveContractTypes(classDecl.sym.type);
            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                    classDecl.pos(),
                    "Contract interfaces for class " + classTree.getSimpleName() + ": " + contracts);

            this.hasContract = !classDecl.sym.type.isInterface() && !contracts.isEmpty();

            if (hasContract) {
                this.currentClassDecl = Optional.of(classDecl);
                optionalDeclareOldVariableAndMethod(classDecl);
                this.classInvariants = ConditionClause.createInvariants(javac.findInvariants(this.currentClassDecl.get()), javac);
            } else {
                this.currentClassDecl = Optional.empty();
                this.optionalOldValuesTableField = Optional.empty();
                this.currentMethodReturnVarDecl = Optional.empty();
                this.overriddenOldMethod = Optional.empty();
                this.classInvariants = List.of();
            }

        });

        return super.visitClass(classTree, relevantScope);
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

    @Override
    public Void visitMethod(MethodTree methodTree, Deque<Tree> relevantScope) {
        var methodDecl = (JCMethodDecl) methodTree;

        this.currentMethodTree = Optional.of(methodTree);

        final List<Symbol> overriddenMethods;
        this.currentMethodReturnVarDecl = Optional.empty();

        if (this.hasContract &&
                currentCompilationUnitTree.isPresent() &&
                !methodDecl.equals(this.overriddenOldMethod.get()) &&
                methodTree.getModifiers().getFlags().contains(Modifier.PUBLIC) &&
                !methodTree.getModifiers().getFlags().contains(Modifier.ABSTRACT)) {

            var methodSymbol = methodDecl.sym;

            overriddenMethods = javac.findOverriddenMethods(this.currentClassDecl.get(), methodDecl);

            if (!overriddenMethods.contains(methodSymbol))
                overriddenMethods.add(methodSymbol);

            if (overriddenMethods.size() > 0) {
                Void w = null;
                JCTry tryBlock = null;

                List<Contract.Requires> requires = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Requires.class))).collect(Collectors.toList());
                List<Contract.Ensures> ensures = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Ensures.class))).collect(Collectors.toList());

                var isMarkedPure = isAnyMethodMarkedAsOrMustBePure(overriddenMethods);

                tryBlock = boxMethodBody(methodTree);
                optionalDeclareReturnValueCatcher(methodTree);

                if (!methodSymbol.isConstructor() && !isMarkedPure) {
                    optionalSaveOldState(methodTree, this.currentClassDecl.get());
                    addLeaveScopeStatement(tryBlock.finalizer, methodDecl.sym.isStatic());
                }

                addPreconditions(methodDecl, methodDecl.body, requires);

                relevantScope.push(methodTree);
                w = super.visitMethod(methodTree, relevantScope);
                relevantScope.pop();

                addPostconditions(methodDecl, tryBlock.finalizer, ensures);
                if (!isMarkedPure && !methodSymbol.isStatic()) {
                    addInvariants(methodDecl, tryBlock.finalizer);
                }
                javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                        methodDecl.pos(), "Code of Instrumented " + methodDecl.sym + " method: " + methodTree.toString());
                return w;
            }
        }

        relevantScope.push(methodTree);
        var w = super.visitMethod(methodTree, relevantScope);
        relevantScope.pop();
        return w;
    }

    private boolean isAnyMethodMarkedAsOrMustBePure(List<Symbol> methodSymbols) {
        return methodSymbols.stream().anyMatch((Symbol methodSymbol) -> isMarkedAsPureOrIsSpecialPureMethod(methodSymbol));
    }

    private boolean isMarkedAsPureOrIsSpecialPureMethod(Symbol symbol) {
        return isMarkedAsPure(symbol) || isSpecialPureMethod(symbol);
    }

    private boolean isMarkedAsPure(Symbol symbol) {
        return symbol.getAnnotationsByType(Contract.Pure.class).length > 0;
    }

    private boolean isSpecialPureMethod(Symbol symbol) {
        /**
         * Issue #13: java.util.Collection#iterator method is exploited by Kryo to serialize collections. 
         * jSicko must thus consider it as as pure.
         */
        var isIteratorMethod = symbol.equals(this.javac.getJavaUtilCollectionIteratorMethodSymbol());
        return isIteratorMethod;
    }

    private void addLeaveScopeStatement(JCBlock finalizer, boolean isStatic) {
        final var factory = javac.getFactory();
        JCMethodInvocation enterScopeStatement = buildLeaveScopeStatement(isStatic);
        finalizer.stats = finalizer.stats.prepend(factory.Exec(enterScopeStatement));
    }

    private void optionalSaveOldState(MethodTree methodTree, JCClassDecl classDecl) {
        final var factory = javac.getFactory();

        this.optionalOldValuesTableField.ifPresent((JCVariableDecl oldValuesTableField) -> {

            var methodDecl = (JCMethodDecl) methodTree;
            var oldValuesTableFieldDecl = (methodDecl.sym.isStatic() ? this.optionalStaticOldValuesTableField.get(): oldValuesTableField );

            if (!methodDecl.sym.isStatic()) {
                var thisType = factory.This(classDecl.sym.type);
                var kryoMethodExpr = javac.constructExpression("ch.usi.si.codelounge.jsicko.plugin.utils.CloneUtils.kryoClone");
                var cloneCall = factory.Apply(com.sun.tools.javac.util.List.nil(), kryoMethodExpr, com.sun.tools.javac.util.List.of(thisType));
                var literal = factory.Literal("this");
                var params = com.sun.tools.javac.util.List.of(literal, cloneCall);
                var methodSelect = factory.Select(factory.Ident(oldValuesTableFieldDecl), javac.nameFromString("putValue"));
                var updateCall = factory.Apply(com.sun.tools.javac.util.List.nil(), methodSelect, params);
                methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(factory.Exec(updateCall));
            }

            for (JCVariableDecl paramDecl: methodDecl.getParameters()) {
                JCExpression paramIdent = factory.Ident(paramDecl);
                var kryoMethodExpr2 = javac.constructExpression("ch.usi.si.codelounge.jsicko.plugin.utils.CloneUtils.kryoClone");
                var paramCloneCall = factory.Apply(com.sun.tools.javac.util.List.nil(), kryoMethodExpr2, com.sun.tools.javac.util.List.of(paramIdent));
                var nameLiteral = factory.Literal(paramDecl.getName().toString());
                var mapSetParams = com.sun.tools.javac.util.List.of(nameLiteral, paramCloneCall);
                var methodSelect2 = factory.Select(factory.Ident(oldValuesTableFieldDecl), javac.nameFromString("putValue"));
                var mapUpdateCall = factory.Apply(com.sun.tools.javac.util.List.nil(), methodSelect2, mapSetParams);
                methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(factory.Exec(mapUpdateCall));
            }

            JCMethodInvocation enterScopeStatement = buildEnterScopeStatement(methodDecl.sym);
            methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(factory.Exec(enterScopeStatement));
        });
    }

    private JCMethodInvocation buildEnterScopeStatement(Symbol.MethodSymbol methodSymbol) {
        final var factory = javac.getFactory();
        var enterMethodSelect = factory.Select(factory.Ident(methodSymbol.isStatic() ? optionalStaticOldValuesTableField.get(): optionalOldValuesTableField.get()), javac.nameFromString("enter"));
        var methodNameLiteral = factory.Literal(methodSymbol.toString());
        var enterParams = com.sun.tools.javac.util.List.of((JCExpression)methodNameLiteral);
        return factory.Apply(com.sun.tools.javac.util.List.nil(), enterMethodSelect, enterParams);
    }

    private JCMethodInvocation buildLeaveScopeStatement(boolean isStatic) {
        final var factory = javac.getFactory();
        var enterMethodSelect = factory.Select(factory.Ident(isStatic ? optionalStaticOldValuesTableField.get(): optionalOldValuesTableField.get()), javac.nameFromString("leave"));
        return factory.Apply(com.sun.tools.javac.util.List.nil(), enterMethodSelect, com.sun.tools.javac.util.List.nil());
    }

    private void optionalDeclareOldVariableAndMethod(JCClassDecl classDecl) {
        final var factory = javac.getFactory();

        if (!this.optionalOldValuesTableField.isPresent()) {

            /* define old and static old values table */
            var oldField = declareOldValuesTableField(classDecl,false);
            declareOldValuesTableField(classDecl,true);

            /* Override @old */
            var typeVar = javac.freshObjectTypeVar(null);

            var baseType = new Type.MethodType(com.sun.tools.javac.util.List.of(javac.stringType(),typeVar),
                    typeVar,
                    com.sun.tools.javac.util.List.nil(),
                    classDecl.sym);

            var overriddenOldMethodType = new Type.ForAll(com.sun.tools.javac.util.List.of(typeVar), baseType);

            var overriddenOldMethodSymbol = new Symbol.MethodSymbol(Flags.PUBLIC, javac.nameFromString(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING), overriddenOldMethodType, classDecl.sym);

            var literal = factory.Ident(javac.nameFromString("x0"));
            com.sun.tools.javac.util.List<JCExpression> params = com.sun.tools.javac.util.List.of(literal);
            var methodSelect = factory.Select(factory.Ident(oldField), javac.nameFromString("getValue"));
            var getCall = factory.Apply(com.sun.tools.javac.util.List.nil(), methodSelect, params);
            var castCall = factory.TypeCast(typeVar.tsym.type, getCall);
            var returnElemStatement = factory.Return(castCall);

            var overriddenOldMethodBody = factory.Block(0, com.sun.tools.javac.util.List.of(returnElemStatement));
            var overriddenOldMethod = factory.MethodDef(overriddenOldMethodSymbol, overriddenOldMethodBody);

            // Strange fix. Do NOT remove this.
            overriddenOldMethod.params.head.sym.adr = 0;
            overriddenOldMethod.params.last().sym.adr = 1;

            this.currentClassDecl.get().defs = this.currentClassDecl.get().defs.prepend(overriddenOldMethod);
            classDecl.sym.members().enter(overriddenOldMethodSymbol);

            this.overriddenOldMethod = Optional.of(overriddenOldMethod);

            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
            null, "Code of overridden old method " + this.overriddenOldMethod);
    
        }
    }

    private JCVariableDecl declareOldValuesTableField(JCClassDecl classDecl, boolean isStatic) {
        final var factory = javac.getFactory();

        var flags = (isStatic ? Flags.STATIC | Flags.PUBLIC : Flags.PRIVATE ) | Flags.FINAL ;
        var fieldName = (isStatic ? Constants.STATIC_OLD_FIELD_IDENTIFIER_STRING : Constants.OLD_FIELD_IDENTIFIER_STRING);

        var init = factory.NewClass(null,
                com.sun.tools.javac.util.List.nil(),
                javac.oldValuesTableTypeExpression(),
                com.sun.tools.javac.util.List.nil(),
                null);

        var varSymbol = new Symbol.VarSymbol(flags,
                javac.nameFromString(fieldName), javac.oldValuesTableClassType(), classDecl.sym);

        var varDef = factory.VarDef(varSymbol, init);

        if (isStatic)
            this.optionalStaticOldValuesTableField = Optional.of(varDef);
        else
            this.optionalOldValuesTableField = Optional.of(varDef);

        this.currentClassDecl.get().defs = this.currentClassDecl.get().defs.prepend(varDef);

        classDecl.sym.members().enter(varSymbol);

        return varDef;
    }

    private JCTry boxMethodBody(MethodTree methodTree) {
        final var factory = javac.getFactory();

        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        var methodSymbol = methodDecl.sym;
        final JCTry tryBlock;

        var finallyBlock = factory.Block(0, com.sun.tools.javac.util.List.nil());

        if (methodSymbol.isConstructor() && javac.isSuperOrThisConstructorCall(body.stats.head)) {
            var firstStatement = body.stats.head;
            var rest = body.stats.tail;
            tryBlock = factory.Try(factory.Block(0, rest), com.sun.tools.javac.util.List.nil(), finallyBlock);
            body.stats = com.sun.tools.javac.util.List.of((JCStatement) tryBlock).prepend(firstStatement);
        } else {
            tryBlock = factory.Try(factory.Block(0, body.stats), com.sun.tools.javac.util.List.nil(), finallyBlock);
            body.stats = com.sun.tools.javac.util.List.of(tryBlock);
        }

        return tryBlock;
    }

    private void optionalDeclareReturnValueCatcher(MethodTree methodTree) {
        final var factory = javac.getFactory();

        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        if (!methodDecl.sym.isConstructor() && !javac.hasVoidReturnType(methodTree)) {
            JCLiteral zeroValue = javac.zeroValue(methodDecl.getReturnType().type);
            var varDef = factory.VarDef(factory.Modifiers(0), javac.nameFromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype, zeroValue);
            body.stats = body.stats.prepend(varDef);

            this.currentMethodReturnVarDecl = Optional.of(varDef);
        } else {
            this.currentMethodReturnVarDecl = Optional.empty();
        }
    }

    private void addPreconditions(JCMethodDecl methodDecl, JCBlock block, List<Contract.Requires> requireClauses) {
        var allRequiresClauses = requireClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup, javac).stream()).collect(Collectors.toList());
        if (allRequiresClauses.size() > 0) {
            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                    methodDecl.pos(),
                    "For method " + methodDecl.sym + " - creating precondition checks " + allRequiresClauses);
        }
        addConditions(methodDecl, block, allRequiresClauses);
    }

    private void addPostconditions(JCMethodDecl methodDecl, JCBlock block, List<Contract.Ensures> ensuresClauses) {
        var allEnsuresClauses = ensuresClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup, javac).stream()).collect(Collectors.toList());
        if (allEnsuresClauses.size() > 0) {
            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                    methodDecl.pos(),
                    "For method " + methodDecl.sym + " - creating postcondition checks " + allEnsuresClauses);
        }
        addConditions(methodDecl, block, allEnsuresClauses);
    }

    private void addInvariants(JCMethodDecl methodDecl, JCBlock block) {
        if (this.classInvariants.size() > 0) {
            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                    methodDecl.pos(), "For method " + methodDecl.sym + " - creating invariant checks " + this.classInvariants);
        }
        addConditions(methodDecl, block, this.classInvariants);
    }

    private void addConditions(JCMethodDecl methodDecl, JCBlock block, List<ConditionClause> conditions) {
        conditions.stream().forEach((ConditionClause ensuresClause) -> {
            ensuresClause.resolveContractMethod(currentClassDecl.get());
            if (!ensuresClause.isResolved()) {
                javac.logError(this.currentCompilationUnitTree.get().getSourceFile(),
                        methodDecl.pos(),
                        "On method " + methodDecl.sym + ", contract condition " + ensuresClause.getClauseRep() + " not resolved, method not found.");
            } else {
                JCIf check = ensuresClause.createConditionCheck(methodDecl, ensuresClause);
                if (javac.isSuperOrThisConstructorCall(block.stats.head)) {
                    block.stats = block.stats.tail.prepend(check).prepend(block.stats.head);
                } else {
                    block.stats = block.stats.prepend(check);
                }
            }
        });
    }

    @Override
    public Void visitReturn(ReturnTree node, Deque<Tree> relevantScope) {
        final var factory = javac.getFactory();

        var returnNode = (JCReturn) node;

        this.currentMethodReturnVarDecl.ifPresent((JCVariableDecl varDecl) -> {
            if (relevantScope.peek().equals(this.currentMethodTree.get())) {
                var newArg = factory.Assign(factory.Ident(javac.nameFromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING)), ((JCReturn) node).expr);
                returnNode.expr = newArg;
            }
        });

        return super.visitReturn(node, relevantScope);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Deque<Tree> relevantScope) {
        Void v;
        relevantScope.push(node);
        v = super.visitLambdaExpression(node, relevantScope);
        relevantScope.pop();
        return v;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Deque<Tree> relevantScope) {
        var oldNodeRep = node.toString();
        var factory = javac.getFactory();
        var w = super.visitMethodInvocation(node, relevantScope);
        var methodInvocation = ((JCMethodInvocation)node);
        var isScopeInStaticMethod = isScopeInStaticMethod(relevantScope);

        if (isScopeInPureMethod(relevantScope) && methodInvocation.meth.toString().equals("old")) {
            var paramName = methodInvocation.args.head.toString();
            if (isScopeInStaticMethod) {
                methodInvocation.meth = factory.Select(javac.constructExpression("ch.usi.si.codelounge.jsicko.Contract"),javac.nameFromString(Constants.STATIC_OLD_METHOD_IDENTIFIER_STRING));
                methodInvocation.args = methodInvocation.args.prepend(factory.Literal(paramName));
                methodInvocation.args = methodInvocation.args.prepend(factory.ClassLiteral(getLastMethodInScope(relevantScope).get().sym.enclClass()));
            } else {
                methodInvocation.meth = factory.Ident(javac.nameFromString(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING));
                methodInvocation.args = methodInvocation.args.prepend(factory.Literal(paramName));
            }

            javac.logNote(this.currentCompilationUnitTree.get().getSourceFile(),
                    methodInvocation.pos(),
                    "Rewrote " + oldNodeRep + " method invocation to: " + node.toString());
        }
        return w;
    }

    private boolean isScopeInStaticMethod(Deque<Tree> relevantScope) {
        Optional<JCMethodDecl> optionalLastMethod = getLastMethodInScope(relevantScope);

        return optionalLastMethod.isPresent() &&
                optionalLastMethod.get() instanceof JCMethodDecl &&
                optionalLastMethod.get().sym.isStatic();
    }


    private boolean isScopeInPureMethod(Deque<Tree> relevantScope) {
        Optional<JCMethodDecl> optionalLastMethod = getLastMethodInScope(relevantScope);

        return optionalLastMethod.isPresent() &&
                optionalLastMethod.get() instanceof JCMethodDecl &&
                isMarkedAsPureOrIsSpecialPureMethod(optionalLastMethod.get().sym);
    }

    private Optional<JCMethodDecl> getLastMethodInScope(Deque<Tree> relevantScope) {
        Optional<Tree> optionalLastMethod = relevantScope.stream().dropWhile(t -> !(t instanceof JCMethodDecl)).findFirst();
        return optionalLastMethod.map(JCMethodDecl.class::cast);
    }
}