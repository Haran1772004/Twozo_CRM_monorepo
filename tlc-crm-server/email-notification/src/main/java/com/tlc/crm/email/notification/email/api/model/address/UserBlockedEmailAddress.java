package com.tlc.crm.email.notification.email.api.model.address;

import com.tlc.notification.model.email.EmailAddress;

import java.util.Objects;

/**
 * A model that represents the user blocked email address
 *
 * @author Kishore J
 * @version 1.0
 */
public class UserBlockedEmailAddress extends EmailAddress {

    private Long userId;

    public UserBlockedEmailAddress(final String emailId) {
        super(emailId);
    }

    public UserBlockedEmailAddress(final Long orgId, final String emailId) {
        super(orgId, emailId);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
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
        final UserBlockedEmailAddress userBlockedEmailAddress = (UserBlockedEmailAddress) object;

        return Objects.equals(super.orgId(), userBlockedEmailAddress.orgId()) && Objects.equals(this.userId, userBlockedEmailAddress.userId)
                && Objects.equals(super.getEmailId(), userBlockedEmailAddress.getEmailId());
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(orgId(), userId, getEmailId());
    }
}
