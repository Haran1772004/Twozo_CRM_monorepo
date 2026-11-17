package com.tlc.crm.entity.models.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

import java.util.Collection;

/**
 * <p>
 * A model that represents the field visibility criteria group
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class FieldVisibilityCriteriaGroup {

    private Long id;
    private Connector connector;
    private Collection<FieldVisibilityCriteria> fieldVisibilityCriteria;
    private Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(final Connector connector) {
        this.connector = connector;
    }

    public Collection<FieldVisibilityCriteria> getFieldVisibilityCriteria() {
        return fieldVisibilityCriteria;
    }

    public void setFieldVisibilityCriteria(final Collection<FieldVisibilityCriteria> fieldVisibilityCriteria) {
        this.fieldVisibilityCriteria = fieldVisibilityCriteria;
    }

    public Collection<FieldVisibilityCriteriaGroup> getChildFieldVisibilityCriteriaGroups() {
        return childFieldVisibilityCriteriaGroups;
    }

    public void setChildFieldVisibilityCriteriaGroups(final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups) {
        this.childFieldVisibilityCriteriaGroups = childFieldVisibilityCriteriaGroups;
    }

    /**
     * <p>
     * Represents the connector
     * </p>
     */
    public enum Connector {

        AND((short) 1),
        OR((short) 2),
        ;

        private final short id;

        Connector(final short id) {
            this.id = id;
        }

        /**
         * <p>
         * Gets the connector id
         * </p>
         *
         * @return the connector id
         */
        public short getId() {
            return this.id;
        }

        /**
         * Gets the {@link FieldVisibilityCriteriaGroup.Connector} by id
         *
         * @param id represents the id of the connector
         * @return {@link FieldVisibilityCriteriaGroup.Connector}
         */
        public static FieldVisibilityCriteriaGroup.Connector valueOf(final short id) {
            return switch (id) {
                case 1 -> AND;
                case 2 -> OR;
                default -> throw ErrorCode.get(EntityErrorCodes.INVALID_TYPE);
            };
        }
    }
}
