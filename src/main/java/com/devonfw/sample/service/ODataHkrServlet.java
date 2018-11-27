package com.devonfw.sample.service;

import javax.servlet.annotation.WebServlet;

import org.springframework.stereotype.Service;
import com.devonfw.module.odata.service.api.AbstractODataServlet;
import com.devonfw.module.odata.service.api.ODataEntityScan;

import static com.devonfw.sample.constant.ServletConstants.SAMPLE_SERVLET_PATH;

@Service
@WebServlet(urlPatterns = {SAMPLE_SERVLET_PATH})
@ODataEntityScan(functionImportPackages = { "com.devonfw.sample.service.api"},
        entityPackages = {"com.devonfw.sample.common.to"})
public class ODataHkrServlet extends AbstractODataServlet {

}
