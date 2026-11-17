package com.tlc.email.common.model.address;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.notification.model.identity.ReceiverIdentity;
import jakarta.mail.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * A model that represents the email recipients.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EmailRecipients implements ReceiverIdentity {

    private Map<AddressType, Collection<EmailAddress>> recipients;

    public Map<AddressType, Collection<EmailAddress>> get() {
        return recipients;
    }

    public void set(final Map<AddressType, Collection<EmailAddress>> recipients) {
        this.recipients = recipients;
    }

    public void addRecipients(final AddressType addressType, final Collection<EmailAddress> newEmailAddresses) {
        this.recipients = Objects.requireNonNullElse(this.recipients, new HashMap<>());
        final Collection<EmailAddress> emailAddresses = new HashSet<>(newEmailAddresses);
        final Collection<EmailAddress> addresses = new HashSet<>(recipients.getOrDefault(addressType, Collections.emptyList()));

        addresses.addAll(emailAddresses);
        recipients.put(addressType, addresses);
    }

    /**
     * <p>
     * Adds a recipient of type {@link AddressType#TO} using the provided email address string.
     * </p>
     *
     * @param emailAddress Represents the recipient's email address
     */
    public void addToRecipient(final String emailAddress) {
        addRecipient(AddressType.TO, new EmailAddress(emailAddress));
    }

    /**
     * <p>
     * Adds a recipient of type {@link AddressType#TO} using the provided email address string.
     * </p>
     *
     * @param emailAddress Represents the recipient's email address
     */
    public void addCcRecipient(final String emailAddress) {
        addRecipient(AddressType.CC, new EmailAddress(emailAddress));
    }

    /**
     * <p>
     * Adds a recipient of type {@link AddressType#TO} using the provided email address string.
     * </p>
     *
     * @param emailAddress Represents the recipient's email address
     */
    public void addBccRecipient(final String emailAddress) {
        addRecipient(AddressType.BCC, new EmailAddress(emailAddress));
    }

    /**
     * <p>
     * Adds a recipient of type {@link AddressType#TO} using the provided {@link EmailAddress} object.
     * </p>
     *
     * @param emailAddress Represents the recipient's {@link EmailAddress}
     */
    public void addToRecipient(final EmailAddress emailAddress) {
        addRecipient(AddressType.TO, emailAddress);
    }

    /**
     * <p>
     * Adds a recipient of the specified {@link AddressType} using the provided {@link EmailAddress}.
     * </p>
     *
     * @param addressType  represents the type of the recipient (e.g., TO, CC, BCC)
     * @param emailAddress represents the recipient's {@link EmailAddress}
     */
    public void addRecipient(final AddressType addressType, final EmailAddress emailAddress) {
        addRecipients(addressType, Collections.singleton(emailAddress));
    }

    /**
     * <p>
     * Converts and gets the recipient type according to given address type
     * </p>
     */
    public static Message.RecipientType fetchRecipientType(final AddressType type) {
        return switch (type) {
            case TO -> Message.RecipientType.TO;
            case BCC -> Message.RecipientType.BCC;
            case CC -> Message.RecipientType.CC;
            default -> throw ErrorCode.get(ErrorCodeProvider.UNKNOWN);
        };
    }

    /**
     * <p>
     * Gets the first To recipient
     * </p>
     *
     * @return {@link EmailAddress}
     */
    public EmailAddress getFirstToRecipient() {
        return getFirstRecipient(AddressType.TO);
    }

    /**
     * <p>
     * Gets the first CC recipient
     * </p>
     *
     * @return {@link EmailAddress}
     */
    public EmailAddress getFirstCcRecipient() {
        return getFirstRecipient(AddressType.CC);
    }

    /**
     * <p>
     * Gets the first BCC recipient
     * </p>
     *
     * @return {@link EmailAddress}
     */
    public EmailAddress getFirstBccRecipient() {
        return getFirstRecipient(AddressType.BCC);
    }

    /**
     * <p>
     * Gets the first recipient based on the address type
     * </p>
     *
     * @param addressType Represents the {@link AddressType}
     * @return {@link EmailAddress}
     */
    private EmailAddress getFirstRecipient(final AddressType addressType) {
        final Collection<EmailAddress> recipients = switch (addressType) {
            case TO -> toRecipients();
            case CC -> ccRecipients();
            case BCC -> bccRecipients();
            default -> throw ErrorCode.get(ErrorCodeProvider.UNKNOWN);
        };

        for (final EmailAddress recipient : recipients) {
            return recipient;
        }
        return null;
    }

    /**
     * <p>
     * Formats the recipients email from comma separated form
     * </p>
     *
     * @param recipient Represents the comma separated email ids
     * @return {@link Collection<EmailAddress>}
     */
    public static Collection<EmailAddress> formatRecipients(final String recipient) {
        final String formattedRecipient = recipient.substring(recipient.indexOf("[") + 1, recipient.indexOf("]"));
        final String[] emailAddress = formattedRecipient.split(",");
        final Collection<EmailAddress> addresses = new HashSet<>();

        for (final String email : emailAddress) {

            if (!email.isBlank()) {
                addresses.add(new EmailAddress(email.trim()));
            }
        }

        return addresses;
    }

    public Collection<EmailAddress> toRecipients() {
        return getEmailAddresses(AddressType.TO);
    }

    public Collection<String> getToRecipients() {
        return this.toRecipients().stream().map(EmailAddress::getEmailAddress).collect(Collectors.toSet());
    }

    public Collection<String> getCcRecipients() {
        return this.ccRecipients().stream().map(EmailAddress::getEmailAddress).collect(Collectors.toSet());
    }

    public Collection<String> getBccRecipients() {
        return this.bccRecipients().stream().map(EmailAddress::getEmailAddress).collect(Collectors.toSet());
    }

    public Collection<EmailAddress> ccRecipients() {
        return getEmailAddresses(AddressType.CC);
    }

    public Collection<EmailAddress> bccRecipients() {
        return getEmailAddresses(AddressType.BCC);
    }

    /**
     * <p>
     * Retrieves the email addresses for specific address type
     * </p>
     *
     * @param addressType Refers to the {@link AddressType}
     * @return {@link Collection} of {@link EmailAddress}
     */
    private Collection<EmailAddress> getEmailAddresses(final AddressType addressType) {
        return Objects.nonNull(recipients) ? recipients.get(addressType) : Collections.emptyList();
    }

    /**
     * <p>
     * Retrieves all the email addresses
     * </p>
     *
     * @return {@link Collection} of {@link String}
     */
    public Collection<String> getAllEmailAddresses() {
        return Objects.isNull(recipients) ? Collections.emptyList() : recipients.values().stream().flatMap(Collection::stream)
                .map(EmailAddress::getEmailAddress).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Calculate and return the count of recipients in an email
     * </p>
     *
     * @return {@link Integer} total number of recipients
     */
    public int size() {
        final Collection<EmailAddress> to = recipients.get(AddressType.TO);
        final Collection<EmailAddress> cc = recipients.get(AddressType.CC);
        final Collection<EmailAddress> bcc = recipients.get(AddressType.BCC);

        return (Objects.isNull(to) ? 0 : to.size()) +
                (Objects.isNull(cc) ? 0 : cc.size()) +
                (Objects.isNull(bcc) ? 0 : bcc.size());
    }
}