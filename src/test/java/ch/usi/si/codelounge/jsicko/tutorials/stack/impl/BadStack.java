/*
 * Copyright (C) 2018 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
 *
 * This file is part of jSicko - Java SImple Contract checKer.
 *
 *  jSicko is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * jSicko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jSicko.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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

    @Override
    public int indexOf(T e) {
        return this.baseObject.indexOf(e);
    }

    @Override
    public T remove(int index) {
        return this.baseObject.remove(index);
    }

}
