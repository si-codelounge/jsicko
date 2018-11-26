package ch.usi.si.codelounge.jsicko.tutorials.inheritance;

import ch.usi.si.codelounge.jsicko.Contract;
import static ch.usi.si.codelounge.jsicko.ContractUtils.*;

import java.util.Collection;
import java.util.Map;
//
//public abstract class AbstractCollection<T> implements Contract {
//
//    private Collection<T> baseCollection;
//
//    private Map<String,Object> somethingBad;
//
//    @Invariant
//    private boolean size_non_negative() {
//        return size() >= 0;
//    }
//
//    @Invariant
//    private boolean empty_iff_size_0() {
//        return iff(size() == 0, isEmpty());
//    }
//
//    @Pure
//    public boolean isEmpty() {
//        return baseCollection.isEmpty();
//    }
//
//    @Pure
//    public int size() {
//        return baseCollection.size();
//    }
//
//    @Requires("arg_not_null")
//    @Pure
//    public boolean contains(Object o) {
//        return baseCollection.contains(o);
//    }
//
//    @Pure
//    private boolean arg_not_null(Object o) {
//        return o != null;
//    }
//
//
//}
