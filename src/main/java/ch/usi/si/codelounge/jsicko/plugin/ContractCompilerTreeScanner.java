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
import ch.usi.si.codelounge.jsicko.plugin.utils.ConditionChecker;
import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * The tree scanner implementing the compiler for jSicko contracts.
 */
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
        this.state.enterCompilationUnit((JCCompilationUnit)node);
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

            List<List<ConditionClause>> classInvariants = List.of(state.classInvariants());

            final List<Symbol> overriddenMethods = state.findOverriddenMethodsOfCurrentMethod();

            if (overriddenMethods.size() > 0) {
                List<List<ConditionClause>> requireClausesByMethod = constructRequireClausesByMethod(overriddenMethods);
                List<ConditionClause> ensuresClauses = constructEnsureClauses(overriddenMethods);
                var isMarkedPure = isAnyMethodMarkedAsOrMustBePure(overriddenMethods);

                declareRaisesValueCatcher();
                final JCTry tryBlock = boxMethodBody();
                optionalDeclareReturnValueCatcher();
                appendRaisesValueCatcher();

                addOldValuesTableInstrumentation(isMarkedPure, tryBlock);
                addConditions(ContractConditionEnum.PRECONDITION, methodDecl.body, isMarkedPure, requireClausesByMethod);
                addConditions(ContractConditionEnum.POSTCONDITION, tryBlock.finalizer, isMarkedPure, List.of(ensuresClauses));
                addConditions(ContractConditionEnum.INVARIANT, tryBlock.finalizer, isMarkedPure, classInvariants);
            }
        }

        relevantScope.push(methodTree);
        var w = super.visitMethod(methodTree, relevantScope);
        relevantScope.pop();
        this.state.exitMethodDecl();
        return w;
    }

    /**
     * From a list of method symbols that represent the sequence of overridden methods, constructs a list of
     * condition clauses, each one representing a postcondition clause of a particular overridden method.
     *
     * The order must respect a) the order of postcondition strengthenings, i.e. the order of overrides, and
     * b) the clause order in a given postcondition annotation in a method.
     *
     * @param overriddenMethods a list of symbols corresponding to the overridden symbols of a given method, including the
     *                          method itself.
     * @return a list of condition clauses, representing the postconditions for a given method.
     */
    private List<ConditionClause> constructEnsureClauses(List<Symbol> overriddenMethods) {
        return overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> {
            return Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Ensures.class))
                    .flatMap((Contract.Ensures ensuresClauseGroup) -> ConditionClause.from(ensuresClauseGroup, overriddenMethod, javac).stream());
        }).collect(List.collector());
    }

    /**
     * From a list of method symbols that represent the sequence of overridden methods, constructs a list of
     * lists of condition clauses, each one representing the preconditions of a particular overridden method.
     *
     * The returned list of lists represents a series of precondition weakenings, from the top-most to the
     * last method in the hierarchy. Thus, the list correspond to a disjunction of conjunctions of clauses.
     *
     * @param overriddenMethods a list of symbols corresponding to the overridden symbols of a given method, including the
     *                          method itself.
     * @return a list of lists of condition clauses, representing the precondition weakenings.
     */
    private List<List<ConditionClause>> constructRequireClausesByMethod(List<Symbol> overriddenMethods) {
        return overriddenMethods.stream()
                .flatMap((Symbol overriddenMethod) -> {
                    return Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Requires.class))
                            .map((Contract.Requires requiresClauseGroup) -> ConditionClause.from(requiresClauseGroup, overriddenMethod, javac));
                }).collect(List.collector());
    }

    /**
     * Checks if any method on a list of symbols is marked as or must be considered as pure.
     * @param methodSymbols a list of symbols (though the check makes sense only for method symbols).
     * @return <code>true</code> iff any method in the list is pure.
     */
    private boolean isAnyMethodMarkedAsOrMustBePure(List<Symbol> methodSymbols) {
        return methodSymbols.stream().anyMatch(this::isMarkedAsPureOrIsSpecialPureMethod);
    }

    /**
     * Checks if the symbol has been marked pure, or must be considered pure for analysis reasons.
     * @param symbol a symbol (can be only a method symbol, though for practical reasons we don't need to check it).
     * @return <code>true</code> iff either the symbol has been marked pure, or must be considered pure for analysis reasons.
     */
    private boolean isMarkedAsPureOrIsSpecialPureMethod(Symbol symbol) {
        return isMarkedAsPure(symbol) || isSpecialPureMethod(symbol);
    }

    /**
     * Returns true iff the symbol has been annotated with Pure by the user.
     * @param symbol a symbol (can be only a method symbol, though for practical reasons we don't need to check it).
     * @return <code>true</code> iff the symbol has been marked with Pure.
     */
    private boolean isMarkedAsPure(Symbol symbol) {
        return symbol.getAnnotationsByType(Contract.Pure.class).length > 0;
    }

    /**
     * Checks if the symbol is a special method that must be considered pure.
     *
     * At the moment, this is true only for a Collection's iterator method, that must be
     * considered pure because of the inner workings of Kryo serialization.
     *
     * @param symbol a symbol (can be only a method symbol, though for practical reasons we don't need to check it).
     * @return <code>true</code> iff the symbol must be considered pure by the jSicko analysis.
     */
    private boolean isSpecialPureMethod(Symbol symbol) {
        /*
         * Issue #13: java.util.Collection#iterator method is exploited by Kryo to serialize collections. 
         * jSicko must thus consider it as as pure.
         */
        return symbol.equals(this.javac.getJavaUtilCollectionIteratorMethodSymbol());
    }

    /**
     * Adds statements to support saving and retrieving old values.
     *
     * In particular, it appends the enter/leave scope statement for the old value table,
     * and saves the pre-values of this and the input parameters.
     * @param isMarkedPure a cached value for the purity of the declaring method.
     * @param tryBlock the block where to add the instrumentation statements.
     */
    private void addOldValuesTableInstrumentation(boolean isMarkedPure, JCTry tryBlock) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            if (!methodDecl.sym.isConstructor() && !isMarkedPure) {
                optionalSaveOldState();
                addEnterScopeStatement();
                addLeaveScopeStatement(tryBlock.finalizer);
            }
        });
    }

    /**
     * Appends the enter scope statement for the old values table.
     */
    private void addEnterScopeStatement() {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            this.state.optionalOldValuesTableField().ifPresent((JCVariableDecl oldValuesTableField) -> {
                JCMethodInvocation enterScopeStatement = buildEnterScopeStatement(methodDecl.sym);
                methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(factory.Exec(enterScopeStatement));
            });
        });
    }

    /**
     * Add the leave-scope statement for the old values table.
     * @param finalizer the finalizer block of the instrumentation try-catch-finally statement.
     */
    private void addLeaveScopeStatement(JCBlock finalizer) {
        JCMethodInvocation leaveScopeStatement = buildLeaveScopeStatement();
        finalizer.stats = finalizer.stats.prepend(factory.Exec(leaveScopeStatement));
    }

    /**
     * Optionally adds statements to save the old state of this/local variables in the old values table.
     */
    private void optionalSaveOldState() {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            this.state.optionalOldValuesTableField().ifPresent((JCVariableDecl oldValuesTableField) -> {

                var oldValuesTableFieldDecl = this.state.oldValuesTableFieldDeclByMethodType();

                if (!methodDecl.sym.isStatic()) {
                    JCStatement saveThisOldValueStatement = buildStatementToSaveThisOldValue(oldValuesTableFieldDecl);
                    methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(saveThisOldValueStatement);
                }

                var saveLocalVariableOldValueStatements = methodDecl.getParameters().stream().map((JCVariableDecl paramDecl) ->
                        buildStatementToSaveLocalVariableOldValue(oldValuesTableFieldDecl, paramDecl))
                        .collect(List.collector());

                methodDecl.getBody().stats = methodDecl.getBody().stats.prependList(saveLocalVariableOldValueStatements);

            });
        });
    }

    /**
     * Builds statement to save a local variable's old value in the old values table.
     * @param oldValuesTableFieldDecl the old values table field declaration.
     * @param paramDecl the parameter declaration.
     * @return the statement that saves the old value in the table.
     */
    private JCStatement buildStatementToSaveLocalVariableOldValue(JCVariableDecl oldValuesTableFieldDecl, JCVariableDecl paramDecl) {
        JCExpression paramIdent = factory.Ident(paramDecl);
        var kryoMethodExpr = javac.Expression(Constants.KRYO_CLONE_METHOD_QUALIFIED_IDENTIFIER);
        var paramCloneCall = factory.Apply(List.nil(), kryoMethodExpr, List.of(paramIdent));
        var nameLiteral = factory.Literal(paramDecl.getName().toString());
        var mapSetParams = List.of(nameLiteral, paramCloneCall);
        return javac.MethodCall(factory.Ident(oldValuesTableFieldDecl), javac.Name("putValue"), mapSetParams);
    }

    /**
     * Builds statement to save the old value of this.
     * @param oldValuesTableFieldDecl the old values table field declaration.
     * @return the statement that saves the old value of this in the table.
     */
    private JCStatement buildStatementToSaveThisOldValue(JCVariableDecl oldValuesTableFieldDecl) {
        return this.state.mapAndGetOnClassDecl((JCClassDecl classDecl) -> {
            var thisType = factory.This(classDecl.sym.type);
            var kryoMethodExpr = javac.Expression(Constants.KRYO_CLONE_METHOD_QUALIFIED_IDENTIFIER);
            var cloneCall = factory.Apply(List.nil(), kryoMethodExpr, List.of(thisType));
            var literal = factory.Literal("this");
            var params = List.of(literal, cloneCall);
            return javac.MethodCall(factory.Ident(oldValuesTableFieldDecl), javac.Name("putValue"), params);
        });
    }

    /**
     * Builds the enter scope statement.
     *
     * @param methodSymbol the method symbol that represents the scope to enter to.
     * @return the enter scope statement.
     */
    private JCMethodInvocation buildEnterScopeStatement(MethodSymbol methodSymbol) {
        List<JCExpression> enterParams = List.of(factory.Literal(methodSymbol.toString()));
        return javac.MethodInvocation(factory.Ident(this.state.oldValuesTableFieldDeclByMethodType()), javac.Name("enter"), enterParams);
    }

    /**
     * Builds the leave scope statement.
     *
     * @return the leave scope statement.
     */
    private JCMethodInvocation buildLeaveScopeStatement() {
        return javac.MethodInvocation(factory.Ident(this.state.oldValuesTableFieldDeclByMethodType()), javac.Name("leave"));
    }

    /**
     * Optionally declares the old variables (instance and static) and overrides the old method,
     * appending them to the currently instrumented class.
     */
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

                var overriddenOldMethodSymbol = new MethodSymbol(Flags.PUBLIC, javac.Name(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING), overriddenOldMethodType, classDecl.sym);

                var literal = factory.Ident(javac.Name("x0"));
                List<JCExpression> params = List.of(literal);
                var getCall = javac.MethodInvocation(factory.Ident(oldField), javac.Name("getValue"), params);
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

    /**
     * Declares the old values table field.
     * @param declareTheStaticOne if the method needs to declare the static one.
     * @return the field declaration.
     */
    private JCVariableDecl declareOldValuesTableField(boolean declareTheStaticOne) {
        return this.state.mapAndGetOnClassDecl((JCClassDecl classDecl) -> {

            var flags = (declareTheStaticOne ? Flags.STATIC | Flags.PUBLIC : Flags.PRIVATE) | Flags.FINAL;
            var fieldName = (declareTheStaticOne ? Constants.STATIC_OLD_FIELD_IDENTIFIER_STRING : Constants.OLD_FIELD_IDENTIFIER_STRING);

            var init = factory.NewClass(null, List.nil(), javac.oldValuesTableTypeExpression(), List.nil(), null);

            var varSymbol = new VarSymbol(flags,
                    javac.Name(fieldName), javac.oldValuesTableClassType(), classDecl.sym);

            var varDef = factory.VarDef(varSymbol, init);
            state.appendOldValuesTableField(varDef, declareTheStaticOne);

            return varDef;
        });
    }

    /**
     * Boxes the method body of the currently instrumented method with a try-catch-finally block
     * that contains the main semantics of jSicko for postconditions.
     *
     * Care must be taken if the method is a constructor and contains super or this as first statement.
     * In that case, the try block must be the second (and not the only) statement of the instrumented body.
     *
     * @return the try block boxing the original method body.
     */
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

    /**
     * Builds the catch clauses of the try statement boxing an instrumented method.
     *
     * The catch block simply catches the most general exception type throwable by the
     * method body, stores into the raises synthetic variable used in postconditions,
     * and rethrows the exception.
     *
     * @return a one-element list of catch clauses.
     */
    private List<JCCatch> buildCatchBlock() {
        return state.mapAndGetOnMethodDecl((JCMethodDecl methodDecl) -> {
            if (state.currentMethodRaisesVarDecl().isPresent()) {

                var exceptionType = javac.deriveMostGeneralExceptionTypeThrown(methodDecl.getThrows());

                /*
                 * The EFFECTIVELY_FINAL flag is needed to enable the
                 * allowImprovedRethrowAnalysis of Java 7.
                 */
                var exceptionVarSymbol = new VarSymbol(Flags.EFFECTIVELY_FINAL,
                        javac.Name(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING), exceptionType, methodDecl.sym);
                var exceptionVariableDecl = factory.VarDef(exceptionVarSymbol, null);
                exceptionVarSymbol.adr = 0;
                var rethrowStatement = factory.Throw(factory.Ident(javac.Name(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING)));
                var assignmentToRaises = factory.Exec(factory.Assign(factory.Ident(javac.Name(Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING)),
                        factory.Ident(javac.Name(Constants.THROWN_SYNTHETIC_IDENTIFIER_STRING))));
                var block = factory.Block(0, List.of(assignmentToRaises, rethrowStatement));
                JCCatch exceptionHandler = factory.Catch(exceptionVariableDecl, block);

                return List.of(exceptionHandler);
            } else
                /*
                 * Here to support the default case.
                 * This should never happen in the actual implementation, since we always generate
                 * the raises var declaration if the method is being instrumented. With a minimal analysis,
                 * we could decided if the clauses never uses the raises variable and thus avoid generating
                 * any catch clause.
                 */
                return List.nil();
        });
    }

    /**
     * Optionally declares and appends the synthetic variable used to catch the return value of a method,
     * to be passed in postcondition clauses.
     */
    private void optionalDeclareReturnValueCatcher() {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            JCBlock body = methodDecl.getBody();

            if (!methodDecl.sym.isConstructor() && !javac.hasVoidReturnType(methodDecl)) {
                JCLiteral zeroValue = javac.zeroValue(methodDecl.getReturnType().type);
                var varDef = factory.VarDef(factory.Modifiers(0), javac.Name(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype, zeroValue);
                body.stats = body.stats.prepend(varDef);

                state.setCurrentMethodReturnVarDecl(varDef);
            }
        });
    }

    /**
     * Declares (but not appends) the synthetic variable used to catch the exception thrown by a method.
     */
    private void declareRaisesValueCatcher() {
        JCLiteral zeroValue = javac.nullLiteral();
        var varDef = factory.VarDef(factory.Modifiers(0),
                javac.Name(Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING), javac.Expression(Throwable.class.getCanonicalName()), zeroValue);

        state.setCurrentMethodRaisesVarDecl(varDef);
    }

    /**
     * Appends the raises synthetic variable to the body of an instrumented method
     * (taking into account the fact that the first statements can be this()/super() in a constructor.
     */
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

    /**
     * Adds conditions to the instrumented method.
     * @param conditionType the type of the condition (precondition, postcondition, invariant).
     * @param block the block where to append the conditions.
     * @param isMarkedPure if the method is marked pure.
     * @param groupedClauses the list of clauses grouped by overriding method, and ordered by hierarchy (starting from
     *                       the method in the topmost class).
     */
    private void addConditions(ContractConditionEnum conditionType, JCBlock block, boolean isMarkedPure, List<List<ConditionClause>> groupedClauses) {
        this.state.ifMethodDeclPresent((JCMethodDecl methodDecl) -> {
            if (shouldAddConditions(conditionType, methodDecl, isMarkedPure, groupedClauses)) {
                if (groupedClauses.size() > 0) {
                    state.logNote(methodDecl.pos(),
                            "For method " + methodDecl.sym + " - creating " + conditionType.toString().toLowerCase() + " checks " + groupedClauses);
                }

                buildConditionsChecker(conditionType, methodDecl, block, groupedClauses);
            }
        });
    }

    /**
     * Returns true if the conditions should be added or not, depending on the type.
     *
     * By default, the method returns <code>true</code> iff the clauses are not empty. For invariants,
     * the method also checks if it is not marked pure and if it is not static.
     * @param conditionType the type of the condition.
     * @param methodDecl the method to instrument.
     * @param isMarkedPure <code>true</code> if the method is marked as pure.
     * @param groupedClauses the clauses grouped by overriding method.
     * @return <code>true</code> iff the clauses should be added in the instrumented method.
     */
    private boolean shouldAddConditions(ContractConditionEnum conditionType, JCMethodDecl methodDecl, boolean isMarkedPure, List<List<ConditionClause>> groupedClauses) {
        BooleanSupplier defaultCheck = () -> groupedClauses.size() > 0 && groupedClauses.stream().allMatch((List<ConditionClause> clause) -> clause.size() > 0);

        switch (conditionType) {
            case INVARIANT:
                return !isMarkedPure && !methodDecl.sym.isStatic() && defaultCheck.getAsBoolean();
            default:
                return defaultCheck.getAsBoolean();
        }
    }

    /**
     * Builds the condition checker statements.
     *
     * This method appends a statements that creates the condition checker object, a set of statements
     * that append the conditions themselves, and a statement that checks them, optionally throwing
     * a {@see ContractConditionViolation} exception.
     * @param conditionType the condition type.
     * @param methodDecl the method to be instrumented.
     * @param block the block where to prepend the statements.
     * @param conditionGroups the conditions grouped by overriding method.
     */
    private void buildConditionsChecker(ContractConditionEnum conditionType, JCMethodDecl methodDecl, JCBlock block, List<List<ConditionClause>> conditionGroups) {
        if (conditionGroups.size() > 0) {
            var checkerVarDef = createCheckerDeclaration(conditionType, methodDecl.sym);
            var lambdaCalls = buildLambdaCalls(methodDecl, checkerVarDef, conditionGroups);
            var checkCall = buildCheckStatement(checkerVarDef);

            var conditionBlock = lambdaCalls.prepend(checkerVarDef).append(checkCall);

            if (javac.isSuperOrThisConstructorCall(block.stats.head)) {
                block.stats = block.stats.tail.prependList(conditionBlock).prepend(block.stats.head);
            } else {
                block.stats = block.stats.prependList(conditionBlock);
            }
        }
    }

    /**
     * Creates the local variable for the condition checker.
     * @param conditionType the type of the condition.
     * @param owner the owner method symbol, i.e., the instrumented method.
     * @return the variable declaration for the condition checker.
     */
    private JCVariableDecl createCheckerDeclaration(ContractConditionEnum conditionType, MethodSymbol owner) {
        String lowerCaseConditionTypeName = conditionType.name().toLowerCase();
        String checkerConstructorMethodName = "new" + conditionType.toString() + "Checker";

        var checkerVarSymbol = new VarSymbol(0,
                javac.Name(lowerCaseConditionTypeName + "Checker"),
                javac.preconditionCheckerType(), owner);
        checkerVarSymbol.adr = 0;

        return factory.VarDef(checkerVarSymbol, factory.Apply(List.nil(),
                factory.Select(javac.Expression(ConditionChecker.class.getCanonicalName()), javac.Name(checkerConstructorMethodName)), List.nil()));
    }

    /**
     * Builds the lambda functions that check each condition clause, and creates
     * the addConditionGroup calls to the condition checker.
     * @param methodDecl the currently instrumented method.
     * @param checkerVarDef the checker variable definition.
     * @param conditionGroups the conditions grouped by overriding method.
     * @return the list of statements corresponding to the conditions to be added to the checker.
     */
    private List<JCStatement> buildLambdaCalls(JCMethodDecl methodDecl, JCVariableDecl checkerVarDef, List<List<ConditionClause>> conditionGroups) {
        return conditionGroups.stream().map((List<ConditionClause> conditionGroup) -> {
            var lambdas = conditionGroup.stream().map((ConditionClause clause) -> {
                clause.resolveContractMethod(state.currentClassDecl().get());
                if (!clause.isResolved()) {
                    state.logError(methodDecl.pos(),clause.getClauseRep() + " not resolved, method not found.");
                }
                return (JCExpression) clause.createConditionLambda(methodDecl);
            }).collect(List.collector());

            return javac.MethodCall(factory.Ident(checkerVarDef), javac.Name("addConditionGroup"), lambdas);

        }).collect(List.collector());
    }

    /**
     * Builds the check statement for the condition checker.
     * @param checkerVarDef the variable declaration of the current condition checker.
     * @return the check statement.
     */
    private JCStatement buildCheckStatement(JCVariableDecl checkerVarDef) {
        return javac.MethodCall(factory.Ident(checkerVarDef), javac.Name("check"));
    }

    @Override
    public Void visitReturn(ReturnTree node, Deque<Tree> relevantScope) {
        var returnNode = (JCReturn) node;

        state.currentMethodReturnVarDecl().ifPresent((JCVariableDecl varDecl) -> {
            if (state.isCurrentMethodDecl(relevantScope.peek())) {
                var newArg = factory.Assign(factory.Ident(javac.Name(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING)), ((JCReturn) node).expr);
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
                methodInvocation.meth = factory.Select(javac.Expression(Contract.class.getCanonicalName()),javac.Name(Constants.STATIC_OLD_METHOD_IDENTIFIER_STRING));
                methodInvocation.args = methodInvocation.args.prepend(factory.Literal(paramName));
                methodInvocation.args = methodInvocation.args.prepend(factory.ClassLiteral(getLastMethodInScope(relevantScope).get().sym.enclClass()));
            } else {
                methodInvocation.meth = factory.Ident(javac.Name(Constants.INSTANCE_OLD_METHOD_IDENTIFIER_STRING));
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