package ch.usi.si.codelounge.jsicko.plugin;

/**
 * Holds some constants needed for code generation.
 */
public final class Constants {

    private Constants() {}

    /**
     * The synthetic local variable for the variable holding the return value
     * in an instrumented method.
     *
     * This variable is essential check post-conditions for query/
     * hybrid command/query methods.
     */
    public static final String RETURNS_SYNTHETIC_IDENTIFIER_STRING = "$returns";

    /**
     * A special identifier to be used in clause methods to refer to the value returned
     * by a method.
     */
    public static final String RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING = "returns";

    /**
     * The name of the synthetic instance field used to store the old values table
     * for instance method calls.
     */
    public static final String OLD_FIELD_IDENTIFIER_STRING = "$oldValuesTable";

    /**
     * The name of the synthetic static field used to store the old values table
     * for static method calls.
     */
    public static final String STATIC_OLD_FIELD_IDENTIFIER_STRING = "$staticOldValuesTable";

    /**
     * The name of the method called to retrieve old values in instance method calls.
     *
     * Calls to the static old method of Contract are rewritten to this method in the
     * case of instance method calls.
     *
     * @see ch.usi.si.codelounge.jsicko.Contract#old(Object)
     * @see ch.usi.si.codelounge.jsicko.Contract#instanceOld(String, Object)
     */
    public static final String INSTANCE_OLD_METHOD_IDENTIFIER_STRING = "instanceOld";

    /**
     * The name of the method called to retrieve old values in static method calls.
     *
     * Calls to the static old method of Contract are rewritten to this method in the
     * case of static method calls.
     *
     * @see ch.usi.si.codelounge.jsicko.Contract#old(Object) s
     * @see ch.usi.si.codelounge.jsicko.Contract#staticOld(Class, String, Object)
     */
    public static final String STATIC_OLD_METHOD_IDENTIFIER_STRING = "staticOld";
}
