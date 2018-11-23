package com.devonfw.module.odata.common.constraint.exception;

import org.springframework.dao.DataIntegrityViolationException;
import com.devonfw.module.odata.common.constraint.Constraint;

public class ConstraintViolationException extends RuntimeException {

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
