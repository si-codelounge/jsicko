package ch.usi.si.codelounge.jsicko;

import ch.usi.si.codelounge.jsicko.tutorials.stack.GoodStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoodStackTests {

    @Test
    public void popOnEmptyStack() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        assertThrows(Contract.PreconditionViolation.class,foo::pop);
    }

    @Test
    public void pushTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
    }

    @Test
    public void elementAtTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
        assertThrows(Contract.PreconditionViolation.class,() -> foo.elementAt(2));
    }

    @Test
    public void baseTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem");
        foo.peek();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem1");
        foo.push("elem2");
        foo.push("elem3");
        foo.peek();
        foo.pop();
        foo.peek();
        foo.pop();
        foo.push("elem4");
        foo.peek();
        foo.pop();
        foo.peek();
        foo.pop();
    }

}
