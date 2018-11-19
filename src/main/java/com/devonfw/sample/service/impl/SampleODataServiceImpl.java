package com.devonfw.sample.service.impl;

import javax.inject.Inject;
import java.util.List;

import org.springframework.stereotype.Service;
import com.devonfw.module.odata.service.api.AbstractODataService;
import com.devonfw.sample.common.to.SampleEntitySet;
import com.devonfw.sample.logic.api.SampleLogic;
import com.devonfw.sample.service.api.SampleService;

@Service
public class SampleODataServiceImpl extends AbstractODataService<SampleEntitySet> implements SampleService {

    @Inject
    private SampleLogic testLogic;

    @Override
    public List<SampleEntitySet> getParents() {

        return testLogic.getParents();
    }

}
