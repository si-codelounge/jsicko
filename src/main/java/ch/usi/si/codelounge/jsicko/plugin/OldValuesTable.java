package ch.usi.si.codelounge.jsicko.plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A data structure holding old values, needed to
 * support recursive method calls.
 */
public class OldValuesTable {

    private static class MethodScope {

        private String methodSignature;
        private Map<String, Object> scopeTable;

        private MethodScope() {
            this("<unknown>");
        }

        private MethodScope(String methodSignature) {
            this.methodSignature = methodSignature;
            this.scopeTable = new HashMap<>();
        }

        void put(String key, Object value) {
            this.scopeTable.put(key, value);
        }

        Object get(String key) {
            return this.scopeTable.get(key);
        }

    }

    private Deque<MethodScope> table;

    public OldValuesTable() {
        this.table = new ArrayDeque<>();
    }

    public void enter(String methodSignature) {
        var newScope = new MethodScope(methodSignature);
        this.table.push(newScope);
    }

    public void leave() {
        this.table.pop();
    }

    public Object getValue(String key) {
        return this.table.peek().get(key);
    }

    public void putValue(String key, Object value) {
        this.table.peek().put(key, value);
    }

}
