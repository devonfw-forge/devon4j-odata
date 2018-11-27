package com.devonfw.module.odata.common.api;

import java.io.Serializable;

public interface ODataSet<ID> extends Serializable {

    ID getId();

    void setId(ID id);
}
