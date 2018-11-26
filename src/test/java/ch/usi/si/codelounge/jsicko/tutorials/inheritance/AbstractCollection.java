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
