package com.tlc.crm.entity.action;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.bulk.handler.AsyncBulkActionHandler;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.service.EntityBulkActionPublisher;
import com.tlc.crm.entity.store.AsyncBulkActionTopicStore;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Abstract class for handling asynchronous bulk operations
 * </p>
 *
 * @param <T> The type of model being processed
 * @author Kishore J
 * @version 1.0
 */
public abstract class AsyncBulkConfigAction<T extends TlcModel> extends CrmConfigAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncBulkConfigAction.class);

    protected static final String IS_ALL_SELECTED = "isAllSelected";
    protected static final String EXCLUDED_IDS = "excludedIds";
    protected static final String BULK_UPDATE_RESPONSE_MESSAGE = "Processing bulk update operation";
    protected static final String BULK_DELETE_RESPONSE_MESSAGE = "Processing bulk delete operation";

    protected final ProducerStore producerStore;
    protected final AsyncBulkActionTopicStore asyncBulkActionTopicStore;

    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final AsyncBulkActionHandler<TlcModel> asyncBulkActionHandler;
    private final EntityBulkActionPublisher entityBulkActionPublisher;

    protected AsyncBulkConfigAction() {
        filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        producerStore = ProducerStore.getInstance();
        asyncBulkActionTopicStore = AsyncBulkActionTopicStore.getInstance();
        asyncBulkActionHandler = AsyncBulkActionHandler.getInstance();
        entityBulkActionPublisher = EntityBulkActionPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     * @param isPartial   A flag indicating whether the update is partial or full
     */
    @Override
    protected void bUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse,
                           final EventData eventData, final boolean isPartial) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final Collection<Long> modelIds = getFilteredIds(orgId, userId, requestJson);

        publish(orgId, userId, requestJson, modelIds, Action.UPDATE);

        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%d", orgId, userId,
                getConfigActionName(), Instant.now().toEpochMilli()).getBytes());

        crmResponse.put(ActionFields.ID, requestId);
        crmResponse.put(ActionFields.MESSAGE, BULK_UPDATE_RESPONSE_MESSAGE);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     */
    @Override
    protected void bDelete(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final Collection<Long> modelIds = getFilteredIds(orgId, userId, requestJson);

        publish(orgId, userId, requestJson, modelIds, Action.DELETE);

        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%d", orgId, userId,
                getConfigActionName(), Instant.now().toEpochMilli()).getBytes());

        crmResponse.put(ActionFields.ID, requestId);
        crmResponse.put(ActionFields.MESSAGE, BULK_DELETE_RESPONSE_MESSAGE);
    }

    /**
     * <p>
     * Fetches a filtered collection of ids based on the request
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param requestJson Represents the request json data to be published
     * @return A collection of filtered ids
     */
    protected Collection<Long> getFilteredIds(final long orgId, final long userId, final JsonObject requestJson) {
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final boolean isAllSelected = data.optBoolean(IS_ALL_SELECTED);
        final JsonArray excludedArray = data.optJsonArray(EXCLUDED_IDS);
        final Collection<Long> ids = new HashSet<>();
        final Collection<Long> excludedIds = new HashSet<>();

        if (!isAllSelected) {
            final JsonArray idsArray = data.getJsonArray(ActionFields.IDS);

            idsArray.forEach(id -> ids.add((Long) id));
        }

        if (Objects.nonNull(excludedArray)) {
            excludedArray.forEach(id -> excludedIds.add((Long) id));
        }
        final Provider provider = getProvider(getConfigActionName());
        final Collection<FilterCriteria> filterCriteriaList = Objects.isNull(provider) ? Collections.emptyList() : filterCriteriaBuilder.constructFilterCriteria(orgId, userId, provider, data);

        return asyncBulkActionHandler.getFilteredIds(orgId, ids, excludedIds, provider, filterCriteriaList, (AbstractConfigManager<TlcModel>) getConfigManager());
    }

    /**
     * <p>
     * Publishes data to a subscription topic for the given organization and user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents an user id
     * @param requestJson Represents the request json data to be published
     * @param modelIds    Which contains the collection of model ids
     * @param action      The action type associated with the request
     */
    protected void publish(final long orgId, final long userId, final JsonObject requestJson, final Collection<Long> modelIds, final Action action) {
        final String configActionName = getConfigActionName();

        LOGGER.info("Bulk Action: Bulk event started for {}, Action: {}", configActionName, action);

        if (!modelIds.isEmpty()) {
            final Optional<SubscriptionTopic> subscriptionTopicOptional = asyncBulkActionTopicStore.getSubscriptionTopic(configActionName, action);

            subscriptionTopicOptional.ifPresent(subscriptionTopic -> entityBulkActionPublisher.publish(orgId, userId, requestJson, modelIds, subscriptionTopic));
            LOGGER.info("Bulk Action: Published the bulk event data for {}, Action: {}", configActionName, action);
        }
    }

    /**
     * <p>
     * Gets the config action name
     * </p>
     *
     * @return The config action name
     */
    protected abstract String getConfigActionName();

    /**
     * <p>
     * Gets the {@link Provider} for the given web controller name
     * </p>
     *
     * @param configActionName Represents The config action name
     * @return The {@link Provider}
     */
    private Provider getProvider(final String configActionName) {
        return switch (configActionName.toLowerCase()) {
            case "contact" -> Provider.CONTACT_LIST;
            case "company" -> Provider.COMPANY_LIST;
            case "deal" -> Provider.DEAL_LIST;
            case "product" -> Provider.PRODUCT_LIST;
            case "activity" -> Provider.ACTIVITY_LIST;
            default -> null;
        };
    }
}
