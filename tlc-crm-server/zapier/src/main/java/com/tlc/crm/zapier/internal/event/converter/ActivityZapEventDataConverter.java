package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.event.ZapEventData;

import com.tlc.i18n.I18nResolver;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the conversion of model data into a JSON format suitable for Zapier event payloads.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityZapEventDataConverter extends AbstractZapEventDataConverter<ActivityHolder> {

    private static final class Instance {
        private static final ActivityZapEventDataConverter EVENT_DATA_CONVERTER = new ActivityZapEventDataConverter();
    }

    public static ActivityZapEventDataConverter getInstance() {
        return Instance.EVENT_DATA_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param activityHolder The specific model instance to convert.
     * @param zapEventData   The {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    @Override
    protected JsonObject convert(final ActivityHolder activityHolder, final ZapEventData zapEventData) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(activityHolder.orgId(), Source.ACTIVITY.getName());
        final JsonObject activityJson = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(zapEventData.getLocale());
        final Map<Long, User> userMap = zapEventData.getGroupedUsers();
        final Activity activity = activityHolder.getActivity();

        for (final FieldInfo fieldInfo : fields) {
            final String aliasName = fieldInfo.aliasName();

            switch (aliasName) {
                case "title" -> activityJson.put(aliasName, activity.getTitle());
                case "status" -> activityJson.put(aliasName, i18nResolver.get(activity.getStatus().getDisplayName()));
                case "dealId" -> {
                    final Deal deal = activityHolder.getDeal();

                    if (Objects.nonNull(deal)) {
                        final JsonObject dealJson = Json.object();

                        dealJson.put(NAME, deal.getTitle());
                        dealJson.put(ID, deal.id());

                        activityJson.put(ZapAttributes.DEAL, dealJson);
                    }
                }
                case "startTime" ->
                        activityJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(activity.getStartTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                case "endTime" ->
                        activityJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(activity.getEndTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                case "contactIds" -> {
                    final List<Contact> contacts = activityHolder.getContacts();

                    if (!contacts.isEmpty()) {
                        final JsonArray array = Json.array();

                        for (final Contact contact : contacts) {
                            final JsonObject contactJson = Json.object();

                            contactJson.put(NAME, contact.getFullName());
                            contactJson.put(ID, contact.id());

                            array.put(contactJson);
                        }
                        activityJson.put(ZapAttributes.CONTACTS, array);
                    }
                }
                case "companyId" -> {
                    final Company company = activityHolder.getCompany();

                    if (Objects.nonNull(company)) {
                        final JsonObject companyJson = Json.object();

                        companyJson.put(NAME, company.getName());
                        companyJson.put(ID, company.id());

                        activityJson.put(ZapAttributes.COMPANY, companyJson);
                    }
                }
                case "assigneeId" -> {
                    final JsonObject saleOwnerJson = Json.object();
                    final CrmUser assignee = activity.getAssignee();

                    saleOwnerJson.put(NAME, userMap.get(assignee.getIamId()).getFullName());
                    saleOwnerJson.put(ID, assignee.id());

                    activityJson.put(ZapAttributes.ASSIGNEE, saleOwnerJson);
                }
                case "markAsDoneTime" -> activityJson.put(aliasName, activity.getCompletedTime());
                case "availability" -> activityJson.put(aliasName, activity.getAvailability().getDisplayName());
                case "createdAt" ->
                        activityJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(activity.getCreatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                case "creator" -> {
                    final JsonObject creatorJson = Json.object();
                    final CrmUser creator = activity.getCreator();

                    creatorJson.put(NAME, userMap.get(creator.getIamId()).getFullName());
                    creatorJson.put(ID, creator.id());

                    activityJson.put(aliasName, creatorJson);
                }
                case "location" -> activityJson.put(aliasName, activity.getLocation());
                case "note" -> {
                    final Note note = activity.getNote();

                    if (Objects.nonNull(note)) {
                        activityJson.put(aliasName, note.getContent());
                    }
                }
                case "description" -> activityJson.put(aliasName, activity.getDescription());
                case "typeId" -> {
                    final ActivityType type = activity.getType();

                    if (Objects.nonNull(type)) {
                        final JsonObject typeJson = Json.object();

                        typeJson.put(NAME, type.getName());
                        typeJson.put(ID, type.id());

                        activityJson.put(ZapAttributes.TYPE, typeJson);
                    }
                }
                case "updatedAt" ->
                        activityJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(activity.getUpdatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                case "collaborators" -> {
                    final Collection<CrmUser> collaborators = activity.getCollaborators();

                    if (Objects.nonNull(collaborators)) {
                        final JsonArray array = Json.array();

                        for (final CrmUser collaborator : collaborators) {
                            final JsonObject contactJson = Json.object();

                            contactJson.put(NAME, userMap.get(collaborator.getIamId()));
                            contactJson.put(ID, collaborator.id());

                            array.put(contactJson);
                        }
                        activityJson.put(aliasName, array);
                    }
                }
                case "reminder" -> activityJson.put(aliasName, activity.getReminder().getDisplayName());
                case "priority" -> activityJson.put(aliasName, activity.getPriority().getDisplayName());
                case "isDone" -> activityJson.put(aliasName, activity.getDone());
            }
        }

        return activityJson;
    }
}
