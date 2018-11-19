package com.devonfw.module.odata.common.api;

import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;

public interface BinaryODataSet extends ODataSet {

  DataSource.BinaryData getBinaryData();

}
