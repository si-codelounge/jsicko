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
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sun.tools.javac.util.List.nil;

class ConditionClause {

    private static Pattern clauseFormatRegexp = Pattern.compile("(\\!?)([a-zA-Z_]+)");

    private final JavacUtils javac;
    private final String clauseRep;
    private final boolean isNegated;
    private final Name methodName;
    private final ContractConditionEnum conditionType;
    private Optional<Symbol.MethodSymbol> resolvedMethodSymbol;

    private ConditionClause(JavacUtils javac,  String clauseRep, ContractConditionEnum conditionType) {
        var clauseRepFormatMatcher = clauseFormatRegexp.matcher(clauseRep);
        if (!clauseRepFormatMatcher.matches())
            throw new IllegalArgumentException("Clause specification \"" + clauseRep + "\" is malformed");
        this.javac = javac;
        this.isNegated = !clauseRepFormatMatcher.group(1).isEmpty();
        this.methodName = javac.nameFromString(clauseRepFormatMatcher.group(2));
        this.clauseRep = clauseRep;
        this.conditionType = conditionType;
    }

    private ConditionClause(JavacUtils javac, Symbol.MethodSymbol invariantSymbol) {
        this.javac = javac;
        this.isNegated = false;
        this.methodName = invariantSymbol.name;
        this.clauseRep = invariantSymbol.name.toString();
        this.conditionType = ContractConditionEnum.INVARIANT;
    }

    public String getClauseRep() {
        return clauseRep;
    }

    public boolean isNegated() {
        return isNegated;
    }

    public Name getMethodName() {
        return methodName;
    }

    public ContractConditionEnum getConditionType() {
        return conditionType;
    }

    public boolean isResolved() {
        return this.resolvedMethodSymbol.isPresent();
    }

    public static List<ConditionClause> from(Contract.Ensures postconditionClauses, JavacUtils javac) {
        return Arrays.stream(postconditionClauses.value())
                .map((String clauseRep) -> new ConditionClause(javac, clauseRep,ContractConditionEnum.POSTCONDITION))
                .collect(Collectors.toList());
    }

    public static List<ConditionClause> from(Contract.Requires preconditionClause, JavacUtils javac) {
        return Arrays.stream(preconditionClause.value())
                .map((String clauseRep) -> new ConditionClause(javac,clauseRep,ContractConditionEnum.PRECONDITION))
                .collect(Collectors.toList());
    }

    public static List<ConditionClause> createInvariants(List<Symbol.MethodSymbol> invariants, JavacUtils javac) {
        return invariants.stream()
                .map((Symbol.MethodSymbol invariantSymbol) -> new ConditionClause(javac, invariantSymbol))
                .collect(Collectors.toList());
    }

    JCTree.JCIf createConditionCheck(MethodTree method, ConditionClause clause) {
        return javac.getFactory().at(((JCTree) method).pos).If(createConditionCheckExpression(method, clause),
                createConditionCheckBlock( method, clause),
                null);
    }

    private JCTree.JCExpression createConditionCheckExpression(MethodTree method, ConditionClause conditionClause) {
        var factory = javac.getFactory();

        if (!resolvedMethodSymbol.isPresent()) {
            return factory.Erroneous(com.sun.tools.javac.util.List.of((JCTree)method));
        }

        var clauseSymbol = resolvedMethodSymbol.get();

        List<JCTree.JCExpression> args = clauseSymbol.params().stream().map((Symbol.VarSymbol clauseParamSymbol) -> {
            var clauseParamName = clauseParamSymbol.name.toString();
            if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                clauseParamName = Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING;
            }
            return factory.Ident(javac.nameFromString(clauseParamName));
        }).collect(Collectors.toList());

        var call = factory.App(factory.Ident(resolvedMethodSymbol.get()), com.sun.tools.javac.util.List.from(args.toArray( new JCTree.JCExpression[] {})));
        var potentiallyNegatedCall = (conditionClause.isNegated() ? call: factory.Unary(JCTree.Tag.NOT,call));
        return factory.Parens(potentiallyNegatedCall);
    }

    private JCTree.JCBlock createConditionCheckBlock(MethodTree method, ConditionClause conditionClause) {
        var factory = javac.getFactory();

        String errorMessagePrefix = String.format("%s %s violated on method %s", conditionClause.getConditionType(), conditionClause.getClauseRep(), method.getName().toString());

        return factory.Block(0, com.sun.tools.javac.util.List.of(
                factory.Throw(
                        factory.NewClass(null, nil(),
                                factory.Ident(javac.nameFromString(conditionClause.getConditionType().getAssertionErrorSpecificClass().getSimpleName())),
                                com.sun.tools.javac.util.List.of(factory.Literal(TypeTag.CLASS, errorMessagePrefix)), null))));
    }

    void resolveContractMethod(JCTree.JCClassDecl classDecl) {
        var classType = classDecl.sym.type;
        var closure = javac.typeClosure(classType);

        var contractMethod = closure.stream().flatMap((Type closureElem) ->
                closureElem.asElement().getEnclosedElements().stream()
                        .filter((Symbol contractElement) -> contractElement.name.equals(this.methodName))
                        .map((Symbol contractElement) -> (Symbol.MethodSymbol) contractElement))
                .findFirst();

        this.resolvedMethodSymbol = contractMethod;
    }

    @Override
    public String toString() {
        return this.conditionType + " " + this.clauseRep;
    }
}
