package com.pietvandongen.purebliss.function;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore("We cannot test this class reliably.")
public class ImpureFunctionTests {

    /**
     * The impure function under test will always return unpredictable results, so we cannot test it reasonably.
     */
    @Test
    public void thatSumAlwaysProducesTheSameOutput() {
        assertThat(ImpureFunction.sum(2, 3), is(5));
    }
}
