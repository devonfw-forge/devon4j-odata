package com.devonfw.module.odata.service.api;

import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource.BinaryData;
import com.devonfw.module.odata.common.api.ODataSet;

public interface FileService<T extends ODataSet> extends ODataService<T> {

    BinaryData readBinaryData(T mediaEntity);

    T writeBinaryData(BinaryData mediaData, T mediaEntity);
}
