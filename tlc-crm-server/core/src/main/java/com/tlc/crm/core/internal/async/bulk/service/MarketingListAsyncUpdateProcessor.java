package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.BeanUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.entity.marketinglist.model.MarketingList;

import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * <p>
 * Async processor for processing marketing list updates
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingListAsyncUpdateProcessor extends AsyncUpdateProcessor<MarketingList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListAsyncUpdateProcessor.class);

    private final MarketingListContactManager marketingListContactManager;
    private final ContactManager contactManager;
    private final ContactConverter contactConverter;

    public MarketingListAsyncUpdateProcessor() {
        marketingListContactManager = MarketingListContactManager.getInstance();
        contactManager = ContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long contactId = asyncActionRequest.modelId();

        try {
            final JsonObject requestJson = asyncActionRequest.requestJson();
            final String type = requestJson.getString(MarketingListFields.TYPE);
            final JsonObject data = requestJson.getJsonObject(MarketingListFields.DATA);
            final Long sourceMarketingListId = data.getLong(MarketingListFields.SOURCE_MARKETING_LIST_ID);
            final Long targetMarketingListId = data.containsKey(MarketingListFields.TARGET_MARKETING_LIST_ID)
                    ? data.getLong(MarketingListFields.TARGET_MARKETING_LIST_ID) : null;

            switch (type) {
                case BulkActionFields.BULK_COPY_CONTACTS -> copyContact(orgId, userId, contactId, targetMarketingListId);
                case BulkActionFields.BULK_MOVE_CONTACTS -> moveContact(orgId, userId, contactId, sourceMarketingListId, targetMarketingListId);
                case BulkActionFields.BULK_REMOVE_CONTACTS -> removeContact(orgId, contactId, sourceMarketingListId);
                case BulkActionFields.BULK_UPDATE_SUBSCRIPTION_DATA -> updateSubscriptionData(orgId, userId, contactId, data);
            }
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Marketing List Bulk Action: Bulk update failed. Org ID: {}, User ID: {}, Contact ID: {}, Error: {}",
                    orgId, userId, contactId, exception.getMessage());
        }
    }

    /**
     * <p>
     * Copies the contact to the target marketing list
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param userId                Represents the user id
     * @param contactId             Represents the contact id
     * @param targetMarketingListId Represents the target marketing list id
     */
    private void copyContact(final Long orgId, final Long userId, final Long contactId, final Long targetMarketingListId) {
        marketingListContactManager.addContacts(orgId, userId, targetMarketingListId, List.of(contactId));
    }

    /**
     * <p>
     * Moves the contact from source marketing list to target marketing list
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param userId                Represents the user id
     * @param contactId             Represents the contact id
     * @param sourceMarketingListId Represents the source marketing list id
     * @param targetMarketingListId Represents the target marketing list id
     */
    private void moveContact(final Long orgId, final Long userId, final Long contactId,
                             final Long sourceMarketingListId, final Long targetMarketingListId) {
        marketingListContactManager.moveContact(orgId, userId, sourceMarketingListId, targetMarketingListId, contactId);
    }

    /**
     * <p>
     * Removes the contact from the source marketing list
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param contactId             Represents the contact id
     * @param sourceMarketingListId Represents the source marketing list id
     */
    private void removeContact(final Long orgId, final Long contactId, final Long sourceMarketingListId) {
        marketingListContactManager.removeContacts(orgId, sourceMarketingListId, List.of(contactId));
    }

    /**
     * <p>
     * Updates the subscription field data for the contact in the marketing list based on the given data json
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param userId    Represents the user id
     * @param contactId Represents the contact id
     * @param dataJson  Represents the json data containing the updated subscription field data
     */
    private void updateSubscriptionData(long orgId, long userId, long contactId, JsonObject dataJson) {
        final JsonObject updateJson = dataJson.getJsonObject(BulkActionFields.UPDATE_ITEM);
        final EventData eventData = new SecureEventData(orgId, userId);
        final UserRegionalPreference userRegionalPreference = ((CrmUser) LoggedInUserInfo.getUser()).getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final Locale locale = Locale.forLanguageTag(userRegionalPreference.getLanguage().getName());
        final Contact contact = contactManager.get(orgId, contactId);

        contactManager.loadSummaryDetails(contact.orgId(), contact);
        eventData.setPreviousValues(List.of(cloneContact(orgId, contact)));
        eventData.setAction(Action.UPDATE);
        eventData.setValues(List.of(contact));

        mapJsonToContact(new ConverterConfig(orgId, userId, locale, true, updateJson), contact);
        eventDataHandler.handleEvents(eventData, Event.Type.PRE);
        contactManager.update(contact);
        eventDataHandler.handleEvents(eventData, Event.Type.POST);
    }

    /**
     * <p>
     * Maps the given json data to the contact model based on the converter config
     * </p>
     *
     * @param converterConfig Represents the converter config containing necessary information for conversion
     * @param contact         Represents the contact model to which the data needs to be mapped
     */
    private void mapJsonToContact(final ConverterConfig converterConfig, final Contact contact) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            contactConverter.setJsonDataToModel(converterConfig, contact, dataJson);
        } else {
            contactConverter.convert(converterConfig, dataJson);
        }
    }

    /**
     * <p>
     * Clones the given contact model and returns the cloned instance
     * </p>
     *
     * @param orgId   Represents the organization id for which the contact belongs to
     * @param contact Represents the contact model to be cloned
     * @return the cloned instance of the {@link Contact}
     */
    private Contact cloneContact(final Long orgId, final Contact contact) {
        final Contact newContact = BeanUtil.clone(contact);

        newContact.setOrgId(orgId);
        newContact.setId(contact.id());

        return newContact;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final MarketingList model) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractConfigManager<MarketingList> getAbstractConfigManager() {
        return null;
    }
}