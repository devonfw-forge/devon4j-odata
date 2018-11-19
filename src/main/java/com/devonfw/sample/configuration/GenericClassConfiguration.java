package com.devonfw.sample.configuration;

import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import com.devonfw.module.odata.logic.impl.AbstractDaoLogic;

@Configuration
public class GenericClassConfiguration {

    public static final String SINGLE_GENERIC_CLASS = AbstractDaoLogic.SINGLE_GENERIC_CLASS;

    @Bean(SINGLE_GENERIC_CLASS)
    @Scope("prototype")
    public Class<?> getGeneric(DependencyDescriptor dependencyDescriptor) {
        return dependencyDescriptor.getResolvableType().resolveGeneric();
    }

}
