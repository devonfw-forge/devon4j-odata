package com.devonfw.module.odata.logic.constraint.exception;

import org.springframework.dao.DataIntegrityViolationException;
import com.devonfw.module.odata.logic.constraint.Constraint;

public class ConstraintViolationException extends RuntimeException {

    public ConstraintViolationException(Constraint constraint) {

        super(constraint.getConstraintName());
    }

    public ConstraintViolationException(String message) {

        super(message);
    }

    public ConstraintViolationException(DataIntegrityViolationException e) {

        this(Constraint.fromDataIntegrityViolationException(e));
    }

    @Override
    public String toString() {

        return getMessage();
    }
}
