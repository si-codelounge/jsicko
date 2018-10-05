package ch.usi.si.codelounge.jsicko;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface Contract<T> {

    default public T old() {
        throw new RuntimeException("Illegal call of old() method outside a compiled contract");
    }

    /*
     * A basic form of purity check
     * that relies on proper implementation of
     * equality.
     */
    default public boolean pure() {
        return this.equals(old());
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

