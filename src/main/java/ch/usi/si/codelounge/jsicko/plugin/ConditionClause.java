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
import ch.usi.si.codelounge.jsicko.plugin.diagnostics.JSickoDiagnostic;
import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Represents a clause in a condition.
 */
public class ConditionClause {

    private static Pattern clauseFormatRegexp = Pattern.compile("(\\!?)([A-Za-z][A-Za-z0-9_]*)");

    private final JavacUtils javac;
    private final JSickoContractCompilerState state;
    private final TreeMaker factory;
    private final boolean isNegated;
    private final Name methodName;
    private final ContractConditionEnum conditionType;
    private final String clauseRep;
    private Optional<MethodSymbol> resolvedMethodSymbol;
    private final Symbol declaringSymbol;

    /**
     * Constructs a new clause.
     *
     * @param declaringSymbol the symbol where the clause is used.
     * @param clauseRep the clause representation.
     * @param conditionType the condition type.
     */
    private ConditionClause(JSickoContractCompilerState state, JavacUtils javac, Symbol declaringSymbol, String clauseRep, ContractConditionEnum conditionType) {
        var clauseRepFormatMatcher = clauseFormatRegexp.matcher(clauseRep);
        if (!clauseRepFormatMatcher.matches())
            throw new IllegalArgumentException("Clause specification name \"" + clauseRep + "\" is malformed. Please use a valid Java identifier / match regexp " + clauseFormatRegexp.toString());
        this.state = state;
        this.javac = javac;
        this.factory = javac.getFactory();
        this.isNegated = !clauseRepFormatMatcher.group(1).isEmpty();
        this.methodName = javac.Name(clauseRepFormatMatcher.group(2));
        this.clauseRep = "clause " + clauseRep + " in " + declaringSymbol.owner.getSimpleName() + "#" + declaringSymbol.toString();
        this.declaringSymbol = declaringSymbol;
        this.conditionType = conditionType;
    }

    /**
     * Declares a clause for an invariant.
     *
     * @param javac the javac utils object for this compilation task.
     * @param invariantSymbol the symbol representing an invariant clause.
     */
    private ConditionClause(JSickoContractCompilerState state, JavacUtils javac, Symbol.MethodSymbol invariantSymbol) {
        this.state = state;
        this.javac = javac;
        this.factory = javac.getFactory();
        this.isNegated = false;
        this.methodName = invariantSymbol.name;
        this.clauseRep = "clause " +  invariantSymbol + " in " + invariantSymbol.owner.getSimpleName();
        this.conditionType = ContractConditionEnum.INVARIANT;
        this.declaringSymbol = invariantSymbol.owner;
        this.resolvedMethodSymbol = Optional.of(invariantSymbol);
    }

    /**
     * Returns the clause representation.
     * @return the clause representation.
     */
    public String getClauseRep() {
        return clauseRep;
    }

    public Optional<Integer> getArity() {
        return this.resolvedMethodSymbol.map(methodSymbol -> methodSymbol.params().length());
    }

    /**
     * Returns the clause representation with explicit param names.
     * @return the clause representation with explicit param names for the clause method and the declaring method.
     */
    public String getResolvedClauseRepWithNames() {
        if (this.conditionType.equals(ContractConditionEnum.INVARIANT)) {
            return clauseRep;
        } else {
            var clauseRepWithNames = this.resolvedMethodSymbol.map(s -> s.getSimpleName() +
                    "(" + s.params().map(p -> p.type + " " + p.name).toString(", ") + ")")
                    .orElse(methodName.toString());
            var declaringSymbolWithNames = this.declaringSymbol.getSimpleName() +
                    "(" + ((MethodSymbol) this.declaringSymbol).params().map(p -> p.type + " " + p.name).toString(", ") + ")";
            return "clause " + clauseRepWithNames + " in " + declaringSymbol.owner.getSimpleName() + "#" + declaringSymbolWithNames;
        }
    }

    /**
     * Returns <code>true</code> iff the clause is negated.
     * @return the negates status of the clause.
     */
    public boolean isNegated() {
        return isNegated;
    }

    /**
     * Returns the name of the clause method.
     * @return the name of the clause method.
     */
    public Name getMethodName() {
        return methodName;
    }

    /**
     * Returns the condition type where the clause is used.
     * @return the condition type.
     */
    public ContractConditionEnum getConditionType() {
        return conditionType;
    }

    /**
     * Returns <code>true</code> iff the clause method is resolved.
     * @return <code>true</code> iff the compiler resolved the clause method.
     */
    public boolean isResolved() {
        return this.resolvedMethodSymbol.isPresent();
    }

    /**
     * Statically constructs clauses from a set of ensures annotations.
     * @param postconditionAnnotation a postcondition annotation.
     * @param declaringSymbol a symbol annotated with the postconditions.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> from(Contract.Ensures postconditionAnnotation, Symbol declaringSymbol, JavacUtils javac, JSickoContractCompilerState state) {
        return Arrays.stream(postconditionAnnotation.value())
                .map((String clauseRep) -> new ConditionClause(state, javac, declaringSymbol, clauseRep, ContractConditionEnum.POSTCONDITION))
                .collect(List.collector());
    }

    /**
     * Statically constructs clauses from a set of requires annotations.
     * @param preconditionAnnotation a precondition annotation.
     * @param declaringSymbol a symbol annotated with the preconditions.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> from(Contract.Requires preconditionAnnotation,  Symbol declaringSymbol, JavacUtils javac, JSickoContractCompilerState state) {
        return Arrays.stream(preconditionAnnotation.value())
                .map((String clauseRep) -> new ConditionClause(state, javac, declaringSymbol, clauseRep, ContractConditionEnum.PRECONDITION))
                .collect(List.collector());
    }

    /**
     * Statically constructs a set of invariant clauses from annotated method symbols.
     * @param invariants a set of method symbols representing class invariants.
     * @param javac the javac utility object.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> createInvariants(List<MethodSymbol> invariants, JavacUtils javac,  JSickoContractCompilerState state) {
        return invariants.stream()
                .map((MethodSymbol invariantSymbol) -> new ConditionClause(state, javac, invariantSymbol))
                .collect(List.collector());
    }

    /**
     * Creates a condition lambda, i.e., a lambda function that evaluates the condition method
     * and optionally returns a string representing the condition violation.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return a lambda expression representing an optional-string supplier.
     */
    JCLambda createConditionLambda(JCMethodDecl methodDecl) {

        var stringBuilderType = javac.retrieveType(javac.javaBaseModule(),"java.lang.StringBuilder");
        var varSymbol = new VarSymbol(0,
                javac.Name("$msg"), stringBuilderType, methodDecl.sym);

        var init = factory.NewClass(null, List.nil(), javac.Type(stringBuilderType), List.nil(), null);
        var ctor = javac.retrieveEmptyConstructor(javac.unnamedModule(), "java.lang.StringBuilder");
        init.constructor = ctor;
        init.setType(stringBuilderType);

        JCStatement varDef = factory.VarDef(varSymbol, init);
        var ident = factory.Ident(varSymbol);
        ident.type = stringBuilderType;
        ident.sym = varSymbol;

        var stmts = createParamValuesStringExpression(ident, methodDecl, List.of(factory.Literal(this.clauseRep + "; params: ")));
        var binaryPlus = javac.MethodInvocation(javac.javaBaseModule(), ident, javac.Name("toString"), List.nil());

        JCStatement optionalOfCall = factory.Return(javac.MethodInvocation(javac.unnamedModule(), javac.Expression(javac.unnamedModule(), "java.util.Optional"), javac.Name("of"),
                List.of(binaryPlus)));

        var allStmts = stmts.prepend(varDef).append(optionalOfCall);
        var ifThen = factory.Block(0, allStmts);
        var optionalEmptyCall = javac.MethodInvocation(javac.unnamedModule(), javac.Expression(javac.unnamedModule(), "java.util.Optional"), javac.Name("empty"));
        var lambdaBody = factory.If(createConditionCheckExpression(methodDecl),
                ifThen,
                factory.Return(optionalEmptyCall));
        var lambda =  factory.Lambda(List.nil(), factory.Block(0, List.of(lambdaBody)));

        javac.visitLambda(lambda);
        return lambda;
    }

    /**
     * Creates the condition check expression for this clause.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return an expression representing the condition to be checked, true when the clause fails.
     */
    private JCExpression createConditionCheckExpression(JCMethodDecl methodDecl) {
        var factory = javac.getFactory();

        var clauseSymbol = resolvedMethodSymbol.get();

        List<Optional<VarSymbol>> resolvedVarSymbols = clauseSymbol.params().stream().map((VarSymbol clauseParamSymbol) -> {
            Optional<VarSymbol> symbol;
            var clauseParamName = clauseParamSymbol.name.toString();
            if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                if (this.getConditionType().equals(ContractConditionEnum.PRECONDITION)) {
                    symbol = Optional.empty();
                    state.logError(methodDecl.pos(), JSickoDiagnostic.ReturnsOnPrecondition(this));
                } else {
                    symbol = state.currentMethodReturnVarDecl().map(s -> s.sym);
                    if (!symbol.isPresent()) {
                        state.logError(methodDecl.pos(), JSickoDiagnostic.ReturnsOnVoidMethod(this));
                    }
                }
            } else if (clauseParamName.equals(Constants.RAISES_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                if (this.getConditionType().equals(ContractConditionEnum.PRECONDITION)) {
                    symbol = Optional.empty();
                    state.logError(methodDecl.pos(), JSickoDiagnostic.RaisesOnPrecondition(this));
                } else {
                    symbol = Optional.of(state.currentMethodRaisesVarDecl().get().sym);
                }
            } else {
                symbol = methodDecl.sym.params().stream().filter(f -> f.name.equals(clauseParamSymbol.name)).findFirst();
                if (!symbol.isPresent()) {
                    state.logError(methodDecl.pos(), JSickoDiagnostic.MissingParamName(clauseParamSymbol.name, this));
                }
            }
            if (symbol.isPresent() && !this.javac.isTypeAssignable(symbol.get(), clauseParamSymbol)) {
                state.logError(methodDecl.pos(), JSickoDiagnostic.WrongParamType(clauseParamSymbol.name, symbol.get().type, clauseParamSymbol.type, this));
                symbol = Optional.empty();
            }
            return symbol;
        }).collect(List.collector());

        if (resolvedVarSymbols.stream().anyMatch(o -> !o.isPresent())) {
            return javac.falseLiteral();
        }

        List<JCExpression> args = resolvedVarSymbols.stream().map(o -> o.get()).map((VarSymbol symbol) -> {
            var ident = factory.Ident(symbol);
            ident.setType(symbol.type);
            ident.sym = symbol;
            return ident;
        }).collect(List.collector());

        var ident = factory.Ident(resolvedMethodSymbol.get());
        ident.setType(resolvedMethodSymbol.get().type);
        ident.sym = resolvedMethodSymbol.get();
        var call = factory.App(ident, List.from(args.toArray( new JCExpression[] {})));
        var unaryOp = factory.Unary(Tag.NOT,call);
        this.javac.setOperator(unaryOp);
        var potentiallyNegatedCall = (this.isNegated() ? call: unaryOp);
        var result = factory.Parens(potentiallyNegatedCall);
        result.setType(javac.booleanType());
        return result;
    }

    /**
     * Creates a string expression that represents the local params value after a violation.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return a string concatenation expression with local params names and values.
     */
    private List<JCStatement> createParamValuesStringExpression(JCIdent sb, JCMethodDecl methodDecl, List<JCExpression> prefix) {
        var factory = javac.getFactory();

        var clauseSymbol = resolvedMethodSymbol.get();

        List<JCExpression> thisExpression = (!methodDecl.sym.isStatic()) ?
                List.of(factory.Literal(  "this: "), factory.This(methodDecl.sym.owner.type), factory.Literal(", ")):
                List.nil();

        List<Optional<VarSymbol>> resolvedVarSymbols = clauseSymbol.params().stream().map((VarSymbol clauseParamSymbol) -> {
                    Optional<VarSymbol> symbol;
                    var clauseParamName = clauseParamSymbol.name.toString();
                    if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                        symbol = state.currentMethodReturnVarDecl().map(s -> s.sym);
                        if (!symbol.isPresent()) {
                            state.logError(methodDecl.pos(), JSickoDiagnostic.ReturnsOnVoidMethod(this));
                        }
                    } else if (clauseParamName.equals(Constants.RAISES_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                        symbol = Optional.of(state.currentMethodRaisesVarDecl().get().sym);
                    } else {
                        symbol = methodDecl.sym.params().stream().filter(f -> f.name.equals(clauseParamSymbol.name)).findFirst();
                        if (!symbol.isPresent()) {
                            state.logError(methodDecl.pos(), JSickoDiagnostic.MissingParamName(clauseParamSymbol.name, this));
                        }
                    }
                    return symbol;
                }).collect(List.collector());

        if (resolvedVarSymbols.stream().anyMatch(o -> !o.isPresent())) {
            return List.nil();
        }

        List<JCExpression> args = resolvedVarSymbols.stream().map(o -> o.get()).flatMap((VarSymbol symbol) -> {
            var clauseParamIdent = factory.Ident(symbol);
            clauseParamIdent.setType(symbol.type);
            clauseParamIdent.sym = symbol;
            return Stream.of(factory.Literal(symbol.name + ": "), clauseParamIdent, factory.Literal(", "));
        }).collect(List.collector()).prependList(thisExpression);

        var stringElems = args.prepend(factory.Literal("["))
                .take(args.size() > 1 ? args.size(): args.size() + 1)
                .append(factory.Literal("]")).prependList(prefix);

        var reducedLiterals = stringElems.stream().reduce(List.<JCExpression>nil(),
                (l,e) -> {
                    if (l.isEmpty())
                        return l.prepend(e);
                    var head = l.head;
                    if (head instanceof JCLiteral && e instanceof  JCLiteral) {
                        return l.tail.prepend(factory.Literal((String) ((JCLiteral) head).value + ((JCLiteral) e).value));
                    } else {
                        return l.prepend(e);
                    }
                }, (l1,l2) -> l1.appendList(l2)).reverse();


        List<JCStatement> sum = reducedLiterals.stream().map((JCExpression elem) -> {
            var arg = elem;
            if (!elem.type.toString().equals(javac.stringType().toString())) {
                if (elem.type instanceof Type.ArrayType) {
                    arg = javac.MethodInvocation(javac.javaBaseModule(),
                            javac.Type(javac.retrieveType(javac.javaBaseModule(), "java.util.Arrays")), javac.Name("toString"), List.of(elem));
                } else {
                    arg = javac.MethodInvocation(javac.javaBaseModule(),
                            javac.Type(javac.stringType()), javac.Name("valueOf"), List.of(elem));
                }
            }
            var append = javac.MethodCall(javac.javaBaseModule(),
                    sb, javac.Name("append"), List.of(arg));
            return append;
        }).collect(List.collector());

        return sum;
    }

    /**
     * Tries to resolve the condition method.
     * @param classDecl the class where the clause is used.
     */
    List<JSickoDiagnostic.JSickoError> resolveContractMethod(JCClassDecl classDecl) {
        var classType = classDecl.sym.type;
        var closure = javac.typeClosure(classType);

        var optionalContractMethod = closure.stream().flatMap((Type closureElem) ->
                closureElem.asElement().getEnclosedElements().stream()
                        .filter((Symbol contractElement) -> contractElement.name.equals(this.methodName))
                        .map((Symbol contractElement) -> (MethodSymbol) contractElement))
                .findFirst();

        if (optionalContractMethod.isPresent()) {
            var resolvedMethod = optionalContractMethod.get();
            if (resolvedMethod.getReturnType() != null && !resolvedMethod.getReturnType().equals(javac.booleanType())) {
                return List.of(JSickoDiagnostic.ClauseIsNotBoolean(this,resolvedMethod));
            }
        }

        if (optionalContractMethod.isEmpty()) {
            return List.of(JSickoDiagnostic.MissingClause(this));
        }

        this.resolvedMethodSymbol = optionalContractMethod;
        return List.nil();
    }

    public boolean isClauseMethodStatic() {
        if (!this.isResolved()) {
            throw new IllegalStateException("Contract method not resolved yet");
        }
        var clauseMethod = this.resolvedMethodSymbol.get();
        return clauseMethod.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public String toString() {
        return this.conditionType + " " + this.clauseRep;
    }


}
