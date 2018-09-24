package ch.usi.si.codelounge.jsicko;

import ch.usi.si.codelounge.jsicko.tutorials.stack.BadStack;
import ch.usi.si.codelounge.jsicko.tutorials.stack.Stack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simple tests for the bad Stack.
 */
public class BadStackTest {

    @Test
    public void popOnEmptyStack() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        assertThrows(Contract.PreconditionViolation.class,foo::pop);
    }

    @Test
    public void pushTest() throws Throwable {
        BadStack<String> foo = new BadStack<String>();
        foo.push("elem");
    }

    @Test
    public void baseTest() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        foo.push("elem");
        foo.peek();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        BadStack<String> foo = new BadStack<>();
        foo.push("elem1");
        assertThrows(Contract.PostconditionViolation.class, () -> foo.push("elem2"));
    }

}
