package com.tlc.email.common.model.address;

import com.tlc.email.common.model.ConsentType;

import com.tlc.notification.model.identity.SenderIdentity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an email address used for sync that also acts as a sender identity.
 * Which extends {@link EmailAddress} to inherit basic email-related fields and functionality,
 * while implementing SenderIdentity to explicitly define its role as a sender
 * in email sync and communication workflows.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailAddress
 * @see SenderIdentity
 */
public class SyncAddressInfo extends EmailAddress implements SenderIdentity, Serializable {

    private ConsentType consentType;

    public SyncAddressInfo() {
    }

    public SyncAddressInfo(final String emailAddress) {
        super(emailAddress);
    }

    public SyncAddressInfo(final Long id, final String emailAddress) {
        super(id, emailAddress);
    }

    public ConsentType getConsentType() {
        return consentType == null ? ConsentType.USER_GRANTED : consentType;
    }

    public void setConsentType(final ConsentType consentType) {
        this.consentType = consentType;
    }

    /**
     * {@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }
        final SyncAddressInfo address = (SyncAddressInfo) object;
        final String emailAddress = getEmailAddress();

        return Objects.equals(emailAddress, address.getEmailAddress()) &&
                Objects.equals(this.consentType, address.consentType);
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(getEmailAddress(), consentType);
    }
}
