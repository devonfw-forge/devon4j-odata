package com.devonfw.module.odata.service.impl;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.springframework.context.ApplicationContext;
import com.devonfw.module.odata.logic.util.ODataAnnotationUtil;
import com.devonfw.module.odata.service.api.AbstractODataServlet;
import com.devonfw.module.odata.service.api.ODataEntityScan;
import com.google.common.base.Preconditions;

@AllArgsConstructor
public class ODataBaseServiceFactory extends ODataServiceFactory {

    private ODataProcessor oDataProcessor;

    private Class<? extends AbstractODataServlet> servletClass;

    private ApplicationContext context;

    @Override
    public ODataService createService(final ODataContext context) throws ODataException {

        Preconditions.checkNotNull(oDataProcessor, "ODataSingleProcessor is not correctly initialized");

        ODataEdmProviderExtension edmProvider = createAnnotationEdmProviderExtension();
        return this.createODataSingleProcessorService(edmProvider, oDataProcessor);
    }

    private ODataEdmProviderExtension createAnnotationEdmProviderExtension() throws ODataException {

        List<String> functionImportPackages = ODataAnnotationUtil
                .getValueAnnotations(servletClass, ODataEntityScan.class, "functionImportPackages");
        List<String> entityPackages =
                ODataAnnotationUtil.getValueAnnotations(servletClass, ODataEntityScan.class, "entityPackages");

        List<Class<?>> extractedEntityClass = ODataAnnotationUtil.extractAnnotatedClass(entityPackages);
        List<Class<?>> extractedFunctionImportClass =
                ODataAnnotationUtil.extractAnnotatedClass(functionImportPackages);

        Map<String, ODataImportFunctionHolder> functionImportHolders =
                ODataAnnotationUtil.extractFunctionImportClass(context, extractedFunctionImportClass);

        List<Class<?>> allClass = new ArrayList(extractedEntityClass);
        allClass.addAll(extractedFunctionImportClass);
        return new ODataEdmProviderExtension(allClass, functionImportHolders);
    }

}
