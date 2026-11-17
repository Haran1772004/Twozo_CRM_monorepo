package com.tlc.notification.internal.contact;

import com.tlc.notification.model.contact.ContactConfigurations;
import com.tlc.notification.model.contact.Person;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.contact.ContactService;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the calendar service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class ContactServiceImpl implements ContactService {

    private final ProviderService contactProviderService;
    private final GoogleContactService googleContactService;
    private final OutlookContactService outlookContactService;

    private ContactServiceImpl() {
        contactProviderService = ProviderService.getContactProviderService();
        googleContactService = GoogleContactService.getInstance();
        outlookContactService = OutlookContactService.getInstance();
    }

    private static class Instance {
        private static final ContactServiceImpl INSTANCE = new ContactServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link ContactServiceImpl} Instance
     * </p>
     *
     * @return {@link ContactServiceImpl}
     */
    public static ContactServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param contactConfigurations Represents the {@link ContactConfigurations}
     * @return the collection of {@link Person}
     */
    @Override
    public Map<String, Collection<Person>> retrieve(final ContactConfigurations contactConfigurations) {
        return getService(contactConfigurations.emailAddress()).retrieve(contactConfigurations);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param person       Represents the {@link Person}
     * @param emailAddress Represents the {@link EmailAddress}
     * @return
     */
    @Override
    public String addContact(final Person person, final EmailAddress emailAddress) {
        return getService(emailAddress).addContact(person, emailAddress);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param person       Represents the {@link Person}
     * @param emailAddress Represents the {@link EmailAddress}
     * @return
     */
    @Override
    public String updateContact(final Person person, final EmailAddress emailAddress) {
        return getService(emailAddress).updateContact(person, emailAddress);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param uniqueIdentifier Represents the unique identifier
     * @param emailAddress     Represents the {@link EmailAddress}
     */
    @Override
    public void deleteContact(final String uniqueIdentifier, final EmailAddress emailAddress) {
        getService(emailAddress).deleteContact(uniqueIdentifier, emailAddress);
    }

    /**
     * <p>
     * Gets the appropriate contact service to be done.
     * </p>
     *
     * @param emailAddress Represents the {@link ContactService}
     * @return {@link ContactService} relevant to the provided contact address
     */
    private ContactService getService(final EmailAddress emailAddress) {
        final ContactProvider contactProvider = contactProviderService.findProvider(
                new com.tlc.sync.service.oauth.model.EmailAddress(emailAddress.getEmailId()));

        return switch (contactProvider) {
            case GOOGLE -> googleContactService;
            case OUTLOOK -> outlookContactService;
        };
    }
}