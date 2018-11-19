package com.devonfw.module.odata.service.api;

import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataException;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;

public interface ODataService<T extends ODataSet> {

    <U extends ODataSet> List<U> readRelated(T sourceData, Class<U> targetToClass, String targetName);

    List<T> readAllEntities();

    T readEntity(Map<String, Object> targetKeys);

    T setRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetToClass,
            ODataAnnotationNavInfoUtil navigationInfo, String navigationProperty)
            throws ODataException;

    T createEntity(T to);

    T updateEntity(T to);

    void delete(Map<String, Object> keys);

    T createEmptyTo();

    void validate(T to);

    int deleteRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo)
            throws ODataException;

    Class<T> getToClass();

    Class getEntityClass();
}
