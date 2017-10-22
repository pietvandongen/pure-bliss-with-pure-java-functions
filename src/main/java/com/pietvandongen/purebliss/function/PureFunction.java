package com.pietvandongen.purebliss.function;

public class PureFunction {

    /**
     * This function takes two numbers and returns their sum, nothing else.
     * This makes the function pure: its output always returns the same result, without causing side effects.
     *
     * @param a The first number.
     * @param b The second number.
     * @return The sum of the numbers.
     */
    public static int sum(int a, int b) {
        return a + b;
    }
}
