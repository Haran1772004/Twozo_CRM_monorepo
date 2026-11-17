package com.tlc.crm.workflow.api.model;

import com.tlc.i18n.I18nKey;

/**
 * A model that holds the {@link TriggerActionEvent} details
 *
 * @author KarthickV
 * @version 1.0
 */
public final class TriggerActionEvent {

    private final long id;
    private final String aliasName;
    private final I18nKey displayName;
    private final boolean isEnabled;

    private TriggerActionEvent(final Builder builder) {
        this.id = builder.id;
        this.aliasName = builder.aliasName;
        this.displayName = builder.displayName;
        this.isEnabled = builder.isEnabled;
    }

    public long id() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * <p>
     * Allows constructing an {@link TriggerActionEvent} step by step, setting only the necessary properties
     * </p>
     */
    public static class Builder {
        private final long id;
        private final String aliasName;
        private final I18nKey displayName;
        private boolean isEnabled;

        public Builder(final long id, final String aliasName, final I18nKey displayName) {
            this.id = id;
            this.aliasName = aliasName;
            this.displayName = displayName;
        }

        public Builder isEnabled(final boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        public TriggerActionEvent build() {
            return new TriggerActionEvent(this);
        }
    }

    /**
     * Checks if the action event is a common action event based on its alias name.
     *
     * @return true if the alias name of the event is common, otherwise false.
     */
    public boolean isCommonActionEvent() {
        return switch (aliasName.toLowerCase()) {
            case "duplicateexistingdeal", "addproducttodeal" -> false;
            default -> true;
        };
    }

    /**
     * Checks if the action event is an update event based on its alias name.
     *
     * @return true if the action is an update event, otherwise false.
     */
    public boolean isUpdateEvent() {
        return switch (aliasName) {
            case "activityUpdate", "companyUpdate", "contactUpdate", "dealUpdate" -> true;
            default -> false;
        };
    }

}