package com.devonfw.module.odata.logic.api;

import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.exception.ODataException;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;

public interface LogicComponent<T extends ODataSet> {

    <U extends ODataSet> List<U> readRelated(T sourceData, Class<U> targetToClass, String targetName);

    int deleteRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo)
            throws ODataException;

    T setRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo, String navigationProperty)
            throws ODataException;

    List<T> readAll();

    T readEntity(Map<String, Object> targetKeys);

    T create(T to);

    T createEmpty();

    T update(T to);

    void delete(Map<String, Object> keys);

    void validate(T to);

    Class<T> getToClass();

    Class<?> getEntityClass();
}
