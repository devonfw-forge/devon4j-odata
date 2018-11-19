package com.devonfw.module.odata.logic.util;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.annotation.processor.core.util.AnnotationHelper;
import org.apache.olingo.odata2.api.annotation.edm.EdmComplexType;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntityType;
import org.apache.olingo.odata2.api.annotation.edm.EdmFacets;
import org.apache.olingo.odata2.api.annotation.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.annotation.edm.*;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty.Multiplicity;
import org.apache.olingo.odata2.api.annotation.edm.EdmProperty;
import org.apache.olingo.odata2.api.annotation.edm.EdmType;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.FunctionImportParameter;
import org.apache.olingo.odata2.api.edm.provider.ReturnType;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import com.devonfw.module.odata.service.impl.ODataImportFunctionHolder;

/**
 *
 */

public class ODataAnnotationUtil {

  private static final String DEFAULT_CONTAINER_NAME = "DefaultContainer";

  public static Map<String, ODataImportFunctionHolder> extractFunctionImportClass(
    ApplicationContext applicationContext,
    List<Class<?>> classesAnnotatedFunctionImport) {

    Map<String, ODataImportFunctionHolder> functionImportHolders =
      new LinkedHashMap();

    for (Class<?> annotatedClass : classesAnnotatedFunctionImport) {
      List<Method> methods = getAnnotatedMethods(annotatedClass,
        EdmFunctionImport.class, false);
      Object functionImport = applicationContext.getBean(annotatedClass);
      for (Method method : methods) {
        ODataImportFunctionHolder
          funcHolder = ODataImportFunctionHolder.initFunctionHolder(functionImport, method);
        EdmFunctionImport efi =
          method.getAnnotation(EdmFunctionImport.class);
        String name = efi.name();
        functionImportHolders.put(name, funcHolder);
      }
    }
    return functionImportHolders;
  }

  public static List<Class<?>> extractAnnotatedClass(List<String> packagesToScan) {

    if (packagesToScan == null) {
      return new ArrayList<>();
    }
    return packagesToScan.stream().filter(packageName -> packageName != null)
      .flatMap(packageName -> getAnnotatedClassesFromPackage(packageName).stream())
      .collect(
        Collectors.toList());

  }

  public static List<Class<?>> getAnnotatedClassesFromPackage(String p) {

    List<Class<?>> listOfAnnotatedClasses = new ArrayList();

    for (Class<?> type : ODataResourcesClassUtil.loadClasses(p)) {
      if (ODataAnnotationUtil.isEdmAnnotated(type)) {
        listOfAnnotatedClasses.add(type);
      }
    }
    return listOfAnnotatedClasses;
  }

  public static List<String> getValueAnnotations(Class annotatedClazz,
    Class customAnnotationClass, String... paramsName) {

    List<String> packageValues = new ArrayList();

    Annotation annotation = AnnotationUtils.findAnnotation(annotatedClazz, customAnnotationClass);
    Map<String, Object> annotationMap = AnnotationUtils.getAnnotationAttributes(annotation, true);
    String[] packagesName = paramsName;

    if (paramsName == null || paramsName.length == 0) {
      packagesName = annotationMap.keySet().toArray(new String[annotationMap.keySet().size()]);
    }
    for (String param : packagesName) {
      String[] packagesByKey = (String[]) annotationMap.get(param);
      packageValues.addAll(Arrays.stream(packagesByKey).collect(Collectors.toList()));
    }
    return packageValues;
  }

  /**
   * Compare keys of both instances.
   *
   * @param firstInstance
   * @param secondInstance
   * @return
   */
  public boolean keyMatch(final Object firstInstance, final Object secondInstance) {

    if (firstInstance == null || secondInstance == null) {
      return false;
    } else if (firstInstance.getClass() != secondInstance.getClass()) {
      return false;
    }

    Map<String, Object> firstKeyFields = getValueForAnnotatedFields(firstInstance, EdmKey.class);
    Map<String, Object> secondKeyFields = getValueForAnnotatedFields(secondInstance, EdmKey.class);
    if (firstKeyFields.isEmpty() && secondKeyFields.isEmpty()) {
      throw new ODataRuntimeException(
        "Both object instances does not have EdmKey fields defined [" + "firstClass="
          + firstInstance.getClass().getName() + " secondClass=" + secondInstance.getClass().getName()
          + "].");
    }

    return keyValuesMatch(firstKeyFields, secondKeyFields);
  }

  /**
   * Compare keys of instance with key values in map.
   *
   * @param instance
   * @param keyName2Value
   * @return
   */
  public boolean keyMatch(final Object instance, final Map<String, Object> keyName2Value) {

    if (instance == null) {
      return false;
    }
    Map<String, Object> instanceKeyFields = getValueForAnnotatedFields(instance, EdmKey.class);
    return keyValuesMatch(instanceKeyFields, keyName2Value);
  }

  private boolean keyValuesMatch(final Map<String, Object> firstKeyValues,
    final Map<String, Object> secondKeyValues) {

    if (firstKeyValues.size() != secondKeyValues.size()) {
      return false;
    } else if (firstKeyValues.isEmpty()) {
      throw new ODataRuntimeException("No keys given for key value matching.");
    } else {
      Set<Map.Entry<String, Object>> entries = firstKeyValues.entrySet();
      for (Map.Entry<String, Object> entry : entries) {
        Object firstKey = entry.getValue();
        Object secondKey = secondKeyValues.get(entry.getKey());
        if (!isEqual(firstKey, secondKey)) {
          return false;
        }
      }
      return true;
    }
  }

  private boolean isEqual(final Object firstKey, final Object secondKey) {

    if (firstKey == null) {
      return secondKey == null || secondKey.equals(firstKey);
    } else {
      return firstKey.equals(secondKey);
    }
  }

  public static String extractEntityTypeName(final EdmNavigationProperty enp, final Class<?> fallbackClass) {

    Class<?> entityTypeClass = enp.toType();
    return extractEntityTypeName(entityTypeClass == Object.class ? fallbackClass : entityTypeClass);
  }

  public static String extractEntityTypeName(final EdmNavigationProperty enp, final Field field) {

    Class<?> entityTypeClass = enp.toType();
    if (entityTypeClass == Object.class) {
      Class<?> toClass = field.getType();
      return extractEntityTypeName((toClass.isArray() || Collection.class.isAssignableFrom(toClass)
        ? (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] : toClass));
    } else {
      return extractEntityTypeName(entityTypeClass);
    }
  }

  /**
   * Returns <code>NULL</code> if given class is not annotated. If annotated the set entity type name is returned and if
   * no name is set the default name is generated from the simple class name.
   *
   * @param annotatedClass
   * @return
   */
  public static String extractEntityTypeName(final Class<?> annotatedClass) {

    return extractTypeName(annotatedClass, EdmEntityType.class);
  }

  /**
   * Returns <code>NULL</code> if given class is not annotated. If annotated the entity set name is returned and if no
   * name is set a default name is generated based on the simple class name.
   *
   * @param annotatedClass
   * @return
   */
  public static String extractEntitySetName(final Class<?> annotatedClass) {

    if (annotatedClass == Object.class) {
      return null;
    }
    EdmEntitySet entitySet = annotatedClass.getAnnotation(EdmEntitySet.class);
    if (entitySet == null) {
      return null;
    }

    String name = entitySet.name();
    if (name.isEmpty()) {
      return getCanonicalName(annotatedClass) + "Set";
    }
    return name;
  }

  public static FullQualifiedName extractEntityTypeFqn(final EdmEntityType type, final Class<?> annotatedClass) {

    if (type.namespace().isEmpty()) {
      return new FullQualifiedName(generateNamespace(annotatedClass), extractEntityTypeName(annotatedClass));
    }
    return new FullQualifiedName(type.namespace(), extractEntityTypeName(annotatedClass));
  }

  public static FullQualifiedName extractEntityTypeFqn(final Class<?> annotatedClass) {

    EdmEntityType type = annotatedClass.getAnnotation(EdmEntityType.class);
    if (type == null) {
      return null;
    }
    return extractEntityTypeFqn(type, annotatedClass);
  }

  public static FullQualifiedName extractComplexTypeFqn(final Class<?> annotatedClass) {

    EdmComplexType type = annotatedClass.getAnnotation(EdmComplexType.class);
    if (type == null) {
      return null;
    }
    return extractComplexTypeFqn(type, annotatedClass);
  }

  public static FullQualifiedName extractComplexTypeFqn(final EdmComplexType type, final Class<?> annotatedClass) {

    if (type.namespace().isEmpty()) {
      return new FullQualifiedName(generateNamespace(annotatedClass), extractComplexTypeName(annotatedClass));
    }
    return new FullQualifiedName(type.namespace(), extractComplexTypeName(annotatedClass));
  }

  public static String extractComplexTypeName(final Class<?> annotatedClass) {

    return extractTypeName(annotatedClass, EdmComplexType.class);
  }

  public static String generateNamespace(final Class<?> annotatedClass) {

    return annotatedClass.getPackage().getName();
  }

  /**
   * @param <T>            must be EdmEntityType or EdmComplexType annotation
   * @param annotatedClass
   * @param typeAnnotation
   * @return null if annotatedClass is not annotated or name set via annotation or generated via
   * {@link #getCanonicalName(Class)}
   */
  private static <T extends Annotation> String extractTypeName(final Class<?> annotatedClass,
    final Class<T> typeAnnotation) {

    if (annotatedClass == Object.class) {
      return null;
    }
    T type = annotatedClass.getAnnotation(typeAnnotation);
    if (type == null) {
      return null;
    }

    String name;
    if (typeAnnotation == EdmEntityType.class) {
      name = ((EdmEntityType) type).name();
    } else if (typeAnnotation == EdmComplexType.class) {
      name = ((EdmComplexType) type).name();
    } else {
      return null;
    }

    if (name.isEmpty()) {
      return getCanonicalName(annotatedClass);
    }
    return name;
  }

  /**
   * Get the set property name from an EdmProperty or EdmNavigationProperty annotation. If no property annotations is
   * set an empty string (<code>""</code>).
   *
   * @param field which is checked
   * @return the name of the property or if no property annotations is set an empty string (<code>""</code>).
   */
  public static String getPropertyNameFromAnnotation(final Field field) {

    EdmProperty property = field.getAnnotation(EdmProperty.class);
    if (property != null) {
      return property.name();
    }
    EdmNavigationProperty navProperty = field.getAnnotation(EdmNavigationProperty.class);
    if (navProperty != null) {
      return navProperty.name();
    }
    return "";
  }

  public static String getPropertyName(final Field field) {

    String propertyName = getPropertyNameFromAnnotation(field);
    if (propertyName.isEmpty()) {
      propertyName = getCanonicalName(field);
    }
    return propertyName;
  }

  public static String extractToRoleName(final EdmNavigationProperty enp, final Field field) {

    String role = enp.toRole();
    if (role.isEmpty()) {
      role = getCanonicalRoleName(field.getName());
    }
    return role;
  }

  public static String extractFromRoleEntityName(final Field field) {

    return extractEntityTypeName(field.getDeclaringClass());
  }

  public static String extractToRoleEntityName(final EdmNavigationProperty enp, final Field field) {

    Class<?> clazz = enp.toType();
    if (clazz == Object.class) {
      if (field.getType().isArray() || Collection.class.isAssignableFrom(field.getType())) {
        clazz = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
      } else {
        clazz = field.getType();
      }
    }
    return extractEntityTypeName(clazz);
  }

  public static String getCanonicalRoleName(String name) {

    return "r_" + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
  }

  public static String extractRelationshipName(final EdmNavigationProperty enp, final Field field) {

    String relationshipName = enp.association();
    if (relationshipName.isEmpty()) {
      final String fromRole = extractFromRoleEntityName(field);
      final String toRole = extractToRoleEntityName(enp, field);
      return createCanonicalRelationshipName(fromRole, toRole);
    }
    return relationshipName;
  }

  public static String createCanonicalRelationshipName(String fromRole, String toRole) {

    if (fromRole.compareTo(toRole) > 0) {
      return toRole + "_2_" + fromRole;
    } else {
      return fromRole + "_2_" + toRole;
    }
  }

  public static EdmMultiplicity extractMultiplicity(final EdmNavigationProperty enp, final Field field) {

    EdmMultiplicity multiplicity = mapMultiplicity(enp.toMultiplicity());
    final boolean isCollectionType =
      field.getType().isArray() || Collection.class.isAssignableFrom(field.getType());

    if (multiplicity == EdmMultiplicity.ONE && isCollectionType) {
      return EdmMultiplicity.MANY;
    }
    return multiplicity;
  }

  /**
   * Set key fields based on values in map. If an key field is not available or <code>NULL</code> in the map it will be
   * not set as <code>NULL</code> at the instance object.
   *
   * @param instance
   * @param keys
   * @return
   */
  public <T> T setKeyFields(final T instance, final Map<String, Object> keys) {

    List<Field> fields = getAnnotatedFields(instance, EdmKey.class);

    for (Field field : fields) {
      String propertyName = getPropertyName(field);
      Object keyValue = keys.get(propertyName);
      setValueForProperty(instance, propertyName, keyValue);
    }

    return instance;
  }

  public static ODataAnnotationNavInfoUtil getCommonNavigationInfo(final Class<?> sourceClass,
    final Class<?> targetClass, String navigationProperty) {

    List<Field> sourceFields = getAnnotatedFields(sourceClass, EdmNavigationProperty.class);
    List<Field> targetFields = getAnnotatedFields(targetClass, EdmNavigationProperty.class);
    String navigation = navigationProperty != null ? Introspector.decapitalize(navigationProperty) : null;

    if (sourceClass == targetClass) {
      //its not true, it must be change becouse it could be selfrelation
      // special case, actual handled as bi-directional
      return getCommonNavigationInfoBiDirectional(sourceClass, targetClass, navigation);
    }

    // first try via association name to get full navigation information
    for (Field sourceField : sourceFields) {
      if (ODataResourcesClassUtil.getFieldType(sourceField) == targetClass) {
        final EdmNavigationProperty sourceNav = sourceField.getAnnotation(EdmNavigationProperty.class);
        final String sourceAssociation = extractRelationshipName(sourceNav, sourceField);
        for (Field targetField : targetFields) {
          if (ODataResourcesClassUtil.getFieldType(targetField) == sourceClass && Objects
            .equals(targetField.getName(), navigation)) {
            final EdmNavigationProperty targetNav = targetField.getAnnotation(EdmNavigationProperty.class);
            final String targetAssociation = extractRelationshipName(targetNav, targetField);
            if (sourceAssociation.equals(targetAssociation)) {
              return new ODataAnnotationNavInfoUtil(sourceField, targetField, sourceNav, targetNav);
            }
          }
        }
      }
    }

    // if nothing was found assume/guess none bi-directional navigation
    return getCommonNavigationInfoBiDirectional(sourceClass, targetClass, navigation);
  }

  private static ODataAnnotationNavInfoUtil getCommonNavigationInfoBiDirectional(final Class<?> sourceClass,
    final Class<?> targetClass, String navigationProperty) {

    List<Field> sourceFields = getAnnotatedFields(sourceClass, EdmNavigationProperty.class);

    String targetEntityTypeName = extractEntityTypeName(targetClass);
    for (Field sourceField : sourceFields) {
      final EdmNavigationProperty sourceNav = sourceField.getAnnotation(EdmNavigationProperty.class);
      final String navTargetEntityName = extractEntityTypeName(sourceNav, sourceField);

      if (navTargetEntityName.equals(targetEntityTypeName) && Objects
        .equals(sourceField.getName(), navigationProperty)) {
        return new ODataAnnotationNavInfoUtil(null, sourceField, null, sourceNav);
      }
    }

    return null;
  }

  public Class<?> getFieldTypeForProperty(final Class<?> clazz, final String propertyName)
    throws AnnotationHelper.ODataAnnotationException {

    if (clazz == null) {
      return null;
    }

    Field field = getFieldForPropertyName(propertyName, clazz, true);
    if (field == null) {
      throw new AnnotationHelper.ODataAnnotationException(
        "No field for property '" + propertyName + "' found at class '" + clazz + "'.");
    }
    return field.getType();
  }

  public Class<?> getFieldTypeForProperty(final Object instance, final String propertyName)
    throws AnnotationHelper.ODataAnnotationException {

    if (instance == null) {
      return null;
    }

    return getFieldTypeForProperty(instance.getClass(), propertyName);
  }

  public Object getValueForProperty(final Object instance, final String propertyName)
    throws AnnotationHelper.ODataAnnotationException {

    if (instance == null) {
      return null;
    }

    Field field = getFieldForPropertyName(propertyName, instance.getClass(), true);
    if (field == null) {
      throw new AnnotationHelper.ODataAnnotationException(
        "No field for property '" + propertyName + "' found at class '" + instance.getClass() + "'.");
    }
    return getFieldValue(instance, field);
  }

  public void setValueForProperty(final Object instance, final String propertyName, final Object propertyValue) {

    if (instance != null) {
      Field field = getFieldForPropertyName(propertyName, instance.getClass(), true);
      if (field != null) {
        setFieldValue(instance, field, propertyValue);
      }
    }
  }

  private Field getFieldForPropertyName(final String propertyName, final Class<?> resultClass,
    final boolean inherited) {

    Field[] fields = resultClass.getDeclaredFields();
    for (Field field : fields) {
      EdmProperty property = field.getAnnotation(EdmProperty.class);
      if (property == null) {
        if (getCanonicalName(field).equals(propertyName)) {
          return field;
        }
      } else {
        if (property.name().isEmpty() && getCanonicalName(field).equals(propertyName)) {
          return field;
        } else if (property.name().equals(propertyName)) {
          return field;
        }
      }
    }

    Class<?> superClass = resultClass.getSuperclass();
    if (inherited && superClass != Object.class) {
      return getFieldForPropertyName(propertyName, superClass, true);
    }

    return null;
  }

  public Object getValueForField(final Object instance, final String fieldName,
    final Class<? extends Annotation> annotation) {

    if (instance == null) {
      return null;
    }
    return getValueForField(instance, fieldName, instance.getClass(), annotation, true);
  }

  public Object getValueForField(final Object instance, final Class<? extends Annotation> annotation) {

    if (instance == null) {
      return null;
    }
    return getValueForField(instance, instance.getClass(), annotation, true);
  }

  private Object getValueForField(final Object instance, final Class<?> resultClass,
    final Class<? extends Annotation> annotation, final boolean inherited) {

    return getValueForField(instance, null, resultClass, annotation, inherited);
  }

  public Map<String, Object> getValueForAnnotatedFields(final Object instance,
    final Class<? extends Annotation> annotation) {

    if (instance == null) {
      return null;
    }
    return getValueForAnnotatedFields(instance, instance.getClass(), annotation, true);
  }

  private Map<String, Object> getValueForAnnotatedFields(final Object instance, final Class<?> resultClass,
    final Class<? extends Annotation> annotation, final boolean inherited) {

    Field[] fields = resultClass.getDeclaredFields();
    Map<String, Object> fieldName2Value = new HashMap<>();

    for (Field field : fields) {
      if (field.getAnnotation(annotation) != null) {
        Object value = getFieldValue(instance, field);
        final String name = extractPropertyName(field);
        fieldName2Value.put(name, value);
      }
    }

    Class<?> superClass = resultClass.getSuperclass();
    if (inherited && superClass != Object.class) {
      Map<String, Object> tmp = getValueForAnnotatedFields(instance, superClass, annotation, true);
      fieldName2Value.putAll(tmp);
    }

    return fieldName2Value;
  }

  private String extractPropertyName(final Field field) {

    final EdmProperty property = field.getAnnotation(EdmProperty.class);
    if (property == null || property.name().isEmpty()) {
      return getCanonicalName(field);
    } else {
      return property.name();
    }
  }

  /**
   * Returns <code>NULL</code> if given method is not annotated. If annotated the function import name is returned and
   * if no name is set a default name is generated based on the method name.
   *
   * @param annotatedMethod
   * @return
   */
  public static String extractFunctionImportName(final Method annotatedMethod) {

    EdmFunctionImport functionImport = annotatedMethod.getAnnotation(EdmFunctionImport.class);
    if (functionImport == null) {
      return null;
    }

    String name = functionImport.name();
    if (name == null || name.isEmpty()) {
      name = annotatedMethod.getName();
    }
    return name;
  }

  public static ArrayList<FunctionImportParameter> extractFunctionImportParameters(final Method method) {

    ArrayList<FunctionImportParameter> functionImportParameters = new ArrayList<>();
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      Annotation[] annotations = parameterAnnotations[i];
      for (Annotation annotation : annotations) {
        if (annotation.annotationType().equals(EdmFunctionImportParameter.class)) {
          FunctionImportParameter fip = new FunctionImportParameter();
          EdmFunctionImportParameter fipAnnotation = (EdmFunctionImportParameter) annotation;
          // Set facets
          if (fipAnnotation.facets() != null) {
            EdmFacets edmFacets = fipAnnotation.facets();
            Facets facets = new Facets();
            // Data which is not available though it should be?
            // facets.setCollation(collation);
            // facets.setConcurrencyMode(concurrencyMode);
            // facets.setDefaultValue(defaultValue);
            // facets.setFixedLength(fixedLength);
            facets.setMaxLength(edmFacets.maxLength());
            facets.setNullable(edmFacets.nullable());
            facets.setPrecision(edmFacets.precision());
            facets.setScale(edmFacets.scale());
            // Data which is not available though it should be?
            // facets.setUnicode(unicode);
            fip.setFacets(facets);
          }
          // Set name
          fip.setName(fipAnnotation.name());
          // Set type
          EdmType edmType;
          EdmSimpleTypeKind edmSimpleTypeKind;
          if (fipAnnotation.type() != null && !fipAnnotation.type().equals(EdmType.NULL)) {
            edmSimpleTypeKind = mapTypeKind(fipAnnotation.type());
          } else {
            edmType = mapType(parameterType);
            edmSimpleTypeKind = mapTypeKind(edmType);
          }
          fip.setType(edmSimpleTypeKind);
          // Add function import parameter
          functionImportParameters.add(fip);
        }
      }

    }

    return functionImportParameters;
  }

  public static ReturnType extractReturnType(final Method annotatedMethod) {

    EdmFunctionImport functionImport = annotatedMethod.getAnnotation(EdmFunctionImport.class);
    ReturnType returnType = new ReturnType();
    if (functionImport.returnType().isCollection()) {
      returnType.setMultiplicity(EdmMultiplicity.MANY);
    } else {
      returnType.setMultiplicity(EdmMultiplicity.ZERO_TO_ONE);
    }

    switch (functionImport.returnType().type()) {
      case SIMPLE:
        EdmType edmType = mapType(annotatedMethod.getReturnType());
        EdmSimpleTypeKind edmSimpleTypeKind = mapTypeKind(edmType);
        returnType.setTypeName(edmSimpleTypeKind.getFullQualifiedName());
        break;
      case ENTITY:
        returnType.setTypeName(extractEntityTypeFqn(determineAnnotatedClass(functionImport, annotatedMethod)));
        break;
      case COMPLEX:
        returnType.setTypeName(extractComplexTypeFqn(determineAnnotatedClass(functionImport, annotatedMethod)));
        break;
      default:
        throw new UnsupportedOperationException(
          "Not yet supported return type type '" + functionImport.returnType().type() + "'.");
    }
    return returnType;
  }

  private static Class<?> determineAnnotatedClass(final EdmFunctionImport functionImport,
    final Method annotatedMethod) {

    if (functionImport.returnType().isCollection()) {
      ParameterizedType parameterizedType = (ParameterizedType) annotatedMethod.getGenericReturnType();
      return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    } else {
      return annotatedMethod.getReturnType();
    }
  }

  public static String extractEntitySetName(final Method annotatedMethod) {

    EdmFunctionImport functionImport = annotatedMethod.getAnnotation(EdmFunctionImport.class);
    if (functionImport.entitySet() != null && !functionImport.entitySet().isEmpty()) {
      return functionImport.entitySet();
    }
    return null;
  }

  public static String extractHttpMethod(final Method annotatedMethod) {

    EdmFunctionImport functionImport = annotatedMethod.getAnnotation(EdmFunctionImport.class);
    if (functionImport.httpMethod() != null) {
      return functionImport.httpMethod().name();
    }
    return null;
  }

  public static void setValueForAnnotatedField(final Object instance, final Class<? extends Annotation> annotation,
    final Object value) throws AnnotationHelper.ODataAnnotationException {

    List<Field> fields = getAnnotatedFields(instance, annotation);

    if (fields.isEmpty()) {
      throw new AnnotationHelper.ODataAnnotationException(
        "No field found for annotation '" + annotation + "' on instance '" + instance + "'.");
    } else if (fields.size() > 1) {
      throw new AnnotationHelper.ODataAnnotationException(
        "More then one field found for annotation '" + annotation + "' on instance '" + instance + "'.");
    }

    setFieldValue(instance, fields.get(0), value);
  }

  public void setValuesToAnnotatedFields(final Object instance, final Class<? extends Annotation> annotation,
    final Map<String, Object> fieldName2Value) {

    List<Field> fields = getAnnotatedFields(instance, annotation);

    // XXX: refactore
    for (Field field : fields) {
      final String canonicalName = getCanonicalName(field);
      if (fieldName2Value.containsKey(canonicalName)) {
        Object value = fieldName2Value.get(canonicalName);
        setFieldValue(instance, field, value);
      }
    }
  }

  public static List<Field> getAnnotatedFields(final Object instance, final Class<? extends Annotation> annotation) {

    if (instance == null) {
      return null;
    }
    return getAnnotatedFields(instance.getClass(), annotation, true);
  }

  public static List<Field> getAnnotatedFields(final Class<?> fieldClass,
    final Class<? extends Annotation> annotation) {

    return getAnnotatedFields(fieldClass, annotation, true);
  }

  /**
   * @param resultClass
   * @param annotation
   * @param inherited
   * @return
   */
  private static List<Field> getAnnotatedFields(final Class<?> resultClass,
    final Class<? extends Annotation> annotation,
    final boolean inherited) {

    if (resultClass == null) {
      return null;
    }

    Field[] fields = resultClass.getDeclaredFields();
    List<Field> annotatedFields = new ArrayList<>();

    for (Field field : fields) {
      if (field.getAnnotation(annotation) != null) {
        annotatedFields.add(field);
      }
    }

    Class<?> superClass = resultClass.getSuperclass();
    if (inherited && superClass != Object.class) {
      List<Field> tmp = getAnnotatedFields(superClass, annotation, true);
      annotatedFields.addAll(tmp);
    }

    return annotatedFields;
  }

  private Object getValueForField(final Object instance, final String fieldName, final Class<?> resultClass,
    final Class<? extends Annotation> annotation, final boolean inherited) {

    if (instance == null) {
      return null;
    }

    Field[] fields = resultClass.getDeclaredFields();
    for (Field field : fields) {
      if (field.getAnnotation(annotation) != null && (fieldName == null || field.getName().equals(fieldName))) {
        return getFieldValue(instance, field);
      }
    }

    Class<?> superClass = resultClass.getSuperclass();
    if (inherited && superClass != Object.class) {
      return getValueForField(instance, fieldName, superClass, annotation, true);
    }

    return null;
  }

  private Object getFieldValue(final Object instance, final Field field) {

    try {
      boolean access = field.isAccessible();
      field.setAccessible(true);
      Object value = field.get(instance);
      field.setAccessible(access);
      return value;
    } catch (IllegalArgumentException | IllegalAccessException ex) { // should never happen
      throw new ODataRuntimeException(ex);
    }
  }

  private static void setFieldValue(final Object instance, final Field field, final Object value) {

    try {
      Object usedValue = value;
      if (value != null && field.getType() != value.getClass() && value.getClass() == String.class) {
        usedValue = convert(field, (String) value);
      }
      boolean access = field.isAccessible();
      field.setAccessible(true);
      field.set(instance, usedValue);
      field.setAccessible(access);
    } catch (IllegalArgumentException | IllegalAccessException ex) { // should never happen
      throw new ODataRuntimeException(ex);
    }
  }

  public static List<Method> getAnnotatedMethods(final Class<?> resultClass,
    final Class<? extends Annotation> annotation,
    final boolean inherited) {

    if (resultClass == null) {
      return null;
    }

    Method[] methods = resultClass.getDeclaredMethods();
    List<Method> annotatedMethods = new ArrayList<>();

    for (Method method : methods) {
      if (method.getAnnotation(annotation) != null) {
        annotatedMethods.add(method);
      }
    }

    Class<?> superClass = resultClass.getSuperclass();
    if (inherited && superClass != Object.class) {
      List<Method> tmp = getAnnotatedMethods(superClass, annotation, true);
      annotatedMethods.addAll(tmp);
    }

    return annotatedMethods;
  }

  private static Object convert(final Field field, final String propertyValue) {

    Class<?> fieldClass = field.getType();
    try {
      EdmProperty property = field.getAnnotation(EdmProperty.class);
      EdmSimpleTypeKind type = mapTypeKind(property.type());
      return type.getEdmSimpleTypeInstance()
        .valueOfString(propertyValue, EdmLiteralKind.DEFAULT, null, fieldClass);
    } catch (EdmSimpleTypeException ex) {
      throw new ODataRuntimeException(
        "Conversion failed for string property [" + propertyValue + "] on field ["
          + field + "] with error: " + ex.getMessage(), ex);
    }
  }

  public boolean isEdmAnnotated(final Object object) {

    if (object == null) {
      return false;
    }
    return isEdmAnnotated(object.getClass());
  }

  public static boolean isEdmTypeAnnotated(final Class<?> clazz) {

    boolean isComplexEntity = clazz.getAnnotation(EdmComplexType.class) != null;
    boolean isEntity = clazz.getAnnotation(EdmEntityType.class) != null;
    return isComplexEntity || isEntity;
  }

  public static boolean isEdmAnnotated(final Class<?> clazz) {

    if (clazz == null) {
      return false;
    } else {
      // TODO: mibo: do next checks only if first was not true
      final boolean isEntity = null != clazz.getAnnotation(EdmEntityType.class);
      final boolean isEntitySet = null != clazz.getAnnotation(EdmEntitySet.class);
      final boolean isComplexEntity = null != clazz.getAnnotation(EdmComplexType.class);
      final boolean hasFunctionImport = hasEdmFunction(clazz);

      return isEntity || isEntitySet || isComplexEntity || hasFunctionImport;
    }
  }

  public static boolean hasEdmFunction(final Class<?> clazz) {

    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      if (method.getAnnotation(EdmFunctionImport.class) != null) {
        return true;
      }
    }
    return false;
  }

  public static String getCanonicalName(final Field field) {

    return firstCharToUpperCase(field.getName());
  }

  public static String getCanonicalName(final Class<?> clazz) {

    return firstCharToUpperCase(clazz.getSimpleName());
  }

  private static String firstCharToUpperCase(final String content) {

    if (content == null || content.isEmpty()) {
      return content;
    }
    return content.substring(0, 1).toUpperCase(Locale.ENGLISH) + content.substring(1);
  }

  public static EdmSimpleTypeKind mapTypeKind(final EdmType type) {

    switch (type) {
      case BINARY:
        return EdmSimpleTypeKind.Binary;
      case BOOLEAN:
        return EdmSimpleTypeKind.Boolean;
      case BYTE:
        return EdmSimpleTypeKind.Byte;
      case COMPLEX:
        return EdmSimpleTypeKind.Null;
      case DATE_TIME:
        return EdmSimpleTypeKind.DateTime;
      case DATE_TIME_OFFSET:
        return EdmSimpleTypeKind.DateTimeOffset;
      case DECIMAL:
        return EdmSimpleTypeKind.Decimal;
      case DOUBLE:
        return EdmSimpleTypeKind.Double;
      case GUID:
        return EdmSimpleTypeKind.Guid;
      case INT16:
        return EdmSimpleTypeKind.Int16;
      case INT32:
        return EdmSimpleTypeKind.Int32;
      case INT64:
        return EdmSimpleTypeKind.Int64;
      case NULL:
        return EdmSimpleTypeKind.Null;
      case SBYTE:
        return EdmSimpleTypeKind.SByte;
      case SINGLE:
        return EdmSimpleTypeKind.Single;
      case STRING:
        return EdmSimpleTypeKind.String;
      case TIME:
        return EdmSimpleTypeKind.Time;
      default:
        throw new ODataRuntimeException("Unknown type '" + type + "' for mapping to EdmSimpleTypeKind.");
    }
  }

  public static EdmType mapType(final Class<?> type) {

    if (type == String.class || type == URI.class || type == URL.class || type.isEnum() || type == TimeZone.class
      || type == Locale.class) {
      return EdmType.STRING;
    } else if (type == boolean.class || type == Boolean.class) {
      return EdmType.BOOLEAN;
    } else if (type == byte.class || type == Byte.class) {
      return EdmType.SBYTE;
    } else if (type == short.class || type == Short.class) {
      return EdmType.INT16;
    } else if (type == int.class || type == Integer.class) {
      return EdmType.INT32;
    } else if (type == long.class || type == Long.class) {
      return EdmType.INT64;
    } else if (type == double.class || type == Double.class) {
      return EdmType.DOUBLE;
    } else if (type == float.class || type == Float.class) {
      return EdmType.SINGLE;
    } else if (type == BigInteger.class || type == BigDecimal.class) {
      return EdmType.DECIMAL;
    } else if (type == Byte[].class || type == byte[].class) {
      return EdmType.BINARY;
    } else if (type == Date.class || type == java.sql.Date.class || type == Timestamp.class) {
      return EdmType.DATE_TIME;
    } else if (type == Calendar.class) {
      return EdmType.DATE_TIME_OFFSET;
    } else if (type == UUID.class) {
      return EdmType.GUID;
    } else if (type == List.class) {
      return EdmType.COMPLEX;
    } else if (type == GregorianCalendar.class) {
      return EdmType.DATE_TIME;
    } else {
      throw new UnsupportedOperationException("Not yet supported type '" + type + "'.");
    }
  }

  public static EdmMultiplicity mapMultiplicity(final Multiplicity multiplicity) {

    switch (multiplicity) {
      case ZERO_OR_ONE:
        return EdmMultiplicity.ZERO_TO_ONE;
      case ONE:
        return EdmMultiplicity.ONE;
      case MANY:
        return EdmMultiplicity.MANY;
      default:
        throw new ODataRuntimeException(
          "Unknown type '" + multiplicity + "' for mapping to EdmMultiplicity.");
    }
  }

  /**
   *
   */
  private static class EdmAnnotationException extends RuntimeException {

    private static final long serialVersionUID = 42L;

    public EdmAnnotationException(final String message) {

      super(message);
    }
  }

  public static String getCanonicalNamespace(final Class<?> aClass) {

    return generateNamespace(aClass);
  }

  public static String extractContainerName(final Class<?> aClass) {

    EdmEntitySet entitySet = aClass.getAnnotation(EdmEntitySet.class);
    if (entitySet != null) {
      String containerName = entitySet.container();
      if (!containerName.isEmpty()) {
        return containerName;
      }
    }
    return DEFAULT_CONTAINER_NAME;
  }
}
