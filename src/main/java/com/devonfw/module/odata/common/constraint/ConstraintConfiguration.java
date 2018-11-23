package com.devonfw.module.odata.common.constraint;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class ConstraintConfiguration {

    private List<Constraint> constraintsList = new ArrayList<>();

    public void registerAllConstraint(Constraint... constraints) {

        this.constraintsList.addAll(Arrays.asList(constraints));
    }

    private static ConstraintConfiguration ourInstance = new ConstraintConfiguration();

    public static ConstraintConfiguration getInstance() {

        return ourInstance;
    }

    private ConstraintConfiguration() {

    }
}
