package ch.usi.si.codelounge.jsicko.tutorials.stack.impl;

import ch.usi.si.codelounge.jsicko.tutorials.stack.Stack;

/**
 * A correct implementation of a Stack, backed by the JDK standard implementation.
 * @param <T> the type of the elements in the Stack.
 */
public class GoodStack<T> implements Stack<T> {
    private java.util.Stack<T> baseObject = new java.util.Stack<T>();

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
