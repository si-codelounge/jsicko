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

import ch.usi.si.codelounge.jsicko.Contract;

import java.util.function.Function;

public enum ContractConditionEnum {

    PRECONDITION {

        public String toString() {
            return "Precondition";
        }

        public Function<String, Contract.ContractConditionViolation> violationConstructor() {
            return Contract.PreconditionViolation::new;
        }

        public Class<Contract.PreconditionViolation> getAssertionErrorSpecificClass() {
            return Contract.PreconditionViolation.class;
        }

    }, POSTCONDITION {

        public String toString() {
            return "Postcondition";
        }

        public Class<Contract.PostconditionViolation> getAssertionErrorSpecificClass() {
            return Contract.PostconditionViolation.class;
        }

        public Function<String, Contract.ContractConditionViolation> violationConstructor() {
            return Contract.PostconditionViolation::new;
        }

    }, INVARIANT {

        public String toString() {
            return "Invariant";
        }

        public Class<Contract.InvariantViolation> getAssertionErrorSpecificClass() {
            return Contract.InvariantViolation.class;
        }

        public Function<String, Contract.ContractConditionViolation> violationConstructor() {
            return Contract.InvariantViolation::new;
        }
    };

    public abstract Class<? extends AssertionError> getAssertionErrorSpecificClass();
    public abstract Function<String, Contract.ContractConditionViolation> violationConstructor();

}
