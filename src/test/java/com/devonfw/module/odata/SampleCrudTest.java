package com.devonfw.module.odata;

import java.util.LinkedList;
import java.util.List;

import com.devonfw.module.odata.api.AbstractCrudTest;
import com.devonfw.module.odata.common.api.ODataEntity;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.sample.common.to.SampleEntitySet;
import com.devonfw.sample.dataaccess.api.SampleEntity;

public class SampleCrudTest extends AbstractCrudTest<SampleEntity> {

    @Override
    protected ODataSet changeEntityForUpdate(final ODataSet oDataSet) {

        SampleEntitySet sampleEntitySet = (SampleEntitySet) oDataSet;
        sampleEntitySet.setExplanation("changed");
        sampleEntitySet.setNote("changed");
        sampleEntitySet.setIsDeleted(!sampleEntitySet.getIsDeleted());
        return sampleEntitySet;
    }

    @Override
    protected ODataEntity createEntity(ODataSet oDataSet) {

        return mapper.map(oDataSet, SampleEntity.class);
    }

    @Override
    protected List<String> getIgnoredFieldsList() {

        List ignoredFieldsInResponse = new LinkedList<String>();
        ignoredFieldsInResponse.add("__metadata");
        ignoredFieldsInResponse.add("Parent");
        ignoredFieldsInResponse.add("Children");
        return ignoredFieldsInResponse;
    }

    @Override
    protected SampleEntitySet createEntitySet() {

        return SampleEntitySet.builder()
                .identifier("Sample Identifier")
                .keyFigure("5")
                .isDeleted(Boolean.TRUE)
                .explanation("Sample Explanation")
                .note("Some Note")
                .build();
    }
}
