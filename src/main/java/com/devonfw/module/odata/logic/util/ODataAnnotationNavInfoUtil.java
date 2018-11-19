package com.devonfw.module.odata.logic.util;


import java.lang.reflect.Field;

import org.apache.olingo.odata2.api.annotation.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;

public class ODataAnnotationNavInfoUtil {

    private final Field fromField;

    private final Field toField;

    private final EdmNavigationProperty fromNavigation;

    private final EdmNavigationProperty toNavigation;

    public ODataAnnotationNavInfoUtil(final Field fromField, final Field toField, final EdmNavigationProperty fromNavigation,
            final EdmNavigationProperty toNavigation) {

        this.fromField = fromField;
        this.toField = toField;
        this.fromNavigation = fromNavigation;
        this.toNavigation = toNavigation;
    }

    public Field getFromField() {

        return this.fromField;
    }

    public String getFromRoleName() {

        if (isBiDirectional()) {
            return ODataAnnotationUtil.extractFromRoleEntityName(this.toField);
        }
        return ODataAnnotationUtil.extractToRoleName(this.toNavigation, this.toField);
    }

    public Field getToField() {

        return this.toField;
    }

    public String getToRoleName() {

        if (isBiDirectional()) {
            return ODataAnnotationUtil.extractToRoleName(this.toNavigation, this.toField);
        }
        return ODataAnnotationUtil.extractToRoleName(this.fromNavigation, this.fromField);
    }

    public EdmMultiplicity getFromMultiplicity() {

        if (isBiDirectional()) {
            return EdmMultiplicity.ONE;
        }
        return ODataAnnotationUtil.extractMultiplicity(this.toNavigation, this.toField);
    }

    public EdmMultiplicity getToMultiplicity() {

        if (isBiDirectional()) {
            return ODataAnnotationUtil.extractMultiplicity(this.toNavigation, this.toField);
        }
        return ODataAnnotationUtil.extractMultiplicity(this.fromNavigation, this.fromField);
    }

    public boolean isBiDirectional() {

        return this.fromNavigation == null;
    }

    public String getRelationshipName() {

        String toAssociation = this.toNavigation.association();
        String fromAssociation = "";
        if (!isBiDirectional()) {
            fromAssociation = this.fromNavigation.association();
        }

        if (fromAssociation.isEmpty() && fromAssociation.equals(toAssociation)) {
            return ODataAnnotationUtil.createCanonicalRelationshipName(getFromRoleName(), getToRoleName());
        } else if (toAssociation.isEmpty()) {
            return fromAssociation;
        } else if (!toAssociation.equals(fromAssociation)) {
            throw new ODataRuntimeException(
                    "Invalid associations for navigation properties '" + toString() + "'");
        }
        return toAssociation;
    }

    public String getFromTypeName() {

        if (isBiDirectional()) {
            return ODataAnnotationUtil.extractEntityTypeName(this.toField.getDeclaringClass());
        }
        return ODataAnnotationUtil.extractEntityTypeName(this.fromField.getDeclaringClass());
    }

    public String getToTypeName() {

        if (isBiDirectional()) {
            return ODataAnnotationUtil.extractEntityTypeName(ODataResourcesClassUtil.getFieldType(this.toField));
        }
        return ODataAnnotationUtil.extractEntityTypeName(this.toField.getDeclaringClass());
    }

    @Override
    public String toString() {

        if (isBiDirectional()) {
            return "AnnotatedNavInfo{biDirectional = true" + ", toField=" + this.toField.getName()
                    + ", toNavigation="
                    + this.toNavigation.name() + '}';
        }
        return "AnnotatedNavInfo{" + "fromField=" + this.fromField.getName() + ", toField=" + this.toField.getName()
                + ", fromNavigation=" + this.fromNavigation.name() + ", toNavigation=" + this.toNavigation.name()
                + '}';
    }
}
