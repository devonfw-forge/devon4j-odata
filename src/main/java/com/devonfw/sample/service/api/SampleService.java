package com.devonfw.sample.service.api;

import java.util.List;

import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImport;
import com.devonfw.module.odata.service.api.ODataService;
import com.devonfw.sample.common.to.SampleEntitySet;

public interface SampleService extends ODataService<SampleEntitySet> {

    @EdmFunctionImport(name = "getParents", returnType = @EdmFunctionImport.ReturnType(type = EdmFunctionImport.ReturnType.Type.COMPLEX, isCollection = true), httpMethod = EdmFunctionImport.HttpMethod.GET)
    List<SampleEntitySet> getParents();
}
