package com.tlc.crm.notifications.api.call.model;

import com.tlc.crm.common.iam.api.model.CrmUser;

/**
 * Represents the details of a forwarded call.
 * This class contains information about which user forwarded the call
 * and to whom the call was forwarded.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallForwardDetails {

    private Long id;
    private CrmUser forwardedBy;
    private CrmUser forwardedTo;
    private boolean isRingBackEnabled;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public CrmUser getForwardedBy() {
        return forwardedBy;
    }

    public void setForwardedBy(final CrmUser forwardedBy) {
        this.forwardedBy = forwardedBy;
    }

    public CrmUser getForwardedTo() {
        return forwardedTo;
    }

    public void setForwardedTo(final CrmUser forwardedTo) {
        this.forwardedTo = forwardedTo;
    }

    public boolean isRingBackEnabled() {
        return isRingBackEnabled;
    }

    public void setRingBackEnabled(final boolean ringBackEnabled) {
        this.isRingBackEnabled = ringBackEnabled;
    }
}