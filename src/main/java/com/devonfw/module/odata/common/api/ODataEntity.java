package com.devonfw.module.odata.common.api;

import java.io.Serializable;

public interface ODataEntity<ID> extends Serializable {

    ID getId();

    void setId(ID id);
}
