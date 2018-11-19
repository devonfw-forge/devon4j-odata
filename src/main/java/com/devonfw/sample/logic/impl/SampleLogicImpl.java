package com.devonfw.sample.logic.impl;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import com.devonfw.module.odata.logic.impl.AbstractDaoLogic;
import com.devonfw.sample.common.to.SampleEntitySet;
import com.devonfw.sample.dataaccess.api.SampleEntity;
import com.devonfw.sample.dataaccess.impl.SampleRepository;
import com.devonfw.sample.logic.api.SampleLogic;

@Component
public class SampleLogicImpl extends AbstractDaoLogic<SampleEntitySet, SampleEntity>
        implements SampleLogic {

    @Inject
    private SampleRepository testEntityRepository;

    @Override
    public List<SampleEntitySet> getParents() {

        List<SampleEntity> plans = testEntityRepository.findByParent_Id(null);
        return plans.stream().map(this::mapToTransferObject).collect(Collectors.toList());
    }

}
