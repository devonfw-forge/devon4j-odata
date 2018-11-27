package com.devonfw.module.odata.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.devonfw.module.odata.common.api.ODataSet;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Iterables;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sap.cloud.sdk.odatav2.connectivity.ODataQueryResult;
import com.sap.cloud.sdk.result.DefaultResultCollection;
import com.sap.cloud.sdk.result.GsonResultObject;
import com.sap.cloud.sdk.result.ResultElement;

public class TestUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtil.class);

    public static String getEntityKeyAsString(Object inputData) {

        return "(" + inputData + "L)";
    }

    public static Map<String, Object> getEntityKeyAsMap(ODataSet inputData) {

        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put("Id", inputData.getId());
        return keys;
    }

    public static Map<String, Object> getEntityKeyAsMap(Map<String, Object> inputData) {

        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put("Id", inputData.get("Id"));
        return keys;
    }

    public static <T> T convertMapToJavaObject(Class<T> classType, Map<String, Object> odataEntryAttributes) {

        if (odataEntryAttributes == null) {
            return null;
        }
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String jsonElement = gson.toJson(odataEntryAttributes);
            return gson.fromJson(jsonElement, classType);
        } catch (Exception e) {
            LOGGER.info("Conversion map to externalTo throws some exceptions " + e);
        }
        return null;
    }

    public static Map<String, Object> convertObjectToMap(Object object) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper.convertValue(object, Map.class);
    }

    public static Map<String, Object> getToAsMapFromQueryResult(ODataQueryResult resultRead, Class toClass,
            List<String> ignoreFields) {

        Map<String, Object> returnedToAsMap = null;
        ResultElement resultElement = resultRead.getResultElement();

        if (resultElement instanceof DefaultResultCollection) {
            resultElement = Iterables.getFirst(resultElement.getAsCollection(), null);
        }

        GsonResultObject gsonResultObject = (GsonResultObject) resultElement.getAsObject();
        JsonObject jsonObject = gsonResultObject.getJsonObject();

        for (String field : ignoreFields) {
            jsonObject.remove(field);
        }

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        Object returnedTo = gson.fromJson(jsonObject, toClass);
        returnedToAsMap = convertObjectToMap(returnedTo);
        return returnedToAsMap;
    }

    private TestUtil() {

    }
}
