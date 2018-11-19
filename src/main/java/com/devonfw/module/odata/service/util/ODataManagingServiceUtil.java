package com.devonfw.module.odata.service.util;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import com.devonfw.module.odata.logic.util.ODataAnnotationNavInfoUtil;
import com.devonfw.module.odata.logic.util.ODataAnnotationUtil;

public class ODataManagingServiceUtil {

  private static final String INSERT_QUERY_NAME = "generateInsertRowString";

  private static final String DELETE_QUERY_NAME = "generateDeleteRowString";

  protected static final String KEY = "Id";

  public static <T> Path<T> getPath(String fieldName, Root<?> root) {

    Path<T> result = null;
    try {
      if (fieldName != null)
        result = root.get(fieldName);
    } catch (Exception e) {
      //do nothing
    }
    return result;
  }

  public static String getToFieldName(ODataAnnotationNavInfoUtil navigationInfo) {

    String fieldName = null;

    if (navigationInfo != null && navigationInfo.getToField() != null) {
      fieldName = navigationInfo.getToField().getName();
    }
    return getFiledName(fieldName);
  }

  public static String getFromFieldName(ODataAnnotationNavInfoUtil navigationInfo) {

    String fieldName = null;

    if (navigationInfo != null && navigationInfo.getFromField() != null) {
      fieldName = navigationInfo.getFromField().getName();
    }

    return getFiledName(fieldName);
  }

  public static String getFiledName(String filedName) {

    return filedName != null ? Introspector.decapitalize(filedName) : null;
  }

  public static String getFieldName(ODataAnnotationNavInfoUtil navigationInfo) {

    String fieldName;

    if (navigationInfo == null) {
      return null;
    } else if (navigationInfo.isBiDirectional()) {
      fieldName = getToFieldName(navigationInfo);
    } else {
      fieldName = getFromFieldName(navigationInfo);
    }
    return fieldName;
  }

  public static ODataAnnotationNavInfoUtil getNavigation(Class sourceToClass, Class targetToClass,
    String navigationProperty) {

    return ODataAnnotationUtil
      .getCommonNavigationInfo(sourceToClass, targetToClass, navigationProperty);
  }

  public static <T> String getQueryFromPersister(String queryName, String targetField,
    Root<T> mainRoute) throws ODataException {

    Method f;
    String query;
    try {
      CollectionPersister per = ((PluralAttributePath<Object>) mainRoute.get(targetField)).getPersister();
      f = per.getClass().getDeclaredMethod(queryName);
      f.setAccessible(true);
      Object r = f.invoke(per);
      query = (String) r;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new ODataException("Can't create right query for deleting nested object " + targetField);
    }
    return query;
  }

  public static <T> String getInsertQueryFromPersister(String targetField, Root<T> mainRouter) throws ODataException {

    return getQueryFromPersister(INSERT_QUERY_NAME, targetField, mainRouter);
  }

  public static <T> String getDeleteQueryFromPersister(String targetField, Root<T> mainRouter) throws ODataException {

    return getQueryFromPersister(DELETE_QUERY_NAME, targetField, mainRouter);
  }

  private ODataManagingServiceUtil() {

  }
}
