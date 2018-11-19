package com.devonfw.module.odata.logic.util;

import java.util.Map;

public class DaoLogicUtil {

    protected static final String KEY = "Id";

    public static Long extractIdFromPropertyMap(Map<String, Object> propertyMap) {

        if (propertyMap.isEmpty() || !propertyMap.containsKey(KEY)) {
            return null;
        }
        return propertyMap.get(KEY) != null ? (Long) propertyMap.get(KEY) : null;
    }
}
