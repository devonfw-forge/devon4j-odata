package com.devonfw.sample.service;

import javax.servlet.annotation.WebServlet;

import org.springframework.stereotype.Service;
import com.devonfw.module.odata.service.api.AbstractODataServlet;
import com.devonfw.module.odata.service.api.ODataEntityScan;

@Service
@WebServlet(urlPatterns = {"/services/odata.svc/*"})
@ODataEntityScan(functionImportPackages = { "com.devonfw.sample.service.api"},
        entityPackages = {"com.devonfw.sample.common.to"})
public class ODataHkrServlet extends AbstractODataServlet {

}
