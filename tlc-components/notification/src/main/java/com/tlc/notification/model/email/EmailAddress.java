package com.tlc.notification.model.email;

import com.tlc.notification.model.identity.ReceiverIdentity;
import com.tlc.notification.model.identity.SenderIdentity;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Email;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the email address.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EmailAddress implements SenderIdentity, ReceiverIdentity, TlcModel {

    private Long id;

    @NotNull(groups = Group.Create.class, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @Email(groups = Group.Create.class, message = "i18n.notification.email.invalid.format")
    private String emailAddress;

    public EmailAddress() {

    }

    public EmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public EmailAddress(final Long orgId, final String emailId) {
        this.orgId = orgId;
        this.emailAddress = emailId;
    }

    public EmailAddress(final Long id, final Long orgId, final String emailId) {
        this.id = id;
        this.orgId = orgId;
        this.emailAddress = emailId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getEmailId() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Extracts the name from the given email address
     *
     * @param emailAddress Represents the email address
     * @return the name
     */
    public static String extractNameFromEmail(final String emailAddress) {
        return emailAddress.split("@")[0];
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
        final EmailAddress emailAddress = (EmailAddress) object;

        return Objects.equals(this.emailAddress, emailAddress.emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(emailAddress);
    }
}