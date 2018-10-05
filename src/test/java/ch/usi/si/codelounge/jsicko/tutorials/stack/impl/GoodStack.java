package ch.usi.si.codelounge.jsicko.tutorials.stack.impl;

import ch.usi.si.codelounge.jsicko.tutorials.stack.Stack;

import java.util.Collection;
import java.util.List;

/**
 * A correct implementation of a Stack, backed by the JDK standard implementation.
 * @param <T> the type of the elements in the Stack.
 */
public class GoodStack<T> implements Stack<T> {
    private java.util.Stack<T> baseObject = new java.util.Stack<T>();

    @Requires("elems_not_null")
    @Ensures("collection_initializer")
    public GoodStack(Collection<T> elems) {
        super();
        this.baseObject = new java.util.Stack<T>();
        this.baseObject.addAll(elems);
    }

    @Ensures("stack_is_empty")
    public GoodStack() {
        this(List.of());
    }


    @Override
    public T pop() {
        return baseObject.pop();
    }

    @Override
    public T top() {
        return baseObject.peek();
    }

    @Override
    public void push(T element) {
        baseObject.push(element);
    }

    @Override
    public int size() {
        return baseObject.size();
    }

    @Override
    public T elementAt(int pos) {
        return baseObject.get(pos);
    }

    @Override
    public String toString() {
        return String.valueOf(this.baseObject);
    }

}
