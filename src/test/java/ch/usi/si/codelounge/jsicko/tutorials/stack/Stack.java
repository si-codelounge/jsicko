package ch.usi.si.codelounge.jsicko.tutorials.stack;

/**
 * A simple interface for a Stack of elements.
 * @param <T> the type for the stack elements.
 */
public interface Stack<T> {

    /**
     * Pops an element out of the stack.
     * @return the element previously on the top of the stack.
     */
    public T pop();

    /*
     * Returns the element on the top of the stack.
     * @return the element on the top of the stack.
     */
    public T peek();


    /**
     * Pushes an element on the stack.
     * @param element
     */
    public void push(T element);

    /**
     * Returns the size of the stack.
     * @return the size (i.e., the number of elements) of the stack.
     */
    public int size();

    /**
     * Returns the element in a given position of the Stack.
     * @param pos the position where to retrieve an element from.
     * @return the element at <code>pos</code> position.
     */
    public T elementAt(int pos);

    /**
     * Returns a String representation of the Stack.
     * @return a String representation of the Stack.
     */
    public String toString();

}
