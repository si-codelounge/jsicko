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

import ch.usi.si.codelounge.jsicko.plugin.Constants;
import ch.usi.si.codelounge.jsicko.plugin.OldValuesTable;
import ch.usi.si.codelounge.jsicko.plugin.utils.ConditionChecker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface Contract {

    default OldValuesTable emptyOldValuesTable() {
        return new OldValuesTable();
    }
    default ConditionChecker emptyPreconditionChecker() { return ConditionChecker.newPreconditionChecker(); }

    default <X> X instanceOld(String rep, X object) {
        throw new RuntimeException("Illegal call of instanceOld(rep,object) method outside a compiled contract");
    }

    @SuppressWarnings("unchecked")
    static <X> X staticOld(Class<? extends Contract> clazz, String rep, X object) {
        try {
            var staticOldValuesTableField = clazz.getDeclaredField(Constants.STATIC_OLD_FIELD_IDENTIFIER_STRING);
            var staticOldValuesTable = (OldValuesTable) staticOldValuesTableField.get(null);
            return (X) staticOldValuesTable.getValue(rep);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Illegal call of staticOld(" + clazz + ",rep,object) method outside a compiled contract", e);
        }
    }

    static <T> T old(T object) {
        throw new RuntimeException("Illegal call of old(object) method outside a compiled contract");
    }

    /*
     * A basic form of purity check
     * that relies on proper implementation of
     * equality.
     */
    default public boolean pure() {
        return this.equals(old(this));
    }

    /**
     * Declares the preconditions of a method.
     *
     * The value of this class corresponds to the name of a boolean method
     * implementing a clause.
     */
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public static @interface Requires {
        String[] value();
    }

    /**
     * Declares the postconditions of a method.
     *
     * The value of this class corresponds to the name of a boolean method
     * implementing a clause.
     */
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public static @interface Ensures {
        String[] value();
    }

    /**
     * Declares the method as specifying a class invariant.
     *
     */
    @Target({ElementType.METHOD})
    public static @interface Invariant {

    }

    /**
     * Declares the method as pure, so that it can be used as a clause in
     * contract annotations.
     */
    @Target({ElementType.METHOD})
    public static @interface Pure {

    }

    /**
     * Abstract class representing generic contract condition violations.
     */
    abstract class ContractConditionViolation extends AssertionError {

        /**
         * Constructs an AssertionError with its detail message derived
         * from the specified object, which is converted to a string as
         * defined in section 15.18.1.1 of
         * <cite>The Java&trade; Language Specification</cite>.
         *<p>
         * If the specified object is an instance of {@code Throwable}, it
         * becomes the <i>cause</i> of the newly constructed assertion error.
         *
         * @param detailMessage value to be used in constructing detail message
         * @see   Throwable#getCause()
         */
        public ContractConditionViolation(Object detailMessage) {
            super(String.valueOf(detailMessage));

        }
    }

    final class PreconditionViolation extends ContractConditionViolation {

        /**
         * Constructs an AssertionError with its detail message derived
         * from the specified object, which is converted to a string as
         * defined in section 15.18.1.1 of
         * <cite>The Java&trade; Language Specification</cite>.
         *<p>
         * If the specified object is an instance of {@code Throwable}, it
         * becomes the <i>cause</i> of the newly constructed assertion error.
         *
         * @param detailMessage value to be used in constructing detail message
         * @see   Throwable#getCause()
         */
        public PreconditionViolation(Object detailMessage) {
            super(String.valueOf(detailMessage));

        }

    }

    final class PostconditionViolation extends ContractConditionViolation {

        /**
         * Constructs an AssertionError with its detail message derived
         * from the specified object, which is converted to a string as
         * defined in section 15.18.1.1 of
         * <cite>The Java&trade; Language Specification</cite>.
         *<p>
         * If the specified object is an instance of {@code Throwable}, it
         * becomes the <i>cause</i> of the newly constructed assertion error.
         *
         * @param detailMessage value to be used in constructing detail message
         * @see   Throwable#getCause()
         */
        public PostconditionViolation(Object detailMessage) {
            super(String.valueOf(detailMessage));

        }

    }

    final class InvariantViolation extends ContractConditionViolation {

        /**
         * Constructs an AssertionError with its detail message derived
         * from the specified object, which is converted to a string as
         * defined in section 15.18.1.1 of
         * <cite>The Java&trade; Language Specification</cite>.
         *<p>
         * If the specified object is an instance of {@code Throwable}, it
         * becomes the <i>cause</i> of the newly constructed assertion error.
         *
         * @param detailMessage value to be used in constructing detail message
         * @see   Throwable#getCause()
         */
        public InvariantViolation(Object detailMessage) {
            super(String.valueOf(detailMessage));

        }

    }


}

