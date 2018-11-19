package com.devonfw.module.odata.service.api;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ODataEntityScan {

    String[] functionImportPackages() default {};

    String[] entityPackages() default {};

}
