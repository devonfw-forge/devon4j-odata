package com.devonfw.module.odata.common.constraint;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public interface Constraint {

    static String fromDataIntegrityViolationException(
            DataIntegrityViolationException e) {

        Optional optionalConstraint = ConstraintConfiguration.getInstance().getConstraintsList().stream()
                .filter(enumObject -> enumObject instanceof Constraint)
                .filter(constraintEnum -> findByException((Constraint) constraintEnum, e)).findFirst();

        if (optionalConstraint.isPresent()) {
            return ((Constraint) optionalConstraint.get()).getConstraintName();
        }
        return e.getMessage();
    }

    static boolean findByException(Constraint constraint, DataIntegrityViolationException exception) {

        return ((ConstraintViolationException) exception.getCause()).getConstraintName().contains(getConstraintName(constraint));
    }

    static String getConstraintName(Constraint constraint) {

        return constraintIsValid(constraint) ? constraint.getConstraintName().toUpperCase() : null;
    }

    static boolean constraintIsValid(Constraint constraint) {

        return constraint != null && constraint.getConstraintName() != null;
    }

    String getConstraintName();

    Constraint[] getValues();
}
