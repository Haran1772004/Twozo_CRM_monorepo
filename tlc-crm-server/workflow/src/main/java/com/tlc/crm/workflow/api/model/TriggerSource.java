package com.tlc.crm.workflow.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * Represents the {@link TriggerSource} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum TriggerSource {

    ACTIVITY("Activity", (short) 1, "i18n.workflow.trigger.source.activity"),
    DEAL("Deal", (short) 2, "i18n.workflow.trigger.source.deal.placeholder"),
    CONTACT("Contact", (short) 3, "i18n.workflow.trigger.source.contact.placeholder"),
    COMPANY("Company", (short) 4, "i18n.workflow.trigger.source.company.placeholder"),
    ;

    private final String name;
    private final short id;
    private final String i18nKey;

    TriggerSource(final String name, final short id, final String i18nKey) {
        this.name = name;
        this.id = id;
        this.i18nKey = i18nKey;
    }

    public String getName() {
        return name;
    }

    public short getId() {
        return id;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * Gets the {@link TriggerSource} by its id.
     *
     * @param id Refers the trigger source id.
     * @return The {@link TriggerSource}.
     */
    public static TriggerSource get(final short id) {
        return switch (id) {
            case 1 -> ACTIVITY;
            case 2 -> DEAL;
            case 3 -> CONTACT;
            case 4 -> COMPANY;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_SOURCE, "i18n.workflow.invalid.trigger.source");
        };
    }

    /**
     * Gets the {@link TriggerSource} by its name.
     *
     * @param name Refers the trigger source name.
     * @return The {@link TriggerSource}.
     */
    public static TriggerSource getByName(final String name) {
        return switch (name.toLowerCase()) {
            case "activity" -> ACTIVITY;
            case "deal" -> DEAL;
            case "contact" -> CONTACT;
            case "company" -> COMPANY;
            default -> null;
        };
    }

    /**
     * Represents the {@link TriggerEvent} details.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    public enum TriggerEvent {

        CREATED((short) 1, "Created"),
        EDITED((short) 2, "Updated"),
        DELETED((short) 3, "Deleted"),
        ;

        private final short id;
        private final String name;

        TriggerEvent(final short id, final String name) {
            this.id = id;
            this.name = name;
        }

        public short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * Gets the {@link TriggerEvent} by its id.
         *
         * @param id Refers the trigger event id.
         * @return The {@link TriggerEvent}.
         */
        public static TriggerEvent get(final short id) {
            return switch (id) {
                case 1 -> CREATED;
                case 2 -> EDITED;
                case 3 -> DELETED;
                default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
            };
        }
    }
}
