package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;

import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;

import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import com.tlc.crm.icon.service.IconService;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Transforms the activity entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "activity", type = Transformer.Type.ENTITY)
public class ActivityTransformer extends AbstractModelTransformer {

    private final IconService iconService;
    private final ContactConverter contactConverter;
    private final DisplayPictureManager displayPictureManager;
    private final ActivityManager activityManager;

    public ActivityTransformer() {
        iconService = IconService.getInstance();
        contactConverter = ContactConverter.getInstance();
        displayPictureManager = DisplayPictureManager.getInstance();
        activityManager = ActivityManager.getInstance();
    }

    /**
     * <p>
     * Fetches and groups the collaborator values to transform the activity entities.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final long orgId = transformerRequest.orgId();
        final Map<Long, Map<String, Object>> activityRowMap = new LinkedHashMap<>();
        final Collection<Long> userIds = new HashSet<>();
        final String statusKey = TransformerFields.STATUS.getName();
        final String userIdKey = TransformerFields.USER_ID.getName();
        final String creatorKey = TransformerFields.CREATOR.getName();
        final String assigneeKey = TransformerFields.ASSIGNEE.getName();
        final String collaboratorsKey = TransformerFields.COLLABORATORS.getName();
        final String rowStateKey = TransformerFields.ROW_STATE.getName();
        final String contactsKey = TransformerFields.CONTACTS.getName();
        final String firstNameKey = TransformerFields.FIRST_NAME.getName();
        final String lastNameKey = TransformerFields.LAST_NAME.getName();
        final String facebookKey = TransformerFields.FACEBOOK.getName();
        final String twitterKey = TransformerFields.TWITTER.getName();
        final String linkedinKey = TransformerFields.LINKEDIN.getName();
        final String companyKey = TransformerFields.COMPANY.getName().toLowerCase();
        final String emailsKey = TransformerFields.EMAILS.getName();
        final String phonesKey = TransformerFields.PHONES.getName();
        final String priorityKey = TransformerFields.PRIORITY.getName();
        final String reminderKey = TransformerFields.REMINDER.getName();
        final String availabilityKey = TransformerFields.AVAILABILITY.getName();
        final String activityIdKey = TransformerFields.ACTIVITY_ID.getName();
        final Collection<Long> contactIds = new HashSet<>();
        final Long loggedInUserId = LoggedInUserInfo.getUser().id();
        final Collection<Long> activityIds = new HashSet<>();
        final boolean isMobileClient = CrmRequest.ClientType.MOBILE == transformerRequest.clientType();

        for (final Map<String, Object> activityRow : records) {
            final Object contactId = activityRow.get(contactsKey);
            final Long pkValue = (Long) activityRow.get(TransformerFields.ID.getName());

            if (isMobileClient) {
                activityIds.add(pkValue);
            }

            if (Objects.nonNull(contactId)) {
                contactIds.add((Long) contactId);
            }
            loadActivityTitle(activityRow);
            mapLinkProperties(pkValue, activityRow, activityIdKey);
            groupUserIdsFields(collaboratorsKey, activityRow, activityRowMap);
            groupFields(contactsKey, activityRow, activityRowMap, firstNameKey, lastNameKey, facebookKey, twitterKey,
                    linkedinKey, companyKey, emailsKey, phonesKey);
        }
        final Collection<Map<String, Object>> activityRows = new LinkedHashSet<>(activityRowMap.values());
        final Map<Long, DisplayPicture> contactProfilePictures = contactIds.isEmpty() ? Collections.emptyMap()
                : displayPictureManager.getDisplayPicture(orgId, contactIds, Source.CONTACT, ProfilePicture.Quality.THUMBNAIL);
        final Map<Long, Long> activityTotalTimeSpentMap = activityIds.isEmpty() ? Collections.emptyMap() : activityManager.getTotalTimeSpentMap(orgId, activityIds);

        for (final Map<String, Object> activityRow : activityRows) {
            final Map<String, Object> users = new HashMap<>();
            final Object contacts = activityRow.get(contactsKey);

            if (activityRow.containsKey(statusKey)) {
                final int statusId = (int) activityRow.remove(statusKey);
                final Status status = Status.get(statusId);

                activityRow.put(rowStateKey, status);
                activityRow.put(statusKey, new I18nKey(status.getDisplayName()));
            }

            if (Objects.nonNull(contacts) && !contactProfilePictures.isEmpty()) {
                loadProfilePicturesIntoContactMap((Collection<Map<String, Object>>) contacts, contactProfilePictures);
            }

            if (activityRow.containsKey(availabilityKey)) {
                final int availabilityId = (int) activityRow.remove(availabilityKey);

                activityRow.put(availabilityKey, new I18nKey(Availability.get(availabilityId).getDisplayName()));
            }

            if (activityRow.containsKey(priorityKey)) {
                final int priorityId = (int) activityRow.remove(priorityKey);

                activityRow.put(priorityKey, new I18nKey(Priority.get(priorityId).getDisplayName()));
            }

            if (activityRow.containsKey(reminderKey)) {
                final int reminderId = (int) activityRow.remove(reminderKey);

                activityRow.put(reminderKey, new I18nKey(Reminder.get(reminderId).getDisplayName()));
            }

            if (activityRow.containsKey(companyKey) && Objects.nonNull(activityRow.get(companyKey))) {
                activityRow.put(TransformerFields.COMPANY_THUMBNAIL_NEEDED.getName(), true);
            }
            collectUserIds(collaboratorsKey, activityRow, userIds, users);
            collectUserIds(creatorKey, activityRow, userIds, users);
            collectUserIds(assigneeKey, activityRow, userIds, users);

            if (!userIds.isEmpty()) {
                activityRow.put(userIdKey, users);
            }

            if (isMobileClient) {
                loadCheckOutAdditionalData(activityRow, activityTotalTimeSpentMap, loggedInUserId);
            }
        }
        transformerResponse.setResults(activityRows);
        transformerResponse.setUserIds(userIds);

        return transformerResponse;
    }

    /**
     * <p>
     * Loads the profile pictures into contact map.
     * </p>
     *
     * @param contacts               Holds the details of the contacts.
     * @param contactProfilePictures Holds the details of the profile pictures.
     */
    private void loadProfilePicturesIntoContactMap(final Collection<Map<String, Object>> contacts,
                                                   final Map<Long, DisplayPicture> contactProfilePictures) {
        for (final Map<String, Object> contact : contacts) {
            final Long contactId = (Long) contact.get(TransformerFields.ID.getName());
            final DisplayPicture displayPicture = contactProfilePictures.get(contactId);

            if (Objects.nonNull(displayPicture)) {
                final JsonObject thumbnailJson = contactConverter.convertThumbnailToJson(contactId, TransformerFields.CONTACT.getName(), displayPicture);

                contact.put(TransformerFields.THUMBNAIL.getName(), thumbnailJson.toMap());
            }
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param field          Name of the field to be transformed.
     * @param activityRow    Retrieved activity entities.
     * @param activityRowMap Holds the details of the transformed entities.
     * @param subFields      Name of the subfields to be grouped. (eg., FIRST_NAME, LAST_NAME)
     */
    private void groupFields(final String field, final Map<String, Object> activityRow,
                             final Map<Long, Map<String, Object>> activityRowMap, final String... subFields) {
        final Long pkValue = (Long) activityRow.get(TransformerFields.ID.getName());
        final Long fieldId = (Long) activityRow.get(field);

        if (activityRowMap.containsKey(pkValue)) {
            final Map<String, Object> activity = activityRowMap.get(pkValue);

            if (activity.containsKey(field)) {
                final Object value = activity.get(field);

                if (value instanceof Collection) {
                    final Collection<Map<String, Object>> fields = new ArrayList<>((Collection<Map<String, Object>>) value);
                    final Map<Object, Map<String, Object>> fieldsMap = fields.stream().collect(Collectors.toMap(fieldValue -> fieldValue.get(TransformerFields.ID.getName()), Function.identity()));
                    final Map<String, Object> fieldEntries = groupFields(field, subFields, activityRow);

                    if (Objects.nonNull(fieldEntries)) {
                        if (fieldsMap.containsKey(fieldId)) {
                            final Map<String, Object> fieldMap = fieldsMap.get(fieldId);

                            for (final Map.Entry<String, Object> fieldEntry : fieldEntries.entrySet()) {
                                final String subFieldKey = fieldEntry.getKey();

                                if (fieldMap.containsKey(subFieldKey)) {
                                    final Object existingValue = fieldMap.get(subFieldKey);
                                    final Object subFieldValue = fieldEntry.getValue();

                                    if (!Objects.equals(existingValue, subFieldValue)) {
                                        if (existingValue instanceof Collection<?> existingValues) {
                                            ((Collection<Object>) existingValues).add(subFieldValue);
                                        } else {
                                            final Collection<Object> values = new LinkedHashSet<>();

                                            values.add(existingValue);
                                            values.add(subFieldValue);
                                            fieldMap.put(subFieldKey, values);
                                        }
                                    }
                                }
                            }
                        } else {
                            fields.add(fieldEntries);
                            activity.put(field, fields);
                        }
                    }
                } else {
                    final Map<String, Object> fieldEntries = groupFields(field, subFields, activityRow);

                    activity.put(field, Objects.isNull(fieldEntries) ? null : List.of(fieldEntries));
                }
            } else {
                final Map<String, Object> fieldEntries = groupFields(field, subFields, activityRow);

                if (Objects.nonNull(fieldEntries)) {
                    activity.put(field, new ArrayList<>(List.of(fieldEntries)));
                }
            }
        } else {
            if (activityRow.containsKey(field) && Objects.nonNull(activityRow.get(field))) {
                final Map<String, Object> fieldEntries = groupFields(field, subFields, activityRow);

                if (Objects.nonNull(fieldEntries)) {
                    final Collection<Object> fieldValue = new ArrayList<>();

                    fieldValue.add(fieldEntries);
                    activityRow.put(field, fieldValue);
                } else {
                    activityRow.remove(field);
                }
            }
            activityRowMap.put(pkValue, activityRow);
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param field       Name of the field to be transformed
     * @param subFields   Name of the subfields to be grouped.
     * @param activityRow Retrieved activity entities.
     * @return Grouped fields.
     */
    private Map<String, Object> groupFields(final String field, final String[] subFields,
                                            final Map<String, Object> activityRow) {
        final Optional<Object> optValue = Optional.ofNullable(activityRow.get(field));

        return optValue.map(fieldId -> {
            final Map<String, Object> fields = new HashMap<>();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            for (final String subField : subFields) {
                final Object value = activityRow.remove(String.format(columnFormat, field, subField));

                if (Objects.nonNull(value)) {
                    fields.put(subField, value);
                }
            }

            if (fields.isEmpty()) {
                return null;
            }
            fields.put(TransformerFields.ID.getName(), fieldId);

            return fields;
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Loads the title for the activity based the given data
     * </p>
     *
     * @param data the map of data to transform
     */
    private void loadActivityTitle(final Map<String, Object> data) {
        final Map<String, Object> transformedValues = new HashMap<>();
        final String titleKey = TransformerFields.TITLE.getName();

        transformedValues.put(TransformerFields.ACTIVITY_TITLE.getName(), data.get(titleKey));
        transformedValues.put(TransformerFields.ICON_NAME.getName(), iconService.get((Long) data.remove(TransformerFields.ICON.getName())).getName());

        data.put(titleKey, transformedValues);
    }

    /**
     * <p>
     * Loads the checkout related additional details into activity
     * </p>
     *
     * @param activityRowData           Represents the activity row data
     * @param activityTotalTimeSpentMap Represents the total time spent
     * @param loggedInUserId            Represents the logged in used id
     */
    private void loadCheckOutAdditionalData(final Map<String, Object> activityRowData, final Map<Long, Long> activityTotalTimeSpentMap, final Long loggedInUserId) {
        final Long assigneeId = (Long) activityRowData.get(ActivityFields.ASSIGNEE.getName());

        activityRowData.put(ActivityFields.IS_CHECK_IN_VISIBLE.getName(), Objects.equals(assigneeId, loggedInUserId));
        final Long totalTimeSpent = activityTotalTimeSpentMap.get((Long) activityRowData.get(ActivityFields.ID.getName()));

        if (Objects.nonNull(totalTimeSpent)) {
            activityRowData.put(ActivityFields.TOTAL_TIME_SPENT.getName(), totalTimeSpent);
        }
    }
}