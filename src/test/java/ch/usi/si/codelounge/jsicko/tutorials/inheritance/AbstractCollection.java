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

package ch.usi.si.codelounge.jsicko.tutorials.inheritance;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.Contract.old;
import static ch.usi.si.codelounge.jsicko.ContractUtils.*;

/**
 * A simplified interface for an abstract collection.
 *
 * It shows how to specify the add and remove methods with specific refinements
 * for two subclasses, List and Set. Refinements happen specifically
 * in the add and remove methods.
 *
 * It also shows two very simple class invariants.
 *
 * Since the collection is essentially uninitialized in the abstract class,
 * the implementation for the pure contract observers is delegated
 * to the subclasses. For this reason, the default constructor must be
 * protected, so that it does not get instrumented. In fact, checking
 * the invariants in the intermediate state produced by this constructor
 * would likely generate a <code>NullPointerException</code>.
 * @param <T> the type of the contained elements.
 */
public abstract class AbstractCollection<T> implements Contract {

    protected AbstractCollection() {}

    /*
     * Essentially a model field - true iff the collection supports
     * null elements
     */
    @Pure
    abstract boolean supports_null_elements();

    @Invariant
    @Pure
    protected boolean size_non_negative() {
        return size() >= 0;
    }

    @Invariant
    @Pure
    protected boolean empty_iff_size_0() {
        return iff(size() == 0, isEmpty());
    }

    @Pure
    public abstract boolean isEmpty();

    @Pure
    public abstract int size();

    @Ensures("raises_exception_if_null_unsupported")
    @Pure
    public abstract boolean contains(T element);

    @Pure
    protected boolean element_not_null(T element) {
        return element != null;
    }

    @Pure
    protected boolean raises_exception_if_null_unsupported(Throwable raises, T element) {
        return implies(!supports_null_elements() && element == null, () -> raises instanceof NullPointerException);
    }

    @Pure
    protected boolean returns_true_if_old_contains(boolean returns, T element) {
        return returns == old(this).contains(element);
    }

    @Pure
    protected boolean size_decreases_if_contained(T element) {
        return implies(old(this).contains(element),
                () -> this.size() == old(this).size() - 1,
                () -> this.size() == old(this).size());
    }

    @Pure
    protected boolean size_may_increase(T element) {
        return this.size() >= old(this).size();
    }

    @Ensures({"raises_exception_if_null_unsupported", "contains", "size_may_increase"})
    public abstract void add(T element);

    @Ensures({"raises_exception_if_null_unsupported", "size_decreases_if_contained", "returns_true_if_old_contains"})
    public abstract boolean remove(T element);

    @Pure
    boolean other_non_null(AbstractCollection<T> other) {
        return other != null;
    }

    @Pure
    boolean other_equal_size(AbstractCollection<T> other) {
        return this.size() == other.size();
    }

    @Pure
    boolean other_non_empty(AbstractCollection<T> other) {
        return other.size() > 0;
    }

    @Requires({"other_non_null", "other_non_empty", "other_equal_size"})
    public abstract AbstractCollection<T> copyFrom(AbstractCollection<T> other);

}
