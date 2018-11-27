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

package ch.usi.si.codelounge.jsicko.tutorials.simple;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.Contract.old;
import static ch.usi.si.codelounge.jsicko.ContractUtils.*;

import java.util.ArrayList;
import java.util.List;

public abstract class Collections implements Contract {

    @Requires("arg_not_null")
    @Ensures({"returns_collection_sorted", "returns_same_elements_contained"})
    public static <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        var clonedList = new ArrayList<>(list);
        java.util.Collections.sort(clonedList);
        return clonedList;
    }

    @Requires("arg_not_null")
    @Ensures({"param_collection_sorted", "param_same_elements_contained"})
    public static <T extends Comparable<? super T>> void mutableSort(List<T> list) {
        list.sort(T::compareTo);
    }

    @Requires("arg_not_null")
    @Ensures({"param_collection_sorted", "param_same_elements_contained"})
    public static <T extends Comparable<? super T>> void badMutableSort(List<T> list) {
        list.addAll(list);
        list.sort(T::compareTo);
    }

    @Requires("arg_not_null")
    @Ensures({"returns_collection_sorted", "returns_same_elements_contained"})
    public static <T extends Comparable<? super T>> List<T> badSort(List<T> list) {
        return list;
    }

    @Requires("arg_not_null")
    @Ensures({"returns_collection_sorted", "returns_same_elements_contained"})
    public static <T extends Comparable<? super T>> List<T> badSort2(List<T> list) {
        var clonedList = new ArrayList<>(list);
        clonedList.addAll(list);
        java.util.Collections.sort(clonedList);
        return clonedList;
    }

    @Pure
    private static boolean arg_not_null(List<?> list) {
        return list != null;
    }

    @Pure
    private static <T extends Comparable<? super T>> boolean param_collection_sorted(List<T> list) {
        return isSorted(list);
    }

    @Pure
    private static <T extends Comparable<? super T>> boolean param_same_elements_contained(List<T> list) {
        return forAll(old(list), (T elem) -> count(old(list), e -> e.equals(elem)) == count(list,e -> e.equals(elem)));
    }

    @Pure
    private static <T extends Comparable<? super T>> boolean returns_same_elements_contained(List<T> returns, List<T> list) {
        return forAll(list, (T elem) -> count(returns, e -> e.equals(elem)) == count(list,e -> e.equals(elem)));
    }

    @Pure
    private static <T extends Comparable<? super T>> boolean returns_collection_sorted(List<T> returns) {
        return isSorted(returns);
    }

}
