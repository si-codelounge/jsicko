package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;

public enum ContractConditionEnum {

    PRECONDITION {

        public String toString() {
            return "Precondition";
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

    }, INVARIANT {

        public String toString() {
            return "Invariant";
        }

        public Class<Contract.InvariantViolation> getAssertionErrorSpecificClass() {
            return Contract.InvariantViolation.class;
        }
    };

    public abstract Class<? extends AssertionError> getAssertionErrorSpecificClass();

}
