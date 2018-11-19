package com.devonfw.module.odata.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource.BinaryData;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.devonfw.module.odata.common.api.BinaryODataSet;
import com.devonfw.module.odata.common.api.ODataSet;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;
import com.devonfw.module.odata.service.api.AbstractODataManagingService;
import com.devonfw.module.odata.service.api.FileService;
import com.devonfw.module.odata.service.api.ODataService;
import com.devonfw.module.odata.service.util.ODataManagingServiceUtil;

@Service
public class ODataManagingServiceImpl extends AbstractODataManagingService {

    private static final Logger logger = LoggerFactory.getLogger(
            ODataManagingServiceImpl.class);

    @Override
    public List readData(EdmEntitySet entitySet) throws EdmException, ODataNotImplementedException {

        return getService(entitySet.getName()).readAllEntities();
    }

    @Override
    public Object readData(EdmEntitySet entitySet, Map<String, Object> keys)
            throws EdmException, ODataNotImplementedException {

        return readEntityById(entitySet.getName(), keys);
    }

    @Override
    public Object readData(EdmFunctionImport function, Map<String, Object> parameters, Map<String, Object> keys)
            throws EdmException {

        ODataImportFunctionHolder funcHolder =
                ODataEdmProviderExtension.getFunctionImportHolders().get(function.getName());
        return funcHolder.execute(parameters);
    }

    @Override
    public Object readRelatedData(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
            Map<String, Object> targetKeys, String propertyName) throws EdmException, ODataNotImplementedException {

        Object result = null;

        if (targetKeys.isEmpty()) {
            result = readEntity(sourceData, sourceEntitySet, targetEntitySet, propertyName);
        } else {
            result = readEntityById(targetEntitySet.getName(), targetKeys);
        }
        return result;
    }

    @Override
    public Object createEmptyTo(EdmEntitySet entitySet) throws EdmException, ODataNotImplementedException {

        return getService(entitySet.getName()).createEmptyTo();
    }

    @Override
    public void deleteData(EdmEntitySet entitySet, Map<String, Object> keys, Map<String, String> queryOptions)
            throws EdmException, ODataNotImplementedException {

        getService(entitySet.getName()).delete(keys);
    }

    @Override
    public Object createData(EdmEntitySet entitySet, Object data) throws EdmException, ODataNotImplementedException {

        return getService(entitySet.getName()).createEntity((ODataSet) data);
    }

    @Override
    public Object updateData(EdmEntitySet entitySet, Object data) throws EdmException, ODataNotImplementedException {

        return getService(entitySet.getName()).updateEntity((ODataSet) data);
    }

    @Override
    public void deleteRelation(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
            Map<String, Object> targetKeys) throws ODataException {

        ODataService targetService = getService(targetEntitySet.getName());
        ODataService sourceService = getService(sourceEntitySet.getName());

        ODataAnnotationNavInfoUtil navigationInfo =
                ODataManagingServiceUtil.getNavigation(sourceService.getToClass(), targetService.getToClass(), null);

        sourceService.deleteRelation(targetKeys, (ODataSet) sourceData,
                targetService.getEntityClass(), navigationInfo);
    }

    @Override
    public Object writeRelation(EdmEntitySet sourceEntitySet, Object sourceData, EdmEntitySet targetEntitySet,
            Map<String, Object> targetKeys, String navigationProperty) throws ODataException {

        ODataService targetService = getService(targetEntitySet.getName());
        ODataService sourceService = getService(sourceEntitySet.getName());

        ODataAnnotationNavInfoUtil navigationInfo =
                ODataManagingServiceUtil.getNavigation(sourceService.getToClass(), targetService.getToClass(), null);

        return sourceService
                .setRelation(targetKeys, (ODataSet) sourceData, targetService.getEntityClass(), navigationInfo,
                        navigationProperty);
    }

    @Override
    public Object writeBinaryData(EdmEntitySet entitySet, Object mediaLinkEntryData, BinaryData binaryData)
            throws ODataNotImplementedException, EdmException {

        if (mediaLinkEntryData instanceof BinaryODataSet) {
            FileService mediaService =
                    (FileService) getService(entitySet.getName());
            return mediaService.writeBinaryData(binaryData, (BinaryODataSet) mediaLinkEntryData);
        }

        logger.warn("Write binary data is not supported for type: " + entitySet.getName());
        throw new ODataNotImplementedException(ODataNotImplementedException.COMMON);
    }

    @Override
    public BinaryData readBinaryData(EdmEntitySet entitySet, Object mediaLinkEntryData)
            throws ODataNotImplementedException, EdmException {

        if (mediaLinkEntryData instanceof BinaryODataSet) {
            FileService mediaService =
                    (FileService) getService(entitySet.getName());
            return mediaService.readBinaryData((BinaryODataSet) mediaLinkEntryData);
        }

        logger.warn("Read binary data is not supported for type: " + entitySet.getName());
        throw new ODataNotImplementedException(ODataNotImplementedException.COMMON);
    }

    private Object readEntityById(String entitySetName, Map<String, Object> targetKeys)
            throws ODataNotImplementedException {

        return getService(entitySetName).readEntity(targetKeys);
    }

    private Object readEntity(Object sourceData,
            EdmEntitySet sourceEntitySet, EdmEntitySet targetEntitySet, String propertyName)
            throws EdmException, ODataNotImplementedException {

        Object result;

        ODataService sourceService = getService(sourceEntitySet.getName());
        ODataService targetService = getService(targetEntitySet.getName());

        ODataAnnotationNavInfoUtil navigationInfo =
                ODataManagingServiceUtil
                        .getNavigation(sourceService.getToClass(), targetService.getToClass(), propertyName);
        String fieldName = ODataManagingServiceUtil.getFieldName(navigationInfo);

        List readResult = new ArrayList<>(
                sourceService.readRelated((ODataSet) sourceData, targetService.getToClass(), fieldName));

        result = navigationInfo.getToMultiplicity()
                .equals(EdmMultiplicity.MANY) ? readResult : getFirstElementFromResult(readResult);

        return result;
    }

    private Object getFirstElementFromResult(List rededResult) {

        return CollectionUtils.isNotEmpty(rededResult) ? rededResult.get(0) : null;
    }

    @Override
    public void validate(EdmEntitySet entitySet, Map<String, Object> keys)
            throws EdmException, ODataNotImplementedException {

        getService(entitySet.getName()).validate((ODataSet) readData(entitySet, keys));
    }
}
