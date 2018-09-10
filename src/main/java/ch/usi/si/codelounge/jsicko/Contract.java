package ch.usi.si.codelounge.jsicko;

public interface Contract<T> {

    default public T old() {
        return null;
    }

    /*
     * A basic form of purity check
     * that relies on proper implementation of
     * equality.
     */
    default public boolean pure() {
        return this.equals(old());
    }
}
