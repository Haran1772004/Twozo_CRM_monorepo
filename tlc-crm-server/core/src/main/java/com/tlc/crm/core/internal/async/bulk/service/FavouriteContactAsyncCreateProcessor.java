package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.features.action.call.manager.FavouriteContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.action.call.model.FavouriteContact;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncCreateProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * <p>
 * Processor responsible for handling async create operations for call {@link FavouriteContact} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class FavouriteContactAsyncCreateProcessor extends AsyncCreateProcessor<FavouriteContact> {

    private final FavouriteContactManager favouriteContactManager;

    FavouriteContactAsyncCreateProcessor() {
        favouriteContactManager = FavouriteContactManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param asyncActionRequest Represents the {@link AsyncActionRequest}
     */
    @Override
    protected void process(final AsyncActionRequest asyncActionRequest) {
        final FavouriteContact favouriteContact = new FavouriteContact();
        final Contact contact = new Contact();

        favouriteContact.setOrgId(asyncActionRequest.orgId());
        favouriteContact.setUserId(asyncActionRequest.userId());

        contact.setId(asyncActionRequest.modelId());
        favouriteContact.setContact(contact);
        favouriteContactManager.create(favouriteContact);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager}
     */
    @Override
    protected AbstractConfigManager<FavouriteContact> getAbstractConfigManager() {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }
}
