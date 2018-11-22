package com.devonfw.sample.common.constarint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.devonfw.module.odata.common.constraint.Constraint;

import static com.devonfw.sample.dataaccess.api.SampleEntity.UQ_CHAPTERGROUPING_KEYFIGURE;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum UniqueConstraint implements Constraint {


    CHAPTERGROUPING_KEYFIGURE(UQ_CHAPTERGROUPING_KEYFIGURE),
    NONE;

    private String constraintName;

    @Override
    public Constraint[] getValues() {

        return values();
    }

}
