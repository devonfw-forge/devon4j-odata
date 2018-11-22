package com.devonfw.sample.common.constarint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.devonfw.module.odata.common.constraint.Constraint;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ForeignKeyConstraint implements Constraint {

    NONE;

    private String constraintName;

    @Override
    public Constraint[] getValues() {

        return values();
    }

}
