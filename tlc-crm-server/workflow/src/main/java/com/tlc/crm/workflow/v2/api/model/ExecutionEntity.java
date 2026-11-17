package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.json.JsonObject;

/**
 * Represents an execution entity.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public record ExecutionEntity(Long orgId, Long executionId, Long nodeId, Long entityId,
                              JsonObject entityJson, ExecutionSource executionSource) {

    /**
     * Represents the execution source.
     */
    public enum ExecutionSource {

        COMPANY((short) 1),
        CONTACT((short) 2),
        DEAL((short) 3),
        ACTIVITY((short) 4),
        EMAIL((short) 5),
        ;

        private final short id;

        ExecutionSource(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * Returns the execution source for the given ID.
         *
         * @param id The ID.
         * @return The execution source.
         */
        public static ExecutionSource get(final short id) {
            return switch (id) {
                case 1 -> COMPANY;
                case 2 -> CONTACT;
                case 3 -> DEAL;
                case 4 -> ACTIVITY;
                case 5 -> EMAIL;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }

        /**
         * Returns the execution source for the given name.
         *
         * @param name The name.
         * @return The execution source.
         */
        public static ExecutionSource get(final String name) {
            return switch (name.toLowerCase()) {
                case "company" -> COMPANY;
                case "contact" -> CONTACT;
                case "deal" -> DEAL;
                case "activity" -> ACTIVITY;
                case "email" -> EMAIL;
                default -> null;
            };
        }
    }
}

