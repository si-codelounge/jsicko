package ch.usi.si.codelounge.jsicko.tutorials.simple;

import ch.usi.si.codelounge.jsicko.Contract;

public abstract class Math implements Contract {

    @Requires("non_negative_arg")
    @Ensures("returns_approximately_equal_to_square_of_arg")
    public static double sqrt(double arg) {
        return java.lang.Math.sqrt(arg);
    }

    @Requires("non_negative_arg")
    @Ensures("returns_approximately_equal_to_square_of_arg")
    public static double badsqrt(double arg) {
        return java.lang.Math.pow(arg,0.48);
    }

    @Pure
    private static boolean non_negative_arg(double arg) {
        return arg >= 0;
    }

    @Pure
    private static boolean returns_approximately_equal_to_square_of_arg(double returns, double arg) {
        return java.lang.Math.abs((returns * returns) - arg) < 0.001;
    }

}
