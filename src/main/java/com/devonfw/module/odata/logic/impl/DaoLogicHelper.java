package com.devonfw.module.odata.logic.impl;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.beans.Introspector;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;
import com.devonfw.module.odata.service.util.ODataManagingServiceUtil;

@Component
public class DaoLogicHelper {

    private static final String ID = "id";

    private final Logger logger = LoggerFactory.getLogger(DaoLogicHelper.class);

    protected Mapper mapper;

    private EntityManager entityManager;

    @Inject
    public void setMapper(Mapper mapper) {

        this.mapper = mapper;
    }

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {

        this.entityManager = entityManager;
    }

    public Object getEntityByClassId(Class clazz, Long id) {

        return entityManager.find(clazz, Math.abs(id));
    }

    public <T extends ODataSet, U extends ODataSet, S> List<U> readRelated(T sourceTo,
            Class<U> targetToClass, String targetName, Class<S> entityClass) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<S> query = builder.createQuery(entityClass);
        Root<S> root = query.from(entityClass);
        query.select(root.get(targetName));
        query.where(builder.equal(root.get(ID), sourceTo.getId()));

        return entityManager.createQuery(query).getResultList().stream()
                .map(entity -> mapper.map(entity, targetToClass))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackOn = Exception.class)
    public <T extends ODataSet, S> T setRelation(Long targetKey, T sourceTo,
            Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo, Class<T> toClass, Class<S> entityClass,
            JpaRepository repository, String navigationProperty) throws ODataException {

        int resultOperation = 0;

        if (targetKey == null || navigationInfo == null) {
            logger.error("Empty target id or navigation, its not possible to set relation for {} ", targetEntityClass);
            throw new ODataException("Empty target key:" + targetKey + " or navigationInfo " + navigationInfo);

        } else if (targetKey < 0) {
            resultOperation =
                    deleteRelation(Math.abs(targetKey), sourceTo, targetEntityClass, navigationInfo, entityClass);

        } else {
            entityManager.flush();
            resultOperation =
                    setRelation(targetKey, sourceTo, targetEntityClass, navigationInfo, entityClass, navigationProperty,
                            resultOperation);
            entityManager.flush();
        }
        showMessageLogFromCrudOperation(resultOperation, sourceTo);
        S sourceEntity = (S) repository.getOne(sourceTo.getId());
        return mapper.map(sourceEntity, toClass);
    }

    private <T extends ODataSet, S> int setRelation(Long targetKey, T sourceTo, Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo, Class<S> entityClass, String navigationProperty,
            int resultOperation) throws ODataException {

        String navigation = ODataManagingServiceUtil.getFieldName(navigationInfo);
        String navigationProp = Introspector.decapitalize(navigationProperty);

        if (navigationInfo.getToMultiplicity().equals(EdmMultiplicity.MANY)) {
            if (navigationProp != null && !navigation.equals(navigationProp)) {
                resultOperation =
                        setRelationToMany(sourceTo.getId(), targetKey, navigation, targetEntityClass);
            } else {
                resultOperation =
                        setRelationToMany(targetKey, sourceTo.getId(), navigation, entityClass);
            }
        } else if (navigationInfo.getToMultiplicity().equals(EdmMultiplicity.ONE)) {
            if (navigationProp != null && !navigation.equals(navigationProp)) {
                resultOperation =
                        setRelationToOne(targetKey, navigation, sourceTo.getId(), targetEntityClass);
            } else {
                resultOperation = setRelationToOne(sourceTo.getId(), navigation, targetKey, entityClass);
            }
        }
        return resultOperation;
    }

    @Transactional
    public <T extends ODataSet, S> int deleteRelation(Long targetKey,
            T sourceTo,
            Class<?> targetEntityClass,
            ODataAnnotationNavInfoUtil navigationInfo, Class<S> entityClass) throws ODataException {

        int result;

        String fieldName = ODataManagingServiceUtil.getFieldName(navigationInfo);
        entityManager.flush();

        if (navigationInfo.getToMultiplicity().equals(EdmMultiplicity.ONE)) {
            result = deleteSingleAttribute(sourceTo, fieldName, entityClass);
        } else if (navigationInfo.getToMultiplicity().equals(EdmMultiplicity.MANY)) {
            result = deleteFromSourceWithBiDirectRel(targetKey, targetEntityClass, fieldName);
        } else {
            result = deleteFromSourceWithOneDirectRel(targetKey, sourceTo, fieldName, entityClass);
        }
        entityManager.flush();
        return result;
    }

    private <T extends ODataSet, AbstractEntity> int setRelationToMany(Object targetKey,
            Object sourceId,
            String fieldName, Class entityClass)
            throws ODataException {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery queryTargetList = builder.createQuery(entityClass);
        Root mainRoot = queryTargetList.from(entityClass);
        String query = ODataManagingServiceUtil.getInsertQueryFromPersister(fieldName, mainRoot);

        return entityManager.createNativeQuery(query, entityClass)
                .setParameter(1, sourceId)
                .setParameter(2, targetKey)
                .executeUpdate();
    }

    private <S> int setRelationToOne(Object sourceId, String fieldName,
            Object targetId, Class entityClass) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<S> criteriaUpdate = builder.createCriteriaUpdate(entityClass);
        Root<S> root = criteriaUpdate.from(entityClass);
        Path<S> path = ODataManagingServiceUtil.getPath(fieldName, root);
        criteriaUpdate.set(path.get(ID), targetId);
        criteriaUpdate.where(builder.equal(root.get(ID), sourceId));
        return entityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    private <T extends ODataSet, S> int deleteSingleAttribute(T sourceTo, String fieldName,
            Class<S> entityClass) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<S> criteriaUpdate = builder.createCriteriaUpdate(entityClass);
        Root<S> root = criteriaUpdate.from(entityClass);

        criteriaUpdate.set(fieldName, null);
        criteriaUpdate.where(builder.equal(root.get(ID), sourceTo.getId()));
        return entityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    private int deleteFromSourceWithBiDirectRel(Long targetKey, Class targetEntityClass, String fromFieldName) {

        if (targetKey == null) {
            logger.error("Empty target id, its not possible to set relation for {} ", targetEntityClass);
            return -1;
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<?> criteriaTargetUpdate = builder.createCriteriaUpdate(targetEntityClass);
        Root<?> rootTarget = criteriaTargetUpdate.from(targetEntityClass);
        criteriaTargetUpdate.set(fromFieldName, null);
        criteriaTargetUpdate.where(builder.equal(rootTarget.get(ID), targetKey));
        return entityManager.createQuery(criteriaTargetUpdate).executeUpdate();
    }

    private <T extends ODataSet, S> int deleteFromSourceWithOneDirectRel(Long targetKey,
            T sourceTo, String fromFieldName, Class<S> entityClass)
            throws ODataException {

        if (targetKey == null) {
            logger.error("Empty target id, its not possible to set relation for {} ", fromFieldName);
            return -1;
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<S> queryTargetList = builder.createQuery(entityClass);

        String query = ODataManagingServiceUtil
                .getDeleteQueryFromPersister(fromFieldName, queryTargetList.from(entityClass));

        return entityManager.createNativeQuery(query, entityClass)
                .setParameter(1, sourceTo.getId())
                .setParameter(2, targetKey)
                .executeUpdate();
    }

    private void showMessageLogFromCrudOperation(int resultOperation, ODataSet sourceTo) {

        if (resultOperation < 0) {
            logger.error("Something went wrong for object {} with id {} ", sourceTo.getClass(), sourceTo.getId());
        } else {
            logger.info("Object was changed {}", sourceTo.getClass());
        }
    }

}
