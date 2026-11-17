package com.tlc.crm.core.notification.email.api.model;

import com.tlc.crm.core.api.contact.model.Contact;

import com.tlc.crm.entity.models.type.Email;

import com.tlc.notification.model.identity.ReceiverIdentity;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * An email recipient model for storing and getting attributes for an contact
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class ContactRecipients implements ReceiverIdentity {

    private Map<AddressType, Collection<Contact>> contactRecipients;

    public Map<AddressType, Collection<Contact>> getContacts() {
        return contactRecipients;
    }

    public void setContacts(final Map<AddressType, Collection<Contact>> contactRecipients) {
        this.contactRecipients = contactRecipients;
    }

    public void addContacts(final AddressType addressType, final Collection<Contact> newContacts) {
        this.contactRecipients = this.contactRecipients == null ? new HashMap<>() : contactRecipients;
        final Collection<Contact> contacts = new HashSet<>(newContacts);
        final Collection<Contact> existingContacts = new HashSet<>(contactRecipients.getOrDefault(addressType, Collections.emptyList()));

        existingContacts.addAll(contacts);
        contactRecipients.put(addressType, existingContacts);
    }

    public void addContact(final AddressType addressType, final Contact contact) {
        addContacts(addressType, Collections.singleton(contact));
    }

    /**
     * <p>
     * Adds the {@link Contact} for an respective contact email id.
     * </p>
     *
     * @param addressType    Represents the {@link AddressType}
     * @param contactEmailId Represents the contact email id
     */
    public void addContact(final AddressType addressType, final Long contactEmailId) {
        final Contact contact = new Contact();
        final Email email = new Email();

        email.setId(contactEmailId);
        contact.setPrimaryEmail(email);

        this.contactRecipients = this.contactRecipients == null ? new HashMap<>() : contactRecipients;

        contactRecipients.computeIfAbsent(addressType, contacts -> new HashSet<>()).add(contact);
    }

    /**
     * <p>
     * Gets the first To recipient
     * </p>
     *
     * @return {@link Contact}
     */
    public Contact getFirstToRecipient() {
        return getFirstRecipient(AddressType.TO);
    }

    /**
     * <p>
     * Gets the first CC recipient
     * </p>
     *
     * @return {@link Contact}
     */
    public Contact getFirstCcRecipient() {
        return getFirstRecipient(AddressType.CC);
    }

    /**
     * <p>
     * Gets the first CC recipient
     * </p>
     *
     * @return {@link Contact}
     */
    public Contact getFirstBccRecipient() {
        return getFirstRecipient(AddressType.BCC);
    }

    /**
     * <p>
     * Gets the first contact recipient based on the address type
     * </p>
     *
     * @param addressType Represents the {@link AddressType}
     * @return {@link Contact}recipients
     */
    private Contact getFirstRecipient(final AddressType addressType) {
        final Map<AddressType, Collection<Contact>> recipients = getContacts();
        final Collection<Contact> contacts = Objects.nonNull(recipients) ? recipients.get(addressType) : null;

        if (Objects.nonNull(contacts)) {

            for (final Contact contact : contacts) {
                return contact;
            }
        }
        return null;
    }

    /**
     * <p>
     * Gets the contact email ids of all recipients.
     * </p>
     *
     * @return {@link Collection} of email ids
     */
    public Collection<Long> getEmailIds() {
        return Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.values().stream().flatMap(Collection::stream)
                                                                             .map(Contact::getPrimaryEmail).map(Email::id).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the contact ids of all recipients.
     * </p>
     *
     * @return {@link Collection} of contact ids
     */
    public Collection<Long> getContactIds() {
        return Objects.isNull(contactRecipients) ? Collections.emptyList() : contactRecipients.values().stream().flatMap(Collection::stream)
                                                                             .map(Contact::id).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the contact ids based on given {@link AddressType}.
     * </p>
     *
     * @param addressType Represents the {@link AddressType}
     * @return {@link Collection} of contact ids
     */
    private Collection<Long> getContactIds(final AddressType addressType) {
        final Collection<Contact> contacts = contactRecipients.get(addressType);

        return contacts == null ? Collections.emptyList() : contacts.stream().map(Contact::id).toList();
    }

    /**
     * <p>
     * Gets the contact email ids based on given {@link AddressType}.
     * </p>
     *
     * @param addressType Represents the {@link AddressType}
     * @return {@link Collection} of email ids
     */
    private Collection<Long> getEmailIds(final AddressType addressType) {
        final Collection<Contact> contacts = Objects.isNull(contactRecipients) ? null : contactRecipients.get(addressType);

        return contacts == null ? Collections.emptyList() : contacts.stream().map(Contact::getPrimaryEmail).map(Email::id).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the {@link EmailAddress} of all recipients.
     * </p>
     *
     * @return {@link Collection} of email ids
     */
    public Map<AddressType, Collection<EmailAddress>> getEmailAddresses() {
        return Objects.isNull(contactRecipients) ? new HashMap<>() : contactRecipients.entrySet().stream()
                                                                     .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(contact -> {
                                                                                 final Email primaryEmail = contact.getPrimaryEmail();

                                                                                 return new EmailAddress(primaryEmail.id(), primaryEmail.getValue());
                                                                             }).toList()
                                                                     ));
    }

    /**
     * <p>
     * Gets the {@link EmailAddress} of all recipients.
     * </p>
     *
     * @return {@link Collection} of email ids
     */
    public Collection<EmailAddress> getAddresses() {

        if (Objects.isNull(contactRecipients)) {
            return Collections.emptyList();
        }

        return contactRecipients.values().stream().flatMap(Collection::stream)
                .map(contact -> {
                    final Email primaryEmail = contact.getPrimaryEmail();

                    return new EmailAddress(primaryEmail.id(), primaryEmail.getValue());
                }).collect(Collectors.toList());
    }

    /**
     * <p>
     * Gets the "to" recipient contact ids.
     * </p>
     *
     * @return contact ids of "to" recipients
     */
    public Collection<Long> toContactIds() {
        return getContactIds(AddressType.TO);
    }

    /**
     * <p>
     * Gets the "to" recipient contact email ids.
     * </p>
     *
     * @return contact email ids of "to" recipients
     */
    public Collection<Long> toEmailIds() {
        return getEmailIds(AddressType.TO);
    }

    /**
     * <p>
     * Gets the "bcc" recipient contact ids.
     * </p>
     *
     * @return contact ids of "bcc" recipients
     */
    public Collection<Long> bccContactIds() {
        return getContactIds(AddressType.BCC);
    }

    /**
     * <p>
     * Gets the "bcc" recipient contact email ids.
     * </p>
     *
     * @return contact email ids of "bcc" recipients
     */
    public Collection<Long> bccEmailIds() {
        return getEmailIds(AddressType.BCC);
    }

    /**
     * <p>
     * Gets the "cc" recipient contact ids.
     * </p>
     *
     * @return contact ids of "cc" recipients
     */
    public Collection<Long> ccContactIds() {
        return getContactIds(AddressType.CC);
    }

    /**
     * <p>
     * Gets the "cc" recipient contact email ids.
     * </p>
     *
     * @return contact email ids of "cc" recipients
     */
    public Collection<Long> ccEmailIds() {
        return getEmailIds(AddressType.CC);
    }

    /**
     * <p>
     * Check whether it has the recipients or not.
     * </p>
     *
     * @return boolean whether it has the recipients or not.
     */
    public boolean hasRecipients() {
        return Objects.nonNull(contactRecipients) && !contactRecipients.isEmpty();
    }
}