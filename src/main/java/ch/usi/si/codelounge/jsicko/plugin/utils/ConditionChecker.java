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

package ch.usi.si.codelounge.jsicko.plugin.utils;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.plugin.ContractConditionEnum;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ConditionChecker {

    private static class ConjunctConditionViolationSuppliers {

        private final List<Supplier<Optional<String>>> conjunctSuppliers;

        ConjunctConditionViolationSuppliers(List<Supplier<Optional<String>>> conjunctSuppliers) {
            this.conjunctSuppliers = Collections.unmodifiableList(conjunctSuppliers);
        }

        @SafeVarargs
        ConjunctConditionViolationSuppliers(Supplier<Optional<String>>... conjunctSuppliers) {
            this(Arrays.asList(conjunctSuppliers));
        }

        Optional<String> getFirstViolation() {
            return conjunctSuppliers.stream().flatMap((Supplier<Optional<String>> conditionElem) -> conditionElem.get().stream()).findFirst();
        }

    }

    public static ConditionChecker newPreconditionChecker() {
        return new ConditionChecker(ContractConditionEnum.PRECONDITION);
    }

    public static ConditionChecker newPostconditionChecker() {
        return new ConditionChecker(ContractConditionEnum.POSTCONDITION);
    }

    public static ConditionChecker newInvariantChecker() {
        return new ConditionChecker(ContractConditionEnum.INVARIANT);
    }

    private final List<ConjunctConditionViolationSuppliers> conditionViolationSuppliersGroups;
    private final ContractConditionEnum contractConditionType;
    private final Function<String, Contract.ContractConditionViolation> violationSupplier;

    private ConditionChecker(ContractConditionEnum contractConditionType) {
        this.contractConditionType = contractConditionType;
        this.conditionViolationSuppliersGroups = new LinkedList<>();
        this.violationSupplier = contractConditionType.violationConstructor();
    }

    @SafeVarargs
    public final void addConditionGroup(Supplier<Optional<String>>... conditionGroupViolationSuppliers) {
        this.conditionViolationSuppliersGroups.add(new ConjunctConditionViolationSuppliers(conditionGroupViolationSuppliers));
    }

    public final void check() throws Contract.ContractConditionViolation {
        var groupedViolations = this.conditionViolationSuppliersGroups.stream()
                .map((ConjunctConditionViolationSuppliers conditionGroup) -> conditionGroup.getFirstViolation())
                .collect(Collectors.toList());
        var hasViolation = groupedViolations.stream().allMatch((Optional<String> results) -> results.isPresent());
        if (hasViolation) {
            var groupedViolationReps = groupedViolations.stream().flatMap((Optional<String> violation) -> violation.stream()).collect(Collectors.toList());
            throw this.violationSupplier.apply(groupedViolationReps.toString());
        }
        return;
    }
}
