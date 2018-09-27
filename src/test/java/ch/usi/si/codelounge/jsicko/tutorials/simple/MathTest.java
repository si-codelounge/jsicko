package ch.usi.si.codelounge.jsicko.tutorials.simple;

import ch.usi.si.codelounge.jsicko.Contract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MathTest {

    @Test
    public void sqrtWithNegativeArgument() throws Throwable {
        Executable textFixture = () -> Math.sqrt(-1.0);
        assertThrows(Contract.PreconditionViolation.class, textFixture);
    }

    @Test
    public void sqrtWithPositiveArgument() throws Throwable {
        Math.sqrt(4.0);
    }

    @Test
    public void badSqrtReturnsViolation() throws Throwable {
        Executable textFixture = () -> Math.badsqrt(4.0);
        assertThrows(Contract.PostconditionViolation.class, textFixture);
    }
}
