package com.tlc.crm.core.internal.async.bulk.processor;

import com.tlc.commons.color.Color;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.BeanUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.search.IndexManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.deal.manager.DealIndexManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.features.field.follower.UserFollowerFields;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.core.internal.async.bulk.service.ActivityAsyncCreateProcessor;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.core.model.SourceTags;
import com.tlc.crm.entity.builder.EntitySalesOwnerHandler;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Abstract async processor for processing update messages
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AsyncUpdateProcessor<T extends TlcModel> extends AsyncActionProcessor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUpdateProcessor.class);

    private final EntitySalesOwnerHandler entitySalesOwnerHandler;
    private final SourceFollowersManager sourceFollowersManager;
    private final SourceTagManager sourceTagManager;
    private final MarketingListContactManager marketingListContactManager;
    private final TagManager tagManager;
    private final FieldService fieldService;

    protected AsyncUpdateProcessor() {
        entitySalesOwnerHandler = EntitySalesOwnerHandler.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        sourceTagManager = SourceTagManager.getInstance();
        tagManager = TagManager.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    @Override
    public void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long modelId = asyncActionRequest.modelId();

        try {
            update(orgId, userId, modelId, asyncActionRequest.requestJson());
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Action: Bulk update failed. Org ID: {}, User ID: {}, Id: {}, Error: {}",
                    orgId, userId, modelId, exception.getMessage());
        }
    }

    /**
     * <p>
     * Updates the model for the given request
     * </p>
     *
     * @param orgId       Represents an org id
     * @param userId      Represents an user id
     * @param id          Represents the model id
     * @param requestJson Represents the request {@link JsonObject}
     */
    protected void update(final Long orgId, final Long userId, final long id, final JsonObject requestJson) {
        final String type = requestJson.getString(BulkActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(BulkActionFields.DATA);
        final TemporalService temporalService = TemporalServiceProvider.get();

        switch (type) {
            case BulkActionFields.BULK_ASSIGN_OWNER -> {
                final long assignedUserId = data.getLong(BulkActionFields.USER_ID);
                final EntityConfigManager<Entity> entityConfigManager = (EntityConfigManager<Entity>) getAbstractConfigManager();
                final Entity entity = entityConfigManager.get(orgId, id);
                final Source source = getSource();

                if (Objects.nonNull(source)) {
                    entityConfigManager.updateOwner(orgId, userId, Set.of(id), assignedUserId, source);
                    final SecureEventData eventData = new SecureEventData(orgId, userId);

                    eventData.setPreviousValues(List.of(cloneModel(orgId, (T) entity)));
                    entity.setOwnerId(assignedUserId);
                    entity.setLastAssignedAt(new DateTime(temporalService.getUtcDateTime()));
                    entitySalesOwnerHandler.assignOwner(orgId, assignedUserId, entity);
                    eventData.setValues(List.of(entity));
                    eventData.setAction(Action.UPDATE);
                    eventDataHandler.handleEvents(eventData, Event.Type.POST);
                }
            }
            case BulkActionFields.BULK_ASSIGN_FOLLOWERS -> {
                final Source source = Source.get(data.getString(BulkActionFields.SOURCE));
                final JsonArray followerJsonArray = data.getJsonArray(BulkActionFields.FOLLOWER_IDS);
                final Collection<Long> followersIds = new HashSet<>();

                followerJsonArray.forEach(followerId -> followersIds.add((Long) followerId));
                ((EntityConfigManager<Entity>) getAbstractConfigManager()).getSalesOwner(orgId, id).ifPresent(user -> followersIds.remove(user.id()));

                if (!followersIds.isEmpty()) {
                    sourceFollowersManager.assignFollowers(orgId, userId, source, Set.of(id), followersIds);
                    updateFollowersInSearch(orgId, id, source);
                }
            }
            case BulkActionFields.BULK_UPDATE_FOLLOWERS -> {
                final Source source = data.containsKey(BulkActionFields.SOURCE)
                        ? Source.get(data.getString(BulkActionFields.SOURCE)) : getSource();
                final Collection<Long> addedFollowerIds = extractIds(id, data, BulkActionFields.ADDED_FOLLOWER_IDS);
                final Collection<Long> removedFollowerIds = extractIds(id, data, BulkActionFields.REMOVED_FOLLOWER_IDS);

                if (Objects.nonNull(source)) {
                    ((EntityConfigManager<Entity>) getAbstractConfigManager()).getSalesOwner(orgId, id)
                            .ifPresent(user -> {
                                addedFollowerIds.remove(user.id());
                                removedFollowerIds.remove(user.id());
                            });

                    final Set<Long> entityIds = Set.of(id);

                    if (!addedFollowerIds.isEmpty()) {
                        sourceFollowersManager.assignFollowers(orgId, userId, source, entityIds, addedFollowerIds);
                        updateFollowersInSearch(orgId, id, source);
                        LOGGER.debug("User Follower: Assigned follower(s) from user {} to entity {} (source={}) for org {}", userId, id, source, orgId);
                    }

                    if (!removedFollowerIds.isEmpty()) {
                        sourceFollowersManager.removeFollowers(orgId, userId, source, entityIds, removedFollowerIds);
                        updateFollowersInSearch(orgId, id, source);
                        LOGGER.debug("User Follower: Removed follower(s) from user {} to entity {} (source={}) for org {}", userId, id, source, orgId);
                    }
                }
            }
            case BulkActionFields.MAP_TAG -> {
                final JsonArray tagArray = data.getJsonArray(BulkActionFields.TAGS);
                final SourceTags sourceTags = new SourceTags(data.getString(BulkActionFields.SOURCE));
                final int tagArraySize = tagArray.size();

                for (int index = 0; index < tagArraySize; index++) {
                    final JsonObject tagJson = tagArray.getJsonObject(index);
                    final Color color = tagJson.containsKey(BulkActionFields.COLOR) ? Color.get(tagJson.getString(BulkActionFields.COLOR)) : Tag.getDefaultColor();

                    if (tagJson.containsKey(BulkActionFields.ID)) {
                        final Long tagId = tagJson.getLong(BulkActionFields.ID);
                        final Tag tag = new Tag();

                        sourceTags.addTagId(tagId);
                        tag.setId(tagId);
                        tag.setColor(color);
                        tagManager.update(orgId, List.of(tag));
                    } else if (tagJson.containsKey(BulkActionFields.TAG)) {
                        final Tag tag = new Tag();

                        tag.setName(tagJson.getString(BulkActionFields.TAG));
                        tag.setOrgId(orgId);
                        tag.setCreatedBy(LoggedInUserInfo.getUser());
                        tag.setColor(color);
                        sourceTags.addNewTag(tag);
                    }
                }
                sourceTags.setUserId(userId);
                sourceTags.addEntityId(id);

                sourceTagManager.mapTagsWithSource(orgId, sourceTags);
            }
            case BulkActionFields.DELETE_MAPPED_TAG -> {
                final Set<Long> tagIds = new HashSet<>();

                if (data.containsKey(BulkActionFields.TAG_IDS)) {
                    final JsonArray tagIdArray = data.getJsonArray(BulkActionFields.TAG_IDS);
                    final int tagIdSize = tagIdArray.size();

                    for (int index = 0; index < tagIdSize; index++) {
                        tagIds.add(tagIdArray.getLong(index));
                    }
                }
                sourceTagManager.deleteMappedTag(orgId, userId, tagIds, Set.of(id), data.getString(BulkActionFields.SOURCE));
            }
            case BulkActionFields.PARTIAL_BULK_UPDATE, BulkActionFields.BULK_UPDATE ->
                    updateField(orgId, userId, id, type.equals(BulkActionFields.PARTIAL_BULK_UPDATE), data);
            case BulkActionFields.BULK_UPDATE_ARCHIVE_STATE -> {
                final DealManager dealManager = (DealManager) getAbstractConfigManager();

                dealManager.updateArchiveState(orgId, userId, id, data.getBoolean(BulkActionFields.IS_ARCHIVED));
            }
            case BulkActionFields.BULK_CREATE_ACTIVITY -> new ActivityAsyncCreateProcessor().create(orgId, userId, id, data);
            case BulkActionFields.BULK_COMMIT -> {
                final Deal deal = new Deal();

                deal.setOrgId(orgId);
                deal.setId(id);
                deal.setUpdatedBy(LoggedInUserInfo.getUser());
                ((DealManager) getAbstractConfigManager()).updateForecastCategory(deal);
            }
            case BulkActionFields.BULK_ADD_TO_MARKETING_LIST -> {
                final JsonObject updateJson = data.getJsonObject(BulkActionFields.UPDATE_ITEM);
                final FieldInfo marketingListField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.MARKETING_LIST).get();
                final MarketingList marketingList = new MarketingList();

                marketingList.setId(updateJson.getLong(marketingListField.idAsString()));
                marketingList.setOrgId(orgId);
                marketingListContactManager.mapContactsWithMarketingList(orgId, userId, id, marketingList);
            }
        }
    }

    /**
     * <p>
     * Updates the field for the given request
     * </p>
     *
     * @param orgId         Represents an org id
     * @param userId        Represents an user id
     * @param id            Represents the model id
     * @param isPartialData A flag indicating whether the update is partial or full
     * @param dataJson      Represents data {@link JsonObject}
     */
    protected void updateField(final Long orgId, final Long userId, final long id, final boolean isPartialData,
                               final JsonObject dataJson) {
        final JsonObject updateJson = dataJson.getJsonObject(BulkActionFields.UPDATE_ITEM);
        final EventData eventData = new SecureEventData(orgId, userId);
        final AbstractConfigManager<T> abstractConfigManager = getAbstractConfigManager();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final Locale locale = Locale.forLanguageTag(userRegionalPreference.getLanguage().getName());
        final T model = abstractConfigManager.get(orgId, id);

        loadSummaryDetails(model);
        eventData.setPreviousValues(List.of(cloneModel(orgId, model)));
        mapJsonToModel(new ConverterConfig(orgId, userId, locale, isPartialData, updateJson), model);

        eventData.setAction(Action.UPDATE);
        eventData.setValues(List.of(model));

        eventDataHandler.handleEvents(eventData, Event.Type.PRE);
        abstractConfigManager.update(model);
        eventDataHandler.handleEvents(eventData, Event.Type.POST);
    }

    /**
     * Extracts a collection of ids from the given json data.
     *
     * @param modelId Represents the model id
     * @param data    Represents the request data
     * @param key     Represents the json key
     * @return The extracted ids
     */
    private Collection<Long> extractIds(final Long modelId, final JsonObject data, final String key) {
        final Collection<Long> ids = new HashSet<>();
        final JsonArray followerChanges = data.optJsonArray(UserFollowerFields.FOLLOWER_CHANGES);

        if (Objects.isNull(followerChanges)) {
            return ids;
        }

        final int size = followerChanges.size();

        for (int index = NumericalConstant.ZERO; index < size; index++) {
            final JsonObject entry = followerChanges.getJsonObject(index);
            final Long entryModelId = entry.getLong(ActionFields.ID);

            if (!modelId.equals(entryModelId)) {
                continue;
            }

            final JsonArray followerIdsArray = entry.optJsonArray(key);

            if (Objects.nonNull(followerIdsArray)) {
                final int followersSize = followerIdsArray.size();

                for (int followerIndex = NumericalConstant.ZERO; followerIndex < followersSize; followerIndex++) {
                    ids.add(followerIdsArray.getLong(followerIndex));
                }
            }

            break;
        }

        return ids;
    }

    /**
     * Updates follower ids in search for the given entity.
     *
     * @param orgId    Represents the organization id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     */
    private void updateFollowersInSearch(final Long orgId, final Long entityId, final Source source) {
        final IndexManager<?> indexManager = switch (source) {
            case COMPANY -> CompanyIndexManager.getInstance();
            case CONTACT -> ContactIndexManager.getInstance();
            case DEAL -> DealIndexManager.getInstance();
            default -> null;
        };

        if (Objects.nonNull(indexManager)) {
            indexManager.updateFollowerIds(orgId, entityId, sourceFollowersManager.getFollowerIdsByEntityId(orgId, entityId, source));
        }
    }

    /**
     * <p>
     * Loads the summary details into model
     * </p>
     *
     * @param model Represents the {@link TlcModel}
     */
    protected void loadSummaryDetails(final T model) {

    }

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return null;
    }

    /**
     * <p>
     * Clones the given model using bean utils.
     * </p>
     *
     * @param orgId Refers the organization id.
     * @param model Refers the model to be cloned.
     * @return The cloned model.
     */
    protected T cloneModel(final long orgId, final T model) {
        return BeanUtil.clone(model);
    }

    /**
     * <p>
     * Maps {@link JsonObject} data to the model
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param model           Represents the model to be updated
     */
    protected void mapJsonToModel(final ConverterConfig converterConfig, final T model){
    }
}