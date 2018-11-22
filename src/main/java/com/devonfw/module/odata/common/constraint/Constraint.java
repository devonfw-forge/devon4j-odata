package com.devonfw.module.odata.common.constraint;

import java.util.Arrays;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public interface Constraint {

    static Constraint fromDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        Constraint constraint = UniqueConstraint.NONE.getByDataIntegrityViolationException(e);
        if (constraint == null) {
            constraint = ForeignKeyConstraint.NONE.getByDataIntegrityViolationException(e);
        }
        return constraint;
    }

    default Constraint getByConstraintName(String constraintName) {

        return Arrays.stream(getValues())
                .filter(value -> checkConstraintContainsName(constraintName, value))
                .findFirst()
                .orElse(null);
    }

    default boolean checkConstraintContainsName(String constraintName,
            Constraint constraint) {

        if (constraintName != null && constraint != null && constraint.getConstraintName() != null) {
            return constraintName.contains(constraint.getConstraintName().toUpperCase());
        }
        return false;
    }

    default Constraint getByDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        return getByConstraintName(((ConstraintViolationException) e.getCause()).getConstraintName());
    }

    Constraint[] getValues();

    String getConstraintName();
}
