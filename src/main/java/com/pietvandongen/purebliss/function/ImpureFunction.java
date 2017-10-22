package com.pietvandongen.purebliss.function;

import java.util.Random;

public class ImpureFunction {

    /**
     * This function takes two numbers and returns their sum plus a random number.
     * This makes the function impure: its output does not always return the same result.
     *
     * @param a The first number.
     * @param b The second number.
     * @return The sum of the numbers and a random number.
     */
    public static int sum(int a, int b) {
        return new Random().nextInt() + a + b;
    }
}
