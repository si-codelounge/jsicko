package ch.usi.si.codelounge.jsicko.tutorials.stack;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.tutorials.stack.impl.GoodStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoodStackTest {

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
        foo.top();
        foo.pop();
    }

    @Test
    public void longTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        foo.push("elem1");
        foo.push("elem2");
        foo.push("elem3");
        foo.top();
        foo.pop();
        foo.top();
        foo.pop();
        foo.push("elem4");
        foo.top();
        foo.pop();
        foo.top();
        foo.pop();
    }

    @Test
    public void clearTest() throws Throwable {
        GoodStack<String> foo = new GoodStack<>();
        for (int i = 0; i < 10; i++)
            foo.push(String.valueOf(i));
        foo.clear();
    }

}
