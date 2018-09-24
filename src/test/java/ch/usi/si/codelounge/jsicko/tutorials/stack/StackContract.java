package ch.usi.si.codelounge.jsicko.tutorials.stack;

import ch.usi.si.codelounge.jsicko.Contract;

import java.util.stream.IntStream;

public interface StackContract<T> extends Contract<Stack<T>> {

    @Invariant
    default public boolean sizeNonNegative() {
        return size() >= 0;
    }

    /*
     * See frame_condition
     */
    @Invariant
    default public boolean elementsNeverNull() {
        return IntStream.range(0, size()).allMatch(pos -> elementAt(pos) != null);
    }

    @Requires("!stack_is_empty")
    @Ensures({"returns_old_last_element", "size_decreases", "pop_frame_condition"})
    public T pop();

    @Requires({"!stack_is_empty"})
    @Ensures({"returns_last_element"})
    @Pure
    public T peek();

    @Requires("element_not_null")
    @Ensures({"size_increases", "push_on_top", "push_frame_condition"})
    public void push(T element);

    @Pure
    public int size();

    @Requires("pos_is_valid")
    @Pure
    public T elementAt(int pos);

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
        return peek().equals(element);
    }

    default boolean frame_condition(int range) {
        /* Range is exclusive - this simulates a "forall" spec with range.
         * Similarly, a range with anyMatch is essentially an exists clause.
         * A set of utility methods can be probably provided to ensure better readability of specs.
         */
        return IntStream.range(0, range).allMatch(pos -> old().elementAt(pos).equals(elementAt(pos)));
    }

    default public boolean push_frame_condition() {
        return frame_condition(old().size());
    }

    default public boolean pop_frame_condition() {
        return frame_condition(size());
    }

}
