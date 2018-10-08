package ch.usi.si.codelounge.jsicko.tutorials.stack.impl;

import ch.usi.si.codelounge.jsicko.tutorials.stack.Stack;

import java.util.Collection;

/**
 * A very buggy implementation of a Stack, that is actually backed by a Queue.
 * @param <T> the type of the elements in the Stack.
 */
public class BadStack<T> implements Stack<T> {

    private final java.util.LinkedList<T> baseObject;

    @Requires("elems_not_null")
    @Ensures("collection_initializer")
    public BadStack(Collection<T> elems) {
        super();
        this.baseObject = new java.util.LinkedList<T>(elems);
    }

    @Ensures("stack_is_empty")
    public BadStack() {
        super();
        this.baseObject = new java.util.LinkedList<T>();
    }

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

    @Override
    public void clear() {
        this.baseObject.clear();
    }

}
