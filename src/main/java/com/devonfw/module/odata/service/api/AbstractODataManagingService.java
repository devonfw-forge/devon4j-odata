package com.devonfw.module.odata.service.api;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import com.devonfw.module.odata.logic.util.ODataAnnotationUtil;

public abstract class AbstractODataManagingService implements ODataManagingService {

    @Inject
    private List<ODataService> oDataServices;

    @PersistenceContext
    private EntityManager entityManager;

    private static Map<String, ODataService> SERVICE_LOOKUP;

    @PostConstruct
    private void mapServiceLookups() {

        SERVICE_LOOKUP = oDataServices.stream()
                .collect(Collectors.toMap(this::getEntityName, Function.identity()));
    }

    private String getEntityName(ODataService service) {

        return ODataAnnotationUtil.extractEntitySetName(service.getToClass());
    }

    @Override
    public ODataService getService(String entityName) throws ODataNotImplementedException {

        if (!SERVICE_LOOKUP.containsKey(entityName)) {
            throw new ODataNotImplementedException(ODataNotImplementedException.COMMON);
        }
        return SERVICE_LOOKUP.get(entityName);
    }


}
