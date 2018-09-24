package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;

public enum ContractConditionEnum {
    PRECONDITION, POSTCONDITION, INVARIANT;

    @Override
    public String toString() {
        switch (this) {
            case PRECONDITION:
                return "Precondition";
            case POSTCONDITION:
                return "Postcondition";
            case INVARIANT:
                return "Invariant";
        }
        throw new RuntimeException("Unreachable code, you probably forgot to implement toString for a specific case of this enum.");
    }

    public Class<? extends AssertionError> getAssertionErrorSpecificClass() {
        switch (this) {
            case PRECONDITION:
                return Contract.PreconditionViolation.class;
            case POSTCONDITION:
                return Contract.PostconditionViolation.class;
            case INVARIANT:
                return Contract.InvariantViolation.class;
        }
        throw new RuntimeException("Unreachable code, you probably forgot to implement getAssertionErrorSpecificClass for a specific case of this enum.");
    }
}
