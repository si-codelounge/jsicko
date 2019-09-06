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
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ContractCompilerTreeScanner extends TreeScanner<Void, Deque<Tree>> {

    private final JavacUtils javac;
    private final JSickoContractCompilerState state;
    private final TreeMaker factory;

    ContractCompilerTreeScanner(BasicJavacTask task) {
        this.javac = new JavacUtils(task);
        this.state = new JSickoContractCompilerState(task);
        this.factory = javac.getFactory();
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Deque<Tree> relevantScope) {
        this.state.enterCompilationUnit(node);
        var w = super.visitCompilationUnit(node, relevantScope);
        this.state.exitCompilationUnit();
        return w;
    }

    @Override
    public Void visitClass(ClassTree classTree, Deque<Tree> relevantScope) {

        this.state.currentCompilationUnitTree().ifPresent((CompilationUnitTree currentCompilationUnitTree) -> {
            var classDecl = (JCClassDecl) classTree;
            this.state.enterClassDecl(classDecl);
            optionalDeclareOldVariableAndMethod();
        });

        var w = super.visitClass(classTree, relevantScope);
        this.state.exitClassDecl();
        return w;
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Deque<Tree> relevantScope) {
        var methodDecl = (JCMethodDecl) methodTree;

        this.state.enterMethodDecl(methodDecl);

        if (this.state.currentMethodShouldBeInstrumented()) {

            final java.util.List<Symbol> overriddenMethods = state.findOverriddenMethodsOfCurrentMethod();

            if (overriddenMethods.size() > 0) {
                java.util.List<Contract.Requires> requires = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Requires.class))).collect(Collectors.toList());
                java.util.List<Contract.Ensures> ensures = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Ensures.class))).collect(Collectors.toList());

                var isMarkedPure = isAnyMethodMarkedAsOrMustBePure(overriddenMethods);

                declareRaisesValueCatcher();
                final JCTry tryBlock = boxMethodBody();
                optionalDeclareReturnValueCatcher();
                appendRaisesValueCatcher();

                addOldValuesTableInstrumentation(isMarkedPure, tryBlock);
                addPreconditions(methodDecl.body, requires);
                addPostconditions(tryBlock.finalizer, ensures);
                addInvariants(isMarkedPure, tryBlock.finalizer);
            }
        }

        relevantScope.push(methodTree);
        var w = super.visitMethod(methodTree, relevantScope);
        relevantScope.pop();
        this.state.exitMethodDecl();
        return w;
    }

    private boolean isAnyMethodMarkedAsOrMustBePure(java.util.List<Symbol> methodSymbols) {
        return methodSymbols.stream().anyMatch(this::isMarkedAsPureOrIsSpecialPureMethod);
    }

    private boolean isMarkedAsPureOrIsSpecialPureMethod(Symbol symbol) {
        return isMarkedAsPure(symbol) || isSpecialPureMethod(symbol);
    }

    private boolean isMarkedAsPure(Symbol symbol) {
        return symbol.getAnnotationsByType(Contract.Pure.class).length > 0;
    }

    private boolean isSpecialPureMethod(Symbol symbol) {
        /*
         * Issue #13: java.util.Collection#iterator method is exploited by Kryo to serialize collections. 
         * jSicko must thus consider it as as pure.
         */
        return symbol.equals(this.javac.getJavaUtilCollectionIteratorMethodSymbol());
    }

    private void addOldValuesTableInstrumentation(boolean isMarkedPure, JCTry tryBlock) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            if (!methodDecl.sym.isConstructor() && !isMarkedPure) {
                optionalSaveOldState(methodDecl);
                addEnterScopeStatement(methodDecl);
                addLeaveScopeStatement(tryBlock.finalizer);
            }
        });
    }

    private void addEnterScopeStatement(JCMethodDecl methodDecl) {
        this.state.optionalOldValuesTableField().ifPresent((JCVariableDecl oldValuesTableField) -> {
            JCMethodInvocation enterScopeStatement = buildEnterScopeStatement(methodDecl.sym);
            methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(factory.Exec(enterScopeStatement));
        });
    }

    private void addLeaveScopeStatement(JCBlock finalizer) {
        JCMethodInvocation leaveScopeStatement = buildLeaveScopeStatement();
        finalizer.stats = finalizer.stats.prepend(factory.Exec(leaveScopeStatement));
    }

    private void optionalSaveOldState(JCMethodDecl methodDecl) {
        this.state.optionalOldValuesTableField().ifPresent((JCVariableDecl oldValuesTableField) -> {

            var oldValuesTableFieldDecl = this.state.oldValuesTableFieldDeclByMethodType();

            if (!methodDecl.sym.isStatic()) {
                JCExpressionStatement saveThisOldValueStatement = buildStatementToSaveThisOldValue(oldValuesTableFieldDecl);
                methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(saveThisOldValueStatement);
            }

            var saveLocalVariableOldValueStatements = methodDecl.getParameters().stream().map((JCVariableDecl paramDecl) ->
                    buildStatementToSaveLocalVariableOldValue(oldValuesTableFieldDecl, paramDecl))
                .collect(List.collector());

            methodDecl.getBody().stats = methodDecl.getBody().stats.prependList(saveLocalVariableOldValueStatements);

        });
    }

    private JCStatement buildStatementToSaveLocalVariableOldValue(JCVariableDecl oldValuesTableFieldDecl, JCVariableDecl paramDecl) {
        JCExpression paramIdent = factory.Ident(paramDecl);
        var kryoMethodExpr = javac.constructExpression(Constants.KRYO_CLONE_METHOD_QUALIFIED_IDENTIFIER);
        var paramCloneCall = factory.Apply(List.nil(), kryoMethodExpr, List.of(paramIdent));
        var nameLiteral = factory.Literal(paramDecl.getName().toString());
        var mapSetParams = List.of(nameLiteral, paramCloneCall);
        var methodSelect2 = factory.Select(factory.Ident(oldValuesTableFieldDecl), javac.nameFromString("putValue"));
        return factory.Exec(factory.Apply(List.nil(), methodSelect2, mapSetParams));
    }

    private JCExpressionStatement buildStatementToSaveThisOldValue(JCVariableDecl oldValuesTableFieldDecl) {
        return this.state.mapAndGetOnClassDecl((JCClassDecl classDecl) -> {
            var thisType = factory.This(classDecl.sym.type);
            var kryoMethodExpr = javac.constructExpression(Constants.KRYO_CLONE_METHOD_QUALIFIED_IDENTIFIER);
            var cloneCall = factory.Apply(List.nil(), kryoMethodExpr, List.of(thisType));
            var literal = factory.Literal("this");
            var params = List.of(literal, cloneCall);
            var methodSelect = factory.Select(factory.Ident(oldValuesTableFieldDecl), javac.nameFromString("putValue"));
            return factory.Exec(factory.Apply(List.nil(), methodSelect, params));
        });
    }

    private JCMethodInvocation buildEnterScopeStatement(Symbol.MethodSymbol methodSymbol) {
        var enterMethodSelect = factory.Select(factory.Ident(this.state.oldValuesTableFieldDeclByMethodType()), javac.nameFromString("enter"));
        var methodNameLiteral = factory.Literal(methodSymbol.toString());
        var enterParams = List.of((JCExpression)methodNameLiteral);
        return factory.Apply(List.nil(), enterMethodSelect, enterParams);
    }

    private JCMethodInvocation buildLeaveScopeStatement() {
        var enterMethodSelect = factory.Select(factory.Ident(this.state.oldValuesTableFieldDeclByMethodType()), javac.nameFromString("leave"));
        return factory.Apply(List.nil(), enterMethodSelect, List.nil());
    }

    private void optionalDeclareOldVariableAndMethod() {
        this.state.ifClassDeclPresent((JCClassDecl classDecl) -> {
            if (this.state.currentClassHasContract() && !this.state.optionalOldValuesTableField().isPresent()) {

                /* define old and static old values table */
                var oldField = declareOldValuesTableField(false);
                declareOldValuesTableField(true);

                /* Override @old */
                var typeVar = javac.freshObjectTypeVar(null);

                var baseType = new Type.MethodType(List.of(javac.stringType(), typeVar),
                        typeVar,
                        List.nil(),
                        classDecl.sym);

                var overriddenOldMethodType = new Type.ForAll(List.of(typeVar), baseType);

                var overriddenOldMethodSymbol = new Symbol.MethodSymbol(Flags.PUBLIC, javac.nameFromString(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING), overriddenOldMethodType, classDecl.sym);

                var literal = factory.Ident(javac.nameFromString("x0"));
                List<JCExpression> params = List.of(literal);
                var methodSelect = factory.Select(factory.Ident(oldField), javac.nameFromString("getValue"));
                var getCall = factory.Apply(List.nil(), methodSelect, params);
                var castCall = factory.TypeCast(typeVar.tsym.type, getCall);
                var returnElemStatement = factory.Return(castCall);

                var overriddenOldMethodBody = factory.Block(0, List.of(returnElemStatement));
                var overriddenOldMethod = factory.MethodDef(overriddenOldMethodSymbol, overriddenOldMethodBody);

                // Strange fix. Do NOT remove this.
                overriddenOldMethod.params.head.sym.adr = 0;
                overriddenOldMethod.params.last().sym.adr = 1;

                this.state.overrideOldMethod(overriddenOldMethod);
            }
        });
    }

    private JCVariableDecl declareOldValuesTableField(boolean declareTheStaticOne) {
        return this.state.mapAndGetOnClassDecl((JCClassDecl classDecl) -> {

            var flags = (declareTheStaticOne ? Flags.STATIC | Flags.PUBLIC : Flags.PRIVATE) | Flags.FINAL;
            var fieldName = (declareTheStaticOne ? Constants.STATIC_OLD_FIELD_IDENTIFIER_STRING : Constants.OLD_FIELD_IDENTIFIER_STRING);

            var init = factory.NewClass(null, List.nil(), javac.oldValuesTableTypeExpression(), List.nil(), null);

            var varSymbol = new Symbol.VarSymbol(flags,
                    javac.nameFromString(fieldName), javac.oldValuesTableClassType(), classDecl.sym);

            var varDef = factory.VarDef(varSymbol, init);
            state.appendOldValuesTableField(varDef, declareTheStaticOne);

            return varDef;
        });
    }

    private JCTry boxMethodBody() {
        return this.state.mapAndGetOnMethodDecl((JCMethodDecl methodDecl) -> {
            JCBlock body = methodDecl.getBody();
            var methodSymbol = methodDecl.sym;
            final JCTry tryBlock;

            var finallyBlock = factory.Block(0, List.nil());
            List<JCCatch> catchBlock = buildCatchBlock();

            if (methodSymbol.isConstructor() && javac.isSuperOrThisConstructorCall(body.stats.head)) {
                var firstStatement = body.stats.head;
                var rest = body.stats.tail;

                tryBlock = factory.Try(factory.Block(0, rest), catchBlock, finallyBlock);
                body.stats = List.of((JCStatement) tryBlock).prepend(firstStatement);
            } else {
                tryBlock = factory.Try(factory.Block(0, body.stats), catchBlock, finallyBlock);
                body.stats = List.of(tryBlock);
            }
            return tryBlock;
        });
    }

    private List<JCCatch> buildCatchBlock() {
        return state.mapAndGetOnMethodDecl((JCMethodDecl methodDecl) -> {
            if (state.currentMethodRaisesVarDecl().isPresent()) {

                var exceptionType = javac.deriveMostGeneralExceptionTypeThrown(methodDecl.getThrows());

                /*
                 * The EFFECTIVELY_FINAL flag is needed to enable the
                 * allowImprovedRethrowAnalysis of Java 7.
                 */
                var exceptionVarSymbol = new Symbol.VarSymbol(Flags.EFFECTIVELY_FINAL,
                        javac.nameFromString(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING), exceptionType, methodDecl.sym);
                var exceptionVariableDecl = factory.VarDef(exceptionVarSymbol, null);
                exceptionVarSymbol.adr = 0;
                var rethrowStatement = factory.Throw(factory.Ident(javac.nameFromString(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING)));
                var assignmentToRaises = factory.Exec(factory.Assign(factory.Ident(javac.nameFromString(Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING)),
                        factory.Ident(javac.nameFromString(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING))));
                var block = factory.Block(0, List.of(assignmentToRaises, rethrowStatement));
                JCCatch exceptionHandler = factory.Catch(exceptionVariableDecl, block);

                return List.of(exceptionHandler);
            } else
                return List.nil();
        });
    }

    private void optionalDeclareReturnValueCatcher() {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            JCBlock body = methodDecl.getBody();

            if (!methodDecl.sym.isConstructor() && !javac.hasVoidReturnType(methodDecl)) {
                JCLiteral zeroValue = javac.zeroValue(methodDecl.getReturnType().type);
                var varDef = factory.VarDef(factory.Modifiers(0), javac.nameFromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype, zeroValue);
                body.stats = body.stats.prepend(varDef);

                state.setCurrentMethodReturnVarDecl(varDef);
            }
        });
    }

    private void declareRaisesValueCatcher() {
        JCLiteral zeroValue = javac.zeroValue(javac.throwableType());
        var varDef = factory.VarDef(factory.Modifiers(0),
                javac.nameFromString(Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING), javac.constructExpression("java.lang.Throwable"), zeroValue);

        state.setCurrentMethodRaisesVarDecl(varDef);
    }

    private void appendRaisesValueCatcher() {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            if (state.currentMethodRaisesVarDecl().isPresent()) {
                JCBlock body = methodDecl.getBody();
                var varDef = state.currentMethodRaisesVarDecl().get();

                if (javac.isSuperOrThisConstructorCall(body.stats.head)) {
                    body.stats = body.stats.tail.prepend(varDef).prepend(body.stats.head);
                } else {
                    body.stats = body.stats.prepend(varDef);
                }
            }
        });
    }

    private void addPreconditions(JCBlock block, java.util.List<Contract.Requires> requireClauses) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            var allRequiresClauses = requireClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup, javac).stream()).collect(Collectors.toList());
            if (allRequiresClauses.size() > 0) {
                state.logNote(methodDecl.pos(),
                        "For method " + methodDecl.sym + " - creating precondition checks " + allRequiresClauses);
            }
            addConditions(methodDecl, block, allRequiresClauses);
        });
    }

    private void addPostconditions(JCBlock block, java.util.List<Contract.Ensures> ensuresClauses) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            var allEnsuresClauses = ensuresClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup, javac).stream()).collect(Collectors.toList());
            if (allEnsuresClauses.size() > 0) {
                state.logNote(methodDecl.pos(),
                        "For method " + methodDecl.sym + " - creating postcondition checks " + allEnsuresClauses);
            }
            addConditions(methodDecl, block, allEnsuresClauses);
        });
    }

    private void addInvariants(boolean isMarkedPure, JCBlock block) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            if (!isMarkedPure && !methodDecl.sym.isStatic()) {
                if (state.classInvariants().size() > 0) {
                    state.logNote(methodDecl.pos(),
                            "For method " + methodDecl.sym + " - creating invariant checks " + state.classInvariants());
                }
                addConditions(methodDecl, block, state.classInvariants());
            }
        });
    }

    private void addConditions(JCMethodDecl methodDecl, JCBlock block, java.util.List<ConditionClause> conditions) {
        var checks = conditions.stream().flatMap((ConditionClause clause) -> {
            clause.resolveContractMethod(state.currentClassDecl().get());
            if (!clause.isResolved()) {
                state.logError(methodDecl.pos(),
                        "On method " + methodDecl.sym + ", contract condition " + clause.getClauseRep() + " not resolved, method not found.");
                return Stream.empty();
            } else {
                JCStatement check = clause.createConditionCheck(methodDecl, clause);
                return Stream.of(check);
            }
        }).collect(List.collector());
        if (javac.isSuperOrThisConstructorCall(block.stats.head)) {
            block.stats = block.stats.tail.prependList(checks).prepend(block.stats.head);
        } else {
            block.stats = block.stats.prependList(checks);
        }
    }

    @Override
    public Void visitReturn(ReturnTree node, Deque<Tree> relevantScope) {
        var returnNode = (JCReturn) node;

        state.currentMethodReturnVarDecl().ifPresent((JCVariableDecl varDecl) -> {
            if (state.isCurrentMethodDecl(relevantScope.peek())) {
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
        var w = super.visitMethodInvocation(node, relevantScope);
        var methodInvocation = ((JCMethodInvocation)node);
        var isScopeInStaticMethod = isScopeInStaticMethod(relevantScope);

        if (isScopeInPureMethod(relevantScope) && methodInvocation.meth.toString().equals("old")) {
            var paramName = methodInvocation.args.head.toString();
            if (isScopeInStaticMethod) {
                methodInvocation.meth = factory.Select(javac.constructExpression(Contract.class.getCanonicalName()),javac.nameFromString(Constants.STATIC_OLD_METHOD_IDENTIFIER_STRING));
                methodInvocation.args = methodInvocation.args.prepend(factory.Literal(paramName));
                methodInvocation.args = methodInvocation.args.prepend(factory.ClassLiteral(getLastMethodInScope(relevantScope).get().sym.enclClass()));
            } else {
                methodInvocation.meth = factory.Ident(javac.nameFromString(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING));
                methodInvocation.args = methodInvocation.args.prepend(factory.Literal(paramName));
            }
        }
        return w;
    }

    private boolean isScopeInStaticMethod(Deque<Tree> relevantScope) {
        Optional<JCMethodDecl> optionalLastMethod = getLastMethodInScope(relevantScope);

        return optionalLastMethod.isPresent() &&
                optionalLastMethod.get().sym.isStatic();
    }

    private boolean isScopeInPureMethod(Deque<Tree> relevantScope) {
        Optional<JCMethodDecl> optionalLastMethod = getLastMethodInScope(relevantScope);

        return optionalLastMethod.isPresent() &&
                isMarkedAsPureOrIsSpecialPureMethod(optionalLastMethod.get().sym);
    }

    private Optional<JCMethodDecl> getLastMethodInScope(Deque<Tree> relevantScope) {
        Optional<Tree> optionalLastMethod = relevantScope.stream().dropWhile(t -> !(t instanceof JCMethodDecl)).findFirst();
        return optionalLastMethod.map(JCMethodDecl.class::cast);
    }
}