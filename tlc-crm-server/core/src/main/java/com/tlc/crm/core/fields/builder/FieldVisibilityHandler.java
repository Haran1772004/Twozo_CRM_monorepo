package com.tlc.crm.core.fields.builder;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteria;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Handles the {@link FieldVisibilityCriteriaGroup} based on the {@link FieldInfo} and respective {@link Source}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface FieldVisibilityHandler {

    /**
     * <p>
     * Fetches the {@link FieldVisibilityHandler} instance based on the source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return the instance of {@link FieldVisibilityHandler}
     */
    static FieldVisibilityHandler get(final Source source) {
        return switch (source) {
            case DEAL -> DealFieldVisibilityHandler.getInstance();
            case CONTACT -> ContactFieldVisibilityHandler.getInstance();
            default -> null;
        };
    }

    /**
     * <p>
     * Fetches the {@link FieldVisibilityHandler} instance based on the source
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return the instance of {@link FieldVisibilityHandler}
     */
    static Optional<FieldVisibilityHandler> getFieldVisibilityHandler(final Source source) {
        return switch (source) {
            case DEAL -> Optional.ofNullable(DealFieldVisibilityHandler.getInstance());
            case CONTACT -> Optional.ofNullable(ContactFieldVisibilityHandler.getInstance());
            default -> Optional.empty();
        };
    }

    /**
     * <p>
     * Gets the {@link FieldVisibilityCriteriaGroup} for the given {@link FieldInfo}
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents the {@link FieldInfo}
     */
    Optional<FieldVisibilityCriteriaGroup> getCriteriaGroup(final long orgId, final FieldInfo fieldInfo);

    /**
     * <p>
     * Builds the {@link FieldVisibilityCriteriaGroup} based on the given field id and value with the connector
     * </p>
     *
     * @param fieldId   Represents the field id
     * @param value     Represents the value of the field
     * @return {@link FieldVisibilityCriteriaGroup}
     */
    default FieldVisibilityCriteriaGroup buildCriteriaGroup(final Long fieldId, final Long value) {
        final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
        final FieldVisibilityCriteria fieldVisibilityCriteria = new FieldVisibilityCriteria();

        fieldVisibilityCriteria.setCondition(FieldVisibilityCriteria.Condition.EQUALS);
        fieldVisibilityCriteria.setFieldId(fieldId);
        fieldVisibilityCriteria.setValue(value);

        fieldVisibilityCriteriaGroup.setFieldVisibilityCriteria(List.of(fieldVisibilityCriteria));

        return fieldVisibilityCriteriaGroup;
    }
}
