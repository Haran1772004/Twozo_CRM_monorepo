package com.tlc.email.common.model.address;

import com.tlc.email.common.model.EmailModel;

import com.tlc.notification.model.identity.ReceiverIdentity;

import com.tlc.validator.type.Email;
import com.tlc.validator.type.Group;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 * Represents an email address entity that implements ReceiverIdentity to identify the recipient of an email.
 * And provides a foundational structure for modeling email-related functionalities
 * in both sending and receiving scenarios.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ReceiverIdentity
 * @see EmailModel
 */
public class EmailAddress implements ReceiverIdentity, EmailModel, Serializable {

    private Long id;

    @Email(groups = Group.Create.class, message = "i18n.notification.email.invalid.format")
    private String emailAddress;

    public EmailAddress() {
    }

    public EmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public EmailAddress(final Long id, final String emailAddress) {
        this.id = id;
        this.emailAddress = emailAddress;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public Long id() {
        return id;
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
        final EmailAddress address = (EmailAddress) object;

        return Objects.equals(this.emailAddress, address.emailAddress);
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