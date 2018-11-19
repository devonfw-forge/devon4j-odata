package com.devonfw.module.odata.service.api;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.olingo.odata2.core.exception.ODataRuntimeException;
import org.apache.olingo.odata2.core.servlet.ODataServlet;
import org.springframework.context.ApplicationContext;
import com.devonfw.module.odata.logic.util.ODataAnnotationUtil;
import com.devonfw.module.odata.service.api.ODataManagingService;
import com.devonfw.module.odata.service.impl.ODataBaseServiceFactory;
import com.devonfw.module.odata.service.impl.ODataProcessor;
import com.google.common.base.Preconditions;

public abstract class AbstractODataServlet extends ODataServlet {

    private static final long serialVersionUID = 1L;

    private Map<String, ODataBaseServiceFactory> baseODataServiceFactoryMap = new HashMap<>();

    private ODataManagingService oDataManagingService;

    private ApplicationContext context;

    @Inject
    public void setContext(ApplicationContext context) {

        this.context = context;
    }

    @Inject
    public void setODataManagingService(ODataManagingService oDataManagingService) {

        this.oDataManagingService = oDataManagingService;
    }

    @PostConstruct
    public void setServletData() {

        Preconditions.checkNotNull(oDataManagingService, "ODataManagingService is not correctly initialized");

        List<String> webServletUrlPatterns =
                ODataAnnotationUtil.getValueAnnotations(this.getClass(), WebServlet.class, "urlPatterns");

        if (CollectionUtils.isNotEmpty(webServletUrlPatterns)) {
            ODataProcessor oDataProcessor = new ODataProcessor(oDataManagingService);
            ODataBaseServiceFactory ODataBaseServiceFactory =
                    new ODataBaseServiceFactory(oDataProcessor, this.getClass(), context);
            this.baseODataServiceFactoryMap.put(webServletUrlPatterns.get(0), ODataBaseServiceFactory);
        }
    }

    @Override
    protected ODataBaseServiceFactory getServiceFactory(HttpServletRequest request) {

        Optional<String> optionalUrl =
                this.baseODataServiceFactoryMap.keySet().stream().filter(url -> url.contains(request.getServletPath()))
                        .findFirst();

        if (optionalUrl.isPresent()) {
            return this.baseODataServiceFactoryMap.get(optionalUrl.get());
        }
        throw new ODataRuntimeException("Couldn't find ODataServiceFactory for this path " + request.getPathInfo()
                + " check settings in web servlet !!!");
    }

}
