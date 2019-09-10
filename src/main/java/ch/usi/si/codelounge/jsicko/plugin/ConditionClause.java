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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Represents a clause in a condition.
 */
class ConditionClause {

    private static Pattern clauseFormatRegexp = Pattern.compile("(\\!?)([A-Za-z][A-Za-z0-9_]*)");

    private final JavacUtils javac;
    private final TreeMaker factory;
    private final String clauseRep;
    private final boolean isNegated;
    private final Name methodName;
    private final ContractConditionEnum conditionType;
    private Optional<MethodSymbol> resolvedMethodSymbol;

    /**
     * Constructs a new clause.
     *
     * @param javac the javac utils object for this compilation task.
     * @param declaringSymbol the symbol where the clause is used.
     * @param clauseRep the clause representation.
     * @param conditionType the condition type.
     */
    private ConditionClause(JavacUtils javac, Symbol declaringSymbol, String clauseRep, ContractConditionEnum conditionType) {
        var clauseRepFormatMatcher = clauseFormatRegexp.matcher(clauseRep);
        if (!clauseRepFormatMatcher.matches())
            throw new IllegalArgumentException("Clause specification name \"" + clauseRep + "\" is malformed. Please use a valid Java identifier / match regexp " + clauseFormatRegexp.toString());
        this.javac = javac;
        this.factory = javac.getFactory();
        this.isNegated = !clauseRepFormatMatcher.group(1).isEmpty();
        this.methodName = javac.Name(clauseRepFormatMatcher.group(2));
        this.clauseRep = "clause " + clauseRep + " in " + declaringSymbol.owner.getSimpleName() + "#" + declaringSymbol.toString();
        this.conditionType = conditionType;
    }

    /**
     * Declares a clause for an invariant.
     *
     * @param javac the javac utils object for this compilation task.
     * @param invariantSymbol the symbol representing an invariant clause.
     */
    private ConditionClause(JavacUtils javac, Symbol invariantSymbol) {
        this.javac = javac;
        this.factory = javac.getFactory();
        this.isNegated = false;
        this.methodName = invariantSymbol.name;
        this.clauseRep = "clause " +  invariantSymbol.name.toString() + " in " + invariantSymbol.owner.getSimpleName();
        this.conditionType = ContractConditionEnum.INVARIANT;
    }

    /**
     * Returns the clause representation.
     * @return the clause representation.
     */
    public String getClauseRep() {
        return clauseRep;
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
     * @param javac the javac utility object.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> from(Contract.Ensures postconditionAnnotation, Symbol declaringSymbol, JavacUtils javac) {
        return Arrays.stream(postconditionAnnotation.value())
                .map((String clauseRep) -> new ConditionClause(javac, declaringSymbol, clauseRep, ContractConditionEnum.POSTCONDITION))
                .collect(List.collector());
    }

    /**
     * Statically constructs clauses from a set of requires annotations.
     * @param preconditionAnnotation a precondition annotation.
     * @param declaringSymbol a symbol annotated with the preconditions.
     * @param javac the javac utility object.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> from(Contract.Requires preconditionAnnotation,  Symbol declaringSymbol, JavacUtils javac) {
        return Arrays.stream(preconditionAnnotation.value())
                .map((String clauseRep) -> new ConditionClause(javac, declaringSymbol, clauseRep, ContractConditionEnum.PRECONDITION))
                .collect(List.collector());
    }

    /**
     * Statically constructs a set of invariant clauses from annotated method symbols.
     * @param invariants a set of method symbols representing class invariants.
     * @param javac the javac utility object.
     * @return a list of condition clauses.
     */
    public static List<ConditionClause> createInvariants(List<MethodSymbol> invariants, JavacUtils javac) {
        return invariants.stream()
                .map((MethodSymbol invariantSymbol) -> new ConditionClause(javac, invariantSymbol))
                .collect(List.collector());
    }

    /**
     * Creates a condition lambda, i.e., a lambda function that evaluates the condition method
     * and optionally returns a string representing the condition violation.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return a lambda expression representing an optional-string supplier.
     */
    JCLambda createConditionLambda(JCMethodDecl methodDecl) {
        var optionalOfExpression = javac.Expression("java.util.Optional.of");
        var optionalEmptyExpression = javac.Expression("java.util.Optional.empty");

        var optionalOfCall = factory.Apply(List.nil(), optionalOfExpression,
                List.of(factory.Binary(Tag.PLUS,factory.Literal(this.clauseRep + "; params: "), createParamValuesStringExpression(methodDecl))));
        var optionalEmptyCall = factory.Apply(List.nil(), optionalEmptyExpression, List.nil());
        var lambdaBody = factory.If(createConditionCheckExpression(methodDecl),
                factory.Return(optionalOfCall),
                factory.Return(optionalEmptyCall));
        return factory.Lambda(List.nil(), factory.Block(0, List.of(lambdaBody)));
    }

    /**
     * Creates the condition check expression for this clause.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return an expression representing the condition to be checked, true when the clause fails.
     */
    private JCExpression createConditionCheckExpression(JCMethodDecl methodDecl) {
        var factory = javac.getFactory();

        if (!resolvedMethodSymbol.isPresent()) {
            return factory.Erroneous(List.of(methodDecl));
        }

        var clauseSymbol = resolvedMethodSymbol.get();

        List<JCExpression> args = clauseSymbol.params().stream().map((VarSymbol clauseParamSymbol) -> {
            var clauseParamName = clauseParamSymbol.name.toString();
            if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                clauseParamName = Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING;
            } else if (clauseParamName.equals(Constants.RAISES_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                clauseParamName = Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING;
            }
            return factory.Ident(javac.Name(clauseParamName));
        }).collect(List.collector());

        var call = factory.App(factory.Ident(resolvedMethodSymbol.get()), List.from(args.toArray( new JCExpression[] {})));
        var potentiallyNegatedCall = (this.isNegated() ? call: factory.Unary(Tag.NOT,call));
        return factory.Parens(potentiallyNegatedCall);
    }

    /**
     * Creates a string expression that represents the local params value after a violation.
     * @param methodDecl the declaring method, used for reporting purposes.
     * @return a string concatenation expression with local params names and values.
     */
    private JCExpression createParamValuesStringExpression(JCMethodDecl methodDecl) {
        var factory = javac.getFactory();

        if (!resolvedMethodSymbol.isPresent()) {
            return factory.Erroneous(List.of(methodDecl));
        }

        var clauseSymbol = resolvedMethodSymbol.get();

        List<JCExpression> thisExpression = (!methodDecl.sym.isStatic()) ?
                List.of(factory.Literal(  "this: "), factory.Ident(javac.Name("this")), factory.Literal(", ")):
                List.nil();

        List<JCExpression> args = clauseSymbol.params().stream().flatMap((VarSymbol clauseParamSymbol) -> {
            var clauseParamName = clauseParamSymbol.name.toString();
            if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                clauseParamName = Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING;
            } else if (clauseParamName.equals(Constants.RAISES_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                clauseParamName = Constants.RAISES_SYNTHETIC_IDENTIFIER_STRING;
            }

            return Stream.of(factory.Literal(clauseParamName + ": "), factory.Ident(javac.Name(clauseParamName)), factory.Literal(", "));
        }).collect(List.collector()).prependList(thisExpression);

        var fullArgs = args.prepend(factory.Literal("["))
                .take(args.size() > 1 ? args.size(): args.size() + 1)
                .append(factory.Literal("]"));

        JCExpression sum = fullArgs.stream().reduce((JCExpression a, JCExpression b) -> factory.Binary(Tag.PLUS,a,b)).orElseGet(() -> (JCExpression)factory.Literal(""));
        return factory.Parens(sum);
    }

    /**
     * Tries to resolve the condition method.
     * @param classDecl the class where the clause is used.
     */
    void resolveContractMethod(JCClassDecl classDecl) {
        var classType = classDecl.sym.type;
        var closure = javac.typeClosure(classType);

        var contractMethod = closure.stream().flatMap((Type closureElem) ->
                closureElem.asElement().getEnclosedElements().stream()
                        .filter((Symbol contractElement) -> contractElement.name.equals(this.methodName))
                        .map((Symbol contractElement) -> (MethodSymbol) contractElement))
                .findFirst();

        this.resolvedMethodSymbol = contractMethod;
    }

    @Override
    public String toString() {
        return this.conditionType + " " + this.clauseRep;
    }
}
