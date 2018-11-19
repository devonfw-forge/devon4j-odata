package com.devonfw.module.odata.logic.api.validation;

import com.devonfw.module.odata.common.api.ODataSet;

public interface ValidationRule<T extends ODataSet> {

    default void validate(T to) {
        if (!isRuleFulfilled(to)) {
            applyReaction(to);
        }
    }

    boolean isRuleFulfilled(T to);

    void applyReaction(T to);
}
