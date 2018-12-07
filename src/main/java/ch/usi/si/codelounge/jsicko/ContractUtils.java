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

package ch.usi.si.codelounge.jsicko;

import com.google.common.collect.Ordering;

import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Utils to simplify the formulation of properties in contracts.
 */
public abstract class ContractUtils {

    private ContractUtils() { }

    /**
     * Represents boolean implication.
     * @param antecedent the antecedent of the implication.
     * @param consequent the consequent of the implication.
     * @return <code>true</code> iff antecedent implies consequent.
     */
    public static boolean implies(boolean antecedent, boolean consequent) {
        return !antecedent || consequent;
    }

    /**
     * Represents a boolean implication with an else condition.
     *
     * This utility method is used when you need to express a pair of implications where
     * the antecedent of the second is the negation of the antecedent of the first,
     * thus representing a logic if-then-else.
     * @param antecedent the antecedent of the implication.
     * @param consequent the consequent of the implication.
     * @param elseCondition the consequent when considering the negated antecedent.
     * @return <code>true</code> iff antecedent implies consequent and !antecedent implies elseCondition.
     */
    public static boolean implies(boolean antecedent, boolean consequent, boolean elseCondition) {
        return (!antecedent || consequent) &&
                (antecedent || elseCondition);
    }


    /**
     * Represents logical equality (if and only if), also called biconditional.
     * @param a the first argument of the equality.
     * @param b the second argument of the equality.
     * @return <code>true</code> iff antecedent is equal to consequent.
     */
    public static boolean iff(boolean a, boolean b) {
        return a == b;
    }

    /**
     * Implication as a binary operator.
     */
    public static BinaryOperator<Boolean> implies = ((Boolean antecedent, Boolean consequent) -> !antecedent || consequent);

    /**
     * Logical Equality as a binary operator.
     */
    public static BinaryOperator<Boolean> iff = ((Boolean antecedent, Boolean consequent) -> antecedent == consequent);


    /**
     * Represents a universal qualification over a finite set (i.e., a range) of integers.
     *
     * @param lower the lower bound of the set.
     * @param upper the upper (excluded) bound of the integer set.
     * @param argument an integer predicate as the argument of the quantification.
     * @return <code>true</code> iff <code>argument</code> holds for every integer between <code>lower</code> (included) and <code>upper</code> excluded.
     */
    public static boolean forAllInts(int lower, int upper, IntPredicate argument) {
        return IntStream.range(lower,upper).allMatch(argument);
    }

    /**
     * Represents a universal qualification over the elements of a collection.
     * @param collection A collection of elements.
     * @param argument a predicate over <code>E</code>.
     * @param <E> the type of the elements in the collection.
     * @return <code>true</code> iff <code>argument</code> holds for every element in the <code>collection</code>.
     */
    public static <E> boolean forAll(Collection<E> collection, Predicate<E> argument) {
        return collection.stream().allMatch(argument);
    }

    /**
     * Represents an existential qualification over a finite set (i.e., a range) of integers.
     * @param lower the lower bound of the set.
     * @param upper the upper (excluded) bound of the integer set.
     * @param argument an integer predicate as the argument of the quantification.
     * @return <code>true</code> iff <code>argument</code> holds for at least one integer between <code>lower</code> (included) and <code>upper</code> excluded.
     */
    public static boolean existsInt(int lower, int upper, IntPredicate argument) {
        return IntStream.range(lower,upper).anyMatch(argument);
    }

    /**
     * Represents an existential qualification over the elements of a collection.
     * @param collection A collection of elements.
     * @param argument a predicate over <code>E</code>.
     * @param <E> the type of the elements in the collection.
     * @return <code>true</code> iff <code>argument</code> holds for at least one element in the <code>collection</code>.
     */
    public static <E> boolean exists(Collection<E> collection, Predicate<E> argument) {
        return collection.stream().anyMatch(argument);
    }

    /**
     * Counts the elements of a collection that satisfy a given predicate.
     * @param collection A collection of elements.
     * @param argument a predicate over <code>E</code>.
     * @param <E> the type of the elements in the collection.
     * @return the number of elements in the collection that satisfy the argument.
     */
    public static <E> long count(Collection<E> collection, Predicate<E> argument) {
        return collection.stream().filter(argument).count();
    }

    /**
     * Checks if a given collection is sorted.
     * @param returns the collection to be checked.
     * @param <T> The type of the collection, must be comparable.
     * @return <code>true</code> iff the collection is sorted.
     */
    public static <T extends Comparable<? super T>> boolean isSorted(Collection<T> returns) {
        return Ordering.natural().isOrdered(returns);
    }
}
