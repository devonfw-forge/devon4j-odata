package com.devonfw.module.odata.service.api;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataException;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.api.LogicComponent;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;

public abstract class AbstractODataService<T extends ODataSet> implements ODataService<T> {

    @Inject
    protected LogicComponent<T> logicComponent;

    @Override
    public <U extends ODataSet> List<U> readRelated(T sourceData, Class<U> targetClass, String targetName) {

        return logicComponent.readRelated(sourceData, targetClass, targetName);
    }

    @Override
    public int deleteRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo)
            throws ODataException {

        return logicComponent.deleteRelation(targetKeys, sourceTo, targetEntityClass, navigationInfo);
    }

    @Override
    public T setRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo, String navigationProperty)
            throws ODataException {

        return logicComponent.setRelation(targetKeys, sourceTo, targetEntityClass, navigationInfo, navigationProperty);
    }

    @Override
    public T createEntity(T to) {

        return logicComponent.create(to);
    }

    @Override
    public T createEmptyTo() {

        return logicComponent.createEmpty();
    }

    @Override
    public T updateEntity(T to) {

        return logicComponent.update(to);
    }

    @Override
    public void delete( Map<String, Object> keys) {

        logicComponent.delete(keys);
    }

    @Override
    public List<T> readAllEntities() {

        return logicComponent.readAll();
    }

    @Override
    public T readEntity(Map<String, Object> targetKeys) {

        return logicComponent.readEntity(targetKeys);
    }

    @Override
    public void validate(T to) {

        logicComponent.validate(to);
    }

    @Override
    public Class<T> getToClass() {

        return logicComponent.getToClass();
    }

    @Override
    public Class getEntityClass() {

        return logicComponent.getEntityClass();
    }
}
