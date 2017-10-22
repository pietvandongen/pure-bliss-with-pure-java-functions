package com.pietvandongen.purebliss.function;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PureFunctionTests {

    /**
     * The pure function under test will always return the same result, no matter how often we call it.
     */
    @Test
    public void thatSumAlwaysProducesTheSameOutput() {
        assertThat(PureFunction.sum(2, 3), is(5));
    }
}
