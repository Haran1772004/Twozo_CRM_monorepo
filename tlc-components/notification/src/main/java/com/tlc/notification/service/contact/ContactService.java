package com.tlc.notification.service.contact;

import com.tlc.notification.internal.contact.ContactServiceImpl;
import com.tlc.notification.model.contact.ContactConfigurations;
import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.email.EmailAddress;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the contact related services in various providers
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactService {

    /**
     * <p>
     * Gets the instance for {@link ContactService}.
     * </p>
     *
     * @return {@link ContactService}
     */
    static ContactService getInstance() {
        return ContactServiceImpl.getInstance();
    }

    short LIMIT = 11;

    /**
     * <p>
     * Retrieves the contacts that are present in the provider
     * </p>
     *
     * @param contactConfigurations Represents the {@link ContactConfigurations}
     * @return the collection of {@link Person}
     */
    Map<String, Collection<Person>> retrieve(final ContactConfigurations contactConfigurations);

    /**
     * <p>
     * Adds a contact in provider with provided details
     * </p>
     *
     * @param person       Represents the {@link Person}
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the unique identifier
     */
    String addContact(final Person person, final EmailAddress emailAddress);

    /**
     * <p>
     * Updates the contact in provider with provided details
     * </p>
     *
     * @param person       Represents the {@link Person}
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the unique identifier
     */
    String updateContact(final Person person, final EmailAddress emailAddress);

    /**
     * <p>
     * Delete a contact in provider with provided details
     * </p>
     *
     * @param uniqueIdentifier Represents the unique identifier
     * @param emailAddress     Represents the {@link EmailAddress}
     */
    void deleteContact(final String uniqueIdentifier, final EmailAddress emailAddress);
}