package com.tlc.crm.core.fields.service.internal;

import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.fields.api.model.FieldVisibilityRule;
import com.tlc.crm.core.fields.service.FieldVisibilityRuleService;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Provides the {@link FieldVisibilityRule} for the given {@link FieldInfo},{@link FieldVisibilityCriteriaGroup}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class FieldVisibilityRuleServiceImpl implements FieldVisibilityRuleService {

    private static final Set<String> VALIDATION_FIELDS = Set.of(
            DealFields.EXPECTED_CLOSE_DATE, DealFields.FORECAST_CATEGORY);

    private static class Instance {
        private static final FieldVisibilityRuleServiceImpl INSTANCE = new FieldVisibilityRuleServiceImpl();
    }

    private FieldVisibilityRuleServiceImpl() {
    }

    /**
     * <p>
     * Gets the {@link FieldVisibilityRuleServiceImpl} Instance
     * </p>
     *
     * @return {@link FieldVisibilityRuleServiceImpl}
     */
    public static FieldVisibilityRuleServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo                    Represents the {@link FieldInfo}
     * @param fieldVisibilityCriteriaGroup Represents the {@link FieldVisibilityCriteriaGroup}
     * @return {@link FieldVisibilityRule}
     */
    @Override
    public FieldVisibilityRule getFieldVisibilityRule(final FieldInfo fieldInfo, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup) {
        return getFieldVisibilityRule(fieldInfo, fieldVisibilityCriteriaGroup, FieldKeys.SHOW_WHEN.getName());
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo                    Represents the {@link FieldInfo}
     * @param fieldVisibilityCriteriaGroup Represents the {@link FieldVisibilityCriteriaGroup}
     * @param fieldVisibilityKey           Represents the field visibility key
     * @return {@link FieldVisibilityRule}
     */
    @Override
    public FieldVisibilityRule getFieldVisibilityRule(final FieldInfo fieldInfo, final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup, final String fieldVisibilityKey) {
        final FieldSource fieldSource = fieldInfo.fieldSource();

        if (Objects.equals(Source.get(fieldSource.getName()), Source.DEAL) && Optional.ofNullable(fieldInfo.aliasName())
                .map(VALIDATION_FIELDS::contains).orElseGet(() -> false)) {

            return new FieldVisibilityRule(fieldInfo, fieldVisibilityCriteriaGroup, FieldKeys.VALIDATION_WHEN.getName());
        }
        return new FieldVisibilityRule(fieldInfo, fieldVisibilityCriteriaGroup, fieldVisibilityKey);
    }
}
