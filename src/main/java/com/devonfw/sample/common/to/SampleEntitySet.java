package com.devonfw.sample.common.to;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import org.apache.olingo.odata2.api.annotation.edm.*;
import com.devonfw.module.odata.common.api.ODataSet;

@Getter
@Setter
@EdmComplexType(name = "Sample")
@EdmEntityType(name = "Sample")
@EdmEntitySet(name = "SampleSet")
public class SampleEntitySet implements ODataSet<Long> {

    @EdmKey
    @EdmProperty
    private Long id;

    @EdmProperty
    private String identifier;

    @EdmProperty
    private String keyFigure;

    @EdmProperty
    private String explanation;

    @EdmProperty
    private String note;

    @EdmProperty
    private Boolean isDeleted;

    @EdmNavigationProperty
    private SampleEntitySet parent;

    @EdmNavigationProperty
    private List<SampleEntitySet> children;


}
