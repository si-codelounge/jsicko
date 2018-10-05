package ch.usi.si.codelounge.jsicko.tutorials.stack;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.ContractUtils.*;

import java.util.Collection;
import java.util.stream.IntStream;

/**
 * A simple interface for a Stack of elements.
 * @param <T> the type for the stack elements.
 */
public interface Stack<T> extends Contract<Stack<T>> {

    @Invariant
    @Pure
    default boolean sizeNonNegative() {
        return size() >= 0;
    }

    /*
     * See frame_condition
     */
    @Invariant
    @Pure
    default boolean elementsNeverNull() {
        return forAllInts(0, size(), pos -> elementAt(pos) != null);
    }

    @Requires("!stack_is_empty")
    @Ensures({"returns_old_last_element", "size_decreases", "pop_frame_condition"})
    T pop();

    @Requires({"!stack_is_empty"})
    @Ensures({"returns_last_element"})
    @Pure
    T top();

    @Requires("element_not_null")
    @Ensures({"push_on_top", "size_increases", "push_frame_condition"})
    void push(T element);

    @Pure
    int size();

    @Requires("pos_is_valid")
    @Pure
    T elementAt(int pos);

    default boolean stack_is_empty() {
        return size() == 0;
    }

    default boolean pos_is_valid(int pos) {
        return pos >= 0 && pos < size();
    }

    default boolean element_not_null(T element) {
        return element != null;
    }

    default boolean returns_last_element(T returns) {
        return returns.equals(elementAt(size() - 1));
    }

    default boolean returns_old_last_element(T returns) {
        return returns.equals(old().elementAt(old().size() - 1));
    }

    default boolean size_increases() { return size() == old().size() + 1; }

    default boolean size_decreases() {
        return size() == old().size() - 1;
    }

    default boolean push_on_top(T element) {
        return top().equals(element);
    }

    default boolean frame_condition(int lastPos) {
        return forAllInts(0, lastPos, pos -> old().elementAt(pos).equals(elementAt(pos)));
    }

    default boolean push_frame_condition() {
        return frame_condition(old().size());
    }

    default boolean pop_frame_condition() {
        return frame_condition(size());
    }

    default boolean elems_not_null(Collection<T> elems) {
        return elems != null;
    }

    default boolean collection_initializer(Collection<T> elems) {
        return forAll(elems,elem -> existsInt(0,size(),pos -> elementAt(pos).equals(elem)));
    }

    /**
     * Returns a String representation of the Stack.
     * @return a String representation of the Stack.
     */
    String toString();

}
