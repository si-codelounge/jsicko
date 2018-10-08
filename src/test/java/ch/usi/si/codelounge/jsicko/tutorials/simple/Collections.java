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
