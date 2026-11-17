package com.tlc.cdc.core.models;

import com.tlc.cdc.core.internal.status.CdcErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.util.Collection;

/**
 * Configuration metadata representing an CDC connector
 * and the set of source tables associated with it.
 *
 * @param id     The unique identifier of the connector configuration.
 * @param name   The name of the CDC connector.
 * @param tables The collection of source tables handled by the connector.
 * @param type   The CDC event type associated with this connector.
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public record ConnectorConfig(Long id, String name, Collection<String> tables, Type type) {

    /**
     * Represents the type of CDC connector within the pipeline.
     */
    public enum Type {

        /**
         * Indicates a CDC source connector that captures
         * changes from a source system.
         */
        SOURCE((short) 1),

        /**
         * Indicates a CDC sink connector that consumes
         * Kafka events and writes to a target system.
         */
        SINK((short) 2);

        private final short id;

        Type(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * Gets the type for the id provided.
         *
         * @param id Refers the id.
         * @return {@code Type}.
         */
        public static Type get(final short id) {
            return switch (id) {
                case 1 -> SOURCE;
                case 2 -> SINK;
                default -> throw ErrorCode.get(CdcErrorCodes.INVALID_CONNECTOR_TYPE);
            };
        }
    }
}
