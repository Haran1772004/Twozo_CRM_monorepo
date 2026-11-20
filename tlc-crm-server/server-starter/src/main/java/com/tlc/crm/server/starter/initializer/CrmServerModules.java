package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.Modules;

/**
 * Enum representing the different modules within the Crm-Server system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum CrmServerModules implements Modules {

    FILE_SERVER("file-server", "com.tlc.file.server"),
    EMAIL_SERVER("email-server", "com.tlc.email.server"),
    SECURITY("security", "com.tlc.crm.security"),
    FIELDS("fields", "com.tlc.crm.fields"),
    COMMON("common", "com.tlc.crm.common"),
    CRM_MARKETPLACE("crm-marketplace", "com.tlc.crm.marketplace.app"),
    ANALYTICS("analytics", "com.tlc.analytics"),
    ICON("icon", "com.tlc.crm.icon"),
    ENTITY("entity", "com.tlc.crm.entity"),
    WEB_FORM("webform", "com.tlc.crm.webform"),
    EMAIL_NOTIFICATION("email-notification", "com.tlc.crm.email.notification"),
    CORE("core", "com.tlc.crm.core"),
    AUTO_ASSIGNMENT("auto-assignment", "com.tlc.crm.auto.assignment"),
    NOTIFICATIONS("notifications", "com.tlc.crm.notifications"),
    MESSENGER("messenger", "com.tlc.crm.messenger"),
    WORKFLOW("workflow", "com.tlc.crm.workflow"),
    EMAIL_CORE("email-core", "com.tlc.crm.email.core"),
    ZAPIER("zapier", "com.tlc.crm.zapier"),
    PLAN_AND_BILLING("plan-and-billing", "com.tlc.crm.plan.and.billing"),
    CONVERSATION_HANDLER("conversation", "com.tlc.crm.conversation.handler"),
    AI("ai", "com.tlc.crm.ai"),
    ;

    private final String artifactId;
    private final String classNamePrefix;
    private final short id;

    CrmServerModules(final String artifactId, final String classNamePrefix) {
        this.artifactId = artifactId;
        this.classNamePrefix = classNamePrefix;
        this.id = ModuleRegistry.getInstance().getId();
    }

    /**
     * {@inheritDoc}
     *
     * @return The artifact ID
     */
    @Override
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getId() {
        return id;
    }
}
