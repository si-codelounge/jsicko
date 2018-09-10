package ch.usi.si.codelounge.jsicko.tutorials.stack;

public class Stack<T> implements StackContract<T> {

    private java.util.Stack<T> baseObject = new java.util.Stack<T>();


    @Override
    public T pop() {
        return baseObject.pop();
    }

    @Override
    public T peek() {
        return baseObject.peek();
    }

    @Override
    public void push(T element) {
        baseObject.push(element);
    }

    @Override
    public int size() {
        return baseObject.size();
    }

    @Override
    public T elementAt(int pos) {
        return baseObject.elementAt(pos);
    }
}
