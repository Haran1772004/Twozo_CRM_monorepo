package com.tlc.crm.workflow.api.model;

import com.tlc.i18n.I18nKey;

import java.util.Collection;

/**
 * A model that holds the {@link TriggerActionSource} details
 *
 * @author KarthickV
 * @version 1.0
 */
public final class TriggerActionSource {

    private final long id;
    private final String aliasName;
    private final I18nKey displayName;
    private final Collection<TriggerActionEvent> triggerActionEvents;

    private TriggerActionSource(final Builder builder) {
        this.id = builder.id;
        this.aliasName = builder.aliasName;
        this.displayName = builder.displayName;
        this.triggerActionEvents = builder.triggerActionEvents;
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

    public Collection<TriggerActionEvent> getTriggerActionEvents() {
        return triggerActionEvents;
    }

    /**
     * <p>
     * Allows constructing an {@link TriggerActionSource} step by step, setting only the necessary properties
     * </p>
     */
    public static class Builder {
        private final long id;
        private final String aliasName;
        private final I18nKey displayName;
        private Collection<TriggerActionEvent> triggerActionEvents;

        public Builder(final long id, final I18nKey displayName, final String aliasName) {
            this.id = id;
            this.aliasName = aliasName;
            this.displayName = displayName;
        }

        public Builder setTriggerActionEvents(final Collection<TriggerActionEvent> triggerActionEvents) {
            this.triggerActionEvents = triggerActionEvents;
            return this;
        }

        public TriggerActionSource build() {
            return new TriggerActionSource(this);
        }
    }

    /**
     * <p>
     * Checks whether the alias name is source or not
     * </p>
     *
     * @return true if the given alias name is source field
     */
    public boolean isCommonActionSource() {
        return switch (aliasName.toLowerCase()) {
            case "contact", "deal", "company" -> true;
            default -> false;
        };
    }
}

