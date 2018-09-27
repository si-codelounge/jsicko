package ch.usi.si.codelounge.jsicko.tutorials.stack.impl;

import ch.usi.si.codelounge.jsicko.tutorials.stack.Stack;

/**
 * A very buggy implementation of a Stack, that is actually backed by a Queue.
 * @param <T> the type of the elements in the Stack.
 */
public class BadStack<T> implements Stack<T> {

    private java.util.LinkedList<T> baseObject = new java.util.LinkedList<T>();

    @Override
    public T pop() {
        return baseObject.remove();
    }

    @Override
    public T top() {
        return baseObject.peek();
    }

    @Override
    public void push(T element) {
        baseObject.offer(element);
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
