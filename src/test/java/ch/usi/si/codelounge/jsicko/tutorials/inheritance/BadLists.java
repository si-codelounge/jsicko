/*
 * Copyright (C) 2019 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
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

package ch.usi.si.codelounge.jsicko.tutorials.inheritance;

import ch.usi.si.codelounge.jsicko.Contract;

import static ch.usi.si.codelounge.jsicko.Contract.old;

public abstract class BadLists {

    public static class BadList1<T> extends AbstractList<T> implements Contract {

        private final java.util.List<T> baseCollection;

        public BadList1() {
            this.baseCollection = new java.util.ArrayList<T>();
        }

        @Override
        boolean supports_null_elements() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return this.baseCollection.isEmpty();
        }

        @Override
        public int size() {
            return this.baseCollection.size();
        }

        @Override
        public boolean contains(T element) {
            return baseCollection.contains(element);
        }

        boolean index_is_valid(int index) {
            return index >= 0 && index < size();
        }

        @Pure
        @Requires("index_is_valid")
        public T get(int index) { return this.baseCollection.get(index); }

        @Pure
        public boolean element_at_old_size_position(T element) {
            return this.get(old(this).size()) == element;
        }

        @Override
        @Ensures("element_at_old_size_position")
        public void add(T element) {
            this.baseCollection.add(0,element);
        }

        @Override
        public boolean remove(T element) {
            return this.baseCollection.remove(element);
        }

        @Override
        public BadList1<T> copyFrom(AbstractCollection<T> other) {
            return new BadList1<>();
        }

        public String toString() {
            return this.baseCollection.toString();
        }

    }

}


