package com.pietvandongen.purebliss.function;

public class SideEffectFunction {

    /**
     * This function takes two numbers and returns their sum plus a random number.
     * But this function cannot be considered pure: it does more than it advertises, thus causing side effects.
     *
     * @param a The first number.
     * @param b The second number.
     * @return The sum of the numbers and a random number.
     */
    public static int sum(int a, int b) {
        writeSomethingToFile();
        return a + b;
    }

    /**
     * A fake function, that could do anything, really.
     */
    private static void writeSomethingToFile() {
        // Write something to a file, for example.
    }
}
