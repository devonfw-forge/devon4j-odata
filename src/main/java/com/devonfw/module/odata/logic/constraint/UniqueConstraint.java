package com.devonfw.module.odata.logic.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum UniqueConstraint implements Constraint {

    NONE;

    private String constraintName;

    @Override
    public Constraint[] getValues() {

        return values();
    }

}
