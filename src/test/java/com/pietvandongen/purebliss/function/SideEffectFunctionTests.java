package com.pietvandongen.purebliss.function;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SideEffectFunctionTests {

    /**
     * This test can verify the function's output being repeatedly correct,
     * but it does not detect its hidden side effects.
     */
    @Test
    public void thatSumAlwaysProducesTheSameOutput() {
        assertThat(SideEffectFunction.sum(2, 3), is(5));
    }
}
