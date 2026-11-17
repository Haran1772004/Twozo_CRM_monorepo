package com.tlc.components.starter.initializer;

/**
 * Defines the names of various modules within the application, along with their corresponding identifiers.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ComponentModules implements Modules {

    COMMONS("commons", "com.tlc.commons"),
    CACHE("cache", "com.tlc.cache"),
    I18N("i18n", "com.tlc.i18n"),
    L10N("l10n", "com.tlc.l10n"),
    VALIDATOR("validator", "com.tlc.validator"),
    ATTACHMENT("attachment", "com.tlc.attachment"),
    OLTP("oltp", "com.tlc.sql.oltp"),
    OLAP("olap", "com.tlc.sql.olap"),
    TOOLS("tools", "com.tlc.tools"),
    SCHEDULER("scheduler", "com.tlc.scheduler"),
    WEB_CLIENT("webclient", "com.tlc.web.client"),
    WEB("web", "com.tlc.web"),
    QUEUE("queue", "com.tlc.queue"),
    CDC_CORE("cdc-core", "com.tlc.cdc.core"),
    CLUSTER("cluster", "com.tlc.cluster"),
    SEARCH("search", "com.tlc.search"),
    SYNC_SERVICE_OAUTH("sync-service-oauth", "com.tlc.sync.service.oauth"),
    NOTIFICATION("notification", "com.tlc.notification"),
    EXCHANGE_RATE("exchange-rate", "com.tlc.exchange.rate"),
    OTP_PROCESSOR("otp-processor", "com.tlc.otp.processor"),
    PLANS_AND_BILLINGS("plans-and-billings", "com.tlc.plans.and.billings"),
    PAYMENT_GATEWAY("payment-gateway", "com.tlc.payment.gateway"),
    MARKETPLACE("marketplace", "com.tlc.marketplace"),
    MCP("mcp", "com.tlc.mcp"),
    ;

    private final String artifactId;
    private final String classNamePrefix;
    private final short id;

    ComponentModules(final String artifactId, final String classNamePrefix) {
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
     *
     * @return id.
     */
    @Override
    public short getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name.
     */
    @Override
    public String getName() {
        return this.name();
    }

}
