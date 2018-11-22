package com.devonfw.module.odata.logic.impl;

import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devonfw.module.basic.common.api.entity.PersistenceEntity;
import com.devonfw.module.odata.common.api.ODataEntity;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.api.LogicComponent;
import com.devonfw.module.odata.logic.api.validation.ValidationRule;
import com.devonfw.module.odata.common.constraint.exception.ConstraintViolationException;
import com.devonfw.module.odata.logic.util.DaoLogicUtil;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;

public abstract class AbstractDaoLogic<T extends ODataSet, S extends ODataEntity> implements LogicComponent<T> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static final String SINGLE_GENERIC_CLASS = "singleGenericClass";

  @Inject
  protected Mapper mapper;

  @Inject
  private JpaRepository<S, Long> repository;

  @Autowired(required = false)
  private List<ValidationRule<T>> validationRules;

  @Inject
  @Named(SINGLE_GENERIC_CLASS)
  @Getter
  private Class<T> toClass;

  @Inject
  @Named(SINGLE_GENERIC_CLASS)
  @Getter
  private Class<S> entityClass;

  @Inject
  private DaoLogicHelper daoLogicHelper;

  @Override
  public <U extends ODataSet> List<U> readRelated(T sourceTo, Class<U> targetToClass, String targetName) {

    return daoLogicHelper.readRelated(sourceTo, targetToClass, targetName, entityClass);
  }

  @Override
  public T setRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
    ODataAnnotationNavInfoUtil navigationInfo, String navigationProperty)
    throws ODataException {

    Long targetId = DaoLogicUtil.extractIdFromPropertyMap(targetKeys);
    PersistenceEntity targetEntity = daoLogicHelper.getEntityByClassId(targetEntityClass, targetId);
    doBeforeSetRelation(sourceTo, targetEntity);
    return daoLogicHelper
      .setRelation(targetId, sourceTo, targetEntityClass, navigationInfo, toClass, entityClass,
        repository, navigationProperty);
  }

  @Override
  public int deleteRelation(Map<String, Object> targetKeys, T sourceTo, Class<?> targetEntityClass,
    ODataAnnotationNavInfoUtil navigationInfo) throws ODataException {

    Long targetKey = DaoLogicUtil.extractIdFromPropertyMap(targetKeys);
    return daoLogicHelper.deleteRelation(targetKey, sourceTo, targetEntityClass, navigationInfo, entityClass);
  }

  @Override
  public List<T> readAll() {

    List<T> results = new ArrayList<>();
    List<S> entities = repository.findAll();
    if (CollectionUtils.isNotEmpty(entities)) {
      results = entities.stream().map(this::mapToTransferObject).collect(Collectors.toList());
    }
    return results;
  }

  @Override
  public T readEntity(Map<String, Object> targetKeys) {

    Long id = DaoLogicUtil.extractIdFromPropertyMap(targetKeys);
    return findById(id);
  }

  private T findById(Long id) {

    Optional entityOpt = repository.findById(id);
    if (entityOpt.isPresent()) {
      return mapToTransferObject((S) entityOpt.get());
    }
    return null;
  }

  @Override
  public T createEmpty() {

    T t = null;
    try {
      t = toClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      logger.error("Could not create empty transfer object", e);
    }
    return t;
  }

  @Override
  public T create(T to) {

    try {
      S entity = doBeforeCreate(to);
      entity = (S) repository.save(entity);
      return doAfterCreate(entity);
    } catch (DataIntegrityViolationException e) {
      throw new ConstraintViolationException(e);
    }
  }

  @Override
  public T update(T to) {

    try {
      S entity = doBeforeUpdate(to);
      entity = (S) repository.save(entity);
      return doAfterUpdate(entity);
    } catch (DataIntegrityViolationException e) {
      throw new ConstraintViolationException(e);
    }
  }

  @Override
  public void delete(Map<String, Object> keys) {

    Long id = DaoLogicUtil.extractIdFromPropertyMap(keys);
    repository.deleteById(id);
  }

  public void validate(T to) {

    if (CollectionUtils.isNotEmpty(validationRules)) {
      validationRules.forEach(validationRule -> validationRule.validate(to));
    }
  }

  protected S doBeforeSetRelation(T abstractTo, PersistenceEntity targetEntity) {

    return mapToEntity(abstractTo);
  }

  protected S doBeforeUpdate(T abstractTo) {

    return mapToEntity(abstractTo);
  }

  protected S doBeforeCreate(T abstractTo) {

    return mapToEntity(abstractTo);
  }

  protected T doAfterSetRelation(S sourceEntity, PersistenceEntity targetEntity) {

    return mapToTransferObject(sourceEntity);
  }

  protected T doAfterUpdate(S sourceEntity) {

    return mapToTransferObject(sourceEntity);
  }

  protected T doAfterCreate(S sourceEntity) {

    return mapToTransferObject(sourceEntity);
  }

  protected T mapToTransferObject(S entity) {

    T transferObject = null;
    if (entity != null) {
      transferObject = mapper.map(entity, toClass);
    }
    return transferObject;
  }

  protected S mapToEntity(T transferObject) {

    return mapper.map(transferObject, entityClass);
  }

}
