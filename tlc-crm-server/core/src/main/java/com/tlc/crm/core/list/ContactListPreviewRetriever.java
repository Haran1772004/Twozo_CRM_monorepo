package com.tlc.crm.core.list;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.ListPreviewFetchRequest;
import com.tlc.crm.entity.list.model.ListPreviewResponse;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the service for retrieving contact related preview details to be loaded into the contact list response
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ContactListPreviewRetriever extends AbstractListPreviewRetriever {

    private final ContactManager contactManager;

    private ContactListPreviewRetriever() {
        this.contactManager = ContactManager.getInstance();
    }

    private static class Instance {
        private static final ContactListPreviewRetriever CONTACT_LIST_PREVIEW_RETRIEVER =  new ContactListPreviewRetriever();
    }

    /**
     * <p>
     * Gets the instance of {@link ContactListPreviewRetriever}
     * </p>
     *
     * @return The instance of {@link ContactListPreviewRetriever}
     */
    public static ContactListPreviewRetriever getInstance() {
        return Instance.CONTACT_LIST_PREVIEW_RETRIEVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param listPreviewFetchRequest Represents the {@link ListPreviewFetchRequest}
     * @return The optional {@link ListPreviewResponse}
     */
    @Override
    public Optional<ListPreviewResponse> fetchPreviewResponse(final ListPreviewFetchRequest listPreviewFetchRequest) {
        final Collection<Long> contactIds = listPreviewFetchRequest.entityIds();
        final DataContainer dataContainer = contactManager.getPreviewDetails(listPreviewFetchRequest.orgId(), contactIds);

        if (!dataContainer.isEmpty()) {
            return fetchContactRelatedListPreview(Source.CONTACT, contactIds, dataContainer);
        }
        return Optional.empty();
    }
}