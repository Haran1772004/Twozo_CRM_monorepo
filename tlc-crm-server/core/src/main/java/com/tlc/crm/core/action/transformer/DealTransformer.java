package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.Counter;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.service.TemporalService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Transforms the deal entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "deal", type = Transformer.Type.ENTITY)
public class DealTransformer extends AbstractModelTransformer {

    private static final String ID_KEY = TransformerFields.ID.getName();
    private static final Collection<String> SUB_FIELDS =
            List.of(TransformerFields.FIRST_NAME.getName(), TransformerFields.LAST_NAME.getName(), TransformerFields.NAME.getName());
    private static final int MAX_PROBABILITY = 100;

    private final DisplayPictureManager displayPictureManager;
    private final ContactConverter contactConverter;
    private final DealManager dealManager;
    private final PipelineStageManager pipelineStageManager;
    private final SourceActivityManager sourceActivityManager;
    private final DateTimeUtil dateTimeUtil;

    public DealTransformer() {
        displayPictureManager = DisplayPictureManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        dealManager = DealManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * <p>
     * Transforms the deal entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return Transformed deal entities.
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Map<Long, Map<String, Object>> deals = new LinkedHashMap<>();
        final long orgId = transformerRequest.orgId();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final Collection<Long> userIds = new HashSet<>();
        final String salesOwner = TransformerFields.SALES_OWNER.getName();
        final String userId = TransformerFields.USER_ID.getName();
        final String createdBy = TransformerFields.CREATED_BY.getName();
        final String updatedBy = TransformerFields.UPDATED_BY.getName();
        final String tagsKey = TransformerFields.TAGS.getName();
        final String followersKey = TransformerFields.FOLLOWERS.getName();
        final String dealIdKey = TransformerFields.DEAL_ID.getName();
        final Collection<FieldInfo> dealPersistentFields = FieldService.getSecureInstance().getPersistentFieldsBySource(orgId, TransformerFields.DEAL.getName());
        final Collection<FieldInfo> ownerFields = Objects.isNull(dealPersistentFields) ? Collections.emptySet() : dealPersistentFields.stream()
                .filter(fieldInfo -> Objects.equals(StoreType.OWNER, fieldInfo.storeType())).collect(Collectors.toUnmodifiableSet());
        final String productDetailsHolderDealId = ListActionFields.PRODUCT_DETAILS_HOLDER_DEAL_ID;
        final Map<Long, Map<String, Boolean>> permissionAccess = getPermissionAccess(orgId, records);

        for (final Map<String, Object> dealRow : records) {
            final Map<String, Object> users = new HashMap<>();
            final Long pkValue = (Long) dealRow.get(TransformerFields.ID.getName());

            if (permissionAccess.containsKey(pkValue)) {
                dealRow.putAll(permissionAccess.get(pkValue));
            }
            groupUserIdsFields(followersKey, dealRow, deals);
            groupRelatedContacts(orgId, dealRow, deals);
            groupFields(tagsKey, dealRow, deals);
            collectUserIds(followersKey, dealRow, userIds, users);
            collectUserIds(salesOwner, dealRow, userIds, users);
            collectUserIds(createdBy, dealRow, userIds, users);
            collectUserIds(updatedBy, dealRow, userIds, users);
            dealRow.put(TransformerFields.IS_PRODUCT_AVAILABLE.getName(), Objects.nonNull(dealRow.get(productDetailsHolderDealId)));
            dealRow.remove(productDetailsHolderDealId);
            mapLinkProperties(pkValue, dealRow, dealIdKey);

            for (final FieldInfo customOwnerField : ownerFields) {
                final String idAsString = customOwnerField.idAsString();

                if (Objects.nonNull(dealRow.get(idAsString))) {
                    collectUserIds(idAsString, dealRow, userIds, users);
                }
            }

            if (!userIds.isEmpty()) {
                dealRow.put(userId, users);
            }
        }
        transformerResponse.setUserIds(userIds);
        transformerResponse.setResults(deals.values());

        return transformerResponse;
    }

    /**
     * <p>
     * Groups the given field and it's sub field.
     * </p>
     *
     * @param fieldKey Name of the field to be grouped.
     * @param dealRow  Holds fetched Deal details.
     * @param deals    Holds stored Deal detail.
     */
    private void groupFields(final String fieldKey, final Map<String, Object> dealRow, final Map<Long, Map<String, Object>> deals) {
        final Long id = (Long) dealRow.get(ID_KEY);
        final Long fieldId = (Long) dealRow.get(fieldKey);

        if (deals.containsKey(id)) {
            final Map<String, Object> deal = deals.get(id);

            if (Objects.nonNull(deal) && deal.containsKey(fieldKey)) {
                final Object value = deal.get(fieldKey);

                if (value instanceof Collection) {
                    populateDeal(value, fieldId, fieldKey, dealRow, deal);
                } else {
                    populateDeal(fieldKey, dealRow, deal);
                }
            }
        } else if (dealRow.containsKey(fieldKey) && Objects.nonNull(fieldId)) {
            populateDeals(id, fieldKey, dealRow, deals);
        } else {
            deals.put(id, dealRow);
        }
    }

    /**
     * <p>
     * Groups related contacts.
     * </p>
     *
     * @param dealRow Holds fetched Deal details.
     * @param deals   Holds stored Deal detail.
     */
    private void groupRelatedContacts(final Long orgId, final Map<String, Object> dealRow, final Map<Long, Map<String, Object>> deals) {
        final String relatedContactsKey = DealFields.RELATED_CONTACTS;
        final Long id = (Long) dealRow.get(ID_KEY);
        final Long relatedContactId = (Long) dealRow.get(relatedContactsKey);
        final Object primaryContactId = dealRow.get(DealFields.PRIMARY_CONTACT);

        if (Objects.equals(relatedContactId, Objects.isNull(primaryContactId) ? dealManager.getPrimaryContactId(orgId, id) : primaryContactId)) {
            dealRow.remove(relatedContactsKey);
        } else {
            if (deals.containsKey(id)) {
                final Map<String, Object> deal = deals.get(id);

                if (Objects.nonNull(deal)) {
                    final Object relatedContacts = deal.get(relatedContactsKey);

                    if (relatedContacts instanceof Collection<?>) {
                        populateDeal(relatedContacts, relatedContactId, relatedContactsKey, dealRow, deal);
                    } else {
                        populateDeal(relatedContactsKey, dealRow, deal);
                    }
                }
            } else if (dealRow.containsKey(relatedContactsKey) && Objects.nonNull(relatedContactId)) {
                populateDeals(id, relatedContactsKey, dealRow, deals);
            } else {
                deals.put(id, dealRow);
            }
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param field   Name of the field to grouped.
     * @param dealRow Holds fetched Deal details.
     * @return Grouped fields.
     */
    private Map<String, Object> groupFields(final String field, final Map<String, Object> dealRow) {
        final Optional<Object> optValue = Optional.ofNullable(dealRow.get(field));
        final String columnFormat = Formats.COLUMN_FORMAT.get();

        return optValue.map(fieldId -> {
            final Map<String, Object> fields = new LinkedHashMap<>();

            for (final String subField : DealTransformer.SUB_FIELDS) {
                final Object value = dealRow.remove(String.format(columnFormat, field, subField));

                if (Objects.nonNull(value)) {
                    fields.put(subField, value);
                }
            }
            fields.put(ID_KEY, fieldId);

            if (Objects.equals(TransformerFields.RELATED_CONTACTS.getName(), field)) {
                fields.put(TransformerFields.SOURCE.getName(), TransformerFields.CONTACTS.getName());
            }

            return fields;
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Populates the deal map.
     * </p>
     *
     * @param value    Value of the field.
     * @param fieldId  Field's id.
     * @param fieldKey Field's key.
     * @param dealRow  Map holding deal rows.
     * @param deal     Map holding deal values.
     */
    private void populateDeal(final Object value, final Long fieldId, final String fieldKey, final Map<String, Object> dealRow,
                              final Map<String, Object> deal) {
        final Collection<Map<String, Object>> existingFields = new ArrayList<>((Collection<Map<String, Object>>) value);
        final Collection<Object> fieldValues = existingFields.stream().map(existingValue -> existingValue.get(ID_KEY))
                .collect(Collectors.toUnmodifiableSet());

        if (Objects.nonNull(fieldId) && !fieldValues.contains(fieldId)) {
            final Map<String, Object> groupedFields = groupFields(fieldKey, dealRow);

            if (Objects.nonNull(groupedFields)) {
                existingFields.add(groupedFields);
                deal.put(fieldKey, existingFields);
            }
        }
    }

    /**
     * <p>
     * Populates deal.
     * </p>
     *
     * @param fieldKey Field's key.
     * @param dealRow  Map holding deal rows.
     * @param deal     Map holding deal values.
     */
    private void populateDeal(final String fieldKey, final Map<String, Object> dealRow, final Map<String, Object> deal) {
        final Map<String, Object> groupedFields = groupFields(fieldKey, dealRow);

        deal.put(fieldKey, Objects.isNull(groupedFields) ? null : List.of(groupedFields));
    }

    /**
     * <p>
     * Populates deals.
     * </p>
     *
     * @param id       Deal's id.
     * @param fieldKey Field's key.
     * @param dealRow  Map holding deal rows.
     * @param deals    Map holding multiple deals values.
     */
    private void populateDeals(final Long id, final String fieldKey, final Map<String, Object> dealRow,
                               final Map<Long, Map<String, Object>> deals) {
        final Map<String, Object> groupedFields = groupFields(fieldKey, dealRow);

        dealRow.put(fieldKey, Objects.isNull(groupedFields) ? null : List.of(groupedFields));
        deals.put(id, dealRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transformDataForMobile(final TransformerRequest transformerRequest) {
        final TransformerResponse transformerResponse = transform(transformerRequest);
        final Collection<Map<String, Object>> results = transformerResponse.getResults();
        final String relatedContactsKey = TransformerFields.RELATED_CONTACTS.getName();
        final String companyKey = TransformerFields.COMPANY.getName().toLowerCase();
        final long orgId = transformerRequest.orgId();
        final String companyThumbnailNeededKey = TransformerFields.COMPANY_THUMBNAIL_NEEDED.getName();
        final String contactThumbnailNeededKey = TransformerFields.CONTACT_THUMBNAIL_NEEDED.getName();
        final String completionPercentage = TransformerFields.COMPLETION_PERCENTAGE.getName();
        final String pipelineStage = TransformerFields.PIPELINE_STAGE.getName();
        final String won = Deal.State.WON.getDisplayName();
        final String iconName = TransformerFields.ICON.getName();
        final Map<Long, Set<Long>> deals = results.stream().collect(Collectors.toMap(result -> (Long) result.get(ID_KEY),
                result -> Optional.ofNullable((List<Map<String, Object>>) result.get(relatedContactsKey))
                        .map(relatedContacts -> relatedContacts.stream().map(relatedContact -> (Long) relatedContact.get(ID_KEY)).collect(Collectors.toSet())).orElse(Collections.emptySet())));
        final Set<Long> dealIds = deals.keySet();
        final Map<Long, Pair<Long, Short>> dealStageInfo = dealManager.getDealsStageInfo(orgId, dealIds);
        final Map<Long, Long> pipelineStagesCount = pipelineStageManager.getPipelineStagesCount(orgId, dealStageInfo.values().stream().map(Pair::getKey).toList());
        final String thumbnailKey = TransformerFields.THUMBNAIL.getName();
        final String contactKey = TransformerFields.CONTACT.getName();
        final Collection<Long> relatedContactIds = deals.values().stream().flatMap(Set::stream).toList();
        final Map<Long, Collection<Activity>> dealActivities = sourceActivityManager.getDealActivities(orgId, dealIds);
        final Map<Long, DisplayPicture> relatedContactProfilePictures = relatedContactIds.isEmpty() ? null : displayPictureManager.
                getDisplayPicture(orgId, relatedContactIds, Source.CONTACT, ProfilePicture.Quality.THUMBNAIL);

        for (final Map<String, Object> result : results) {
            final List<Map<String, Object>> relatedContacts = (List<Map<String, Object>>) result.get(relatedContactsKey);
            final Long dealId = (Long) result.get(ID_KEY);
            final Pair<Long, Short> stageInfo = dealStageInfo.get(dealId);
            final Collection<Activity> activities = dealActivities.get(dealId);

            if (Objects.nonNull(relatedContacts)) {
                for (final Map<String, Object> relatedContact : relatedContacts) {
                    final Long contactId = (Long) relatedContact.get(ID_KEY);

                    if (Objects.nonNull(relatedContactProfilePictures)) {
                        final DisplayPicture displayPicture = relatedContactProfilePictures.get(contactId);

                        if (Objects.nonNull(displayPicture)) {
                            final JsonObject thumbnailJson = contactConverter.convertThumbnailToJson(contactId, contactKey, displayPicture);

                            relatedContact.put(thumbnailKey, thumbnailJson.toMap());
                        }
                    }
                }
            }

            if (result.containsKey(companyKey) && Objects.nonNull(result.get(companyKey))) {
                result.put(companyThumbnailNeededKey, true);
            }

            result.put(contactThumbnailNeededKey, true);
            result.put(completionPercentage, Objects.equals(won, result.get(pipelineStage))
                    ? MAX_PROBABILITY : ((double) stageInfo.getValue() / pipelineStagesCount.get(stageInfo.getKey()) * MAX_PROBABILITY));

            if (Objects.nonNull(activities)) {
                result.put(iconName, getActivityIconType(activities, transformerRequest.timezoneInfo()));
            }
        }

        return transformerResponse;
    }

    /**
     * <p>
     * Gets the activity icon type related to the deal
     * </p>
     *
     * @param activities   Represents the collection of {@link Activity}
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return the activity icon type related to the deal.
     */
    private String getActivityIconType(final Collection<Activity> activities, final TimezoneInfo timezoneInfo) {
        final String activityIconType;

        if (activities.isEmpty()) {
            activityIconType = ActivityFields.NO_ACTIVITIES.getName();
        } else {
            final long currentTime = Instant.now().toEpochMilli();
            final Counter overDueActivitiesCount = new Counter();
            final Counter todayActivitiesCount = new Counter();
            final TemporalService temporalService = TemporalServiceProvider.get();
            final long startTime = temporalService.getStartTimeOfDay(timezoneInfo);
            final long endTime = temporalService.getEndTimeOfDay(timezoneInfo);

            for (final Activity activity : activities) {
                final Long activityStartTime = dateTimeUtil.getTimeStamp(activity.getStartTime());
                final Long activityEndTime = dateTimeUtil.getTimeStamp(activity.getEndTime());

                if (activityEndTime < currentTime) {
                    overDueActivitiesCount.getAndIncrement();
                    break;
                } else if (activityStartTime >= startTime && activityStartTime <= endTime) {
                    todayActivitiesCount.getAndIncrement();
                }
            }

            if (overDueActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.OVER_DUE.getName();
            } else if (todayActivitiesCount.getCount() > 0) {
                activityIconType = ActivityFields.SCHEDULED.getName();
            } else {
                activityIconType = ActivityFields.UPCOMING_ACTIVITIES.getName();
            }
        }

        return activityIconType;
    }
}