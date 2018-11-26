/*
 * Copyright (C) 2018 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
 *
 * This file is part of jSicko - Java SImple Contract checKer.
 *
 *  jSicko is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * jSicko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jSicko.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package ch.usi.si.codelounge.jsicko.plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * A symbol table-like data structure used to hold
 * old values, needed to support
 * internal/recursive method calls.
 */
public class OldValuesTable {

    /**
     * A scope of values for a single method call.
     */
    private static class MethodScope {

        private String methodSignature;
        private Map<String, Object> scopeTable;

        /**
         * Constructs a new instance for an unknown
         * method call. Should not be used, it is
         * present only for deserialization purposes.
         */
        private MethodScope() {
            this("<unknown>");
        }

        /**
         * Constructs a new instance for a given
         * method call with a specific signature.
         */
        private MethodScope(String methodSignature) {
            this.methodSignature = methodSignature;
            this.scopeTable = new HashMap<>();
        }

        /**
         * Inserts a value for a specified key.
         * @param key A string key (e.g., the name of a variable).
         * @param value An object value.
         */
        void put(String key, Object value) {
            this.scopeTable.put(key, value);
        }

        /**
         * Returns the value hold by a given key
         * @param key a string key (e.g., the name of a variable).
         * @return the value hold in the current scope for the key.
         */
        Object get(String key) {
            return this.scopeTable.get(key);
        }

    }

    private Deque<MethodScope> table;

    /**
     * Creates a new empty table.
     */
    public OldValuesTable() {
        this.table = new ArrayDeque<>();
    }

    /**
     * Enters a new scope for a given method.
     * @param methodSignature the signature of the method.
     */
    public void enter(String methodSignature) {
        var newScope = new MethodScope(methodSignature);
        this.table.push(newScope);
    }

    /**
     * Leaves the current scope.
     */
    public void leave() {
        this.table.pop();
    }

    /**
     * Gets the value hold by the table in the current
     * scope for a given key.
     * @param key A string key (e.g., a variable name).
     * @return an object value.
     */
    public Object getValue(String key) {
        return this.table.peek().get(key);
    }

    /**
     * Adds a key-value pair in the current scope.
     * @param key a string key (e.g., a variable name).
     * @param value an object value.
     */
    public void putValue(String key, Object value) {
        this.table.peek().put(key, value);
    }

}
