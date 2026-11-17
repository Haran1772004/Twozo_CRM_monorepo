package com.tlc.crm.core.action.marketinglist;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.api.marketinglist.model.converter.MarketingListConverter;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.core.internal.marketinglist.status.MarketingListErrorCodes;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.security.permission.annotation.authorizer.MarketingListAuthorizer;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.producer.ProducerStore;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Manages the marketing list related web actions
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/marketing/list/mgmt")
@MarketingListAuthorizer()
public class MarketingListManagement extends AsyncBulkConfigAction<MarketingList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListManagement.class);

    private static final String CONFIG_ACTION_NAME = "marketingList";

    private final MarketingListConverter marketingListConverter;
    private final MarketingListManager marketingListManager;
    private final MarketingListContactManager marketingListContactManager;
    private final ContactManager contactManager;
    private final ProducerStore producerStore;
    private final FieldService fieldService;
    private final OptionService optionService;
    private final Set<String> subscriptionFieldAliases;

    public MarketingListManagement() {
        marketingListConverter = MarketingListConverter.getInstance();
        marketingListManager = MarketingListManager.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
        contactManager = ContactManager.getInstance();
        producerStore = ProducerStore.getInstance();
        fieldService = FieldService.getSecureInstance();
        optionService = OptionService.getInstance();
        subscriptionFieldAliases = Set.of(ContactFields.SUBSCRIPTION_STATUS, ContactFields.UNSUBSCRIBE_REASON, ContactFields.OTHER_UNSUBSCRIBE_REASON);
    }

    /**
     * <p>
     * Handles incoming requests for marketing list operations based on the request type.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest} containing the request data
     * @param response Represents the {@link CrmResponse} to populate with the result
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(MarketingListFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(MarketingListFields.DATA);
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case "list" -> list(orgId, userId, data, response);
            case "addContacts" -> addContacts(orgId, userId, data, response);
            case "transferAndDelete" -> transferAndDelete(orgId, userId, data);
            case "bCopyContacts", "bMoveContacts", "bRemoveContacts" -> bUpdate(request, response, ((CrmEventRequest) request).getEventData(), false);
            case "bDeleteContacts" -> bDelete(request, response);
            case "bUpdateSubscriptionData" -> {
                if (isValidSubscriptionData(orgId, data, response)) {
                    bUpdate(request, response, ((CrmEventRequest) request).getEventData(), false);
                }
            }
        }
    }

    /**
     * <p>
     * Validates the subscription update data when a contact is being marked as unsubscribed.
     * When the subscription status is {@code UNSUBSCRIBED}, an unsubscribe reason is mandatory,
     * and if the reason is {@code OTHER}, the other unsubscribe reason text is also mandatory.
     * Only the subscription related fields are resolved (via {@link FieldService} and
     * {@link OptionService}) instead of converting the whole contact. Any validation failures
     * are loaded onto the response.
     * </p>
     *
     * @param orgId    Represents the org id
     * @param data     Represents the request data containing the update item
     * @param response Represents the {@link CrmResponse} to populate with validation errors
     * @return {@code true} if the subscription update data is valid, {@code false} otherwise
     */
    private boolean isValidSubscriptionData(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonObject updateItem = data.getJsonObject(BulkActionFields.UPDATE_ITEM);
        final Map<String, FieldInfo> fieldsByAlias = fieldService.groupDefaultFieldsByAliasName(orgId, Contact.ENTITY_TYPE, subscriptionFieldAliases);
        final FieldInfo subscriptionStatusField = fieldsByAlias.get(ContactFields.SUBSCRIPTION_STATUS);
        final String subscriptionStatusKey = subscriptionStatusField.idAsString();

        if (!updateItem.containsKey(subscriptionStatusKey) || updateItem.isKeyPresentWithNullValue(subscriptionStatusKey)) {
            return true;
        }

        final Option subscriptionStatusOption = optionService.get(orgId, subscriptionStatusField.id(), updateItem.getLong(subscriptionStatusKey));

        if (Objects.isNull(subscriptionStatusOption)
                || !Objects.equals(Contact.SubscriptionStatus.UNSUBSCRIBED.getName(), subscriptionStatusOption.getOption())) {
            return true;
        }

        final FieldInfo unsubscribeReasonField = fieldsByAlias.get(ContactFields.UNSUBSCRIBE_REASON);
        final String unsubscribeReasonKey = unsubscribeReasonField.idAsString();
        final Collection<ErrorDescriptor> errors = new HashSet<>();
        final Long unsubscribeReasonId = (updateItem.containsKey(unsubscribeReasonKey) && !updateItem.isKeyPresentWithNullValue(unsubscribeReasonKey))
                ? updateItem.getLong(unsubscribeReasonKey) : null;

        if (Objects.isNull(unsubscribeReasonId)) {
            errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.contact.unsubscribeReason.not.found"),
                    null, unsubscribeReasonField.aliasName()));
        } else {
            final Option unsubscribeReasonOption = optionService.get(orgId, unsubscribeReasonField.id(), unsubscribeReasonId);

            if (Objects.nonNull(unsubscribeReasonOption)
                    && Objects.equals(Contact.UnsubscribeReason.OTHER_UNSUBSCRIBE_REASON.getName(), unsubscribeReasonOption.getOption())) {
                final FieldInfo otherUnsubscribeReasonField = fieldsByAlias.get(ContactFields.OTHER_UNSUBSCRIBE_REASON);
                final String otherReasonText = updateItem.optString(otherUnsubscribeReasonField.idAsString(), null);

                if (Objects.isNull(otherReasonText)) {
                    errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.contact.otherUnsubscribeReason.not.found"),
                            null, otherUnsubscribeReasonField.aliasName()));
                }
            }
        }

        if (errors.isEmpty()) {
            return true;
        }

        loadValidationErrors(Action.UPDATE, response, errors);

        return false;
    }

    /**
     * <p>
     * Fetches the list of marketing lists with contact counts
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param userId      Represents the user ID
     * @param requestData Represents the request data
     * @param response    Represents the {@link CrmResponse}
     */
    private void list(final Long orgId, final Long userId, final JsonObject requestData, final CrmResponse response) {
        final int start = requestData.optInt(MarketingListFields.START, NumericalConstant.ONE);
        final int limit = requestData.optInt(MarketingListFields.LIMIT, NumericalConstant.TEN);
        final String searchText = requestData.optString(MarketingListFields.SEARCH_TEXT, null);
        final long totalRecords = marketingListManager.getTotalCount(orgId, searchText);
        final boolean isSearchCriteriaIncluded = Objects.nonNull(searchText) && !searchText.isBlank();
        final JsonArray listArray = Json.array();

        if (totalRecords > 0) {
            final boolean isDeletable = NumericalConstant.ONE < marketingListManager.getTotalCount(orgId);
            final boolean isFirstPageWithoutSearch = start == NumericalConstant.ONE && (!isSearchCriteriaIncluded);
            final Long preferredMarketingListId;

            if (isSearchCriteriaIncluded) {
                preferredMarketingListId = null;
            } else {
                preferredMarketingListId = marketingListManager.getPreferredMarketingListId(orgId, userId).orElseGet(() -> null);
            }

            if (isFirstPageWithoutSearch) {

                if (Objects.nonNull(preferredMarketingListId)) {
                    final Optional<Pair<MarketingList, Long>> preferredMarketingList = marketingListContactManager.fetchWithContactCount(orgId, preferredMarketingListId);

                    preferredMarketingList.ifPresent(pair -> listArray.put(buildMarketingListJson(pair, isDeletable, true)));
                    final int remainingLimit = preferredMarketingList.isPresent() ? limit - NumericalConstant.ONE : limit;

                    if (remainingLimit > NumericalConstant.ZERO) {
                        final LimitConfig limitConfig = new LimitConfig(start, remainingLimit);
                        final Collection<Pair<MarketingList, Long>> remainingLists = marketingListContactManager.getListWithContactCounts(orgId, limitConfig, null, preferredMarketingListId);

                        remainingLists.forEach(pair -> listArray.put(buildMarketingListJson(pair, isDeletable, false)));
                    }
                } else {
                    final LimitConfig limitConfig = new LimitConfig(start, limit);
                    final Collection<Pair<MarketingList, Long>> marketingListsWithCounts = marketingListContactManager.getListWithContactCounts(orgId, limitConfig, null, null);
                    boolean isFirst = true;

                    for (final Pair<MarketingList, Long> pair : marketingListsWithCounts) {
                        listArray.put(buildMarketingListJson(pair, isDeletable, isFirst));
                        isFirst = false;
                    }
                }
            } else {
                final Collection<Pair<MarketingList, Long>> marketingListsWithCounts;

                if (isSearchCriteriaIncluded) {
                    marketingListsWithCounts = marketingListContactManager.getListWithContactCounts(orgId, new LimitConfig(start, limit), searchText, null);
                } else {
                    if (Objects.nonNull(preferredMarketingListId)) {
                        marketingListsWithCounts = marketingListContactManager.getListWithContactCounts(orgId,
                                new LimitConfig(start - NumericalConstant.ONE, limit), null, preferredMarketingListId);
                    } else {
                        marketingListsWithCounts = marketingListContactManager.getListWithContactCounts(orgId, new LimitConfig(start, limit), null, null);
                    }
                }

                marketingListsWithCounts.forEach(pair -> listArray.put(buildMarketingListJson(pair, isDeletable, false)));
            }
        }

        response.put(MarketingListFields.TOTAL_RECORDS, totalRecords);
        response.put(MarketingListFields.HAS_MORE, (start + limit) < totalRecords);
        response.put(MarketingListFields.LIST, listArray);
    }

    /**
     * <p>
     * Builds a JSON object for a marketing list with contact count
     * </p>
     *
     * @param marketingListPair        The pair containing marketing list and contact count
     * @param isDeletable Whether the marketing list is deletable
     * @param isDefault   Whether this marketing list is the default one
     * @return The JSON object representing the marketing list
     */
    private JsonObject buildMarketingListJson(final Pair<MarketingList, Long> marketingListPair, final boolean isDeletable, final boolean isDefault) {
        final MarketingList marketingList = marketingListPair.getKey();
        final JsonObject marketingListJson = Json.object();

        marketingListJson.put(MarketingListFields.ID, marketingList.id());
        marketingListJson.put(MarketingListFields.NAME, marketingList.getName());
        marketingListJson.put(MarketingListFields.COUNT, marketingListPair.getValue());
        marketingListJson.put(MarketingListFields.IS_DELETABLE, isDeletable);

        if (isDefault) {
            marketingListJson.put(MarketingListFields.IS_DEFAULT, true);
        }

        return marketingListJson;
    }

    /**
     * <p>
     * Adds contacts to the specified marketing list
     * </p>
     * @param orgId       Represents the org id
     * @param userId      Represents the user id
     * @param requestData Represents the request data
     * @param response    Represents the {@link CrmResponse}
     */
    private void addContacts(final Long orgId, final Long userId, final JsonObject requestData, final CrmResponse response) {
        final Long marketingListId = requestData.getLong(MarketingListFields.MARKETING_LIST_ID);
        final Collection<Long> contactIds = (Collection<Long>) requestData.get(MarketingListFields.CONTACT_IDS);

        if (!marketingListManager.exists(orgId, marketingListId)) {
            throw ErrorCode.get(MarketingListErrorCodes.MARKETING_LIST_NOT_FOUND,
                    "i18n.marketing.list.not.found");
        }

        if (!contactManager.exists(orgId, contactIds)) {
            throw ErrorCode.get(MarketingListErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }

        marketingListContactManager.addContacts(orgId, userId, marketingListId, contactIds);
        response.put(MarketingListFields.MESSAGE, response.resolveI18nPlaceholder("i18n.marketing.list.contacts.added.successfully"));
    }

    /**
     * <p>
     * Transfers the contacts of the marketing list to the targeting marketing list and deletes it
     * </p>
     *
     * @param orgId       Represents the org id
     * @param userId      Represents the user id performing the transfer
     * @param requestData Represents the request data
     */
    private void transferAndDelete(final Long orgId, final Long userId, final JsonObject requestData) {
        final Long sourceMarketingListId = requestData.getLong(MarketingListFields.SOURCE_MARKETING_LIST_ID);
        final boolean containsTargetMarketingList = requestData.containsKey(MarketingListFields.TARGET_MARKETING_LIST_ID);
        final Set<Long> marketingListIds = new HashSet<>(NumericalConstant.TWO);
        final Long targetMarketingListId;

        marketingListIds.add(sourceMarketingListId);

        if (containsTargetMarketingList) {
            targetMarketingListId = requestData.getLong(MarketingListFields.TARGET_MARKETING_LIST_ID);
            marketingListIds.add(targetMarketingListId);
        } else {
            targetMarketingListId = null;
        }

        if (!marketingListManager.exists(orgId, marketingListIds)) {
            throw ErrorCode.get(MarketingListErrorCodes.MARKETING_LIST_NOT_FOUND, "i18n.marketing.list.not.found");
        }

        if (!containsTargetMarketingList && !marketingListManager.existsExcluding(orgId, sourceMarketingListId)) {
            throw ErrorCode.get(MarketingListErrorCodes.FAILED_TO_DELETE_MARKETING_LIST, "i18n.marketing.list.deletion.failed");
        }

        if (!containsTargetMarketingList) {
            final MarketingList marketingList = new MarketingList();

            marketingList.setOrgId(orgId);
            marketingList.setId(sourceMarketingListId);
            marketingListManager.delete(marketingList);
        } else if (!Objects.equals(sourceMarketingListId, targetMarketingListId)) {
            publishTransferEvent(orgId, userId, sourceMarketingListId, targetMarketingListId);
        }
    }

    /**
     * <p>
     * Publishes the marketing list transfer event to Kafka
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param userId                Represents the user id
     * @param sourceMarketingListId Represents the source marketing list id
     * @param targetMarketingListId Represents the target marketing list id
     */
    private void publishTransferEvent(final Long orgId, final Long userId, final Long sourceMarketingListId, final Long targetMarketingListId) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(MarketingListFields.ORG_ID, orgId);
        jsonObject.put(MarketingListFields.USER_ID, userId);
        jsonObject.put(MarketingListFields.SOURCE_MARKETING_LIST_ID, sourceMarketingListId);
        jsonObject.put(MarketingListFields.TARGET_MARKETING_LIST_ID, targetMarketingListId);

        final SubscriptionTopic transferEventTopic = CoreQueueTopics.getMarketingListContactTransferEventTopic(QueueModel.Type.KAFKA);
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s", sourceMarketingListId, orgId, userId).getBytes());
        final Optional<Producer<byte[]>> optionalProducer = producerStore.getProducer(transferEventTopic);

        optionalProducer.ifPresentOrElse(producer -> {
            producer.send(new StreamMessage<>(requestId, transferEventTopic, jsonObject.getBytes()));
            LOGGER.info("Marketing List: Data published for marketing list transfer event. Org ID: {}, User ID: {}, Source marketing list ID: {}, Target marketing list ID:{}}",
                    orgId, userId, sourceMarketingListId, targetMarketingListId);
        }, () -> LOGGER.error("Marketing List: Kafka producer not found for the topic:{}", transferEventTopic.getName()));
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link ConfigManager}
     */
    @Override
    public ConfigManager<MarketingList> getConfigManager() {
        return MarketingListManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link MarketingList}
     */
    @Override
    public MarketingList convert(final CrmRequest request, final JsonObject jsonObject) {
        return marketingListConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link MarketingList}
     */
    @Override
    public MarketingList convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Organization id
     * @param userId Represents the user id.
     * @param action Perform action like create, update or delete
     * @param models Collection of models
     * @return true if the model is valid, false otherwise
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<MarketingList> models) {
        switch (action) {
            case CREATE, UPDATE -> {
                final MarketingList marketingList = models.stream().findFirst().get();

                if (marketingListManager.existsByName(orgId, marketingList.getName(), marketingList.id())) {
                    throw ErrorCode.get(MarketingListErrorCodes.MARKETING_LIST_NAME_ALREADY_EXIST, "i18n.marketing.list.name.already.exist");
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param requestJson Represents the request json data to be published
     * @return the collection of filtered ids
     */
    @Override
    protected Collection<Long> getFilteredIds(final long orgId, final long userId, final JsonObject requestJson) {
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final boolean isAllSelected = data.optBoolean(IS_ALL_SELECTED);
        final JsonArray excludedArray = data.optJsonArray(EXCLUDED_IDS);
        final Collection<Long> contactIds = new HashSet<>();
        final Collection<Long> excludedContactIds = new HashSet<>();

        if (!isAllSelected) {
            final JsonArray idsArray = data.getJsonArray(ActionFields.IDS);

            idsArray.forEach(id -> contactIds.add((Long) id));

            return contactIds;
        }

        if (Objects.nonNull(excludedArray)) {
            excludedArray.forEach(id -> excludedContactIds.add((Long) id));
        }

        return marketingListContactManager.getContactIds(orgId, data.getLong(MarketingListFields.SOURCE_MARKETING_LIST_ID), excludedContactIds, null);
    }
}
