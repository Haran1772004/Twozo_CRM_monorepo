package com.tlc.email.common.model;

/**
 * <p>
 * Represents metadata information associated with an email domain.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainMetaData {

    private Long externalId;
    private ServiceProvider serviceProvider;

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(final Long externalId) {
        this.externalId = externalId;
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(final ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }
}