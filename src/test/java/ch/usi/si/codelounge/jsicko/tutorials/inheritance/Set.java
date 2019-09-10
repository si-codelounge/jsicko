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
import static ch.usi.si.codelounge.jsicko.ContractUtils.implies;

public class Set<T> extends AbstractCollection<T> implements Contract {

    private final java.util.Set<T> baseCollection;

    public Set() {
        this.baseCollection = new java.util.TreeSet<T>();
    }

    @Override
    boolean supports_null_elements() {
        return false;
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

    @Pure
    protected boolean size_increases_iff_not_contained(T element) {
        return implies(!old(this).contains(element),
                () -> this.size() == old(this).size() + 1,
                () -> this.size() == old(this).size());
    }

    @Override
    @Ensures("size_increases_iff_not_contained")
    public void add(T element) {
        this.baseCollection.add(element);
    }

    @Override
    public boolean remove(T element) {
        return this.baseCollection.remove(element);
    }

    @Override
    public Set<T> copyFrom(AbstractCollection<T> other) {
        return new Set<T>();
    }


}
