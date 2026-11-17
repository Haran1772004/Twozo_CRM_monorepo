package com.tlc.crm.core.fields.api.model;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;

/**
 * <p>
 * Holds the {@link FieldVisibilityRule} values which contain the {@link FieldInfo},{@link FieldVisibilityCriteriaGroup}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public record FieldVisibilityRule(FieldInfo fieldInfo, FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup, String fieldVisibilityKey) {

    public FieldVisibilityRule(final FieldInfo fieldInfo) {
        this(fieldInfo, null);
    }

    public FieldVisibilityRule(final FieldInfo fieldInfo, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup) {
        this(fieldInfo, fieldVisibilityCriteriaGroup, FieldKeys.SHOW_WHEN.getName());
    }
}
