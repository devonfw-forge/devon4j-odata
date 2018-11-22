package com.devonfw.module.odata.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.transaction.Transactional;
import java.io.InputStream;
import java.util.*;

import org.apache.olingo.odata2.annotation.processor.core.datasource.AnnotationValueAccess;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource.BinaryData;
import org.apache.olingo.odata2.annotation.processor.core.datasource.ValueAccess;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.batch.BatchHandler;
import org.apache.olingo.odata2.api.batch.BatchRequestPart;
import org.apache.olingo.odata2.api.batch.BatchResponsePart;
import org.apache.olingo.odata2.api.commons.HttpContentType;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.commons.InlineCount;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.*;
import org.apache.olingo.odata2.api.ep.callback.*;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.*;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.*;
import org.apache.olingo.odata2.api.uri.expression.*;
import org.apache.olingo.odata2.api.uri.info.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.devonfw.module.odata.common.constraint.exception.ConstraintViolationException;
import com.devonfw.module.odata.service.api.ODataManagingService;
import com.google.common.primitives.Ints;

/**
 * @author manjinsi
 */

public class ODataProcessor extends ODataSingleProcessor {

  private ODataManagingService oDataManagingService;

  private ValueAccess valueAccess;

  private static final int SERVER_PAGING_SIZE = 100;

  private final Logger logger = LoggerFactory.getLogger(ODataProcessor.class);

  public ODataProcessor(ODataManagingService managingService) {

    this.oDataManagingService = managingService;
    this.valueAccess = new AnnotationValueAccess();
  }

  @Override
  public ODataResponse readEntitySet(final GetEntitySetUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      ArrayList<Object> data = new ArrayList<Object>();
      try {
        data.addAll(
                (List<?>) retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
                        uriInfo.getFunctionImport(),
                        mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                        uriInfo.getNavigationSegments()));
      } catch (final ODataNotFoundException e) {
        data.clear();
      }

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
      final InlineCount inlineCountType = uriInfo.getInlineCount();
      final Integer count = applySystemQueryOptions(entitySet, data, uriInfo.getFilter(), inlineCountType,
              uriInfo.getOrderBy(), uriInfo.getSkipToken(), uriInfo.getSkip(), uriInfo.getTop());

      ODataContext context = getContext();
      String nextLink = null;

      // Limit the number of returned entities and provide a "next" link
      // if there are further entities.
      // Almost all system query options in the current request must be carried
      // over to the URI for the "next" link, with the exception of $skiptoken
      // and $skip.
      if (data.size() > SERVER_PAGING_SIZE) {
        if (uriInfo.getOrderBy() == null && uriInfo.getSkipToken() == null && uriInfo.getSkip() == null
                && uriInfo.getTop() == null) {
          sortInDefaultOrder(entitySet, data);
        }

        nextLink =
                context.getPathInfo().getServiceRoot().relativize(context.getPathInfo().getRequestUri())
                        .toString();
        nextLink = percentEncodeNextLink(nextLink);
        nextLink +=
                (nextLink.contains("?") ? "&" : "?") + "$skiptoken="
                        + getSkipToken(entitySet, data.get(SERVER_PAGING_SIZE));

        while (data.size() > SERVER_PAGING_SIZE) {
          data.remove(SERVER_PAGING_SIZE);
        }
      }

      final EdmEntityType entityType = entitySet.getEntityType();
      List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
      for (final Object entryData : data) {
        values.add(getStructuralTypeValueMap(entryData, entityType));
      }

      final EntityProviderWriteProperties feedProperties = EntityProviderWriteProperties
              .serviceRoot(context.getPathInfo().getServiceRoot()).inlineCountType(inlineCountType)
              .inlineCount(count)
              .expandSelectTree(UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand()))
              .callbacks(getCallbacks(data, entityType)).nextLink(nextLink).build();

      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeFeed");
      final ODataResponse response = EntityProvider.writeFeed(contentType, entitySet, values, feedProperties);

      context.stopRuntimeMeasurement(timingHandle);
      return ODataResponse.fromResponse(response).build();
    } catch (ODataException e) {
      logger.error("Error during read EntitySet appeared ", e);
      throw new ODataException(e);
    }
  }

  public String percentEncodeNextLink(final String link) {

    if (link == null) {
      return null;
    }

    return link.replaceAll("\\$skiptoken=.+?(?:&|$)", "").replaceAll("\\$skip=.+?(?:&|$)", "")
            .replaceFirst("(?:\\?|&)$", ""); // Remove potentially trailing "?" or "&" left over
    // from remove actions
  }

  @Override
  public ODataResponse countEntitySet(final GetEntitySetCountUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      ArrayList<Object> data = new ArrayList<Object>();

      try {
        data.addAll(
                (List<?>) retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
                        uriInfo.getFunctionImport(),
                        mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                        uriInfo.getNavigationSegments()));

      } catch (final ODataNotFoundException e) {
        data.clear();
      }

      applySystemQueryOptions(uriInfo.getTargetEntitySet(), data, uriInfo.getFilter(), null, null, null,
              uriInfo.getSkip(), uriInfo.getTop());

      return ODataResponse.fromResponse(EntityProvider.writeText(String.valueOf(data.size()))).build();
    } catch (ODataException e) {
      logger.error("Error during read EntitySet appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntityLinks(final GetEntitySetLinksUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      ArrayList<Object> data = new ArrayList<Object>();
      try {

        data.addAll(
                (List<?>) retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
                        uriInfo.getFunctionImport(),
                        mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                        uriInfo.getNavigationSegments()));

      } catch (final ODataNotFoundException e) {
        data.clear();
      }

      final Integer count =
              applySystemQueryOptions(uriInfo.getTargetEntitySet(), data, uriInfo.getFilter(),
                      uriInfo.getInlineCount(), null, // uriInfo.getOrderBy(),
                      uriInfo.getSkipToken(), uriInfo.getSkip(), uriInfo.getTop());

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();

      List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
      for (final Object entryData : data) {
        Map<String, Object> entryValues = new HashMap<String, Object>();
        for (final EdmProperty property : entitySet.getEntityType().getKeyProperties()) {
          entryValues.put(property.getName(), this.valueAccess.getPropertyValue(entryData, property));
        }
        values.add(entryValues);
      }

      ODataContext context = getContext();
      final EntityProviderWriteProperties entryProperties =
              EntityProviderWriteProperties.serviceRoot(context.getPathInfo().getServiceRoot())
                      .inlineCountType(uriInfo.getInlineCount()).inlineCount(count).build();

      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeLinks");

      final ODataResponse response = EntityProvider.writeLinks(contentType, entitySet, values, entryProperties);

      context.stopRuntimeMeasurement(timingHandle);

      return ODataResponse.fromResponse(response).build();

    } catch (ODataException e) {
      logger.error("Error during readEntityLinks appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse countEntityLinks(final GetEntitySetLinksCountUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      return countEntitySet((GetEntitySetCountUriInfo) uriInfo, contentType);
    } catch (ODataException e) {
      logger.error("Error during countEntityLinks appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntity(final GetEntityUriInfo uriInfo, final String contentType) throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        return getEmptyResponse(uriInfo, contentType);
        // throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final ExpandSelectTreeNode expandSelectTreeNode =
              UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
      return ODataResponse
              .fromResponse(writeEntry(uriInfo.getTargetEntitySet(), expandSelectTreeNode, data, contentType))
              .build();

    } catch (ODataException e) {
      logger.error("Error during read Entity appeared for type" + contentType, e);
      throw new ODataException(e);
    }
  }

  private ODataResponse getEmptyResponse(GetEntityUriInfo uriInfo, final String contentType)
          throws ODataException {

    final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();

    ODataContext context = getContext();
    final EntityProviderWriteProperties feedProperties = EntityProviderWriteProperties
            .serviceRoot(context.getPathInfo().getServiceRoot())
            .inlineCount(1)
            .expandSelectTree(UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand()))
            .callbacks(getCallbacks(new ArrayList<>(), entitySet.getEntityType()))
            .build();

    return EntityProvider.writeFeed(contentType, entitySet, new ArrayList<Map<String, Object>>(), feedProperties);
  }

  @Override
  public ODataResponse existsEntity(final GetEntityCountUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      final Object data =
              retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(), uriInfo.getFunctionImport(),
                      mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                      uriInfo.getNavigationSegments());
      return ODataResponse
              .fromResponse(EntityProvider.writeText(appliesFilter(data, uriInfo.getFilter()) ? "1" : "0"))
              .build();
    } catch (ODataException e) {
      logger.error("Error during existsEntity appeared ", e);
      throw new ODataException(e);
    }
  }

  @Transactional(rollbackOn = ODataException.class)
  @Override
  public ODataResponse deleteEntity(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {

    try {
      this.oDataManagingService.deleteData(uriInfo.getStartEntitySet(), mapKey(uriInfo.getKeyPredicates()),
              uriInfo.getCustomQueryOptions());
      return ODataResponse.newBuilder().build();
    } catch (EdmException | ConstraintViolationException e) {
      logger.error("Error during deleteEntity appeared ", e);
      throw new ODataException(e);
    }
  }

  @Transactional(rollbackOn = ODataException.class)
  @Override
  public ODataResponse createEntity(final PostUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
      final EdmEntityType entityType = entitySet.getEntityType();

      Object data = this.oDataManagingService.createEmptyTo(entitySet);
      ExpandSelectTreeNode expandSelectTree = null;

      if (entityType.hasStream()) {
        data = this.oDataManagingService.createData(entitySet, data);
        data = this.oDataManagingService.writeBinaryData(entitySet, data,
                new BinaryData(EntityProvider.readBinary(content), requestContentType));

      } else {
        final EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false)
                .addTypeMappings(getStructuralTypeTypeMap(data, entityType)).build();
        final ODataEntry entryValues = parseEntry(entitySet, content, requestContentType, properties);

        setStructuralTypeValuesFromMap(data, entityType, entryValues.getProperties(), false);
        data = this.oDataManagingService.createData(entitySet, data);
        createInlinedEntities(entitySet, data, entryValues);
        expandSelectTree = entryValues.getExpandSelectTree();
      }

      Map<String, Object> structuralTypeValueMap = getStructuralTypeValueMap(data, entityType);

      // Link back to the entity the target entity set is related to, if any.
      final List<NavigationSegment> navigationSegments = uriInfo.getNavigationSegments();

      if (!navigationSegments.isEmpty()) {
        ODataProcessorDAO oDataProcessorDAO = createODataProcessorDao((UriInfo) uriInfo);
        Object sourceData = oDataProcessorDAO.getSourceData();
        final EdmEntitySet lastEntitySet = oDataProcessorDAO.getEntitySet();
        final String navigationProperty = oDataProcessorDAO.getNavigation();

        this.oDataManagingService
                .writeRelation(lastEntitySet, sourceData, entitySet, structuralTypeValueMap,
                        navigationProperty);
      }

      oDataManagingService.validate(entitySet, structuralTypeValueMap);

      return ODataResponse
              .fromResponse(writeEntry(uriInfo.getTargetEntitySet(), expandSelectTree, data, contentType))
              .eTag(constructETag(entitySet, data)).build();
    } catch (ODataException | ConstraintViolationException e) {
      logger.error("Error during creating Entity appeared ", e);
      throw new ODataException(e);
    }
  }

  @Transactional(rollbackOn = ODataException.class)
  @Override
  public ODataResponse updateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final boolean merge, final String contentType) throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      ExpandSelectTreeNode expandSelectTree = null;
      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
      final EdmEntityType entityType = entitySet.getEntityType();
      final EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(merge)
              .addTypeMappings(getStructuralTypeTypeMap(data, entityType)).build();
      final ODataEntry entryValues = parseEntry(entitySet, content, requestContentType, properties);

      setStructuralTypeValuesFromMap(data, entityType, entryValues.getProperties(), merge);
      data = this.oDataManagingService.updateData(entitySet, data);
      createInlinedEntities(entitySet, data, entryValues);
      expandSelectTree = entryValues.getExpandSelectTree();

      oDataManagingService.validate(entitySet, getStructuralTypeValueMap(data, entityType));
      return ODataResponse
              .fromResponse(writeEntry(uriInfo.getTargetEntitySet(), expandSelectTree, data, contentType))
              .eTag(constructETag(entitySet, data)).build();

    } catch (EdmException | ConstraintViolationException e) {
      logger.error("Error(EdmException) during updateEntity appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntityLink(final GetEntityLinkUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      // if (!appliesFilter(data, uriInfo.getFilter()))
      if (data == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();

      Map<String, Object> values = new HashMap<String, Object>();
      for (final EdmProperty property : entitySet.getEntityType().getKeyProperties()) {
        values.put(property.getName(), this.valueAccess.getPropertyValue(data, property));
      }

      ODataContext context = getContext();
      final EntityProviderWriteProperties entryProperties =
              EntityProviderWriteProperties.serviceRoot(context.getPathInfo().getServiceRoot()).build();

      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeLink");

      final ODataResponse response = EntityProvider.writeLink(contentType, entitySet, values, entryProperties);

      context.stopRuntimeMeasurement(timingHandle);

      return ODataResponse.fromResponse(response).build();

    } catch (ODataException e) {
      logger.error("Error(ODataException) during creating Entity appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse existsEntityLink(final GetEntityLinkCountUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      return existsEntity((GetEntityCountUriInfo) uriInfo, contentType);
    } catch (ODataException e) {
      logger.error("Error(ODataException) during checking existsEntityLink appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse deleteEntityLink(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {

    try {
      ODataProcessorDAO oDataProcessorDAO = createODataProcessorDao((UriInfo) uriInfo);
      Object sourceData = oDataProcessorDAO.getSourceData();

      final EdmEntitySet entitySet = oDataProcessorDAO.getEntitySet();
      final EdmEntitySet targetEntitySet = oDataProcessorDAO.getTargetEntitySet();
      final String navigationPropertyName = oDataProcessorDAO.getNavigation();
      final Map<String, Object> keys = mapKey(uriInfo.getTargetKeyPredicates());

      final Object targetData = this.oDataManagingService
              .readRelatedData(entitySet, sourceData, targetEntitySet, keys, navigationPropertyName);

      if (!appliesFilter(targetData, ((UriInfo) uriInfo).getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      this.oDataManagingService.deleteRelation(entitySet, sourceData, targetEntitySet, keys);
      return ODataResponse.newBuilder().build();

    } catch (ODataException e) {
      logger.error("Error during deleteEntityLink appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse createEntityLink(final PostUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {
      ODataProcessorDAO oDataProcessorDAO = createODataProcessorDao((UriInfo) uriInfo);
      Object sourceData = oDataProcessorDAO.getSourceData();
      final EdmEntitySet entitySet = oDataProcessorDAO.getEntitySet();
      final EdmEntitySet targetEntitySet = oDataProcessorDAO.getTargetEntitySet();
      final String navigationProperty = oDataProcessorDAO.getNavigation();

      final Map<String, Object> targetKeys = parseLink(targetEntitySet, content, requestContentType);

      this.oDataManagingService
              .writeRelation(entitySet, sourceData, targetEntitySet, targetKeys, navigationProperty);
      return ODataResponse.newBuilder().build();

    } catch (ODataException e) {
      logger.error("Error during createEntityLink appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse updateEntityLink(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {
      ODataProcessorDAO oDataProcessorDAO = createODataProcessorDao((UriInfo) uriInfo);
      Object sourceData = oDataProcessorDAO.getSourceData();
      final EdmEntitySet entitySet = oDataProcessorDAO.getEntitySet();
      final EdmEntitySet targetEntitySet = oDataProcessorDAO.getTargetEntitySet();
      final Map<String, Object> keys = mapKey(uriInfo.getTargetKeyPredicates());
      final String navigationPropertyName = oDataProcessorDAO.getNavigation();

      final Object targetData = this.oDataManagingService
              .readRelatedData(entitySet, sourceData, targetEntitySet, keys, navigationPropertyName);

      if (!appliesFilter(targetData, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      this.oDataManagingService.deleteRelation(entitySet, sourceData, targetEntitySet, keys);
      final Map<String, Object> newKeys = parseLink(targetEntitySet, content, requestContentType);
      this.oDataManagingService
              .writeRelation(entitySet, sourceData, targetEntitySet, newKeys, navigationPropertyName);
      return ODataResponse.newBuilder().build();

    } catch (ODataException e) {
      logger.error("Error during updateEntityLink appeared ", e);
      throw new ODataException(e);
    }
  }

  private ODataProcessorDAO createODataProcessorDao(final UriInfo uriInfo) throws ODataException {

    final List<NavigationSegment> navigationSegments = uriInfo.getNavigationSegments();
    final List<NavigationSegment> previousSegments =
            navigationSegments.subList(0, navigationSegments.size() - 1);

    Object sourceData = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
            uriInfo.getFunctionImport(), mapFunctionParameters(uriInfo.getFunctionImportParameters()),
            previousSegments);

    final NavigationSegment navigationSegment =
            navigationSegments.isEmpty() ? null : navigationSegments.get(navigationSegments.size() - 1);
    final String navigationPropertyName =
            navigationSegment != null ? getNavigationName(navigationSegment.getNavigationProperty()) : null;
    final EdmEntitySet previousEntitySet = previousSegments.isEmpty() ? uriInfo.getStartEntitySet()
            : previousSegments.get(previousSegments.size() - 1).getEntitySet();
    final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();

    return new ODataProcessorDAO(previousEntitySet, sourceData, targetEntitySet, navigationPropertyName);
  }

  @Override
  public ODataResponse readEntityComplexProperty(final GetComplexPropertyUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (data == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
      final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
      final Object value = property.isSimple()
              ? property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null
              ? getPropertyValue(data, propertyPath)
              : getSimpleTypeValueMap(data, propertyPath)
              : getStructuralTypeValueMap(getPropertyValue(data, propertyPath),
              (EdmStructuralType) property.getType());

      ODataContext context = getContext();
      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeProperty");

      final ODataResponse response = EntityProvider.writeProperty(contentType, property, value);
      context.stopRuntimeMeasurement(timingHandle);
      return ODataResponse.fromResponse(response).eTag(constructETag(uriInfo.getTargetEntitySet(), data)).build();

    } catch (ODataException e) {
      logger.error("Error during readEntityComplexProperty appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntitySimpleProperty(final GetSimplePropertyUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      return readEntityComplexProperty((GetComplexPropertyUriInfo) uriInfo, contentType);
    } catch (ODataException e) {
      logger.error("Error during readEntitySimpleProperty appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntitySimplePropertyValue(final GetSimplePropertyUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      // if (!appliesFilter(data, uriInfo.getFilter()))
      if (data == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
      final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
      final Object value =
              property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null
                      ? getPropertyValue(data, propertyPath)
                      : getSimpleTypeValueMap(data, propertyPath);

      return ODataResponse.fromResponse(EntityProvider.writePropertyValue(property, value))
              .eTag(constructETag(uriInfo.getTargetEntitySet(), data)).build();

    } catch (ODataException e) {
      logger.error("Error during readEntitySimplePropertyValue appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse deleteEntitySimplePropertyValue(final DeleteUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (data == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
      final EdmProperty property = propertyPath.get(propertyPath.size() - 1);

      data = getPropertyValue(data, propertyPath.subList(0, propertyPath.size() - 1));
      this.valueAccess.setPropertyValue(data, property, null);
      this.valueAccess.setMappingValue(data, property.getMapping(), null);

      return ODataResponse.newBuilder().build();

    } catch (ODataException e) {
      logger.error("Error during deleteEntitySimplePropertyValue appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse updateEntityComplexProperty(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final boolean merge, final String contentType) throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
      final EdmProperty property = propertyPath.get(propertyPath.size() - 1);

      data = getPropertyValue(data, propertyPath.subList(0, propertyPath.size() - 1));

      ODataContext context = getContext();
      int timingHandle = context.startRuntimeMeasurement("EntityConsumer", "readProperty");

      Map<String, Object> values;
      try {
        values = EntityProvider.readProperty(requestContentType, property, content,
                EntityProviderReadProperties.init().mergeSemantic(merge).build());
      } catch (final EntityProviderException e) {
        throw new ODataBadRequestException(ODataBadRequestException.BODY, e);
      }

      context.stopRuntimeMeasurement(timingHandle);

      final Object value = values.get(property.getName());
      if (property.isSimple()) {
        this.valueAccess.setPropertyValue(data, property, value);
      } else {
        @SuppressWarnings("unchecked") final Map<String, Object> propertyValue = (Map<String, Object>) value;
        setStructuralTypeValuesFromMap(this.valueAccess.getPropertyValue(data, property),
                (EdmStructuralType) property.getType(), propertyValue, merge);
      }

      return ODataResponse.newBuilder().eTag(constructETag(uriInfo.getTargetEntitySet(), data)).build();

    } catch (ODataException e) {
      logger.error("Error during updateEntityComplexProperty appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse updateEntitySimpleProperty(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {
      return updateEntityComplexProperty(uriInfo, content, requestContentType, false, contentType);
    } catch (ODataException e) {
      logger.error("Error during updateEntitySimpleProperty appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse updateEntitySimplePropertyValue(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {
      Object data = retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(),
              uriInfo.getFunctionImport(),
              mapFunctionParameters(uriInfo.getFunctionImportParameters()), uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
      final EdmProperty property = propertyPath.get(propertyPath.size() - 1);

      data = getPropertyValue(data, propertyPath.subList(0, propertyPath.size() - 1));

      ODataContext context = getContext();
      int timingHandle = context.startRuntimeMeasurement("EntityConsumer", "readPropertyValue");

      Object value;
      try {
        value = EntityProvider.readPropertyValue(property, content);
      } catch (final EntityProviderException e) {
        throw new ODataBadRequestException(ODataBadRequestException.BODY, e);
      }

      context.stopRuntimeMeasurement(timingHandle);

      this.valueAccess.setPropertyValue(data, property, value);
      this.valueAccess.setMappingValue(data, property.getMapping(), requestContentType);

      return ODataResponse.newBuilder().eTag(constructETag(uriInfo.getTargetEntitySet(), data)).build();

    } catch (ODataException e) {
      logger.error("Error during updateEntitySimplePropertyValue appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse readEntityMedia(final GetMediaResourceUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      final Object data =
              retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(), uriInfo.getFunctionImport(),
                      mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                      uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
      final BinaryData binaryData = this.oDataManagingService.readBinaryData(entitySet, data);
      if (binaryData == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      final String mimeType =
              binaryData.getMimeType() == null ? HttpContentType.APPLICATION_OCTET_STREAM
                      : binaryData.getMimeType();

      return ODataResponse.fromResponse(EntityProvider.writeBinary(mimeType, binaryData.getData()))
              .eTag(constructETag(entitySet, data)).build();

    } catch (ODataException e) {
      logger.error("Error during readEntityMedia appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse deleteEntityMedia(final DeleteUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      final Object data =
              retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(), uriInfo.getFunctionImport(),
                      mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                      uriInfo.getNavigationSegments());

      if (data == null) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      this.oDataManagingService.writeBinaryData(uriInfo.getTargetEntitySet(), data, new BinaryData(null, null));

      return ODataResponse.newBuilder().build();

    } catch (ODataException e) {
      logger.error("Error during deleteEntityMedia appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse updateEntityMedia(final PutMergePatchUriInfo uriInfo, final InputStream content,
          final String requestContentType, final String contentType) throws ODataException {

    try {
      final Object data =
              retrieveData(uriInfo.getStartEntitySet(), uriInfo.getKeyPredicates(), uriInfo.getFunctionImport(),
                      mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                      uriInfo.getNavigationSegments());

      if (!appliesFilter(data, uriInfo.getFilter())) {
        throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
      }

      ODataContext context = getContext();
      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "readBinary");

      final byte[] value = EntityProvider.readBinary(content);

      context.stopRuntimeMeasurement(timingHandle);

      final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
      this.oDataManagingService.writeBinaryData(entitySet, data, new BinaryData(value, requestContentType));

      return ODataResponse.newBuilder().eTag(constructETag(entitySet, data)).build();

    } catch (ODataException e) {
      logger.error("Error during updateEntityMedia appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse executeFunctionImport(final GetFunctionImportUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      final EdmFunctionImport functionImport = uriInfo.getFunctionImport();
      final EdmType type = functionImport.getReturnType().getType();

      final Object data =
              this.oDataManagingService
                      .readData(functionImport, mapFunctionParameters(uriInfo.getFunctionImportParameters()),
                              null);
      if (data == null) {
        throw new ODataNotFoundException(ODataHttpException.COMMON);
      }

      if (data instanceof BinaryData) {
        BinaryData media = (BinaryData) data;
        return ODataResponse.fromResponse(EntityProvider.writeBinary(media.getMimeType(), media.getData()))
                .build();
      }

      Object value;
      if (type.getKind() == EdmTypeKind.SIMPLE) {
        value = type == EdmSimpleTypeKind.Binary.getEdmSimpleTypeInstance() ? ((BinaryData) data).getData()
                : data;
      } else if (functionImport.getReturnType().getMultiplicity() == EdmMultiplicity.MANY) {
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        for (final Object typeData : (List<?>) data) {
          values.add(getStructuralTypeValueMap(typeData, (EdmStructuralType) type));
        }
        value = values;
      } else {
        value = getStructuralTypeValueMap(data, (EdmStructuralType) type);
      }

      ODataContext context = getContext();

      final EntityProviderWriteProperties entryProperties =
              EntityProviderWriteProperties.serviceRoot(context.getPathInfo().getServiceRoot()).build();

      final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeFunctionImport");

      final ODataResponse response =
              EntityProvider.writeFunctionImport(contentType, functionImport, value, entryProperties);

      context.stopRuntimeMeasurement(timingHandle);

      return ODataResponse.fromResponse(response).build();
    } catch (ODataException e) {
      logger.error("Error during executeFunctionImport appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public ODataResponse executeFunctionImportValue(final GetFunctionImportUriInfo uriInfo, final String contentType)
          throws ODataException {

    try {
      final EdmFunctionImport functionImport = uriInfo.getFunctionImport();
      final EdmSimpleType type = (EdmSimpleType) functionImport.getReturnType().getType();

      final Object data = this.oDataManagingService.readData(functionImport,
              mapFunctionParameters(uriInfo.getFunctionImportParameters()),
              null);

      if (data == null) {
        throw new ODataNotFoundException(ODataHttpException.COMMON);
      }

      ODataResponse response;
      if (type == EdmSimpleTypeKind.Binary.getEdmSimpleTypeInstance()) {
        response = EntityProvider.writeBinary(((BinaryData) data).getMimeType(), ((BinaryData) data).getData());
      } else {
        final String value = type.valueToString(data, EdmLiteralKind.DEFAULT, null);
        response = EntityProvider.writeText(value == null ? "" : value);
      }
      return ODataResponse.fromResponse(response).build();

    } catch (ODataException e) {
      logger.error("Error during executeFunctionImportValue appeared ", e);
      throw new ODataException(e);
    }
  }

  private static Map<String, Object> mapKey(final List<KeyPredicate> keys) throws EdmException {

    Map<String, Object> keyMap = new HashMap<String, Object>();
    for (final KeyPredicate key : keys) {
      final EdmProperty property = key.getProperty();
      final EdmSimpleType type = (EdmSimpleType) property.getType();
      keyMap.put(property.getName(),
              type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(),
                      type.getDefaultType()));
    }
    return keyMap;
  }

  private static Map<String, Object> mapFunctionParameters(final Map<String, EdmLiteral> functionImportParameters)
          throws EdmSimpleTypeException {

    if (functionImportParameters == null) {
      return Collections.emptyMap();
    } else {
      Map<String, Object> parameterMap = new HashMap<String, Object>();
      for (final String parameterName : functionImportParameters.keySet()) {
        final EdmLiteral literal = functionImportParameters.get(parameterName);
        final EdmSimpleType type = literal.getType();
        parameterMap.put(parameterName,
                type.valueOfString(literal.getLiteral(), EdmLiteralKind.DEFAULT, null, type.getDefaultType()));
      }
      return parameterMap;
    }
  }

  private Object retrieveData(final EdmEntitySet startEntitySet, final List<KeyPredicate> keyPredicates,
          final EdmFunctionImport functionImport, final Map<String, Object> functionImportParameters,
          final List<NavigationSegment> navigationSegments) throws ODataException {

    Object data;
    final Map<String, Object> keys = mapKey(keyPredicates);

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "retrieveData");

    try {
      data = functionImport == null
              ? keys.isEmpty() ? this.oDataManagingService.readData(startEntitySet)
              : this.oDataManagingService.readData(startEntitySet, keys)
              : this.oDataManagingService.readData(functionImport, functionImportParameters, keys);

      EdmEntitySet currentEntitySet = functionImport == null ? startEntitySet : functionImport.getEntitySet();
      for (NavigationSegment navigationSegment : navigationSegments) {

        data = this.oDataManagingService
                .readRelatedData(currentEntitySet, data, navigationSegment.getEntitySet(),
                        mapKey(navigationSegment.getKeyPredicates()),
                        getNavigationName(navigationSegment.getNavigationProperty()));
        currentEntitySet = navigationSegment.getEntitySet();
      }
    } finally {
      context.stopRuntimeMeasurement(timingHandle);
    }
    return data;
  }

  private String getNavigationName(EdmNavigationProperty navigationProperty) throws EdmException {

    return navigationProperty != null ? navigationProperty.getName() : null;
  }

  private <T> String constructETag(final EdmEntitySet entitySet, final T data) throws ODataException {

    final EdmEntityType entityType = entitySet.getEntityType();
    String eTag = null;
    for (final String propertyName : entityType.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) entityType.getProperty(propertyName);
      if (property.getFacets() != null && property.getFacets().getConcurrencyMode() == EdmConcurrencyMode.Fixed) {
        final EdmSimpleType type = (EdmSimpleType) property.getType();
        final String component = type.valueToString(this.valueAccess.getPropertyValue(data, property),
                EdmLiteralKind.DEFAULT, property.getFacets());
        eTag = eTag == null ? component : eTag + Edm.DELIMITER + component;
      }
    }
    return eTag == null ? null : "W/\"" + eTag + "\"";
  }

  private <T> Map<String, ODataCallback> getCallbacks(final T data, final EdmEntityType entityType)
          throws EdmException {

    final List<String> navigationPropertyNames = entityType.getNavigationPropertyNames();
    if (navigationPropertyNames.isEmpty()) {
      return null;
    } else {
      final WriteCallback callback = new WriteCallback(data);
      Map<String, ODataCallback> callbacks = new HashMap<String, ODataCallback>();
      for (final String name : navigationPropertyNames) {
        callbacks.put(name, callback);
      }
      return callbacks;
    }
  }

  private class WriteCallback implements OnWriteEntryContent, OnWriteFeedContent {

    private final Object data;

    private <T> WriteCallback(final T data) {

      this.data = data;
    }

    @Override
    public WriteFeedCallbackResult retrieveFeedResult(final WriteFeedCallbackContext context)
            throws ODataApplicationException {

      try {
        final EdmEntityType entityType =
                context.getSourceEntitySet().getRelatedEntitySet(context.getNavigationProperty())
                        .getEntityType();
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        Object relatedData = null;
        try {
          relatedData = readRelatedData(context);
          for (final Object entryData : (List<?>) relatedData) {
            values.add(getStructuralTypeValueMap(entryData, entityType));
          }
        } catch (final ODataNotFoundException e) {
          values.clear();
        }
        WriteFeedCallbackResult result = new WriteFeedCallbackResult();
        result.setFeedData(values);
        EntityProviderWriteProperties inlineProperties = EntityProviderWriteProperties
                .serviceRoot(getContext().getPathInfo().getServiceRoot())
                .callbacks(getCallbacks(relatedData, entityType))
                .expandSelectTree(context.getCurrentExpandSelectTreeNode()).selfLink(context.getSelfLink())
                .build();
        result.setInlineProperties(inlineProperties);
        return result;
      } catch (final ODataException e) {
        logger.error("Error during retrieveFeedResult appeared ", e);
        throw new ODataApplicationException(e.getLocalizedMessage(), Locale.ROOT, e);
      }
    }

    @Override
    public WriteEntryCallbackResult retrieveEntryResult(final WriteEntryCallbackContext context)
            throws ODataApplicationException {

      try {
        final EdmEntityType entityType =
                context.getSourceEntitySet().getRelatedEntitySet(context.getNavigationProperty())
                        .getEntityType();
        WriteEntryCallbackResult result = new WriteEntryCallbackResult();
        Object relatedData;
        try {
          relatedData = readRelatedData(context);
        } catch (final ODataNotFoundException e) {
          logger.error("Error ODataNotFoundException during retrieveEntryResult appeared ", e);
          relatedData = null;
        }

        if (relatedData == null) {
          result.setEntryData(Collections.emptyMap());
        } else {
          result.setEntryData(getStructuralTypeValueMap(relatedData, entityType));

          EntityProviderWriteProperties inlineProperties = EntityProviderWriteProperties
                  .serviceRoot(getContext().getPathInfo().getServiceRoot())
                  .callbacks(getCallbacks(relatedData, entityType))
                  .expandSelectTree(context.getCurrentExpandSelectTreeNode()).build();
          result.setInlineProperties(inlineProperties);
        }
        return result;
      } catch (final ODataException e) {
        logger.error("Error during retrieveEntryResult appeared ", e);
        throw new ODataApplicationException(e.getLocalizedMessage(), Locale.ROOT, e);
      }
    }

    private Object readRelatedData(final WriteCallbackContext context) throws ODataException {

      final EdmEntitySet entitySet = context.getSourceEntitySet();
      Object dataEntry = this.data;
      if (this.data instanceof List) {
        dataEntry = readEntryData((List<?>) this.data, entitySet.getEntityType(),
                context.extractKeyFromEntryData());
      }
      return ODataProcessor.this.oDataManagingService.readRelatedData(entitySet,
              dataEntry, entitySet.getRelatedEntitySet(context.getNavigationProperty()),
              Collections.emptyMap(), getNavigationName(context.getNavigationProperty()));
    }

    private <T> T readEntryData(final List<T> data, final EdmEntityType entityType, final Map<String, Object> key)
            throws ODataException {

      for (final T entryData : data) {
        if (checkAllPropertiesExistInEntries(key, entryData, entityType.getKeyProperties())) {
          return entryData;
        }
      }
      return null;
    }

    private <T> boolean checkAllPropertiesExistInEntries(Map<String, Object> keys, T entryData,
            List<EdmProperty> keyProperties) throws ODataException {

      for (final EdmProperty keyProperty : keyProperties) {
        if (!ODataProcessor.this.valueAccess.getPropertyValue(entryData, keyProperty)
                .equals(keys.get(keyProperty.getName()))) {
          return false;
        }
      }
      return true;
    }
  }

  private <T> ODataResponse writeEntry(final EdmEntitySet entitySet, final ExpandSelectTreeNode expandSelectTree,
          final T data, final String contentType) throws ODataException {

    final EdmEntityType entityType = entitySet.getEntityType();
    final Map<String, Object> values = getStructuralTypeValueMap(data, entityType);

    ODataContext context = getContext();
    EntityProviderWriteProperties writeProperties =
            EntityProviderWriteProperties.serviceRoot(context.getPathInfo().getServiceRoot())
                    .expandSelectTree(expandSelectTree).callbacks(getCallbacks(data, entityType)).build();

    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "writeEntry");
    final ODataResponse response = EntityProvider.writeEntry(contentType, entitySet, values, writeProperties);
    context.stopRuntimeMeasurement(timingHandle);

    return response;
  }

  private ODataEntry parseEntry(final EdmEntitySet entitySet, final InputStream content,
          final String requestContentType, final EntityProviderReadProperties properties)
          throws ODataBadRequestException {

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement("EntityConsumer", "readEntry");

    ODataEntry entryValues;
    try {
      entryValues = EntityProvider.readEntry(requestContentType, entitySet, content, properties);
    } catch (final EntityProviderException e) {
      logger.error("Error EntityProviderException during parseEntry appeared ", e);
      throw new ODataBadRequestException(ODataBadRequestException.BODY, e);
    }

    context.stopRuntimeMeasurement(timingHandle);
    return entryValues;
  }

  private Map<String, Object> parseLink(final EdmEntitySet entitySet, final InputStream content,
          final String contentType) throws ODataException {

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement("EntityProvider", "readLink");

    final String uriString = EntityProvider.readLink(contentType, entitySet, content);

    context.stopRuntimeMeasurement(timingHandle);

    final Map<String, Object> targetKeys = parseLinkUri(entitySet, uriString);
    if (targetKeys == null) {
      throw new ODataBadRequestException(ODataBadRequestException.BODY);
    }
    return targetKeys;
  }

  private Map<String, Object> parseLinkUri(final EdmEntitySet targetEntitySet, final String uriString)
          throws ODataException {

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement("UriParser", "getKeyPredicatesFromEntityLink");

    List<KeyPredicate> key = UriParser.getKeyPredicatesFromEntityLink(targetEntitySet, uriString,
            context.getPathInfo().getServiceRoot());
    context.stopRuntimeMeasurement(timingHandle);

    return key == null ? null : mapKey(key);
  }

  private <T> void createInlinedEntities(final EdmEntitySet entitySet, final T data, final ODataEntry entryValues)
          throws ODataException {

    final EdmEntityType entityType = entitySet.getEntityType();
    for (final String navigationPropertyName : entityType.getNavigationPropertyNames()) {

      final EdmNavigationProperty navigationProperty =
              (EdmNavigationProperty) entityType.getProperty(navigationPropertyName);
      final EdmEntitySet relatedEntitySet = entitySet.getRelatedEntitySet(navigationProperty);
      final EdmEntityType relatedEntityType = relatedEntitySet.getEntityType();

      final Object relatedValue = entryValues.getProperties().get(navigationPropertyName);
      if (relatedValue == null) {
        for (final String uriString : entryValues.getMetadata().getAssociationUris(navigationPropertyName)) {
          final Map<String, Object> key = parseLinkUri(relatedEntitySet, uriString);
          if (key != null) {
            this.oDataManagingService
                    .writeRelation(entitySet, data, relatedEntitySet, key, navigationPropertyName);
          }
        }

      } else {
        if (relatedValue instanceof ODataFeed) {
          ODataFeed feed = (ODataFeed) relatedValue;
          final List<ODataEntry> relatedValueList = feed.getEntries();
          for (final ODataEntry relatedValues : relatedValueList) {
            Object relatedData = this.oDataManagingService.createEmptyTo(relatedEntitySet);
            setStructuralTypeValuesFromMap(relatedData, relatedEntityType, relatedValues.getProperties(),
                    false);
            this.oDataManagingService.createData(relatedEntitySet, relatedData);
            this.oDataManagingService.writeRelation(entitySet, data, relatedEntitySet,
                    getStructuralTypeValueMap(relatedData, relatedEntityType), navigationPropertyName);
            createInlinedEntities(relatedEntitySet, relatedData, relatedValues);
          }
        } else if (relatedValue instanceof ODataEntry) {
          final ODataEntry relatedValueEntry = (ODataEntry) relatedValue;
          Object relatedData = this.oDataManagingService.createEmptyTo(relatedEntitySet);
          setStructuralTypeValuesFromMap(relatedData, relatedEntityType, relatedValueEntry.getProperties(),
                  false);
          this.oDataManagingService.createData(relatedEntitySet, relatedData);
          this.oDataManagingService.writeRelation(entitySet, data, relatedEntitySet,
                  getStructuralTypeValueMap(relatedData, relatedEntityType), navigationPropertyName);
          createInlinedEntities(relatedEntitySet, relatedData, relatedValueEntry);
        } else {
          throw new ODataException(
                  "Unexpected class for a related value: " + relatedValue.getClass().getSimpleName());
        }

      }
    }
  }

  private <T> Integer applySystemQueryOptions(final EdmEntitySet entitySet, final List<T> data,
          final FilterExpression filter, final InlineCount inlineCount, final OrderByExpression orderBy,
          final String skipToken, final Integer skip, final Integer top) throws ODataException {

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "applySystemQueryOptions");

    if (filter != null) {
      // Remove all elements the filter does not apply for.
      // A for-each loop would not work with "remove", see Java documentation.
      for (Iterator<T> iterator = data.iterator(); iterator.hasNext(); ) {
        if (!appliesFilter(iterator.next(), filter)) {
          iterator.remove();
        }
      }
    }

    final Integer count = inlineCount == InlineCount.ALLPAGES ? data.size() : null;

    if (orderBy != null) {
      sort(data, orderBy);
    } else if (skipToken != null || skip != null || top != null) {
      sortInDefaultOrder(entitySet, data);
    }

    if (skipToken != null) {
      while (!data.isEmpty() && !getSkipToken(entitySet, data.get(0)).equals(skipToken)) {
        data.remove(0);
      }
    }

    if (skip != null) {
      if (skip >= data.size()) {
        data.clear();
      } else {
        for (int i = 0; i < skip; i++) {
          data.remove(0);
        }
      }
    }

    if (top != null) {
      while (data.size() > top) {
        data.remove(top.intValue());
      }
    }

    context.stopRuntimeMeasurement(timingHandle);

    return count;
  }

  private <T> void sort(final List<T> data, final OrderByExpression orderBy) {

    data.sort((entity1, entity2) -> {

      try {
        int result = 0;
        for (final OrderExpression expression : orderBy.getOrders()) {
          String first = evaluateExpression(entity1, expression.getExpression());
          String second = evaluateExpression(entity2, expression.getExpression());

          Integer firstInteger = Ints.tryParse(first);
          Integer secondInteger = Ints.tryParse(second);

          if (firstInteger != null && secondInteger != null) {
            result = firstInteger.compareTo(secondInteger);
          } else if (first != null && second != null) {
            result = first.compareTo(second);
          } else if (first == null && second != null) {
            result = 1;
          } else if (first != null && second == null) {
            result = -1;
          }

          if (expression.getSortOrder() == SortOrder.desc) {
            result = -result;
          }

          if (result != 0) {
            break;
          }
        }
        return result;
      } catch (final ODataException e) {
        return 0;
      }
    });
  }

  private <T> void sortInDefaultOrder(final EdmEntitySet entitySet, final List<T> data) {

    data.sort((entity1, entity2) -> {

      try {
        return getSkipToken(entitySet, entity1).compareTo(getSkipToken(entitySet, entity2));
      } catch (final ODataException e) {
        return 0;
      }
    });
  }

  private <T> boolean appliesFilter(final T data, final FilterExpression filter) throws ODataException {

    ODataContext context = getContext();
    final int timingHandle = context.startRuntimeMeasurement(getClass().getSimpleName(), "appliesFilter");

    try {
      return data != null && (filter == null || evaluateExpression(data, filter.getExpression()).equals("true"));
    } catch (final RuntimeException e) {
      return false;
    } finally {
      context.stopRuntimeMeasurement(timingHandle);
    }
  }

  private <T> String evaluateExpression(final T data, final CommonExpression expression) throws ODataException {

    switch (expression.getKind()) {
      case UNARY:
        final UnaryExpression unaryExpression = (UnaryExpression) expression;
        final String operand = evaluateExpression(data, unaryExpression.getOperand());

        switch (unaryExpression.getOperator()) {
          case NOT:
            return Boolean.toString(!Boolean.parseBoolean(operand));
          case MINUS:
            return operand.startsWith("-") ? operand.substring(1) : "-" + operand;
          default:
            throw new ODataNotImplementedException();
        }

      case BINARY:
        final BinaryExpression binaryExpression = (BinaryExpression) expression;
        final EdmSimpleType type = (EdmSimpleType) binaryExpression.getLeftOperand().getEdmType();
        final String left = evaluateExpression(data, binaryExpression.getLeftOperand());
        final String right = evaluateExpression(data, binaryExpression.getRightOperand());

        switch (binaryExpression.getOperator()) {
          case ADD:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single
                    .getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) + Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) + Long.valueOf(right));
            }
          case SUB:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single
                    .getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) - Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) - Long.valueOf(right));
            }
          case MUL:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single
                    .getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) * Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) * Long.valueOf(right));
            }
          case DIV:
            final String number = Double.toString(Double.valueOf(left) / Double.valueOf(right));
            return number.endsWith(".0") ? number.replace(".0", "") : number;
          case MODULO:
            if (binaryExpression.getEdmType() == EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance()
                    || binaryExpression.getEdmType() == EdmSimpleTypeKind.Single
                    .getEdmSimpleTypeInstance()) {
              return Double.toString(Double.valueOf(left) % Double.valueOf(right));
            } else {
              return Long.toString(Long.valueOf(left) % Long.valueOf(right));
            }
          case AND:
            return Boolean.toString(left.equals("true") && right.equals("true"));
          case OR:
            return Boolean.toString(left.equals("true") || right.equals("true"));
          case EQ:
            return Boolean.toString(left.equals(right));
          case NE:
            return Boolean.toString(!left.equals(right));
          case LT:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) < 0);
            } else {
              return Boolean.toString(Double.valueOf(left) < Double.valueOf(right));
            }
          case LE:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) <= 0);
            } else {
              return Boolean.toString(Double.valueOf(left) <= Double.valueOf(right));
            }
          case GT:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) > 0);
            } else {
              return Boolean.toString(Double.valueOf(left) > Double.valueOf(right));
            }
          case GE:
            if (type == EdmSimpleTypeKind.String.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Guid.getEdmSimpleTypeInstance()
                    || type == EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance()) {
              return Boolean.toString(left.compareTo(right) >= 0);
            } else {
              return Boolean.toString(Double.valueOf(left) >= Double.valueOf(right));
            }
          case PROPERTY_ACCESS:
            throw new ODataNotImplementedException();
          default:
            throw new ODataNotImplementedException();
        }

      case PROPERTY:
        final EdmProperty property = (EdmProperty) ((PropertyExpression) expression).getEdmProperty();
        final EdmSimpleType propertyType = (EdmSimpleType) property.getType();
        return propertyType.valueToString(this.valueAccess.getPropertyValue(data, property),
                EdmLiteralKind.DEFAULT,
                property.getFacets());

      case MEMBER:
        final MemberExpression memberExpression = (MemberExpression) expression;
        final PropertyExpression propertyExpression = (PropertyExpression) memberExpression.getProperty();
        final EdmProperty memberProperty = (EdmProperty) propertyExpression.getEdmProperty();
        final EdmSimpleType memberType = (EdmSimpleType) memberExpression.getEdmType();
        List<EdmProperty> propertyPath = new ArrayList<EdmProperty>();
        CommonExpression currentExpression = memberExpression;
        while (currentExpression != null) {
          final PropertyExpression currentPropertyExpression =
                  (PropertyExpression) (currentExpression.getKind() == ExpressionKind.MEMBER
                          ? ((MemberExpression) currentExpression).getProperty()
                          : currentExpression);
          final EdmTyped currentProperty = currentPropertyExpression.getEdmProperty();
          final EdmTypeKind kind = currentProperty.getType().getKind();
          if (kind == EdmTypeKind.SIMPLE || kind == EdmTypeKind.COMPLEX) {
            propertyPath.add(0, (EdmProperty) currentProperty);
          } else {
            throw new ODataNotImplementedException();
          }
          currentExpression = currentExpression.getKind() == ExpressionKind.MEMBER
                  ? ((MemberExpression) currentExpression).getPath()
                  : null;
        }
        return memberType.valueToString(getPropertyValue(data, propertyPath), EdmLiteralKind.DEFAULT,
                memberProperty.getFacets());

      case LITERAL:
        final LiteralExpression literal = (LiteralExpression) expression;
        final EdmSimpleType literalType = (EdmSimpleType) literal.getEdmType();
        return literalType.valueToString(
                literalType.valueOfString(literal.getUriLiteral(), EdmLiteralKind.URI, null,
                        literalType.getDefaultType()),
                EdmLiteralKind.DEFAULT, null);

      case METHOD:
        final MethodExpression methodExpression = (MethodExpression) expression;
        final String first = evaluateExpression(data, methodExpression.getParameters().get(0));
        final String second = methodExpression.getParameterCount() > 1
                ? evaluateExpression(data, methodExpression.getParameters().get(1))
                : null;
        final String third = methodExpression.getParameterCount() > 2
                ? evaluateExpression(data, methodExpression.getParameters().get(2))
                : null;

        switch (methodExpression.getMethod()) {
          case ENDSWITH:
            return Boolean.toString(first.endsWith(second));
          case INDEXOF:
            return Integer.toString(first.indexOf(second));
          case STARTSWITH:
            return Boolean.toString(first.startsWith(second));
          case TOLOWER:
            return first.toLowerCase(Locale.ROOT);
          case TOUPPER:
            return first.toUpperCase(Locale.ROOT);
          case TRIM:
            return first.trim();
          case SUBSTRING:
            final int offset = Integer.parseInt(second);
            return first.substring(offset, offset + Integer.parseInt(third));
          case SUBSTRINGOF:
            return Boolean.toString(second.contains(first));
          case CONCAT:
            return first + second;
          case LENGTH:
            return Integer.toString(first.length());
          case YEAR:
            return String.valueOf(Integer.parseInt(first.substring(0, 4)));
          case MONTH:
            return String.valueOf(Integer.parseInt(first.substring(5, 7)));
          case DAY:
            return String.valueOf(Integer.parseInt(first.substring(8, 10)));
          case HOUR:
            return String.valueOf(Integer.parseInt(first.substring(11, 13)));
          case MINUTE:
            return String.valueOf(Integer.parseInt(first.substring(14, 16)));
          case SECOND:
            return String.valueOf(Integer.parseInt(first.substring(17, 19)));
          case ROUND:
            return Long.toString(Math.round(Double.valueOf(first)));
          case FLOOR:
            return Long.toString(Math.round(Math.floor(Double.valueOf(first))));
          case CEILING:
            return Long.toString(Math.round(Math.ceil(Double.valueOf(first))));
          default:
            throw new ODataNotImplementedException();
        }

      default:
        throw new ODataNotImplementedException();
    }
  }

  private <T> String getSkipToken(final EdmEntitySet entitySet, final T data) throws ODataException {

    String skipToken = "";
    for (final EdmProperty property : entitySet.getEntityType().getKeyProperties()) {
      final EdmSimpleType type = (EdmSimpleType) property.getType();
      skipToken = skipToken.concat(type.valueToString(this.valueAccess.getPropertyValue(data, property),
              EdmLiteralKind.DEFAULT, property.getFacets()));
    }
    return skipToken;
  }

  private <T> Object getPropertyValue(final T data, final List<EdmProperty> propertyPath) throws ODataException {

    Object dataObject = data;
    for (final EdmProperty property : propertyPath) {
      if (dataObject != null) {
        dataObject = this.valueAccess.getPropertyValue(dataObject, property);
      }
    }
    return dataObject;
  }

  private void handleMimeType(final Object data, final EdmMapping mapping, final Map<String, Object> valueMap)
          throws ODataException {

    final String mimeTypeName = mapping.getMediaResourceMimeTypeKey();
    if (mimeTypeName != null) {
      Object value = this.valueAccess.getMappingValue(data, mapping);
      valueMap.put(mimeTypeName, value);
    }
  }

  private <T> Map<String, Object> getSimpleTypeValueMap(final T data, final List<EdmProperty> propertyPath)
          throws ODataException {

    final EdmProperty property = propertyPath.get(propertyPath.size() - 1);
    Map<String, Object> valueWithMimeType = new HashMap<String, Object>();
    valueWithMimeType.put(property.getName(), getPropertyValue(data, propertyPath));

    handleMimeType(data, property.getMapping(), valueWithMimeType);
    return valueWithMimeType;
  }

  private <T> Map<String, Object> getStructuralTypeValueMap(final T data, final EdmStructuralType type)
          throws ODataException {

    ODataContext context = getContext();
    final int timingHandle =
            context.startRuntimeMeasurement(getClass().getSimpleName(), "getStructuralTypeValueMap");

    Map<String, Object> valueMap = new HashMap<String, Object>();

    EdmMapping mapping = type.getMapping();
    if (mapping != null) {
      handleMimeType(data, mapping, valueMap);
    }

    for (final String propertyName : type.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) type.getProperty(propertyName);
      final Object value = this.valueAccess.getPropertyValue(data, property);

      if (property.isSimple()) {
        if (property.getMapping() == null || property.getMapping().getMediaResourceMimeTypeKey() == null) {
          valueMap.put(propertyName, value);
        } else {
          // TODO: enable MIME type mapping outside the current subtree
          valueMap.put(propertyName, getSimpleTypeValueMap(data, Arrays.asList(property)));
        }
      } else {
        valueMap.put(propertyName, getStructuralTypeValueMap(value, (EdmStructuralType) property.getType()));
      }
    }

    context.stopRuntimeMeasurement(timingHandle);
    return valueMap;
  }

  private <T> Map<String, Object> getStructuralTypeTypeMap(final T data, final EdmStructuralType type)
          throws ODataException {

    ODataContext context = getContext();
    final int timingHandle =
            context.startRuntimeMeasurement(getClass().getSimpleName(), "getStructuralTypeTypeMap");

    Map<String, Object> typeMap = new HashMap<String, Object>();
    for (final String propertyName : type.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) type.getProperty(propertyName);
      if (property.isSimple()) {
        Object value = this.valueAccess.getPropertyType(data, property);
        if (value != null) {
          typeMap.put(propertyName, value);
        }
      } else {
        Object value = this.valueAccess.getPropertyValue(data, property);
        if (value == null) {
          Class<?> complexClass = this.valueAccess.getPropertyType(data, property);
          value = createInstance(complexClass);
        }
        typeMap.put(propertyName, getStructuralTypeTypeMap(value, (EdmStructuralType) property.getType()));
      }
    }

    context.stopRuntimeMeasurement(timingHandle);
    return typeMap;
  }

  private <T> void setStructuralTypeValuesFromMap(final T data, final EdmStructuralType type,
          final Map<String, Object> valueMap, final boolean merge) throws ODataException {

    if (data == null) {
      throw new ODataException("Unable to set structural type values to NULL data.");
    }
    ODataContext context = getContext();
    final int timingHandle =
            context.startRuntimeMeasurement(getClass().getSimpleName(), "setStructuralTypeValuesFromMap");

    for (final String propertyName : type.getPropertyNames()) {
      final EdmProperty property = (EdmProperty) type.getProperty(propertyName);
      if (type instanceof EdmEntityType && ((EdmEntityType) type).getKeyProperties().contains(property)) {
        Object v = this.valueAccess.getPropertyValue(data, property);
        if (v != null) {
          continue;
        }
      }

      if (!merge || valueMap != null && valueMap.containsKey(propertyName)) {
        final Object value = valueMap == null ? null : valueMap.get(propertyName);
        if (property.isSimple()) {
          this.valueAccess.setPropertyValue(data, property, value);
        } else {
          @SuppressWarnings("unchecked") final Map<String, Object> values = (Map<String, Object>) value;
          Object complexData = this.valueAccess.getPropertyValue(data, property);
          if (complexData == null) {
            Class<?> complexClass = this.valueAccess.getPropertyType(data, property);
            complexData = createInstance(complexClass);
            this.valueAccess.setPropertyValue(data, property, complexData);
          }
          setStructuralTypeValuesFromMap(complexData, (EdmStructuralType) property.getType(), values, merge);
        }
      }
    }
    context.stopRuntimeMeasurement(timingHandle);
  }

  private Object createInstance(final Class<?> complexClass) throws ODataException {

    try {
      return complexClass.newInstance();
    } catch (InstantiationException e) {
      logger.error("Error InstantiationException during createInstance appeared ", e);
      throw new ODataException("Unable to create instance for complex data class '" + complexClass + "'.", e);
    } catch (IllegalAccessException e) {
      logger.error("Error IllegalAccessException during createInstance appeared ", e);
      throw new ODataException("Unable to create instance for complex data class '" + complexClass + "'.", e);
    }
  }

  @Override
  public ODataResponse executeBatch(final BatchHandler handler, final String contentType, final InputStream content)
          throws ODataException {

    try {
      ODataResponse batchResponse;
      List<BatchResponsePart> batchResponseParts = new ArrayList<BatchResponsePart>();
      PathInfo pathInfo = getContext().getPathInfo();
      EntityProviderBatchProperties batchProperties =
              EntityProviderBatchProperties.init().pathInfo(pathInfo).build();
      List<BatchRequestPart> batchParts = EntityProvider.parseBatchRequest(contentType, content, batchProperties);
      for (BatchRequestPart batchPart : batchParts) {
        batchResponseParts.add(handler.handleBatchPart(batchPart));
      }
      batchResponse = EntityProvider.writeBatchResponse(batchResponseParts);
      return batchResponse;
    } catch (ODataException e) {
      logger.error("Error during executeBatch appeared ", e);
      throw new ODataException(e);
    }
  }

  @Override
  public BatchResponsePart executeChangeSet(final BatchHandler handler, final List<ODataRequest> requests)
          throws ODataException {

    try {
      List<ODataResponse> responses = new ArrayList<ODataResponse>();
      for (ODataRequest request : requests) {
        ODataResponse response = handler.handleRequest(request);
        if (response.getStatus().getStatusCode() >= HttpStatusCodes.BAD_REQUEST.getStatusCode()) {
          // Rollback
          List<ODataResponse> errorResponses = new ArrayList<ODataResponse>(1);
          errorResponses.add(response);
          return BatchResponsePart.responses(errorResponses).changeSet(false).build();
        }
        responses.add(response);
      }
      return BatchResponsePart.responses(responses).changeSet(true).build();
    } catch (ODataException e) {
      logger.error("Error during executeChangeSet appeared ", e);
      throw new ODataException(e);
    }
  }
}

@AllArgsConstructor
@Getter
@Setter final class ODataProcessorDAO {

  private EdmEntitySet entitySet;

  private Object sourceData;

  private EdmEntitySet targetEntitySet;

  private String navigation;
}
