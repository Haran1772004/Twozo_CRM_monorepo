package com.tlc.crm.core.workflow.internal.trigger;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.source.Source;

/**
 * Holds the information of entity of update event which is to be triggered.
 *
 * @param id         Refers the entity trigger id.
 * @param entityId   Refers the entity id.
 * @param orgId      Refers the organization id.
 * @param source     Refers the source of the entity.
 * @param priorState Refers the {@link JsonObject} which holds the previous values of fields of an entity.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record EntityTrigger(Long id, Long entityId, Long orgId, Source source, JsonObject priorState) {

    public EntityTrigger(final Long entityId, final Long orgId, final Source source, final JsonObject priorState) {
        this(null, entityId, orgId, source, priorState);
    }

    /**
     * Represents the status of the entity trigger
     */
    public enum Status {

        PENDING((short) 1),
        COMPLETED((short) 2);

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }
    }
}
