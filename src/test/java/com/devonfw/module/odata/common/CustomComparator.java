package com.devonfw.module.odata.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public final class CustomComparator {

    public static Map<String, Object> compare(Map<String, Object> entitySetPrevious,
            Map<String, Object> entitySetAfterOperation,
            List<String> fieldsToIgnore) {

        MapDifference<String, Object> mapDifference = Maps.difference(
                entitySetPrevious, entitySetAfterOperation);

        Map<String, Object> difOnLeft = removeFieldsToIgnore(mapDifference.entriesOnlyOnLeft(), fieldsToIgnore);
        Map<String, Object> difOnRight = removeFieldsToIgnore(mapDifference.entriesOnlyOnRight(), fieldsToIgnore);

        Map<String, Object> result = new HashMap<>();
        result.putAll(difOnLeft);
        result.putAll(difOnRight);
        return result;
    }

    private static Map<String, Object> removeFieldsToIgnore(Map<String, Object> entitySetAfterOperation,
            List<String> fieldsToIgnore) {

        if (CollectionUtils.isNotEmpty(fieldsToIgnore)) {
            return entitySetAfterOperation.keySet().stream()
                    .filter(field -> !fieldsToIgnore.contains(field))
                    .collect(Collectors.toMap(key -> key, key -> entitySetAfterOperation.get(key)));
        }

        return entitySetAfterOperation;
    }

    private static CustomComparator ourInstance = new CustomComparator();

    public static CustomComparator getInstance() {

        return ourInstance;
    }

    private CustomComparator() {

    }

}
