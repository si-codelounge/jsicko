package ch.usi.si.codelounge.jsicko.tutorials.stack;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.Invariant;
import ch.usi.si.codelounge.jsicko.Requires;
import ch.usi.si.codelounge.jsicko.Ensures;

import java.util.stream.IntStream;

public interface StackContract<T> extends Contract<Stack<T>> {

    @Invariant
    default public boolean sizeNonNegative() {
        return size() >= 0;
    }

    /*
     * See frameCondition
     */
    @Invariant
    default public boolean elementsNeverNull() {
        return IntStream.range(0, size()).allMatch(pos -> elementAt(pos) != null);
    }

    @Requires("!stackIsEmpty")
    @Ensures({"returnsLastElement", "sizeDecreases", "popFrameCondition"})
    public T pop();

    @Requires("!stackIsEmpty")
    @Ensures({"returnsLastElement", "pure"})
    public T peek();

    @Requires("elementNotNull")
    @Ensures({"sizeIncreases", "pushOnTop", "pushFrameCondition"})
    public void push(T element);

    @Ensures("pure")
    public int size();

    @Requires("posIsValid")
    @Ensures("pure")
    public T elementAt(int pos);


    default public boolean stackIsEmpty() {
        return size() == 0;
    }

    default public boolean posIsValid(int pos) {
        return pos >= 0 && pos < size();
    }

    default public boolean elementNotNull(T element) {
        return element != null;
    }

    default public boolean returnsLastElement(T returns) {
        return returns.equals(elementAt(size() - 1));
    }

    default public boolean sizeIncreases(T element) {
        return size() == old().size() + 1;
    }

    default public boolean sizeDecreases(T element) {
        return size() == old().size() - 1;
    }

    default public boolean pushOnTop(T element) {
        return elementAt(size()).equals(element);
    }

    default public boolean frameCondition(int range) {
        /* Range is exclusive - this simulates a "forall" spec with range.
         * Similarly, a range with anyMatch is essentially an exists clause.
         * A set of utility methods can be probably provided to ensure better readability of specs.
         */
        return IntStream.range(0, range).allMatch(pos -> old().elementAt(pos).equals(elementAt(pos)));
    }

    default public boolean pushFrameCondition() {
        return frameCondition(old().size());
    }

    default public boolean popFrameCondition() {
        return frameCondition(size());
    }

}
