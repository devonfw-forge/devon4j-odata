package com.devonfw.sample.logic.api;

import java.util.List;

import com.devonfw.module.odata.logic.api.LogicComponent;
import com.devonfw.sample.common.to.SampleEntitySet;

public interface SampleLogic extends LogicComponent<SampleEntitySet> {

    List<SampleEntitySet> getParents();

}
