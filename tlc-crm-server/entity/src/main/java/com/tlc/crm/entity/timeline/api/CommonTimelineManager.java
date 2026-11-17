package com.tlc.crm.entity.timeline.api;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.internal.status.TimelineErrorCodes;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLFIELDVALUETIMELINE;
import com.tlc.crm.entity.sql.resource.OMTLTIMELINE;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.ActivityTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline.Category;
import com.tlc.crm.entity.timeline.model.TimelineManager;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.icon.model.Icon;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implements the {@link Timeline} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class CommonTimelineManager implements TimelineManager {

    private final IconService iconService;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final CommonTimelineManager INSTANCE = new CommonTimelineManager();
    }

    private CommonTimelineManager() {
        iconService = IconService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CommonTimelineManager} Instance
     * </p>
     *
     * @return {@link CommonTimelineManager}
     */
    public static CommonTimelineManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the timeline row into the container
     * </p>
     *
     * @param timeline      An entity that holds the timeline attributes
     * @param timelineRow   Holds the row of a timeline table
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    public void loadTimelineRowIntoContainer(final Timeline timeline, final Row timelineRow, final DataContainer dataContainer) {
        final int timelineType = timeline.getTimelineType().getId();
        final Icon timelineIcon = getIconType(TimelineType.valueOf(timelineType), timeline);
        final Icon icon = iconService.get(timelineIcon.id());
        final FieldTimeline fieldTimeline = timeline.getFieldTimeline();
        final Category category = timeline.getCategory();
        final String categoryName = category.name();

        switch (category) {
            case CONTACT, DEAL, COMPANY, NOTES, ACTIVITY, FILE, EMAILS, CALLS, CHATS, INVOICE -> {
                timelineRow.set(OMTLTIMELINE.OWNER_ID, timeline.getOwnerId());
                timelineRow.set(OMTLTIMELINE.CATEGORY, categoryName);
                timeline.setId(timelineRow.getPKValue());
            }
        }

        if (timelineRow.isNewRow()) {
            timelineRow.setOrgId(timeline.orgId());
        }
        timelineRow.set(OMTLTIMELINE.ACTION, timeline.getAction());
        timelineRow.set(OMTLTIMELINE.TYPE, timelineType);
        timelineRow.set(OMTLTIMELINE.AUDITED_TIME, dateTimeUtil.getTimeStamp(timeline.getAuditedTime()));
        timelineRow.set(OMTLTIMELINE.ICON_ID, icon.id());
        dataContainer.updateRow(timelineRow);

        if (Objects.nonNull(fieldTimeline)) {
            processFieldTimeline(timeline.id(), fieldTimeline, dataContainer);
        }
    }

    /**
     * <p>
     * Gets the icon type by using the type of timeline created
     * </P>
     *
     * @param type     Represents the {@link TimelineType}
     * @param timeline Represents the {@link Timeline}
     * @return {@link Icon}
     */
    private Icon getIconType(final TimelineType type, final Timeline timeline) {
        final Category category = timeline.getCategory();

        if (Category.ACTIVITY == category) {
            final ActivityTimeline activityTimeline = timeline.getEntityTimeline().get(ActivityTimeline.TYPE);

            if (Objects.nonNull(activityTimeline)) {
                final Long iconId = activityTimeline.getIconId();

                if (Objects.nonNull(iconId)) {
                    final Icon icon = iconService.getIconById(Icon.Type.ACTIVITY, iconId);

                    timeline.setIconType(icon.getName());
                }
            }

            return iconService.fetchIconByTypeAndName(Icon.Type.ACTIVITY, timeline.getIconType());
        } else {
            switch (type) {
                case CONTACT_CREATED, CONTACT_LIFE_CYCLE_STAGE_UPDATED, CONTACT_LIFE_CYCLE_STATUS_UPDATED,
                     NOW_POINT_OF_CONTACT_FOR_THIS_DEAL, SUBSCRIPTION_STATUS_CHANGED, CONTACT_FORM_SUBMISSION,
                     CONTACT_NAME_UPDATED, CONTACT_CLOSED_LOST_REASON_SET, CONTACT_CLOSED_LOST_REASON_UPDATED,
                     CONTACT_LIFE_CYCLE_STATUS_UPDATED_UPON_DEAL_CREATION,
                     CONTACT_LIFE_CYCLE_STATUS_UPDATED_UPON_DEAL_WON,
                     CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_WON,
                     CONTACT_LIFE_CYCLE_LOST_REASON_UPDATED_UPON_DEAL_CREATION,
                     CONTACT_LIFE_CYCLE_STAGE_UPDATED_UPON_DEAL_CREATION,
                     CONTACT_LIFE_CYCLE_STAGE_UPDATED_UPON_DEAL_WON,
                     CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_WON,
                     CONTACT_LIFE_CYCLE_LOST_REASON_SET_UPON_DEAL_CREATION,
                     DEAL_RELATED_CONTACTS_REMOVED, DEAL_RELATED_CONTACTS_ADDED,
                     CONTACT_CREATED_TIME_CHANGED ->
                     timeline.setIconType(TimelineFields.CONTACT.getName());

                case DEAL_CREATED, DEAL_VALUE_ASSIGNED, DEAL_VALUE_UPDATED, DEAL_PIPELINE_STAGE_MOVED,
                     DEAL_STATE_MOVED, DEAL_FORM_SUBMISSION, DEAL_TITLE_UPDATED, DEAL_ASSOCIATED_WITH_CONTACT,
                     DEAL_ASSOCIATED_WITH_COMPANY, DEAL_PIPELINE_MOVED, DEAL_CREATED_TIME_CHANGED, FORECAST_CATEGORY_ADDED,
                     FORECAST_CATEGORY_REMOVED, FORECAST_CATEGORY_UPDATED  -> timeline.setIconType(TimelineFields.DEAL.getName());
                case COMPANY_CREATED, COMPANY_NAME_UPDATED, COMPANY_CREATED_TIME_CHANGED, DEAL_COMPANY_ADDED,
                     DEAL_COMPANY_REMOVED, DEAL_COMPANY_UPDATED -> timeline.setIconType(TimelineFields.COMPANY.getName());
                case NOTES_ADDED, NOTES_EDITED -> timeline.setIconType(TimelineFields.NOTE.getName());
                case FILE_UPLOADED, ATTACHMENT_EDITED -> timeline.setIconType(TimelineFields.FILE.getName());
                case DEAL_EXPECTED_CLOSE_DATE_ASSIGNED, DEAL_EXPECTED_CLOSE_DATE_CHANGED ->
                        timeline.setIconType(TimelineFields.CALENDAR.getName());
                case COMPANY_MERGED, CONTACT_MERGED, DEAL_MERGED -> timeline.setIconType(TimelineFields.COPY.getName());
                case DEAL_LOST_REASON_UPDATED, DEAL_LOST_REASON_SET -> timeline.setIconType(TimelineFields.LOST.getName());
                case EMAIL_REPLIED, EMAIL_RECEIVED, EMAIL_SENT -> timeline.setIconType(TimelineFields.MAIL.getName());
                case FOLLOWERS_ADDED, FOLLOWERS_REMOVED, COMPANY_SALES_OWNER_REASSIGNED, CONTACT_SALES_OWNER_REASSIGNED, DEAL_SALES_OWNER_REASSIGNED -> timeline.setIconType(TimelineFields.USER.getName());
                case DEAL_ARCHIVED, DEAL_UNARCHIVED -> timeline.setIconType(TimelineFields.ARCHIVE.getName());
                case CALL_LOG, TRANSFERRED_CALL -> timeline.setIconType(TimelineFields.CALL.getName());
                case INCOMING_CALL, CONTACT_CREATE_UPON_CALL -> timeline.setIconType(TimelineFields.INCOMING_CALL.getName());
                case OUTGOING_CALL -> timeline.setIconType(TimelineFields.OUTGOING_CALL.getName());
                case MISSED_INCOMING_CALL -> timeline.setIconType(TimelineFields.MISSED_INCOMING_CALL.getName());
                case UN_ANSWERED_OUTGOING_CALL -> timeline.setIconType(TimelineFields.UNANSWERED_OUTGOING_CALL.getName());
                case DEAL_PRODUCTS_ADDED, DEAL_PRODUCTS_REMOVED -> timeline.setIconType(TimelineFields.PRODUCT.getName());
                case INVOICE_DRAFT, INVOICE_STATUS_UPDATED -> timeline.setIconType(TimelineFields.INVOICE.getName());
            }

            if (Objects.equals(TimelineType.CHECK_IN, type) || Objects.equals(TimelineType.CHECK_OUT, type) || Objects.equals(TimelineType.ASSIGNED_TO_CHAT, type)) {
                return timeline.getIcon();
            }

            return iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE, timeline.getIconType());
        }
    }

    /**
     * <p>
     * Loads the source timeline row into the container.
     * </p>
     *
     * @param timelineId      Represents timeline id
     * @param sourceId        Represents source id
     * @param row             Holds the row of the source timeline table
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadSourceTimelineRowIntoContainer(final Long timelineId, final Long sourceId, final String sourceColumn,
                                                   final String timelineColumn, final Row row, final DataContainer targetContainer) {

        if (Objects.nonNull(row)) {
            row.set(sourceColumn, sourceId);
            row.set(timelineColumn, timelineId);

            targetContainer.updateRow(row);
        }
    }

    /**
     * <p>
     * Processes the field value timeline.
     * </p>
     *
     * @param timelineId    Represents timelineId
     * @param fieldTimeline Holds the updated timeline values
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    private void processFieldTimeline(final Long timelineId, final FieldTimeline fieldTimeline,
                                      final DataContainer dataContainer) {
        final Collection<String> oldValues = fieldTimeline.getOldValues();
        final Table fieldValueTimelineTable = getFieldValueTimelineTable();

        if (Objects.isNull(oldValues) || oldValues.isEmpty()) {
            loadFieldValueRowIntoContainer(fieldValueTimelineTable, timelineId, fieldTimeline, fieldTimeline.getOldValue(), dataContainer);
        } else {
            oldValues.forEach(oldValue -> loadFieldValueRowIntoContainer(fieldValueTimelineTable, timelineId, fieldTimeline, oldValue, dataContainer));
        }
    }

    /**
     * <p>
     * Loads the field value timeline row into target {@link DataContainer}.
     * </p>
     *
     * @param fieldValueTimelineTable Represents the {@link Table} of field value timeline
     * @param timelineId              Represents the timeline id
     * @param fieldTimeline           Represents the {@link FieldTimeline}
     * @param oldValue                Represents the existing value
     * @param targetContainer           Holds the updated information of timeline field values
     */
    private void loadFieldValueRowIntoContainer(final Table fieldValueTimelineTable, final Long timelineId, final FieldTimeline fieldTimeline,
                                                final String oldValue, final DataContainer targetContainer) {
        final Row fieldValueTimelineRow = new Row(fieldValueTimelineTable);
        final Collection<String> newValues = fieldTimeline.getNewValues();

        fieldValueTimelineRow.set(MTLFIELDVALUETIMELINE.TIMELINE_ID, timelineId);
        fieldValueTimelineRow.set(MTLFIELDVALUETIMELINE.FIELD_NAME, fieldTimeline.getFieldName());
        fieldValueTimelineRow.set(MTLFIELDVALUETIMELINE.OLD_VALUE, oldValue);

        if (Objects.nonNull(newValues) && !newValues.isEmpty()) {
            fieldValueTimelineRow.set(MTLFIELDVALUETIMELINE.NEW_VALUE, newValues.iterator().next());
        }

        targetContainer.addNewRow(fieldValueTimelineRow);
    }

    /**
     * <p>
     * Gets the timeline table
     * </p>
     *
     * @return the {@link Table} of timeline
     */
    public Table getTable() {
        return Table.get(OMTLTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the field value timeline table.
     * </p>
     *
     * @return the {@link Table} of field value timeline
     */
    public Table getFieldValueTimelineTable() {
        return Table.get(MTLFIELDVALUETIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the contact timeline table.
     * </p>
     *
     * @return the {@link Table} of contact timeline
     */
    public Table getContactTimelineTable() {
        return Table.get(MTLCONTACTTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the deal timeline table.
     * </p>
     *
     * @return the {@link Table} of deal timeline
     */
    public Table getDealTimelineTable() {
        return Table.get(MTLDEALTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the company timeline table.
     * </p>
     *
     * @return the {@link Table} of company timeline
     */
    public Table getCompanyTimelineTable() {
        return Table.get(MTLCOMPANYTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the timeline will focus exclusively on merge-related actions within the context of a given organization,
     * owner, and category.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerId  Represents the owner id
     * @param category Represents the {@link Timeline.Category} of timeline
     * @return {@link Timeline}
     */
    public Timeline getMergedTimeline(final long orgId, final long ownerId, final Timeline.Category category) {
        final Timeline timeline = new Timeline();

        timeline.setOrgId(orgId);
        timeline.setAction(TimelineActionFields.MERGED);
        timeline.setCategory(category);
        timeline.setTimelineType(switch (category) {
            case COMPANY -> TimelineType.COMPANY_MERGED;
            case CONTACT -> TimelineType.CONTACT_MERGED;
            case DEAL -> TimelineType.DEAL_MERGED;
            default ->
                    throw ErrorCode.get(TimelineErrorCodes.UNSUPPORTED_MERGE_CATEGORY, "i18n.timeline.merge.category.unsupported");
        });
        timeline.setOwnerId(ownerId);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));

        return timeline;
    }

    /**
     * <p>
     * Loads the merge timeline and removing the duplicate rows into the target {@link DataContainer}.
     * </p>
     *
     * @param timeline        Represents the {@link Timeline}
     * @param duplicateIds    Represents the duplicate contact/company/deal ids
     * @param id              Represents the primary id
     * @param dataContainer   Holds the existing information of timeline
     * @param targetContainer Holds the updated information of timeline
     */
    public void loadMergeTimeline(final Timeline timeline, final Collection<Long> duplicateIds, final Long id,
                                  final DataContainer dataContainer, final DataContainer targetContainer) {
        final Set<Long> entityIds = new HashSet<>(duplicateIds);
        final Table timelineTable = getTable();
        final String idKey;
        final int type;
        final Table table;
        final Category category = timeline.getCategory();
        final String timelineIdKey = TimelineFields.TIMELINE_ID.name();

        switch (category) {
            case COMPANY -> {
                idKey = TimelineFields.COMPANY_ID.name();
                type = TimelineType.COMPANY_CREATED.getId();
                table = getCompanyTimelineTable();
            }
            case CONTACT -> {
                idKey = TimelineFields.CONTACT_ID.name();
                type = TimelineType.CONTACT_CREATED.getId();
                table = getContactTimelineTable();
            }
            case DEAL -> {
                idKey = TimelineFields.DEAL_ID.name();
                type = TimelineType.DEAL_CREATED.getId();
                table = getDealTimelineTable();
            }
            default ->
                    throw ErrorCode.get(TimelineErrorCodes.UNSUPPORTED_SOURCE_CATEGORY, "i18n.timeline.source.category.unsupported");
        }

        entityIds.add(id);
        final Collection<Row> rows = dataContainer.getRows(table,
                new WhereClause(Criteria.in(table.getColumn(idKey), entityIds))).toList();
        final Collection<Long> timelineIds = rows.stream().map(row -> (Long) row.get(timelineIdKey)).toList();
        final Collection<Row> timelineRows = dataContainer.getRows(timelineTable, new WhereClause(Criteria.in(timelineTable.getPKColumn(), timelineIds))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.ACTION), TimelineActionFields.CREATED.toLowerCase()))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.TYPE), type))
                .and(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), category.name()))).toList();
        Row minimumAuditedTimeRow = null;

        for (final Row row : timelineRows) {
            final DateTime auditedTime = row.get(OMTLTIMELINE.AUDITED_TIME);
            final boolean isMinimumAuditedTimeRowNull = Objects.isNull(minimumAuditedTimeRow);
            final DateTime minimumAuditedTime = isMinimumAuditedTimeRowNull ? null : minimumAuditedTimeRow.get(OMTLTIMELINE.AUDITED_TIME);

            if (isMinimumAuditedTimeRowNull || auditedTime.getTimeStamp() < minimumAuditedTime.getTimeStamp()) {
                minimumAuditedTimeRow = row;
            } else if (!row.equals(minimumAuditedTimeRow)) {
                targetContainer.deleteRow(row);
            }
        }

        loadTimelineRowIntoContainer(timeline, new Row(timelineTable), targetContainer);
        loadSourceTimelineRowIntoContainer(timeline.id(), id, idKey, timelineIdKey, new Row(table), targetContainer);
    }
}