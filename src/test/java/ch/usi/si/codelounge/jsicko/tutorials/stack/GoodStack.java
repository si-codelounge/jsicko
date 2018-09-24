package ch.usi.si.codelounge.jsicko.tutorials.stack;

/**
 * A correct implementation of a Stack, backed by the JDK standard implementation.
 * @param <T> the type of the elements in the Stack.
 */
public class GoodStack<T> implements Stack<T>,  StackContract<T> {
    private java.util.LinkedList<T> baseObject = new java.util.LinkedList<T>();

    @Override
    public T pop() {
        return baseObject.remove();
    }

    @Override
    public T peek() {
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
}
