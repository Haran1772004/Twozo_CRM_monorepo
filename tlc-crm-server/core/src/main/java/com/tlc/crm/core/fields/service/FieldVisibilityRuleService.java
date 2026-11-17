package com.tlc.crm.core.fields.service;

import com.tlc.crm.core.fields.api.model.FieldVisibilityRule;
import com.tlc.crm.core.fields.service.internal.FieldVisibilityRuleServiceImpl;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;

/**
 * <p>
 * Provides the {@link FieldVisibilityRule} for the given {@link FieldInfo}, {@link FieldVisibilityCriteriaGroup}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface FieldVisibilityRuleService {

    /**
     * <p>
     * Fetches the {@link FieldVisibilityRuleService} instance
     * </p>
     *
     * @return the instance of {@link FieldVisibilityRuleService}
     */
    static FieldVisibilityRuleService getInstance() {
        return FieldVisibilityRuleServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link FieldVisibilityRule}
     * </p>
     *
     * @param fieldInfo                    Represents the {@link FieldInfo}
     * @param fieldVisibilityCriteriaGroup Represents the {@link FieldVisibilityCriteriaGroup}
     * @return {@link FieldVisibilityRule
     */
    FieldVisibilityRule getFieldVisibilityRule(final FieldInfo fieldInfo, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup);

    /**
     * <p>
     * Gets the {@link FieldVisibilityRule}
     * </p>
     *
     * @param fieldInfo                    Represents the {@link FieldInfo}
     * @param fieldVisibilityCriteriaGroup Represents the {@link FieldVisibilityCriteriaGroup}
     * @param fieldVisibilityKey           Represents the field visibility key
     * @return {@link FieldVisibilityRule}
     */
    FieldVisibilityRule getFieldVisibilityRule(final FieldInfo fieldInfo, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup, final String fieldVisibilityKey);
}
