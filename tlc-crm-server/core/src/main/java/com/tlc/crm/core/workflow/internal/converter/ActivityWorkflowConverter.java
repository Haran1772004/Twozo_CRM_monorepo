package com.tlc.crm.core.workflow.internal.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.activity.model.converter.ActivityConverter;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.note.model.converter.NoteConverter;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.field.activity.ActivityHolderFields;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The converter that facilitates different ways to convert data from/to {@link ActivityHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActivityWorkflowConverter extends EntityWorkflowConverter<ActivityHolder> {

    private final ActivityConverter activityConverter;
    private final DateTimeUtil dateTimeUtil;
    private final NoteConverter noteConverter;

    private ActivityWorkflowConverter() {
        this.activityConverter = ActivityConverter.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.noteConverter = NoteConverter.getInstance();
    }

    private static class Instance {
        private static final ActivityWorkflowConverter INSTANCE = new ActivityWorkflowConverter();
    }

    public static ActivityWorkflowConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param activityHolder Refers the model.
     * @return The converted json.
     */
    @Override
    public JsonObject convert(final ActivityHolder activityHolder) {
        final JsonObject activityJson = Json.object();
        final Activity activity = activityHolder.getActivity();
        final Note note = activity.getNote();
        final ActivityType activityType = activity.getType();
        final Collection<Long> collaborators = activity.getCollaboratorIds();
        final Status status = activity.getStatus();
        final Reminder reminder = activity.getReminder();
        final Availability availability = activity.getAvailability();
        final Priority priority = activity.getPriority();
        final CrmUser creator = activity.getCreator();
        final CrmUser assignee = activity.getAssignee();
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();

        if (!contacts.isEmpty()) {
            final JsonArray contactArray = Json.array();

            contacts.forEach(contact -> contactArray.put(contact.id()));
            activityJson.put(ActivityHolderFields.CONTACT_IDS.getName(), contactArray);
        }
        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), activity.orgId());
        activityJson.put(ActivityFields.TITLE.getName(), activity.getTitle());
        activityJson.put(ActivityFields.START_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getStartTime()));
        activityJson.put(ActivityFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCreatedTime()));
        activityJson.put(ActivityFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getUpdatedTime()));
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        activityJson.put(ActivityFields.STATUS.getName(), Objects.isNull(status) ? null : status.getId());
        activityJson.put(ActivityFields.REMINDER.getName(), Objects.isNull(reminder) ? null : reminder.getId());
        activityJson.put(ActivityFields.AVAILABILITY.getName(), Objects.isNull(availability) ? null : availability.getId());
        activityJson.put(ActivityFields.PRIORITY.getName(), Objects.isNull(priority) ? null : priority.getId());
        activityJson.put(ActivityFields.END_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getEndTime()));
        activityJson.put(ActivityFields.DURATION.getName(), activity.getDuration());
        activityJson.put(ActivityFields.LOCATION.getName(), activity.getLocation());
        activityJson.put(ActivityFields.DESCRIPTION.getName(), activity.getDescription());

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.TYPE_ID.getName(), activityType.id());
        }

        if (activity.getDone()) {
            activityJson.put(ActivityFields.COMPLETED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCompletedTime()));
        }

        if (Objects.nonNull(note)) {
            activityJson.put(ActivityFields.NOTE.getName(), noteConverter.convertPartialData(note));
        }

        if (!collaborators.isEmpty()) {
            activityJson.put(ActivityFields.COLLABORATORS.getName(), collaborators);
        }

        if (Objects.nonNull(assignee)) {
            activityJson.put(ActivityFields.ASSIGNEE_ID.getName(), assignee.id());
        }

        if (Objects.nonNull(creator)) {
            activityJson.put(ActivityFields.CREATOR.getName(), creator.id());
        }

        if (Objects.nonNull(company)) {
            activityJson.put(ActivityHolderFields.COMPANY_ID.getName(), company.id());
        }

        if (Objects.nonNull(deal)) {
            activityJson.put(ActivityHolderFields.DEAL_ID.getName(), deal.id());
        }

        return activityJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object.
     * @return The {@link ActivityHolder}.
     */
    @Override
    public ActivityHolder convert(final JsonObject jsonObject) {
        final Long orgId = jsonObject.getLong(ActivityFields.ORG_ID.getName());
        final ConverterConfig converterConfig = new ConverterConfig(orgId, jsonObject.getLong(ActivityFields.CREATOR.getName()), null, false, jsonObject);
        final Activity activity = activityConverter.buildActivity(converterConfig, new Activity(), jsonObject);
        final ActivityHolder activityHolder = new ActivityHolder();
        final String contactIdsKey = ActivityHolderFields.CONTACT_IDS.getName();
        final String dealIdKey = ActivityHolderFields.DEAL_ID.getName();
        final String companyIdKey = ActivityHolderFields.COMPANY_ID.getName();

        activity.setCreatedTime(new DateTime(jsonObject.getLong(ActivityFields.CREATED_TIME.getName())));
        activity.setUpdatedTime(new DateTime(jsonObject.getLong(ActivityFields.UPDATED_TIME.getName())));
        activityHolder.setActivity(activity);

        if (jsonObject.containsKey(contactIdsKey)) {
            final JsonArray contactArray = jsonObject.getJsonArray(contactIdsKey);

            contactArray.forEach(contactId -> {
                final Contact contact = new Contact();

                contact.setId(NumberUtils.parseNumber(contactId).longValue());
                contact.setOrgId(converterConfig.orgId());
                activityHolder.addContact(contact);
            });
        }

        if (jsonObject.containsKey(dealIdKey)) {
            final Deal deal = new Deal();

            deal.setId(jsonObject.getLong(dealIdKey));
            deal.setOrgId(converterConfig.orgId());
            activityHolder.setDeal(deal);
        }

        if (jsonObject.containsKey(companyIdKey)) {
            final Company company = new Company();

            company.setId(jsonObject.getLong(companyIdKey));
            company.setOrgId(converterConfig.orgId());
            activityHolder.setCompany(company);
        }

        return activityHolder;
    }
}
