package com.tlc.crm.core.api.insights.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * <p>
 * Represents the entity reference identifier type
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum EntityInsightsIdentifierType {

    /**
     * <p>
     * Indicates the {@code ENTITY_ID} column stores a direct entity identifier
     * (for example, the contact id, company id, deal id, etc.).
     * </p>
     */
    ENTITY((short) 1),

    /**
     * <p>
     * Indicates the {@code ENTITY_ID} column stores a source reference id.
     * Source references are ids from the originating source (for example an
     * activity id) which need to be expanded to entities when recomputing
     * insights.
     * </p>
     */
    SOURCE_REF((short) 2);

    private final short id;

    EntityInsightsIdentifierType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static EntityInsightsIdentifierType fromId(final short id) {

        for (final EntityInsightsIdentifierType type : values()) {

            if (type.id == id) {
                return type;
            }
        }

        throw ErrorCode.get(CoreErrorCodes.INVALID_IDENTIFIER_TYPE);
    }
}