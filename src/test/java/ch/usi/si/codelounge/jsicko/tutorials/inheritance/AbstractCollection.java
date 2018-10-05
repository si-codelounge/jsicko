package ch.usi.si.codelounge.jsicko.tutorials.inheritance;

import ch.usi.si.codelounge.jsicko.Contract;

import java.util.Collection;

public abstract class AbstractCollection<T> implements Contract<AbstractCollection<T>> {

    private Collection<T> baseCollection;

    @Invariant
    private boolean size_non_negative() {
        return size() >= 0;
    }

    @Pure
    public int size() {
        return baseCollection.size();
    }

    @Requires("arg_not_null")
    @Pure
    public boolean contains(Object o) {
        return baseCollection.contains(o);
    }

    private boolean arg_not_null(Object o) {
        return o != null;
    }


}
