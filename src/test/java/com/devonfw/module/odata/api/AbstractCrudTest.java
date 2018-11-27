package com.devonfw.module.odata.api;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.olingo.odata2.api.annotation.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.junit.After;
import org.junit.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devonfw.module.odata.common.CustomComparator;
import com.devonfw.module.odata.common.TestUtil;
import com.devonfw.module.odata.common.api.ODataEntity;
import com.devonfw.module.odata.common.api.ODataSet;
import com.sap.cloud.sdk.odatav2.connectivity.*;
import com.sap.cloud.sdk.odatav2.connectivity.internal.ODataHttpResponseWrapper;

import static org.junit.Assert.*;

public abstract class AbstractCrudTest<E extends ODataEntity> extends AbstractTest {

    @Inject
    protected JpaRepository<E, Long> repository;

    protected CustomComparator customComparator = CustomComparator.getInstance();

    @After
    public void clearDatabase() {

        repository.deleteAll();
        assertEquals(repository.findAll().size(), 0);
    }

    @Test
    public void create() throws ODataException {

        ODataSet entitySet = createEntitySet();
        Map entitySetMap = TestUtil.convertObjectToMap(entitySet);

        ODataCreateResult result = sendRequestCreate(getEntitySetName(entitySet), entitySetMap);
        checkStatusCreated(result);

        Map returned = result.asMap();
        assertNotNull(returned);

        Long id = (Long) returned.get(ID_KEY);
        Optional<E> entity = repository.findById(id);
        assertTrue(entity.isPresent());

        entitySetMap.put(ID_KEY, id);
        assertEquals(customComparator.compare(entitySetMap, returned, getIgnoredFieldsList()).size(), 0);
    }

    @Test
    public void readAll() throws IllegalArgumentException, ODataException {

        ODataSet entitySet = createEntitySet();
        E entity = (E) createEntity(entitySet);
        repository.save(entity);

        Map entitySetMap = TestUtil.convertObjectToMap(entitySet);
        List savedEntities = repository.findAll();
        assertEquals(savedEntities.size(), 1);

        ODataQueryResult result = sendRequestQuery(getEntitySetName(entitySet));
        checkStatusOk(result);

        List<Map> returned = result.asList(Map.class);
        assertNotNull(returned);
        assertTrue(CollectionUtils.isNotEmpty(returned));
        assertEquals(returned.size(), 1);
        assertTrue(returned.size() > 0);

        List<String> ignore = getIgnoredFieldsList();
        ignore.add(ID_KEY);

        assertEquals(customComparator.compare(entitySetMap, returned.get(0), ignore).size(), 0);
    }

    @Test
    public void readOne() throws IllegalArgumentException, ODataException {

        ODataSet entitySet = createEntitySet();
        E entity = (E) createEntity(entitySet);
        repository.save(entity);

        String entityNameWithId = getEntitySetName(entitySet) + TestUtil.getEntityKeyAsString(entity.getId());
        ODataQueryResult result = sendRequestQuery(entityNameWithId);
        checkStatusOk(result);

        Map entitySetMap = TestUtil.convertObjectToMap(entitySet);
        Map returned = result.as(Map.class);
        assertNotNull(returned);

        List<String> ignore = getIgnoredFieldsList();
        ignore.add(ID_KEY);
        assertEquals(customComparator.compare(entitySetMap, returned, ignore).size(), 0);
    }

    @Test
    public void update() throws ODataException {

        ODataSet entitySet = createEntitySet();
        E entity = (E) createEntity(entitySet);
        repository.save(entity);

        ODataSet changedEntitySet = changeEntityForUpdate((ODataSet) SerializationUtils.clone(entitySet));
        assertNotEquals(entitySet, changedEntitySet);

        changedEntitySet.setId(entity.getId());

        Map entitySetChangedMap = TestUtil.convertObjectToMap(changedEntitySet);
        ODataUpdateResult result =
                sendQueryUpdate(getEntitySetName(entitySet), entitySetChangedMap);
        checkStatusNoContent(result);

        Optional<E> entityOptional = repository.findById((Long) entity.getId());
        assertTrue(entityOptional.isPresent());
        E entityAfterUpdate = entityOptional.get();

        Map entityFromDatabaseAfterUpdate = TestUtil.convertObjectToMap(entityAfterUpdate);
        assertEquals(
                customComparator.compare(entitySetChangedMap, entityFromDatabaseAfterUpdate, getIgnoredFieldsList())
                        .size(), 0);
    }

    @Test
    public void delete() throws ODataException {

        ODataSet entitySet = createEntitySet();
        E entity = (E) createEntity(entitySet);
        repository.save(entity);

        entitySet.setId(entity.getId());

        Map entitySetMap = TestUtil.getEntityKeyAsMap(entitySet);
        ODataDeleteResult result =
                sendRequestDelete(getEntitySetName(entitySet), entitySetMap);
        checkStatusNoContent(result);
    }

    public static String getEntitySetName(ODataSet oDataSet) {

        String name = oDataSet.getClass().getAnnotation(EdmEntitySet.class).name();
        assertNotNull(name, EdmEntitySet.class + " name is empty, check annotation name");
        return name;
    }

    public static void checkStatusOk(ODataHttpResponseWrapper result) {

        assertEquals(HttpStatusCodes.OK.getStatusCode(), result.getHttpStatusCode());
    }

    public static void checkStatusNoContent(ODataHttpResponseWrapper result) {

        assertEquals(HttpStatusCodes.NO_CONTENT.getStatusCode(), result.getHttpStatusCode());
    }

    public static void checkStatusCreated(ODataHttpResponseWrapper result) {

        assertEquals(HttpStatusCodes.CREATED.getStatusCode(), result.getHttpStatusCode());
    }

    protected static ODataUpdateResult sendQueryUpdate(String entitySetName, Map<String, Object> entitySet)
            throws ODataException {

        return ODataUpdateRequestBuilder
                .withEntity(TEST_ODATA_NAME, entitySetName, TestUtil.getEntityKeyAsMap(entitySet))
                .withBodyAsMap(TestUtil.convertObjectToMap(entitySet)).build().execute(TEST_DESTINATION_NAME);
    }

    protected static ODataQueryResult sendRequestQuery(String entitySetName) throws ODataException {

        return ODataQueryBuilder.withEntity(TEST_ODATA_NAME, entitySetName)
                .withoutMetadata()
                .build()
                .execute(TEST_DESTINATION_NAME);
    }

    protected static ODataCreateResult sendRequestCreate(String entitySetName, Map<String, Object> entitySet)
            throws ODataException {

        return ODataCreateRequestBuilder.withEntity(TEST_ODATA_NAME, entitySetName)
                .withBodyAs(entitySet).build()
                .execute(TEST_DESTINATION_NAME);
    }

    protected static ODataDeleteResult sendRequestDelete(String entitySetName, Map<String, Object> entitySet)
            throws ODataException {

        return ODataDeleteRequestBuilder
                .withEntity(TEST_ODATA_NAME, entitySetName, entitySet).build()
                .execute(TEST_DESTINATION_NAME);
    }

    protected abstract ODataSet changeEntityForUpdate(ODataSet oDataSet);

    protected abstract ODataEntity createEntity(ODataSet oDataSet);

    protected abstract List<String> getIgnoredFieldsList();

    protected abstract ODataSet createEntitySet();

}
