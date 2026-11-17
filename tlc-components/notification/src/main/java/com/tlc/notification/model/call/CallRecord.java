package com.tlc.notification.model.call;

/**
 * <p>
 * A model that represents the call record.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CallRecord {

    private Long id;
    private String serviceId;
    private String uri;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }
}
