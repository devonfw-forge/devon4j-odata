package com.devonfw.sample.common.constarint;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import com.devonfw.module.odata.common.constraint.ConstraintConfiguration;

@Configuration
public class ConstraintConfig {

    @PostConstruct
    public void addAllConstraints() {

        ConstraintConfiguration.getInstance().registerAllConstraint(UniqueConstraint.values());
    }
}
