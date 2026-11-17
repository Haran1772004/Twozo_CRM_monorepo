package com.tlc.crm.core.features.api.activity.manager;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.features.api.activity.model.ActivityFilterCriteria;
import com.tlc.crm.core.features.api.activity.model.ActivityGuest;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.core.features.field.activity.ActivityHolderFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.index.service.IndexDataFetcher;
import com.tlc.crm.core.sql.resource.MACACTIVITYGUEST;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MTLACTIVITYTIMELINE;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.CheckInOutRecord;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MACNOTEACTIVITY;
import com.tlc.crm.entity.sql.resource.MNTNOTEMENTION;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYCOLLABORATOR;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.entity.sql.resource.OMCALEXTERNALEVENTS;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.icon.service.IconService;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.oltp.api.dml.OltpConditionalColumn;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.entity.features.api.activity.model.Status.OVERDUE;
import static com.tlc.crm.entity.features.api.activity.model.Status.UPCOMING;
import static com.tlc.crm.entity.features.api.activity.model.Status.COMPLETED;
import static com.tlc.crm.entity.fields.source.Source.COMPANY;
import static com.tlc.crm.entity.fields.source.Source.CONTACT;
import static com.tlc.crm.entity.fields.source.Source.DEAL;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.INNER;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;
import static com.tlc.sql.common.api.dml.OrderByClause.OrderType.ASCENDING;

/**
 * <p>
 * Implements the {@link Activity} related DB actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class SourceActivityManager extends AbstractConfigManager<ActivityHolder> {

    private static final int ZERO = 0;
    private static final short TIMELINE_NOTE_MAX_LENGTH = 200;

    private final SourceNoteManager sourceNoteManager;
    private final CommonTimelineManager commonTimelineManager;
    private final ReadableTimelineManager readableTimelineManager;
    private final ActivityTypeManager activityTypeManager;
    private final ActivityManager activityManager;
    private final MonetaryManager monetaryManager;
    private final PipelineStageManager pipelineStageManager;
    private final UserService userService;
    private final IconService iconService;
    private final EmailManager emailManager;
    private final AuditLogger auditLogger;
    private final FieldService fieldService;
    private final ModificationHandler modificationHandler;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final NoteManager noteManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;

    private static class Instance {
        private static final SourceActivityManager INSTANCE = new SourceActivityManager();
    }

    /**
     * <p>
     * Gets the {@link SourceActivityManager} Instance
     * </p>
     *
     * @return {@link SourceActivityManager}
     */
    public static SourceActivityManager getInstance() {
        return Instance.INSTANCE;
    }

    private SourceActivityManager() {
        sourceNoteManager = SourceNoteManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        readableTimelineManager = ReadableTimelineManager.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        activityManager = ActivityManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        userService = UserService.getInstance();
        iconService = IconService.getInstance();
        emailManager = EmailManager.getInstance();
        auditLogger = AuditLogger.getInstance();
        fieldService = FieldService.getSecureInstance();
        modificationHandler = ModificationHandler.getInstance();
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        noteManager = NoteManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of activity
     */
    @Override
    public Table getTable() {
        return Table.get(OMACACTIVITY.TABLE);
    }

    /**
     * <p>
     * Return contactActivity Table
     * </p>
     *
     * @return the {@link Table} of contactActivity
     */
    public Table getContactActivityTable() {
        return Table.get(MACCONTACTACTIVITY.TABLE);
    }

    /**
     * <p>
     * Return dealActivity Table
     * </p>
     *
     * @return the {@link Table} of dealActivity
     */
    public Table getDealActivityTable() {
        return Table.get(MACDEALACTIVITY.TABLE);
    }

    /**
     * <p>
     * Return companyActivity Table
     * </p>
     *
     * @return the {@link Table} of companyActivity
     */
    public Table getCompanyActivityTable() {
        return Table.get(MACCOMPANYACTIVITY.TABLE);
    }

    /**
     * <p>
     * Gets the guest email contact activity table
     * </p>
     *
     * @return The guest contact activity {@link Table}
     */
    private Table getActivityGuestTable() {
        return Table.get(MACACTIVITYGUEST.TABLE);
    }

    /**
     * <p>
     * Gets the calendar external events table.
     * </p>
     *
     * @return The {@link Table} of calendar external event
     */
    private Table getCalendarExternalEventsTable() {
        return Table.get(OMCALEXTERNALEVENTS.TABLE);
    }

    /**
     * <p>
     * Gets the email table.
     * </p>
     *
     * @return The {@link Table} of email
     */
    private Table getEmailTable() {
        return Table.get(OMETEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the phone table.
     * </p>
     *
     * @return The {@link Table} of phone
     */
    private Table getPhoneTable() {
        return Table.get(OMETPHONE.TABLE);
    }

    /**
     * <p>
     * Gets the deal table.
     * </p>
     *
     * @return The {@link Table} of deal
     */
    private Table getDealTable() {
        return Table.get(MDODEAL.TABLE);
    }


    /**
     * <p>
     * Gets the deal table.
     * </p>
     *
     * @return The {@link Table} of deal
     */
    private Table getContactTable() {
        return Table.get(MCOCONTACT.TABLE);
    }

    /**
     * <p>
     * Gets the company table.
     * </p>
     *
     * @return The {@link Table} of company
     */
    private Table getCompanyTable() {
        return Table.get(MCOCOMPANY.TABLE);
    }

    /**
     * <p>
     * Gets the {@link DealManager}
     * </p>
     *
     * @return the {@link DealManager}
     */
    private DealManager getDealManager() {
        return DealManager.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Activity} that has one of the id values mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Activity ids
     * @return Collection of {@link Activity}
     */
    @Override
    public Collection<ActivityHolder> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer activitydatacontainer = getActivityDataContainer(orgId, ids);
        final Stream<Row> activityRows = activitydatacontainer.getRows(getTable());

        return activityRows.map(row -> convertRowToModel(row, activitydatacontainer)).toList();
    }

    /**
     * <p>
     * Fetches the summary information for an activity by given activity id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the activity id
     * @return The {@link ActivityHolder} details
     */
    public ActivityHolder getActivities(final Long orgId, final Long id) {
        final Table table = getTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table contactTable = Table.get(contactManager.getTable().getName(), ActivityHolderFields.CONTACTS.getName());
        final Table activityGuestTable = getActivityGuestTable();
        final Table emailTable = Table.get(emailManager.getTable().getName(), ActivityHolderFields.EMAILS.getName());
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column activityPkColumn = table.getPKColumn();
        final Column activityTypePkColumn = activityTypeTable.getPKColumn();
        final Column typeIdColumn = table.getColumn(OMACACTIVITY.TYPE_ID);

        selectQuery.addJoinClause(List.of(
                new JoinClause(typeIdColumn, activityTypePkColumn),
                new JoinClause(activityPkColumn, activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID), leftJoin),
                new JoinClause(activityGuestTable.getColumn(MACACTIVITYGUEST.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                new JoinClause(activityGuestTable.getColumn(MACACTIVITYGUEST.CONTACT_ID), contactTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(table.getColumn(OMACACTIVITY.ORG_ID), table.getColumn(OMACACTIVITY.STATUS), table.getColumn(OMACACTIVITY.END_TIME),
                table.getColumn(OMACACTIVITY.DESCRIPTION), table.getColumn(OMACACTIVITY.REMINDER), table.getColumn(OMACACTIVITY.IS_DONE),
                table.getColumn(OMACACTIVITY.CREATOR_ID), table.getColumn(OMACACTIVITY.TITLE), table.getColumn(OMACACTIVITY.PRIORITY),
                typeIdColumn, table.getColumn(OMACACTIVITY.LOCATION), table.getColumn(OMACACTIVITY.AVAILABILITY), table.getColumn(OMACACTIVITY.START_TIME), table.getColumn(OMACACTIVITY.COMPLETED_TIME),
                table.getColumn(OMACACTIVITY.ASSIGNEE_ID), table.getColumn(OMACACTIVITY.CHECK_IN_INFO), table.getColumn(OMACACTIVITY.CHECK_OUT_INFO),
                table.getColumn(OMACACTIVITY.CHECKED_IN_TIME), table.getColumn(OMACACTIVITY.CHECKED_OUT_TIME), table.getColumn(OMACACTIVITY.CREATED_TIME),
                table.getColumn(OMACACTIVITY.MODIFIED_BY), table.getColumn(OMACACTIVITY.MODIFIED_TIME), activityTypePkColumn,
                activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), emailTable.getColumn(OMETEMAIL.VALUE),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getPKColumn()));
        selectQuery.addSelectClause(activityGuestTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityPkColumn, id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }
        dataContainer.append(getContactDataContainer(orgId, List.of(id)));
        dataContainer.append(getCompanyDataContainer(orgId, id));
        dataContainer.append(getDealDataContainer(orgId, id));
        dataContainer.append(getNoteDataContainer(orgId, id));

        return convertRowToActivitySummary(dataContainer.getRow(table), dataContainer);
    }

    /**
     * <p>
     * Gets the note data container
     * </p>
     *
     * @param orgId      Represents the organisation id
     * @param activityId Represents the activity id
     * @return The note data container
     */
    private DataContainer getNoteDataContainer(final Long orgId, final Long activityId) {
        final Table noteTable = sourceNoteManager.getTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final SelectQuery noteSelectQuery = SelectQuery.get(noteTable);

        noteSelectQuery.addJoinClause(new JoinClause(noteTable.getPKColumn(), noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID)));
        noteSelectQuery.addSelectClause(noteActivityTable);
        noteSelectQuery.addSelectClause(noteTable.getColumn(OMNTNOTE.CONTENT));
        noteSelectQuery.setWhereClause(new WhereClause(Criteria.eq(noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID), activityId)));

        return orgDataStore(orgId).get(noteSelectQuery);
    }

    /**
     * <p>
     * Gets the company data container
     * </p>
     *
     * @param orgId      Represents the organisation id
     * @param activityId Represents the activity id
     * @return The company data container
     */
    private DataContainer getCompanyDataContainer(final Long orgId, final Long activityId) {
        final Table companyTable = companyManager.getTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final SelectQuery companySelectQuery = SelectQuery.get(companyTable);

        companySelectQuery.addJoinClause(new JoinClause(companyTable.getPKColumn(), companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID)));
        companySelectQuery.addSelectClause(companyActivityTable);
        companySelectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID),
                companyTable.getColumn(MCOCOMPANY.OWNER_ID), companyTable.getColumn(MCOCOMPANY.WEBSITE)));
        companySelectQuery.setWhereClause(new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), activityId)));

        return orgDataStore(orgId).get(companySelectQuery);
    }

    /**
     * <p>
     * Gets the deal data container
     * </p>
     *
     * @param orgId      Represents the organisation id
     * @param activityId Represents the activity id
     * @return The deal data container
     */
    private DataContainer getDealDataContainer(final Long orgId, final Long activityId) {
        final Table dealTable = getDealManager().getTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery dealSelectQuery = SelectQuery.get(dealTable);

        dealSelectQuery.addJoinClause(List.of(
                new JoinClause(dealTable.getPKColumn(), dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID)),
                new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()),
                new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable.getPKColumn())
        ));
        dealSelectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.MONETARY_ID),
                dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), monetaryTable.getColumn(OMETMONETARY.ORG_ID), monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID),
                monetaryTable.getColumn(OMETMONETARY.VALUE), pipelineStageTable.getPKColumn(), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        dealSelectQuery.addSelectClause(dealActivityTable);
        dealSelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityId)));

        return orgDataStore(orgId).get(dealSelectQuery);
    }

    /**
     * <p>
     * Converts the row to activity summary details
     * </p>
     *
     * @param row           Represents the activity row
     * @param dataContainer Holds the field values of existing activity
     * @return The {@link ActivityHolder} details
     */
    private ActivityHolder convertRowToActivitySummary(final Row row, final DataContainer dataContainer) {
        final long orgId = row.getOrgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE);
        final Long emailFieldId = fields.get(ContactFields.EMAILS).id();
        final Long companyFieldId = fields.get(ContactFields.COMPANY).id();
        final Long phoneFieldId = fields.get(ContactFields.PHONES).id();
        final ActivityHolder activityHolder = new ActivityHolder();
        final Activity activity = new Activity();
        final Row companyActivityRow = dataContainer.getRow(getCompanyActivityTable());
        final Row dealActivityRow = dataContainer.getRow(getDealActivityTable());
        final Row noteActivityRow = dataContainer.getRow(activityManager.getNoteActivityTable());
        final Row activityTypeRow = dataContainer.getRow(activityTypeManager.getTable(), (Long) row.get(OMACACTIVITY.TYPE_ID));
        final Table contactTable = contactManager.getTable();
        final Table contactAliasTable = Table.get(contactTable.getName(), ActivityHolderFields.CONTACTS.getName());
        final Table activityGuestTable = getActivityGuestTable();
        final Table emailTable = emailManager.getTable();
        final Table emailAliasTable = Table.get(emailTable.getName(), ActivityHolderFields.EMAILS.getName());
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Table phoneTable = Table.get(OMETPHONE.TABLE);
        final List<Contact> contacts = new ArrayList<>();
        final Collection<Long> userIds = new HashSet<>();
        final Long assigneeId = row.get(OMACACTIVITY.ASSIGNEE_ID);
        final Long creatorId = row.get(OMACACTIVITY.CREATOR_ID);
        final Long modifiedById = row.get(OMACACTIVITY.MODIFIED_BY);

        if (Objects.nonNull(companyActivityRow)) {
            final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                    companyActivityRow.get(MACCOMPANYACTIVITY.COMPANY_ID))));

            if (Objects.nonNull(companyRow)) {
                final Company company = new Company();

                company.setId(companyRow.getPKValue());
                company.setOrgId(orgId);
                company.setOwnerId(companyRow.get(MCOCOMPANY.OWNER_ID));
                company.setName(companyRow.get(MCOCOMPANY.NAME));
                company.setWebsite(companyRow.get(MCOCOMPANY.WEBSITE));
                activityHolder.setCompany(company);
            }
        }

        if (Objects.nonNull(dealActivityRow)) {
            final Table dealTable = getDealManager().getTable();
            final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(),
                    dealActivityRow.get(MACDEALACTIVITY.DEAL_ID))));

            if (Objects.nonNull(dealRow)) {
                final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable());
                final Row pipelineStageRow = dataContainer.getRow(pipelineStageManager.getTable());
                final Deal deal = new Deal();

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
                }
                deal.setId(dealRow.getPKValue());
                deal.setTitle(dealRow.get(MDODEAL.TITLE));
                deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
                deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
                activityHolder.setDeal(deal);
            }
        }

        if (Objects.nonNull(noteActivityRow)) {
            final Table noteTable = sourceNoteManager.getTable();
            final Row noteRow = dataContainer.getRow(noteTable, new WhereClause(Criteria.eq(noteTable.getPKColumn(),
                    noteActivityRow.get(MACNOTEACTIVITY.NOTE_ID))));

            if (Objects.nonNull(noteRow)) {
                final Note note = new Note();

                note.setOrgId(orgId);
                note.setContent(noteRow.get(OMNTNOTE.CONTENT));
                activity.setNote(note);
            }
        }

        if (Objects.nonNull(activityTypeRow)) {
            final ActivityType activityType = new ActivityType();

            activityType.setName(activityTypeRow.get(OMACACTIVITYTYPE.NAME));
            activityType.setActivityIcon(IconService.getInstance().get(activityTypeRow.get(OMACACTIVITYTYPE.ICON_ID)));
            activity.setType(activityType);
        }

        dataContainer.getRows(contactTable).forEach(contactRow -> {
            final Contact contact = new Contact();
            final Long contactId = contactRow.getPKValue();
            final Stream<Row> contactEmailRows = dataContainer.getRows(contactEmailTable, new WhereClause(Criteria
                    .eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId)).and(Criteria
                    .eq(contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), emailFieldId)));
            final Stream<Row> contactPhoneRows = dataContainer.getRows(contactPhoneTable, new WhereClause(Criteria
                    .eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId)).and(Criteria
                    .eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID), phoneFieldId)));
            final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria.
                    eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId)).and(Criteria
                    .eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId)));
            final Collection<Long> emailIds = contactEmailRows.map(contactEmailRow -> (Long) contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID)).toList();
            final Collection<Long> phoneIds = contactPhoneRows.map(contactPhoneRow -> (Long) contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID)).toList();

            if (!emailIds.isEmpty()) {
                final Stream<Row> emailRows = dataContainer.getRows(emailTable,
                        new WhereClause(Criteria.in(emailTable.getPKColumn(), emailIds)));

                emailRows.forEach(emailRow -> {
                    final Email email = new Email();

                    email.setId(emailRow.getPKValue());
                    email.setValue(emailRow.get(OMETEMAIL.VALUE));
                    contact.addEmail(email);
                });
            }

            if (!phoneIds.isEmpty()) {
                final Stream<Row> phoneRows = dataContainer.getRows(phoneTable,
                        new WhereClause(Criteria.in(phoneTable.getPKColumn(), phoneIds)));

                phoneRows.forEach(phoneRow -> {
                    final Phone phone = new Phone();
                    final Integer countryId = phoneRow.get(OMETPHONE.COUNTRY_ID);

                    if (Objects.nonNull(countryId)) {
                        phone.setCountry(Country.getCountryById(countryId));
                    }
                    phone.setId(phoneRow.getPKValue());
                    phone.setNumber(phoneRow.get(OMETPHONE.NUMBER));
                    contact.addPhone(phone);
                });
            }

            if (Objects.nonNull(contactCompanyRow)) {
                final Row companyRow = dataContainer.getRow(companyTable, (Long) contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID));

                if (Objects.nonNull(companyRow)) {
                    contact.setCompany(companyManager.convertRowToPartialModel(companyRow));
                }
            }
            contact.setId(contactId);
            contact.setOrgId(orgId);
            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            contact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
            contact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
            contact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
            contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));

            contacts.add(contact);
        });
        activity.setId(row.getPKValue());
        activity.setOrgId(orgId);
        activity.setDone(row.get(OMACACTIVITY.IS_DONE));
        activity.setEndTime(row.get(OMACACTIVITY.END_TIME));
        activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
        activity.setCreatorId(creatorId);
        activity.setAssigneeId(assigneeId);
        activity.setModifiedById(modifiedById);
        activity.setReminder(Reminder.get(row.get(OMACACTIVITY.REMINDER)));
        activity.setDescription(row.get(OMACACTIVITY.DESCRIPTION));
        activity.setPriority(Priority.get(row.get(OMACACTIVITY.PRIORITY)));
        activity.setAvailability(Availability.get(row.get(OMACACTIVITY.AVAILABILITY)));
        activity.setStatus(Status.get(row.get(OMACACTIVITY.STATUS)));
        activity.setTitle(row.get(OMACACTIVITY.TITLE));
        activity.setLocation(row.get(OMACACTIVITY.LOCATION));
        loadCheckInOutRecordIntoActivity(row, activity);
        activity.setCreatedTime(row.get(OMACACTIVITY.CREATED_TIME));
        activity.setUpdatedTime(row.get(OMACACTIVITY.MODIFIED_TIME));
        activityHolder.setActivity(activity);
        activityHolder.setContacts(contacts);
        userIds.add(assigneeId);
        userIds.add(creatorId);
        userIds.add(modifiedById);
        activityHolder.setUserIds(userIds);
        final Stream<Row> activityGuestRows = dataContainer.getRows(activityGuestTable,
                new WhereClause(Criteria.eq(activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID), activity.id())));

        convertRowToActivityGuest(dataContainer, activityGuestRows, contactAliasTable, emailAliasTable, activityHolder);

        return activityHolder;
    }

    /**
     * <p>
     * Loads the {@link CheckInOutRecord} into activity
     * </p>
     *
     * @param row      Represents the activity row
     * @param activity Represents the {@link Activity}
     */
    private void loadCheckInOutRecordIntoActivity(final Row row, final Activity activity) {
        final String checkIn = row.get(OMACACTIVITY.CHECK_IN_INFO);

        if (Objects.nonNull(checkIn)) {
            final CheckInOutRecord checkInRecord = new CheckInOutRecord();

            checkInRecord.setDataAsString(checkIn);
            final DateTime dateTime = row.get(OMACACTIVITY.CHECKED_IN_TIME);

            if (Objects.nonNull(dateTime)) {
                checkInRecord.setTime(dateTime.getTimeStamp());
            }
            activity.setCheckInRecord(checkInRecord);
        }
        final String checkOut = row.get(OMACACTIVITY.CHECK_OUT_INFO);

        if (Objects.nonNull(checkOut)) {
            final CheckInOutRecord checkOutRecord = new CheckInOutRecord();
            final DateTime checkedInTime = row.get(OMACACTIVITY.CHECKED_IN_TIME);
            final DateTime checkedOutTime = row.get(OMACACTIVITY.CHECKED_OUT_TIME);

            checkOutRecord.setDataAsString(checkOut);
            checkOutRecord.setTime(checkedOutTime.getTimeStamp());
            activity.setCheckOutRecord(checkOutRecord);
            activity.setTotalTimeSpent(checkedOutTime.getTimeStamp() - checkedInTime.getTimeStamp());
        }
    }

    /**
     * <p>
     * Converts row into {@link Activity}
     * </p>
     *
     * @param row Activity row to be converted
     * @return {@link Activity}
     */
    private ActivityHolder convertRowToModel(final Row row, final DataContainer dataContainer) {
        final ActivityHolder activityHolder = new ActivityHolder();
        final Collection<Long> userIds = new HashSet<>();
        final Table contactActivityTable = getContactActivityTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table contactTable = contactManager.getTable();
        final Table dealTable = getDealManager().getTable();
        final Table companyTable = companyManager.getTable();
        final Table activityGuestTable = getActivityGuestTable();
        final Table emailTable = emailManager.getTable();
        final Table contactAliasTable = Table.get(contactTable.getName(), ActivityHolderFields.CONTACTS.getName());
        final Activity activity = activityManager.convertRowToModel(row, dataContainer);

        loadCheckInOutRecordIntoActivity(row, activity);
        activityHolder.setActivity(activity);
        final Long id = activityHolder.id();
        final List<Long> contactIds = dataContainer.getRows(contactActivityTable, new WhereClause(Criteria.eq(
                contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), id))).map(contactActivityRow ->
                (Long) contactActivityRow.get(MACCONTACTACTIVITY.CONTACT_ID)).collect(Collectors.toList());
        final Stream<Row> contactRows = dataContainer.getRows(contactTable, new WhereClause(Criteria.in(contactTable.getPKColumn(),
                contactIds)));

        activityHolder.setContacts(contactRows.map(contactManager::convertRowToPartialModel).toList());
        final Row dealActivityRow = dataContainer.getRow(dealActivityTable, new WhereClause(Criteria.eq(dealActivityTable
                .getColumn(MACDEALACTIVITY.ACTIVITY_ID), id)));

        if (Objects.nonNull(dealActivityRow)) {
            final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(),
                    dealActivityRow.get(MACDEALACTIVITY.DEAL_ID))));

            if (Objects.nonNull(dealRow)) {
                activityHolder.setDeal(getDealManager().convertRowToPartialModel(dealRow));
            }
        }
        final Row companyActivityRow = dataContainer.getRow(companyActivityTable, new WhereClause(Criteria.eq(companyActivityTable.
                getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), id)));

        if (Objects.nonNull(companyActivityRow)) {
            final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                    companyActivityRow.get(MACCOMPANYACTIVITY.COMPANY_ID))));

            if (Objects.nonNull(companyRow)) {
                activityHolder.setCompany(companyManager.convertRowToPartialModel(companyRow));
            }
        }
        final Stream<Row> activityGuestRows = dataContainer.getRows(activityGuestTable,
                new WhereClause(Criteria.eq(activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID), id)));

        convertRowToActivityGuest(dataContainer, activityGuestRows, contactAliasTable, emailTable, activityHolder);

        userIds.add(activity.getAssigneeId());
        userIds.add(activity.getCreatorId());
        userIds.add(activity.getModifiedById());
        userIds.addAll(activity.getCollaboratorIds());
        activityHolder.setUserIds(userIds);

        return activityHolder;
    }

    /**
     * <p>
     * Converts Activity rows to guest email contact
     * </p>
     *
     * @param dataContainer     Holds the updated information of field values provided by user
     * @param activityGuestRows Represents the activity rows
     * @param contactAliasTable Represents the contact alias table
     * @param emailTable        Represents the email table
     * @param activityHolder    Represents the {@link ActivityHolder} details
     */
    private void convertRowToActivityGuest(final DataContainer dataContainer, final Stream<Row> activityGuestRows, final Table contactAliasTable, final Table emailTable, final ActivityHolder activityHolder) {
        activityGuestRows.forEach(activityGuestRow -> {
            final ActivityGuest activityGuest = new ActivityGuest();
            final Row contactRow = dataContainer.getRow(contactAliasTable, (Long) activityGuestRow.get(MACACTIVITYGUEST.CONTACT_ID));
            final Row emailRow = dataContainer.getRow(emailTable, (Long) activityGuestRow.get(MACACTIVITYGUEST.EMAIL_ID));
            final Email email = new Email();

            email.setValue(emailRow.get(OMETEMAIL.VALUE));
            email.setId(emailRow.getPKValue());
            email.setOrgId(emailRow.getOrgId());

            if (Objects.nonNull(contactRow)) {
                final Contact contact = new Contact();

                contact.setId(contactRow.getPKValue());
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                contact.addEmail(email);
                activityGuest.setContact(contact);
            } else {
                activityGuest.setEmail(email);
            }
            activityHolder.addActivityGuests(activityGuest);
        });
    }

    /**
     * <p>
     * Converts the {@link Row} into a {@link ActivityHolder}.
     * </p>
     *
     * @param dataContainer Entity of {@link DataContainer}
     * @param row           Activity row to be converted
     * @return Entity of {@link ActivityHolder}
     */
    public ActivityHolder convertRowToActivityHolder(final Row row, final DataContainer dataContainer) {
        final ActivityHolder activityHolder = new ActivityHolder();

        activityHolder.setActivity(activityManager.convertRowToActivity(row, dataContainer));

        return activityHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ActivityHolder> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * Saves the attributes of {@link Activity} into the database
     * </p>
     *
     * @param activityHolder An entity that holds the activity attributes which have to be saved into database
     * @return {@link Activity}
     */
    @Override
    public ActivityHolder create(final ActivityHolder activityHolder) {
        final long orgId = activityHolder.orgId();
        final DataContainer targetContainer = DataContainer.create();
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        loadActivityIntoContainer(true, activityHolder, null, targetContainer, updateQueries);
        orgDataStore(orgId).commitChanges(targetContainer);
        orgDataStore(orgId).update(updateQueries);
        final Activity activity = activityHolder.getActivity();

        auditLogger.log(orgId, List.of(new AuditLogData(activity.getCreatorId(), activity.id(), activity, AuditSource.ACTIVITY, AuditAction.ADDED)));

        return activityHolder;
    }

    /**
     * <p>
     * Loads the activity details into the target container and entity data into activity holder
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityHolder  An entity that holds the activity attributes
     * @param entityContainer Holds the field values of existing activity
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadActivityIntoContainer(final boolean isCreateEvent, final ActivityHolder activityHolder,
                                          final DataContainer entityContainer, final DataContainer targetContainer,
                                          final List<UpdateQuery> updateQueries) {
        loadActivityDataIntoContainer(isCreateEvent, activityHolder, entityContainer, targetContainer, updateQueries);
        loadEntityDataIntoActivityHolder(activityHolder);
    }

    /**
     * <p>
     * Loads the activity details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityHolder  An entity that holds the activity attributes
     * @param entityContainer Holds the field values of existing activity
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadActivityDataIntoContainer(final boolean isCreateEvent, final ActivityHolder activityHolder,
                                              final DataContainer entityContainer, final DataContainer targetContainer,
                                              final List<UpdateQuery> updateQueries) {
        activityManager.loadActivityIntoContainer(isCreateEvent, activityHolder.getActivity(), entityContainer, targetContainer);
        final Long id = activityHolder.id();
        final Long orgId = activityHolder.orgId();
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final Long activityPkValue = activityHolder.id();
        final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();
        final Activity activity = activityHolder.getActivity();
        final Long userId = activity.getCreatorId();

        if (!contacts.isEmpty()) {
            loadContactActivityRowIntoContainer(isCreateEvent, orgId, userId, activityPkValue, contacts, targetContainer, entityContainer, updateQueries);
        }

        if (Objects.nonNull(deal)) {
            loadDealActivityRowIntoContainer(isCreateEvent, orgId, userId, activityPkValue, deal, targetContainer, entityContainer, updateQueries);
        }

        if (Objects.nonNull(company)) {
            loadCompanyActivityRowIntoContainer(isCreateEvent, orgId, userId, activityPkValue, company, targetContainer, entityContainer, updateQueries);
        }

        if (Objects.nonNull(activityGuests) && !activityGuests.isEmpty()) {

            for (final ActivityGuest activityGuest : activityGuests) {
                final Email email = activityGuest.getEmail();

                if (Objects.nonNull(email) && Objects.isNull(email.id())) {
                    email.setOrgId(orgId);
                    emailManager.loadEmailIntoContainer(true, email, targetContainer);
                }
            }
            loadActivityGuestRowIntoContainer(orgId, isCreateEvent, activityPkValue, activityGuests, targetContainer, entityContainer);
        }

        final Table activityTypeTable = activityTypeManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

        selectQuery.addSelectClause(activityTypeTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTypeTable.getPKColumn(), activityHolder.getActivity().getType().id())));
        final Row typeRow = orgDataStore(orgId).get(selectQuery).getRow(activityTypeTable);
        final Map<String, String> oldActivityValues = new HashMap<>();

        if (!isCreateEvent) {
            loadOldActivityValues(activity, oldActivityValues, entityContainer);
        }

        loadActivityTimeline(isCreateEvent, activityHolder, oldActivityValues, typeRow, entityContainer, targetContainer);

        if (!isCreateEvent) {
            deleteUnmappedContactActivity(id, orgId, contacts, targetContainer);
            deleteUnmappedDealActivity(id, orgId, userId, deal, targetContainer);
            deleteUnmappedCompanyActivity(id, orgId, company, targetContainer);
            deleteUnmappedActivityGuest(id, orgId, activityGuests, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the entity data into activity holder
     * </p>
     *
     * @param activityHolder An entity that holds the activity attributes
     */
    private void loadEntityDataIntoActivityHolder(final ActivityHolder activityHolder) {
        final Long orgId = activityHolder.orgId();
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final IndexDataFetcher indexDataFetcher = IndexDataFetcher.get();

        if (!contacts.isEmpty()) {
            activityHolder.setContacts(contactManager.partialGet(orgId, contacts.stream().map(Contact::id).
                    collect(Collectors.toSet())).stream().toList());
        }

        if (Objects.nonNull(deal)) {
            activityHolder.setDeal(indexDataFetcher.fetchDeal(orgId, deal.id()));
        }

        if (Objects.nonNull(company)) {
            activityHolder.setCompany(indexDataFetcher.fetchCompany(orgId, company.id()));
        }
    }

    /**
     * <p>
     * Deletes the email and contact that are not associated with the activity
     * </p>
     *
     * @param activityId     Represents activity id
     * @param orgId          Represents organisation id
     * @param activityGuests Represents guest contact details
     * @param dataContainer  Holds the field values of existing activity
     */
    private void deleteUnmappedActivityGuest(final Long activityId, final Long orgId, final Collection<ActivityGuest> activityGuests,
                                             final DataContainer dataContainer) {
        final Table activityGuestTable = getActivityGuestTable();
        WhereClause whereClause = new WhereClause(Criteria.eq(activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID),
                activityId));

        if (Objects.nonNull(activityGuests)) {
            final Collection<Contact> contacts = activityGuests.stream().map(ActivityGuest::getContact).filter(Objects::nonNull).collect(Collectors.toSet());
            final Collection<Email> emails = activityGuests.stream().map(ActivityGuest::getEmail).filter(Objects::nonNull).collect(Collectors.toSet());
            final Collection<Long> emailIds = new HashSet<>();

            if (!emails.isEmpty()) {
                emailIds.addAll(emails.stream().map(Email::id).collect(Collectors.toSet()));
            }

            if (!contacts.isEmpty()) {
                final Collection<Email> contactEmails = new HashSet<>();

                for (final Contact contact : contacts) {
                    final Collection<Email> mails = contact.getEmails();

                    if (Objects.nonNull(mails)) {
                        contactEmails.addAll(mails);
                    }
                }
                emailIds.addAll(contactEmails.stream().map(Email::id).collect(Collectors.toSet()));
            }
            whereClause = whereClause.and(Criteria.notIn(activityGuestTable.getColumn(MACACTIVITYGUEST.EMAIL_ID), emailIds));
        }
        final Stream<Row> rows = orgDataStore(orgId).get(activityGuestTable, whereClause).getRows(activityGuestTable);

        rows.forEach(dataContainer::deleteRow);
    }

    /**
     * <p>
     * Loads the {@link ActivityGuest} details row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityId      Represents the activity id
     * @param activityGuests  Represents the collection of {@link ActivityGuest} details
     * @param targetContainer Holds the updated information of field values provided by user
     * @param entityContainer Holds the field values of existing activity
     */
    private void loadActivityGuestRowIntoContainer(final Long orgId, final boolean isCreateEvent, final Long activityId,
                                                   final Collection<ActivityGuest> activityGuests, final DataContainer targetContainer,
                                                   final DataContainer entityContainer) {
        final Collection<Long> contactIds = activityGuests.stream().map(ActivityGuest::getContact).filter(Objects::nonNull).map(Contact::id).collect(Collectors.toUnmodifiableSet());
        final Map<Long, String> contactMap = contactIds.isEmpty() ? Collections.emptyMap() : contactManager.partialGet(orgId, contactIds).stream().filter(Objects::nonNull).collect(Collectors.toMap(Contact::id, Contact::getFullName));

        for (final ActivityGuest activityGuest : activityGuests) {
            final Contact contact = activityGuest.getContact();
            final Email email = activityGuest.getEmail();

            if (Objects.nonNull(contact)) {
                final Email contactEmail = contact.getEmails().stream().findFirst().get();

                contactEmail.setValue(emailManager.get(orgId, contactEmail.id()).getValue());
                contact.addEmail(contactEmail);
                loadActivityGuestRowIntoContainer(isCreateEvent, activityId, contact.id(), contactEmail.id(), targetContainer,
                        entityContainer);
                contact.setFullName(contactMap.get(contact.id()));
                activityGuest.setContact(contact);
                activityGuest.setEmail(contactEmail);
            }

            if (Objects.nonNull(email)) {
                loadActivityGuestRowIntoContainer(isCreateEvent, activityId, null, email.id(), targetContainer,
                        entityContainer);
                final Contact activityGuestContact = new Contact();

                activityGuestContact.setFullName(EmailAddress.extractNameFromEmail(email.getValue()));
                activityGuest.setContact(activityGuestContact);
                activityGuest.setEmail(email);
            }
        }
    }

    /**
     * <p>
     * Loads the {@link ActivityGuest} activity row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param activityId      Represents the activity id
     * @param contactId       Represents the contact id
     * @param emailId         Represents the email id
     * @param targetContainer Holds the updated information of field values provided by user
     * @param entityContainer Holds the field values of existing activity
     */
    private void loadActivityGuestRowIntoContainer(final boolean isCreateEvent, final Long activityId,
                                                   final Long contactId, final Long emailId, final DataContainer targetContainer,
                                                   final DataContainer entityContainer) {
        final Table activityGuestTable = getActivityGuestTable();
        final Row emailRow;

        if (isCreateEvent) {
            emailRow = new Row(activityGuestTable);
        } else {
            final Row oldRow = entityContainer.getRow(activityGuestTable,
                    new WhereClause(Criteria.eq(activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID), activityId))
                            .and(Criteria.eq(activityGuestTable.getColumn(MACACTIVITYGUEST.EMAIL_ID), emailId)));
            emailRow = Objects.isNull(oldRow) ? new Row(activityGuestTable) : oldRow;
        }
        emailRow.set(MACACTIVITYGUEST.EMAIL_ID, emailId);
        emailRow.set(MACACTIVITYGUEST.CONTACT_ID, contactId);
        emailRow.set(MACACTIVITYGUEST.ACTIVITY_ID, activityId);
        targetContainer.updateRow(emailRow);
    }

    /**
     * <p>
     * Loads the company activity row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgId           Represents an organisation id
     * @param userId          Represents an user id
     * @param activityId      Represents the activity id
     * @param company         An entity that holds the company attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadCompanyActivityRowIntoContainer(final boolean isCreateEvent, final Long orgId, final Long userId,
                                                     final Long activityId, final Company company, final DataContainer targetContainer,
                                                     final DataContainer entityContainer, final List<UpdateQuery> updateQueries) {
        final Table companyActivityTable = getCompanyActivityTable();
        final Long companyId = company.id();
        final Row companyActivityRow;

        if (isCreateEvent) {
            companyActivityRow = new Row(companyActivityTable);

            companyActivityRow.setOrgId(orgId);
        } else {
            final Row oldRow = entityContainer.getRow(companyActivityTable, new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), activityId))
                    .and(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyId)));
            companyActivityRow = Objects.isNull(oldRow) ? new Row(companyActivityTable) : oldRow;
        }
        companyActivityRow.set(MACCOMPANYACTIVITY.ACTIVITY_ID, activityId);
        companyActivityRow.set(MACCOMPANYACTIVITY.COMPANY_ID, companyId);
        targetContainer.updateRow(companyActivityRow);
        updateRecentActivityInEntity(activityId, companyId, getCompanyTable(), updateQueries);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(companyId), COMPANY, targetContainer);
    }

    /**
     * <p>
     * Loads the deal activity row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgId           Represents an organisation id
     * @param userId          Represents an user id
     * @param activityId      Represents the activity id
     * @param deal            An entity that holds the deal attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDealActivityRowIntoContainer(final boolean isCreateEvent, final Long orgId, final Long userId,
                                                  final Long activityId, final Deal deal, final DataContainer targetContainer,
                                                  final DataContainer entityContainer, final List<UpdateQuery> updateQueries) {
        final Table dealActivityTable = getDealActivityTable();
        final Long dealId = deal.id();
        final Row dealActivityRow;

        if (isCreateEvent) {
            dealActivityRow = new Row(dealActivityTable);

            dealActivityRow.setOrgId(orgId);
        } else {
            final Row oldRow = entityContainer.getRow(dealActivityTable, new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityId))
                    .and(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealId)));
            dealActivityRow = Objects.isNull(oldRow) ? new Row(dealActivityTable) : oldRow;
        }
        dealActivityRow.set(MACDEALACTIVITY.ACTIVITY_ID, activityId);
        dealActivityRow.set(MACDEALACTIVITY.DEAL_ID, dealId);
        targetContainer.updateRow(dealActivityRow);
        updateRecentActivityInEntity(activityId, dealId, getDealTable(), updateQueries);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(dealId), DEAL, targetContainer);
    }

    /**
     * <p>
     * Loads the contact activity row into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param orgId           Represents an organisation id
     * @param userId          Represents an user id
     * @param activityId      Represents the activity id
     * @param contacts        An entity that holds the list of contact attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadContactActivityRowIntoContainer(final boolean isCreateEvent, final Long orgId, final Long userId,
                                                     final Long activityId, final List<Contact> contacts,
                                                     final DataContainer targetContainer, final DataContainer entityContainer,
                                                     final List<UpdateQuery> updateQueries) {
        final Counter iterationCounter = new Counter();
        final Table contactActivityTable = getContactActivityTable();
        final Collection<Long> contactIds = new HashSet<>();
        final Table contactTable = getContactTable();

        for (final Contact contact : contacts) {
            final Long contactId = contact.id();
            final Row contactActivityRow;

            if (isCreateEvent) {
                contactActivityRow = new Row(contactActivityTable);

                contactActivityRow.setOrgId(orgId);
            } else {
                final Row oldRow = entityContainer.getRow(contactActivityTable, new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), activityId))
                        .and(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactId)));

                contactActivityRow = Objects.isNull(oldRow) ? new Row(contactActivityTable) : oldRow;
            }
            contactActivityRow.set(MACCONTACTACTIVITY.ACTIVITY_ID, activityId);
            contactActivityRow.set(MACCONTACTACTIVITY.CONTACT_ID, contactId);
            contactActivityRow.set(MACCONTACTACTIVITY.IS_PRIMARY, ZERO == iterationCounter.getCount());
            targetContainer.updateRow(contactActivityRow);
            contactIds.add(contactId);
            iterationCounter.increment();
            updateRecentActivityInEntity(activityId, contactId, contactTable, updateQueries);
        }
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, contactIds, CONTACT, targetContainer);
    }

    /**
     * <p>
     * Updates activity id in entity table.
     * </p>
     *
     * @param activityId  An activity's id.
     * @param entityId    An entity's id.
     * @param entityTable An entity's table.
     */
    private void updateRecentActivityInEntity(final Long activityId, final Long entityId, final Table entityTable,
                                              final List<UpdateQuery> updateQueries) {

        if (Objects.nonNull(updateQueries)) {
            final UpdateQuery entityUpdateQuery = UpdateQuery.get(entityTable);

            entityUpdateQuery.addUpdateColumn(entityTable.getColumn(MCOCONTACT.RECENT_ACTIVITY_ID), activityId);
            entityUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(entityTable.getPKColumn(), entityId)));
            updateQueries.add(entityUpdateQuery);
        }
    }

    /**
     * Deletes the contacts that are not associated with the activity
     *
     * @param activityId    Represents activity id
     * @param orgId         Represents organisation id
     * @param contacts      Represents contact ids
     * @param dataContainer Holds the new/updated/removed field values
     */
    private void deleteUnmappedContactActivity(final Long activityId, final Long orgId, final List<Contact> contacts, final DataContainer dataContainer) {
        final Table contactActivityTable = getContactActivityTable();
        final List<Long> contactIds = Objects.nonNull(contacts) ? contacts.stream().map(Contact::id).toList() : null;
        final WhereClause activityIdWhereclause = new WhereClause(Criteria.eq(contactActivityTable
                .getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), activityId));
        final WhereClause whereClause = Objects.nonNull(contactIds) && !contactIds.isEmpty() ? activityIdWhereclause
                .and(Criteria.notIn(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactIds)) : activityIdWhereclause;
        final Stream<Row> contactActivityRows = orgDataStore(orgId).get(contactActivityTable, whereClause).getRows(contactActivityTable);

        contactActivityRows.forEach(dataContainer::deleteRow);
    }

    /**
     * Delete the deal that are not associated with the activity
     *
     * @param activityId    Represents activity id
     * @param orgId         Represents organisation id
     * @param userId        Represents the user id
     * @param deal          Represents deal id
     * @param dataContainer Holds the new/updated/removed field values
     */
    private void deleteUnmappedDealActivity(final Long activityId, final Long orgId, final Long userId, final Deal deal, final DataContainer dataContainer) {
        final Table dealActivityTable = getDealActivityTable();
        final Long dealId = Objects.nonNull(deal) ? deal.id() : null;
        final WhereClause activityIdWhereclause = new WhereClause(Criteria.eq(dealActivityTable
                .getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityId));
        final WhereClause whereClause = Objects.nonNull(dealId) ? activityIdWhereclause.and(Criteria
                .notEq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealId)) : activityIdWhereclause;
        final Row dealActivityRow = orgDataStore(orgId).get(dealActivityTable, whereClause).getRow(dealActivityTable);

        if (Objects.nonNull(dealActivityRow)) {
            dataContainer.deleteRow(dealActivityRow);
            modificationHandler.loadModificationDetailsIntoContainer(orgId, userId,
                    List.of((Long) dealActivityRow.get(MACDEALACTIVITY.DEAL_ID)), DEAL, dataContainer);
        }
    }

    /**
     * Delete the company that are not associated with the activity
     *
     * @param activityId    Represents activity id
     * @param orgId         Represents organisation id
     * @param company       Represents company id
     * @param dataContainer Holds the new/updated/removed field values
     */
    private void deleteUnmappedCompanyActivity(final Long activityId, final Long orgId, final Company company, final DataContainer dataContainer) {
        final Table companyActivityTable = getCompanyActivityTable();
        final Long companyId = Objects.nonNull(company) ? company.id() : null;
        final WhereClause activityIdWhereclause = new WhereClause(Criteria.eq(companyActivityTable
                .getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), activityId));

        final WhereClause whereClause = Objects.nonNull(companyId) ? activityIdWhereclause
                .and(Criteria.notEq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyId)) : activityIdWhereclause;
        final Row companyActivityRow = orgDataStore(orgId).get(companyActivityTable, whereClause).getRow(companyActivityTable);

        if (Objects.nonNull(companyActivityRow)) {
            dataContainer.deleteRow(companyActivityRow);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents organisation id
     * @param activityHolders Collection of {@link Activity} have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<ActivityHolder> activityHolders) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = activityHolders.stream().map(ActivityHolder::getActivity).map(Activity::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = getActivityDataContainer(orgId, ids);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.UPDATED;
        final AuditSource auditSource = AuditSource.ACTIVITY;

        activityHolders.forEach(activityHolder -> {
            try {
                final DataContainer targetContainer = DataContainer.create();
                final List<UpdateQuery> updateQueries = new ArrayList<>();

                loadActivityIntoContainer(false, activityHolder, entityContainer, targetContainer, updateQueries);
                orgDataStore.commitChanges(targetContainer);
                orgDataStore.update(updateQueries);
                final Long activityId = activityHolder.id();
                final Row activityRow = entityContainer.getRow(getTable(), activityId);
                final Activity oldActivity = Objects.isNull(activityRow) ? null : convertRowToModel(activityRow, entityContainer).getActivity();
                final Activity activity = activityHolder.getActivity();

                processOutcome.addProcessedRecord(activityId);
                auditLogDataList.add(new AuditLogData(activity.getCreatorId(), activity.id(), oldActivity, activity, auditSource, auditAction));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Activity update failed Org ID : {} ID : {} reason : {}", orgId, activityHolder.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(ActivityErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, activityHolder.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(ActivityErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * <p>
     * Construct select query for activity
     * </p>
     *
     * @return selectQuery
     */
    private SelectQuery buildSelectQuery(final String source) {
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final Table noteTable = sourceNoteManager.getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table activityTable = getTable();
        final Column activityPkColumn = activityTable.getPKColumn();
        final Column activityCompanyIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID);
        final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column activityNoteIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID);
        final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column noteActivityIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addJoinClause(List.of(
                new JoinClause(activityPkColumn, dealActivityIdColumn, leftJoin), new JoinClause(activityPkColumn, companyActivityIdColumn, leftJoin),
                new JoinClause(activityPkColumn, noteActivityIdColumn, leftJoin),
                new JoinClause(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeTable.getPKColumn()),
                new JoinClause(activityNoteIdColumn, noteTable.getPKColumn(), leftJoin)));

        if (!source.equals(Deal.ENTITY_TYPE)) {
            final Table dealTable = getDealManager().getTable();
            final Table monetaryTable = MonetaryManager.getInstance().getTable();
            final Table pipelineStageTable = PipelineStageManager.getInstance().getTable();
            final Column dealPkColum = dealTable.getPKColumn();

            selectQuery.addJoinClause(List.of(new JoinClause(activityDealIdColumn, dealPkColum, leftJoin),
                    new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn(), leftJoin),
                    new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable.getPKColumn(), leftJoin)));
            selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID),
                    dealTable.getColumn(MDODEAL.MONETARY_ID), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
            selectQuery.addSelectClause(monetaryTable);
        }

        if (!source.equals(Company.ENTITY_TYPE)) {
            final Table companyTable = companyManager.getTable();
            final Column companyPkColumn = companyTable.getPKColumn();

            selectQuery.addJoinClause(new JoinClause(activityCompanyIdColumn, companyPkColumn, leftJoin));
            selectQuery.addSelectClause(List.of(companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID), companyTable.getColumn(MCOCOMPANY.WEBSITE)));
        }

        selectQuery.addSelectClause(activityTypeTable);
        selectQuery.addSelectClause(List.of(activityCompanyIdColumn, companyActivityIdColumn, activityDealIdColumn, dealActivityIdColumn,
                activityNoteIdColumn, noteActivityIdColumn, activityTable.getColumn(OMACACTIVITY.START_TIME),
                activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID),
                activityTable.getColumn(OMACACTIVITY.IS_DONE), activityTable.getColumn(OMACACTIVITY.TYPE_ID),
                activityTable.getColumn(OMACACTIVITY.END_TIME), noteTable.getColumn(OMNTNOTE.CONTENT),
                activityTable.getColumn(OMACACTIVITY.STATUS), activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID),
                activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO), activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO),
                activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME),
                activityTable.getColumn(OMACACTIVITY.CREATOR_ID), activityTable.getColumn(OMACACTIVITY.MODIFIED_BY),
                activityTable.getColumn(OMACACTIVITY.CREATED_TIME), activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME)));

        return selectQuery;
    }

    /**
     * <p>
     * Builds the dataContainer for activity
     * </P>
     *
     * @param orgId       Represents organisation id
     * @param activityIds Represents collection of activity id
     * @return {@link DataContainer} of mapping tables and its respective tables
     */
    private DataContainer getActivityDataContainer(final Long orgId, final Collection<Long> activityIds) {
        final Table activityTable = getTable();
        final Table contactActivityTable = getContactActivityTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final Table activityTimelineTable = readableTimelineManager.getActivityTimelineTable();
        final Table timelineTable = commonTimelineManager.getTable();
        final Table noteTable = sourceNoteManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table contactTable = contactManager.getTable();
        final Table dealTable = getDealManager().getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table collaboratorTable = activityManager.getCollaboratorTable();
        final Table activityGuestTable = getActivityGuestTable();
        final Table emailTable = emailManager.getTable();
        final Table contactAliasTable = Table.get(contactTable.getName(), ActivityHolderFields.CONTACTS.getName());
        final Column activityPkColumn = activityTable.getPKColumn();
        final Column dealPkColum = dealTable.getPKColumn();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column activityCompanyIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID);
        final Column activityContactIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID);
        final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column activityNoteIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID);
        final Column activityTimelineIdColumn = activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID);
        final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);
        final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column noteActivityIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);
        final Column timelineActivityIdColumn = activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.ACTIVITY_ID);
        final Column notePkColumn = noteTable.getPKColumn();
        final Table noteMentionedTable = Table.get(MNTNOTEMENTION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(activityPkColumn, contactActivityIdColumn, leftJoin),
                new JoinClause(activityPkColumn, dealActivityIdColumn, leftJoin), new JoinClause(activityPkColumn, companyActivityIdColumn, leftJoin),
                new JoinClause(activityPkColumn, noteActivityIdColumn, leftJoin),
                new JoinClause(activityPkColumn, timelineActivityIdColumn, leftJoin), new JoinClause(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeTable.getPKColumn()),
                new JoinClause(activityTimelineIdColumn, timelineTable.getPKColumn(), leftJoin), new JoinClause(activityNoteIdColumn, notePkColumn, leftJoin),
                new JoinClause(notePkColumn, noteMentionedTable.getColumn(MNTNOTEMENTION.NOTE_ID), leftJoin),
                new JoinClause(activityCompanyIdColumn, companyPkColumn, leftJoin),
                new JoinClause(activityContactIdColumn, contactPkColumn, leftJoin), new JoinClause(activityDealIdColumn, dealPkColum, leftJoin),
                new JoinClause(activityPkColumn, collaboratorTable.getColumn(OMACACTIVITYCOLLABORATOR.ACTIVITY_ID), leftJoin),
                new JoinClause(activityPkColumn, activityGuestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID), leftJoin),
                new JoinClause(activityGuestTable.getColumn(MACACTIVITYGUEST.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                new JoinClause(activityGuestTable.getColumn(MACACTIVITYGUEST.CONTACT_ID), contactAliasTable.getPKColumn(), leftJoin)));

        selectQuery.addSelectClause(activityTable, timelineTable, noteTable, noteMentionedTable, activityTypeTable, collaboratorTable, activityGuestTable);
        selectQuery.addSelectClause(List.of(activityContactIdColumn, contactActivityIdColumn,
                activityCompanyIdColumn, companyActivityIdColumn, activityDealIdColumn, dealActivityIdColumn,
                activityNoteIdColumn, noteActivityIdColumn,
                activityTimelineIdColumn, timelineActivityIdColumn, companyPkColumn, contactPkColumn,
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                dealTable.getColumn(MDODEAL.TITLE), companyTable.getColumn(MCOCOMPANY.NAME), dealPkColum, dealTable.getColumn(MDODEAL.TITLE), companyTable.getColumn(MCOCOMPANY.NAME),
                dealPkColum, emailTable.getColumn(OMETEMAIL.VALUE), contactAliasTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactAliasTable.getColumn(MCOCONTACT.LAST_NAME), contactAliasTable.getPKColumn(), emailTable.getColumn(OMETEMAIL.ORG_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(getTable().getPKColumn(), activityIds)));
        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Gets the activities which is mapped with entity
     * </p>
     *
     * @param orgId                  Represents the organisation id
     * @param entityId               Represents the entity id
     * @param start                  Represents the lower bound of activity to be fetched
     * @param limit                  Represents the upper bound of activity to be fetched
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param activityTypeIds        Represents the collection of activity type ids
     * @param source                 Represents the {@link Source}
     * @return Collection of {@link Activity}
     */
    public Collection<ActivityHolder> getSourceActivities(final Long orgId, final Long entityId, final int start, final int limit,
                                                          final ActivityFilterCriteria activityFilterCriteria, final Collection<Long> activityTypeIds, final Source source) {
        final DataContainer activityDataContainer = getActivitiesContainer(
                orgId, source, entityId, new LimitClause(start, limit), activityTypeIds, activityFilterCriteria
        );
        final Map<String, Map<Long, Row>> rowsByTableName = getRowsByTableName(activityDataContainer);

        return activityDataContainer.getRows(getTable()).map(activityRow ->
                convertActivityRowToModel(activityRow, activityDataContainer, rowsByTableName)).collect(Collectors.toList());
    }

    /**
     * <p>
     * Gets the rows from the datacontainer and stores in the Map using table name as key.
     * </p>
     *
     * @param activityDataContainer Represents the activity data container
     * @return a map of table name with its rows.
     */
    private Map<String, Map<Long, Row>> getRowsByTableName(final DataContainer activityDataContainer) {
        final Map<String, Map<Long, Row>> rowsByTableName = new HashMap<>();

        loadRowsByTableName(
                rowsByTableName, activityDataContainer, getDealActivityTable(), getCompanyActivityTable(),
                getContactActivityTable(), activityManager.getNoteActivityTable(), getCompanyTable(), getDealTable(),
                activityTypeManager.getTable(), monetaryManager.getTable(), pipelineStageManager.getTable(),
                sourceNoteManager.getTable(), getContactTable(), contactManager.getContactCompanyTable()
        );

        return rowsByTableName;
    }

    /**
     * <p>
     * Loads the rows from the datacontainer and stores in the Map using table name as key.
     * </p>
     *
     * @param rowsByTableName Holds the rows of each table in a map with table name as key
     * @param container       Represents the {@link DataContainer}
     * @param tables          Represents the list of {@link Table}
     */
    private void loadRowsByTableName(final Map<String, Map<Long, Row>> rowsByTableName,
                                     final DataContainer container, final Table... tables) {
        for (Table table : tables) {
            rowsByTableName.put(table.getName(), container.getRows(table)
                    .collect(Collectors.toMap(Row::getPKValue, Function.identity()))
            );
        }
    }

    /**
     * <p>
     * Loads related data such as deal, contact, company and note preview data into the container
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param source                Represents the {@link Source}
     * @param activityDataContainer Represents the {@link DataContainer}
     */
    private void loadRelatedDataIntoContainer(final long orgId, final Source source,
                                              final DataContainer activityDataContainer) {
        final Collection<Long> dealIds = getSpecifiedColumnIds(
                MACDEALACTIVITY.DEAL_ID, getDealActivityTable(), null, activityDataContainer);
        final Collection<Long> companyIds = getSpecifiedColumnIds(
                MACCOMPANYACTIVITY.COMPANY_ID, getCompanyActivityTable(), null, activityDataContainer);
        final Collection<Long> noteIds = getSpecifiedColumnIds(
                MACNOTEACTIVITY.NOTE_ID, activityManager.getNoteActivityTable(), null, activityDataContainer);

        if (!dealIds.isEmpty()) {
            getDealManager().loadDealPreviewDataIntoContainer(orgId, dealIds, activityDataContainer);
        }

        if (!companyIds.isEmpty()) {
            companyManager.loadCompanyPreviewDataIntoContainer(orgId, companyIds, activityDataContainer);
        }

        if (!noteIds.isEmpty()) {
            noteManager.loadNotePreviewDataIntoContainer(orgId, noteIds, activityDataContainer);
        }

        if (COMPANY.equals(source) || DEAL.equals(source)) {
            final Collection<Long> activityIds = getSpecifiedColumnIds(
                    ActivityHolderFields.ACTIVITY_ID.getName(), getSourceActivityTable(source), null, activityDataContainer);

            loadActivityContactDataIntoContainer(orgId, activityIds, activityDataContainer);
        }
    }

    /**
     * <p>
     * Loads the contact data for the specified activity ids into the container
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param activityIds   Represents the collection of activity ids
     * @param dataContainer Represents the {@link DataContainer}
     */
    private void loadActivityContactDataIntoContainer(final long orgId, final Collection<Long> activityIds,
                                                      final DataContainer dataContainer) {
        final Table contactActivityTable = getContactActivityTable();
        final Table contactTable = contactManager.getTable();
        final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);
        final Column contactIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID);
        final SelectQuery selectQuery = SelectQuery.get(contactActivityTable);

        selectQuery.addSelectClause(List.of(
                contactActivityIdColumn, contactIdColumn, contactActivityTable.getColumn(MACCONTACTACTIVITY.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.ORG_ID)
        ));
        selectQuery.addJoinClause(new JoinClause(contactIdColumn, contactTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactActivityIdColumn, activityIds)));

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Gets the specified IDs of the specified Table from the provider data container
     * </p>
     *
     * @param columnName Represents the column name
     * @param table      Represents the {@link Table}
     * @param container  Represents the data container which holds the IDs
     * @return the collection of IDs for the specified column
     */
    private Collection<Long> getSpecifiedColumnIds(final String columnName, final Table table,
                                                   final WhereClause whereClause, final DataContainer container) {
        final Stream<Row> rows = Objects.isNull(whereClause) ?
                container.getRows(table) : container.getRows(table, whereClause);

        return rows.map(row -> (Long) row.get(table.getColumn(columnName))).toList();
    }

    /**
     * <p>
     * Builds the partial select query with select and join clauses to get Activity IDs
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return the partially built query with select and join clauses
     */
    private SelectQuery buildPartialSelectQuery(final Source source) {
        final Table activityTable = getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table contactActivityTable = getContactActivityTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();

        final Column activityPkColumn = activityTable.getPKColumn();
        final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
        final Column noteActivityIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addSelectClause(List.of(
                activityTable.getColumn(OMACACTIVITY.ORG_ID), activityTable.getColumn(OMACACTIVITY.TITLE),
                activityTable.getColumn(OMACACTIVITY.CREATED_TIME), activityTable.getColumn(OMACACTIVITY.START_TIME),
                activityTable.getColumn(OMACACTIVITY.END_TIME), activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME),
                activityTable.getColumn(OMACACTIVITY.CREATOR_ID), activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID),
                activityTable.getColumn(OMACACTIVITY.MODIFIED_BY), activityTable.getColumn(OMACACTIVITY.IS_DONE),
                activityTable.getColumn(OMACACTIVITY.STATUS), activityTypeIdColumn,
                activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO), activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO),
                activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE),

                dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealActivityIdColumn,
                companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyActivityIdColumn,
                noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), noteActivityIdColumn)
        );
        selectQuery.addJoinClause(List.of(
                new JoinClause(activityTypeIdColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityPkColumn, dealActivityIdColumn, source == DEAL ? INNER : LEFT),
                new JoinClause(activityPkColumn, companyActivityIdColumn, source == COMPANY ? INNER : LEFT),
                new JoinClause(activityPkColumn, noteActivityIdColumn, LEFT))
        );

        if (source == CONTACT) {
            selectQuery.addSelectClause(List.of(
                    contactActivityTable.getColumn(MACCONTACTACTIVITY.ORG_ID), contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID),
                    contactActivityTable.getColumn(MACCONTACTACTIVITY.IS_PRIMARY), contactActivityIdColumn
            ));
            selectQuery.addJoinClause(
                    new JoinClause(activityPkColumn, contactActivityIdColumn, INNER)
            );
        }

        return selectQuery;
    }

    /**
     * <p>
     * Gets the data container with activities of specified filter.
     * </p>
     *
     * @param orgId                  Represents the organisation id
     * @param source                 Represents the {@link Source}
     * @param entityId               Represents the entity ID
     * @param limitClause            Represents the {@link LimitClause}
     * @param activityTypeIds        Represents the activity type ids
     * @param activityFilterCriteria Represents the filter criteria
     * @return The activity {@link DataContainer}
     */
    private DataContainer getActivitiesContainer(final Long orgId, final Source source, final long entityId,
                                                 final LimitClause limitClause, final Collection<Long> activityTypeIds,
                                                 final ActivityFilterCriteria activityFilterCriteria) {
        final Table activityTable = getTable();
        final SelectQuery selectQuery = buildPartialSelectQuery(source);
        final WhereClause entityCriteria = new WhereClause(
                Criteria.eq(getSourceActivityTable(source).getColumn(getSourceColumn(source)), entityId)
        );

        selectQuery.setWhereClause(
                activityTypeIds.isEmpty() ? entityCriteria : entityCriteria.and(
                        Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds))
        );
        activityFilterCriteria.getWhereClause().ifPresent(criteriaWhereClause ->
                selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), criteriaWhereClause, Connector.AND))
        );

        for (final Column column : getOrderByColumns(activityFilterCriteria, activityTable)) {
            selectQuery.addOrderByClause(new OrderByClause(column, activityFilterCriteria.getOrderType()));
        }
        selectQuery.setLimitClause(limitClause);
        final DataContainer activityDataContainer = orgDataStore(orgId).get(selectQuery);

        loadRelatedDataIntoContainer(orgId, source, activityDataContainer);

        return activityDataContainer;
    }

    /**
     * <p>
     * Gets the activity mapping {@link Table} with the {@link Source}
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The activity mapping {@link Table}
     */
    private Table getSourceActivityTable(final Source source) {
        return switch (source) {
            case CONTACT -> getContactActivityTable();
            case COMPANY -> getCompanyActivityTable();
            case DEAL -> getDealActivityTable();
            default -> throw ErrorCode.get(ActivityErrorCodes.INVALID_SOURCE, "i18n.activity.source.invalid");
        };
    }

    /**
     * <p>
     *  Gets the activity mapping table column name with the {@link Source}
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The activity mapping table column name
     */
    private String getSourceColumn(final Source source) {
        return switch (source) {
            case CONTACT -> MACCONTACTACTIVITY.CONTACT_ID;
            case COMPANY -> MACCOMPANYACTIVITY.COMPANY_ID;
            case DEAL -> MACDEALACTIVITY.DEAL_ID;
            default -> throw ErrorCode.get(ActivityErrorCodes.INVALID_SOURCE, "i18n.activity.source.invalid");
        };
    }

    /**
     * <p>
     * Gets the order by column with {@link ActivityFilterCriteria}
     * </p>
     *
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param activityTable          Represents the activity table
     * @return The collection of order by column
     */
    private Collection<Column> getOrderByColumns(final ActivityFilterCriteria activityFilterCriteria, final Table activityTable) {
        return switch (activityFilterCriteria) {
            case ALL_ACTIVITIES -> List.of(getStatusConditionalColumn(), getStartTimeConditionalColumn());
            case OVERDUE_ACTIVITIES, UPCOMING_ACTIVITIES,
                 COMPLETED_ACTIVITIES -> List.of(activityTable.getColumn(OMACACTIVITY.START_TIME));
            default -> Collections.emptyList();
        };
    }

    /**
     * <p>
     * Gets the status conditional column based on the activity status.
     * </p>
     *
     * @return The status conditional column
     */
    private Column getStatusConditionalColumn() {
        final Table activityTable = getTable();
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);

        return OltpConditionalColumn.when(List.of(
                ConditionalColumn.WhenClause.create(
                        new WhereClause(Criteria.eq(isDoneColumn, true)), (int) COMPLETED.getId()),
                ConditionalColumn.WhenClause.create(
                        new WhereClause(Criteria.eq(isDoneColumn, false))
                                .and(Criteria.le(
                                        activityTable.getColumn(OMACACTIVITY.START_TIME),
                                        Instant.now().toEpochMilli())), (int) OVERDUE.getId()))
        ).otherwise((int) UPCOMING.getId()).column(isDoneColumn).build();
    }

    /**
     * <p>
     * Builds a conditional column for start_time ordering so that completed activities are
     * ordered by -start_time (most recent first) while other activities use start_time (earliest first).
     * </p>
     *
     * @return Conditional column based on the startTime for all Activities ordering.
     */
    private Column getStartTimeConditionalColumn() {
        final Table activityTable = getTable();
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);

        return OltpConditionalColumn.when(
                List.of(ConditionalColumn.WhenClause.create(
                        new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)),
                        startTimeColumn.multiply(NumericalConstant.NEGATIVE_ONE))
                )
        ).otherwise(startTimeColumn).column(startTimeColumn).build();
    }

    /**
     * <p>
     * Gets the total number of upcoming and overdue Activities with the given source.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @return The total number of upcoming and overdue Activities.
     */
    public Long getTodoActivitiesCount(final Long orgId, final Long entityId, final Source source) {
        final Table sourceTable = getSourceActivityTable(source);
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(sourceTable);

        countQuery.addJoinClause(new JoinClause(sourceTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(sourceTable.getColumn(getSourceColumn(source)), entityId))
                .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the upcoming deal activities, including the overdue activities.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @param start  Represents the lower bound of activity to be fetched
     * @param limit  Represents the upper bound of activity to be fetched
     * @return Collection of {@link ActivityHolder}.
     */
    public Collection<ActivityHolder> getTodoActivities(final Long orgId, final Long dealId, final int start, final int limit) {
        final Table dealActivityTable = getDealActivityTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table activityTable = getTable();

        final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column dealActivityColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column activityStartTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
        final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getPKColumn(), dealActivityColumn),
                new JoinClause(activityTypeIdColumn, activityTypeTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(activityDealIdColumn, dealActivityColumn, activityStartTimeColumn,
                activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID), isDoneColumn,
                activityTypeIdColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE),
                activityTable.getColumn(OMACACTIVITY.CREATOR_ID), activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityDealIdColumn, dealId))
                .and(Criteria.eq(isDoneColumn, false)));
        selectQuery.addOrderByClause(new OrderByClause(activityStartTimeColumn, ASCENDING));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : dataContainer.getRows(activityTable).map(activityRow ->
                convertRowToActivityHolder(activityRow, dataContainer)).toList();
    }

    /**
     * Gets the contact data container
     *
     * @param orgId       Represents the organisation id
     * @param activityIds Represents the activity ids
     * @return {@link DataContainer}
     */
    private DataContainer getContactDataContainer(final Long orgId, final List<Long> activityIds) {
        final Table contactTable = contactManager.getTable();
        final Table contactActivityTable = getContactActivityTable();

        final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);
        final Column activityContactIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID);
        final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);

        contactSelectQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), activityContactIdColumn));
        contactSelectQuery.addSelectClause(List.of(activityContactIdColumn, contactActivityIdColumn, contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN), contactTable.getColumn(MCOCONTACT.TWITTER)));
        contactSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactActivityIdColumn, activityIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(contactSelectQuery);

        appendDataContainer(orgId, dataContainer);
        return dataContainer;
    }

    /**
     * <p>
     * Fetches the activity count for given contactId
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param contactId Represents the contact id
     * @return contact activity count
     */
    public Long getContactActivityCount(final Long orgId, final Long contactId) {
        final Table contactActivityTable = getContactActivityTable();
        final CountQuery countQuery = CountQuery.get(contactActivityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given contactId and isDone
     * </p>
     *
     * @param orgId                  Represents the organization id
     * @param contactId              Represents the contact id
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param activityTypeIds        Represents the activity type ids
     * @return contact activity count
     */
    public Long getContactActivityCount(final Long orgId, final Long contactId, final ActivityFilterCriteria activityFilterCriteria,
                                        final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(contactId, activityFilterCriteria, CONTACT);

        if (Objects.nonNull(activityTypeIds) && !activityTypeIds.isEmpty()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given contactId and isDone
     * </p>
     *
     * @param orgId                    Represents the organization id
     * @param contactId                Represents the contact id
     * @param activityFilterCriteria   Represents the {@link ActivityFilterCriteria}
     * @param includeStartTimeCriteria Represent whether to include the start time criteria or not
     * @return contact activity count
     */
    public Long getContactActivityCount(final Long orgId, final Long contactId, final ActivityFilterCriteria activityFilterCriteria,
                                        final boolean includeStartTimeCriteria) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(contactId, activityFilterCriteria, CONTACT);

        if (includeStartTimeCriteria) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.gt(activityTable.getColumn(OMACACTIVITY.START_TIME), Instant.now().toEpochMilli())), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given contactIds
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param contactIds {@link Collection<Long>} contact ids
     * @return {@link Long} of contact activity count
     */
    public Long getContactActivityCount(final Long orgId, final Collection<Long> contactIds) {
        final Table contactActivityTable = getContactActivityTable();
        final CountQuery countQuery = CountQuery.get(contactActivityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactIds)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given dealId
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param dealId Represents the deal id
     * @return deal activity count
     */
    public Long getDealActivityCount(final Long orgId, final Long dealId) {
        final Table dealActivityTable = getDealActivityTable();
        final CountQuery countQuery = CountQuery.get(dealActivityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given dealId and isDone
     * </p>
     *
     * @param orgId                  Represents the organization id
     * @param dealId                 Represents the deal id
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param activityTypeIds        Represent the activity type ids
     * @return deal activity count
     */
    public Long getDealActivityCount(final Long orgId, final Long dealId, final ActivityFilterCriteria activityFilterCriteria,
                                     final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(dealId, activityFilterCriteria, DEAL);

        if (Objects.nonNull(activityTypeIds) && !activityTypeIds.isEmpty()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given dealId and isDone
     * </p>
     *
     * @param orgId                    Represents the organization id
     * @param dealId                   Represents the deal id
     * @param activityFilterCriteria   Represents the {@link ActivityFilterCriteria}
     * @param includeStartTimeCriteria Represent whether to include the start time criteria or not
     * @return deal activity count
     */
    public Long getDealActivityCount(final Long orgId, final Long dealId, final ActivityFilterCriteria activityFilterCriteria, final boolean includeStartTimeCriteria) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(dealId, activityFilterCriteria, DEAL);

        if (includeStartTimeCriteria) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.gt(activityTable.getColumn(OMACACTIVITY.START_TIME), Instant.now().toEpochMilli())), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given companyId
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param companyId Represents the company id
     * @return company activity count
     */
    public Long getCompanyActivityCount(final Long orgId, final Long companyId) {
        final Table companyActivityTable = getCompanyActivityTable();
        final CountQuery countQuery = CountQuery.get(companyActivityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given companyId and isDone
     * </p>
     *
     * @param orgId                  Represents the organization id
     * @param companyId              Represents the company id
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param activityTypeIds        Represent the activity type ids
     * @return company activity count
     */
    public Long getCompanyActivityCount(final Long orgId, final Long companyId, final ActivityFilterCriteria activityFilterCriteria,
                                        final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(companyId, activityFilterCriteria, COMPANY);

        if (Objects.nonNull(activityTypeIds) && !activityTypeIds.isEmpty()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the activity count for given companyId and isDone
     * </p>
     *
     * @param orgId                    Represents the organization id
     * @param companyId                Represents the company id
     * @param activityFilterCriteria   Represents the {@link ActivityFilterCriteria}
     * @param includeStartTimeCriteria Represent whether to include the start time criteria or not
     * @return company activity count
     */
    public Long getCompanyActivityCount(final Long orgId, final Long companyId, final ActivityFilterCriteria activityFilterCriteria,
                                        final boolean includeStartTimeCriteria) {
        final Table activityTable = getTable();
        final CountQuery countQuery = getActivitySourceCountQuery(companyId, activityFilterCriteria, COMPANY);

        if (includeStartTimeCriteria) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(),
                    new WhereClause(Criteria.gt(activityTable.getColumn(OMACACTIVITY.START_TIME), Instant.now().toEpochMilli())), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the count query for given companyId
     * </p>
     *
     * @param source                 Refers to the source
     * @param sourceId               Refers to the sourceId
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @return company activity count query
     */
    private CountQuery getActivitySourceCountQuery(final Long sourceId, final ActivityFilterCriteria activityFilterCriteria,
                                                   final Source source) {
        final Table sourceTable;
        final String sourceColumnName;

        switch (source) {
            case CONTACT -> {
                sourceTable = getContactActivityTable();
                sourceColumnName = MACCONTACTACTIVITY.CONTACT_ID;
            }
            case COMPANY -> {
                sourceTable = getCompanyActivityTable();
                sourceColumnName = MACCOMPANYACTIVITY.COMPANY_ID;
            }
            case DEAL -> {
                sourceTable = getDealActivityTable();
                sourceColumnName = MACDEALACTIVITY.DEAL_ID;
            }
            default -> {
                sourceTable = null;
                sourceColumnName = null;
            }
        }
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        if (Objects.nonNull(sourceTable)) {
            countQuery.addJoinClause(new JoinClause(activityTable.getPKColumn(), sourceTable.getColumn(ActivityFields.ACTIVITY_ID.name())));
            countQuery.setWhereClause(new WhereClause(Criteria.eq(sourceTable.getColumn(sourceColumnName), sourceId)));
        }
        final Optional<WhereClause> optionalWhereClause = activityFilterCriteria.getWhereClause();

        optionalWhereClause.ifPresent(whereClause -> countQuery.setWhereClause(WhereClauseUtil
                .connect(countQuery.getWhereClause(), whereClause, Connector.AND)));

        return countQuery;
    }

    /**
     * <p>
     * Fetches the open activities count for given Contact ID.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param contactId Represents the contact id.
     * @return contact activity count.
     */
    public Long getOpenContactActivities(final Long orgId, final Long contactId) {
        final Table contactActivityTable = getContactActivityTable();
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(contactActivityTable);

        countQuery.addJoinClause(new JoinClause(contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactId))
                .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false))
                .and(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.IS_PRIMARY), true)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the open activities count for given Deal ID.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param dealId Represents the deal id
     * @return deal activity count
     */
    public Long getOpenDealActivities(final Long orgId, final Long dealId) {
        final Table dealActivityTable = getDealActivityTable();
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealActivityTable);

        countQuery.addJoinClause(new JoinClause(dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealId)).
                and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the open activities count for given Company ID.
     * </p>
     *
     * @param orgId     organization id
     * @param companyId company id
     * @return company activity count
     */
    public Long getOpenCompanyActivities(final Long orgId, final Long companyId) {
        final Table activityTable = getTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final CountQuery countQuery = CountQuery.get(companyActivityTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyId)).
                and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false));

        countQuery.addJoinClause(new JoinClause(companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the ActivityHolder with complete data (contacts, deal, company) for given note IDs.
     * Returns a map keyed by note ID with the associated {@link ActivityHolder}.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param noteIds Represents the collection of note IDs
     * @return Map keyed by note ID with the associated {@link ActivityHolder} containing complete data
     */
    public Map<Long, ActivityHolder> getActivityHoldersByNoteIds(final Long orgId, final Collection<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final Table activityTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(noteActivityTable);

        selectQuery.addSelectClause(noteActivityTable);
        selectQuery.addSelectClause(List.of(
                activityTable.getColumn(OMACACTIVITY.ORG_ID), activityTable.getColumn(OMACACTIVITY.ID))
        );
        selectQuery.addJoinClause(new JoinClause(noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), noteIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final List<Long> activityIds = dataContainer.getRows(noteActivityTable)
                .map(row -> (Long) row.get(MACNOTEACTIVITY.ACTIVITY_ID)).toList();

        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final DataContainer activityDataContainer = getActivityDataContainer(orgId, activityIds);
        final Map<Long, ActivityHolder> activityHoldersByActivityId = activityDataContainer.getRows(activityTable)
                .map(row -> convertRowToModel(row, activityDataContainer)).collect(
                        Collectors.toMap(ActivityHolder::id, holder -> holder, (existing, replacement) -> replacement));

        return dataContainer.getRows(noteActivityTable).collect(Collectors.toMap(
                row -> row.get(MACNOTEACTIVITY.NOTE_ID),
                row -> {
                    final Long activityId = row.get(MACNOTEACTIVITY.ACTIVITY_ID);

                    return activityHoldersByActivityId.get(activityId);
                }, (existing, replacement) -> replacement)
        );
    }

    /**
     * <p>
     * Loads the activity related timeline
     * </p>
     *
     * @param timeline        An entity that holds the timeline attributes
     * @param activityHolder  An entity that holds the activity attributes
     * @param targetContainer Holds the updated information of field values provided by user
     * @param sourceContainer Holds the field values of existing activity
     */
    private void loadActivityTimeline(final Timeline timeline, final ActivityHolder activityHolder,
                                      final DataContainer targetContainer, final DataContainer sourceContainer) {
        final List<Contact> contacts = activityHolder.getContacts();
        final Deal deal = activityHolder.getDeal();
        final Company company = activityHolder.getCompany();
        final boolean hasContacts = !contacts.isEmpty();
        final boolean hasDeal = Objects.nonNull(deal);
        final boolean hasCompany = Objects.nonNull(company);
        final List<Long> contactIds = hasContacts ? contacts.stream().map(Contact::id).toList() : null;
        final Long dealId = hasDeal ? deal.id() : null;
        final Long companyId = hasCompany ? company.id() : null;

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        final Long timelineId = timeline.id();

        commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, activityHolder.getActivity().id(), MTLACTIVITYTIMELINE.ACTIVITY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                new Row(readableTimelineManager.getActivityTimelineTable()), targetContainer);

        if (hasContacts) {
            loadContactTimelineIntoContainer(contactIds, timelineId, targetContainer);
        }

        if (hasDeal) {
            loadDealTimelineIntoContainer(dealId, timelineId, targetContainer);
        }

        if (hasCompany) {
            loadCompanyTimelineIntoContainer(companyId, timelineId, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the existing values in to this old activity values map.
     * </p>
     *
     * @param activity          Represents the {@link Activity}
     * @param oldActivityValues Holds the old activity values
     * @param entityContainer   Holds an existing activity information in the entity {@link DataContainer}
     */
    private void loadOldActivityValues(final Activity activity, final Map<String, String> oldActivityValues, final DataContainer entityContainer) {
        final Row oldActivityRow = entityContainer.getRow(getTable(), activity.id());
        final Table activityNoteTable = activityManager.getNoteActivityTable();
        final Note note = activity.getNote();

        if (Objects.nonNull(note)) {
            final Row activityNoteRow = entityContainer.getRow(activityNoteTable, new WhereClause(Criteria.eq(activityNoteTable
                    .getColumn(MACNOTEACTIVITY.ACTIVITY_ID), oldActivityRow.getPKValue())));

            if (Objects.nonNull(activityNoteRow)) {
                final Table noteTable = sourceNoteManager.getTable();
                final Row noteRow = entityContainer.getRow(noteTable, new WhereClause(Criteria.eq(noteTable.getPKColumn(), activityNoteRow.get(MACNOTEACTIVITY.NOTE_ID))));

                if (Objects.nonNull(noteRow)) {
                    final String oldNote = noteRow.get(OMNTNOTE.CONTENT);

                    oldActivityValues.put(ActivityFields.NOTE.getName(), oldNote);
                }
            }
        }

        if (Objects.nonNull(oldActivityRow)) {
            final String oldTitle = oldActivityRow.get(OMACACTIVITY.TITLE);
            final String title = activity.getTitle();

            if (Objects.nonNull(title) && !Objects.equals(oldTitle, title)) {
                oldActivityValues.put(ActivityFields.TITLE.getName(), oldTitle);
            }
        }
    }

    /**
     * <p>
     * Loads the activity timelines based on the existing values.
     * </p>
     *
     * @param isCreateEvent   A flag which represents the event is created or not
     * @param activityHolder  Represents the {@link ActivityHolder}
     * @param oldValues       Represents the old values for the respective field
     * @param typeRow         Represents the activity type row to fetch the respective icon
     * @param entityContainer Holds an existing activity fields in the entity {@link DataContainer}
     * @param targetContainer Holds the updated information of activity into the target {@link DataContainer}
     */
    private void loadActivityTimeline(final boolean isCreateEvent, final ActivityHolder activityHolder, final Map<String, String> oldValues,
                                      final Row typeRow, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Activity activity = activityHolder.getActivity();
        final I18nKey titleI18nKey = new I18nKey("i18n.activity.title");
        final I18nKey noteI18nKey = new I18nKey("i18n.activity.note");

        if (isCreateEvent) {
            loadEventCreationTimeline(typeRow, activityHolder, TimelineType.ACTIVITY_ADDED, titleI18nKey, entityContainer, targetContainer);
        } else {
            final String titleKey = ActivityFields.TITLE.getName();
            final String noteKey = ActivityFields.NOTE.getName();
            final String oldTitle = oldValues.get(titleKey);
            final Note activityNote = activity.getNote();

            if (Objects.nonNull(oldTitle)) {
                buildAndLoadTimeline(typeRow, activityHolder, oldTitle, activity.getTitle(), titleI18nKey, titleKey, entityContainer, targetContainer);
            }

            if (Objects.nonNull(activityNote)) {
                final String note = activityNote.getTrimmedContent(TIMELINE_NOTE_MAX_LENGTH);
                final String oldNoteValue = oldValues.get(noteKey);
                final String trimmedOldNoteValue = Objects.isNull(oldNoteValue) ? null :
                        new Note() {{
                            setContent(oldNoteValue);
                        }}.getTrimmedContent(TIMELINE_NOTE_MAX_LENGTH);

                if (Objects.nonNull(note) && !Objects.equals(trimmedOldNoteValue, note)) {
                    buildAndLoadTimeline(typeRow, activityHolder, trimmedOldNoteValue, note, noteI18nKey, noteKey, entityContainer, targetContainer);
                }
            }
        }
    }

    /**
     * <p>
     * Builds and loads the activity timeline.
     * </p>
     *
     * @param typeRow         Represents the activity type row
     * @param activityHolder  Represents the {@link ActivityHolder}
     * @param oldValue        Represents the old value
     * @param newValue        Represents the new value
     * @param fieldKey        Represents the {@link I18nKey} field key
     * @param activityField   Represents the activity field
     * @param entityContainer Holds an existing activity fields in the entity {@link DataContainer}
     * @param targetContainer Holds the updated information of activity into the target {@link DataContainer}
     */
    private void buildAndLoadTimeline(final Row typeRow, final ActivityHolder activityHolder, final String oldValue,
                                      final String newValue, final I18nKey fieldKey, final String activityField,
                                      final DataContainer entityContainer, final DataContainer targetContainer) {
        final Timeline timeline = activityManager.buildActivityTimeline(false, activityHolder.getActivity(), typeRow);

        switch (activityField) {
            case "title" -> {

                if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.ACTIVITY_TITLE_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "note" -> {

                if (Objects.isNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.ACTIVITY_NOTE_ADDED);
                    timeline.setAction(TimelineActionFields.ADDED);
                } else {
                    timeline.setTimelineType(TimelineType.ACTIVITY_NOTE_EDITED);
                    timeline.setAction(TimelineActionFields.EDITED);
                }
            }
        }

        if (!Objects.equals(oldValue, newValue)) {
            final FieldTimeline fieldTimeline = new FieldTimeline(Objects.isNull(oldValue) ? Collections.emptyList() : List.of(oldValue), newValue, fieldKey);

            timeline.setFieldTimeline(fieldTimeline);
        }

        loadActivityTimeline(timeline, activityHolder, targetContainer, entityContainer);
    }

    /**
     * <p>
     * Loads only the event creation timeline.
     * </p>
     *
     * @param typeRow         Represents the activity type row
     * @param activityHolder  Represents the {@link ActivityHolder}
     * @param type            Represents the {@link TimelineType}
     * @param key             Represents the {@link I18nKey} field key
     * @param entityContainer Holds an existing activity fields in the entity {@link DataContainer}
     * @param targetContainer Holds the updated information of activity into the target {@link DataContainer}
     */
    private void loadEventCreationTimeline(final Row typeRow, final ActivityHolder activityHolder,
                                           final TimelineType type, final I18nKey key, final DataContainer entityContainer, final DataContainer targetContainer) {
        final Activity activity = activityHolder.getActivity();
        final Timeline timeline = activityManager.buildActivityTimeline(true, activity, typeRow);

        switch (type) {
            case ACTIVITY_ADDED -> {
                final FieldTimeline fieldTimeline = new FieldTimeline(Collections.emptyList(), activity.getTitle(), key);

                timeline.setFieldTimeline(fieldTimeline);
                timeline.setAction(TimelineActionFields.ADDED);
                timeline.setTimelineType(TimelineType.ACTIVITY_ADDED);
            }
        }

        loadActivityTimeline(timeline, activityHolder, targetContainer, entityContainer);
    }

    /**
     * Loads the contact timeline row into container
     *
     * @param contactIds      Represents contact id
     * @param timelineId      Represents timeline id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadContactTimelineIntoContainer(final List<Long> contactIds, final Long timelineId, final DataContainer targetContainer) {
        final Table contactTimelineTable = readableTimelineManager.getContactTimelineTable();

        for (final Long contactId : contactIds) {
            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contactId, MTLCONTACTTIMELINE.CONTACT_ID,
                    MTLCONTACTTIMELINE.TIMELINE_ID, new Row(contactTimelineTable), targetContainer);
        }
    }

    /**
     * Loads the deal timeline row into container
     *
     * @param dealId          Represents deal id
     * @param timelineId      Represents timeline id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDealTimelineIntoContainer(final Long dealId, final Long timelineId, final DataContainer targetContainer) {
        final Table dealTimelineTable = readableTimelineManager.getDealTimelineTable();

        commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, dealId, MTLDEALTIMELINE.DEAL_ID,
                MTLDEALTIMELINE.TIMELINE_ID, new Row(dealTimelineTable), targetContainer);
    }

    /**
     * Loads the company timeline row into container
     *
     * @param companyId       Represents company id
     * @param timelineId      Represents timeline id
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadCompanyTimelineIntoContainer(final Long companyId, final Long timelineId, final DataContainer targetContainer) {
        final Table companyTimelineTable = readableTimelineManager.getCompanyTimelineTable();

        commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID,
                MTLCOMPANYTIMELINE.TIMELINE_ID, new Row(companyTimelineTable), targetContainer);
    }

    /**
     * <p>
     * Delete the row for given activities
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param activityHolders Represents the collection of activity
     */
    @Override
    public void delete(final Long orgId, final Collection<ActivityHolder> activityHolders) {
        final Table timelineTable = commonTimelineManager.getTable();
        final Table activityTimelineTable = readableTimelineManager.getActivityTimelineTable();
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final Collection<Long> activityIds = new HashSet<>();
        final SelectQuery selectQuery = SelectQuery.get(timelineTable);
        final AuditSource auditSource = AuditSource.ACTIVITY;
        final AuditAction auditAction = AuditAction.DELETED;
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();

        for (final ActivityHolder activityHolder : activityHolders) {
            final Activity activity = activityHolder.getActivity();

            activityIds.add(activityHolder.id());
            auditLogDataList.add(new AuditLogData(activity.getCreatorId(), activity.id(), activity, auditSource, auditAction));
        }

        selectQuery.addJoinClause(new JoinClause(timelinePKColumn, activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID)));
        selectQuery.addSelectClause(timelinePKColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.ACTIVITY_ID), activityIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Table activityTable = getTable();
            final DeleteQuery activityDeleteQuery = DeleteQuery.get(activityTable);
            final DeleteQuery timelineDeleteQuery = DeleteQuery.get(timelineTable);
            final Collection<Long> timelineIds = dataContainer.getRows(timelineTable).map(Row::getPKValue).collect(Collectors.toSet());

            activityDeleteQuery.setWhereClause(new WhereClause(Criteria.in(activityTable.getPKColumn(), activityIds)));
            timelineDeleteQuery.setWhereClause(new WhereClause(Criteria.in(timelinePKColumn, timelineIds)));
            orgDataStore(orgId).delete(List.of(activityDeleteQuery, timelineDeleteQuery));
            LOGGER.info("Deleted activity and timeline for orgId: {}, activityIds: {}, timelineIds: {}", orgId, activityIds, timelineIds);

            if (!auditLogDataList.isEmpty()) {
                auditLogger.log(orgId, auditLogDataList);
            }
        }
    }

    /**
     * Retrieves all the activity related details for summary.
     *
     * @param orgId    Refers the organization id.
     * @param entityId Refers the entity id.
     * @param source   Refers the source name.
     * @return {@link ActivitySummary} details.
     */
    public ActivitySummary getActivitySummary(final Long orgId, final Long entityId, final String source) {
        final Table table = getTable();
        final ActivitySummary activitySummary = new ActivitySummary();

        switch (source) {
            case Contact.ENTITY_TYPE -> {
                final Table contactActivityTable = getContactActivityTable();
                final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);
                final WhereClause whereClause = new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), entityId));

                loadLastActivityDateAndType(orgId, table, contactActivityIdColumn, whereClause, activitySummary);
                activitySummary.setNextActivityDate(getNextActivityDate(orgId, table, contactActivityIdColumn, whereClause));
                activitySummary.setDoneActivities(getContactActivityCount(orgId, entityId, ActivityFilterCriteria.COMPLETED_ACTIVITIES, false));
                activitySummary.setUpcomingActivities(getContactActivityCount(orgId, entityId, ActivityFilterCriteria.UPCOMING_ACTIVITIES, false));
                activitySummary.setPendingActivities(getPendingActivitiesCount(orgId, table, contactActivityIdColumn, whereClause));
                activitySummary.setTotalActivities(getContactActivityCount(orgId, entityId));
            }
            case Company.ENTITY_TYPE -> {
                final Table companyActivityTable = getCompanyActivityTable();
                final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
                final WhereClause whereClause = new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), entityId));

                loadLastActivityDateAndType(orgId, table, companyActivityIdColumn, whereClause, activitySummary);
                activitySummary.setNextActivityDate(getNextActivityDate(orgId, table, companyActivityIdColumn, whereClause));
                activitySummary.setDoneActivities(getCompanyActivityCount(orgId, entityId, ActivityFilterCriteria.COMPLETED_ACTIVITIES, false));
                activitySummary.setUpcomingActivities(getCompanyActivityCount(orgId, entityId, ActivityFilterCriteria.UPCOMING_ACTIVITIES, true));
                activitySummary.setPendingActivities(getPendingActivitiesCount(orgId, table, companyActivityIdColumn, whereClause));
                activitySummary.setTotalActivities(getCompanyActivityCount(orgId, entityId));
            }
            case Deal.ENTITY_TYPE -> {
                final Table dealActivityTable = getDealActivityTable();
                final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
                final WhereClause whereClause = new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), entityId));

                loadLastActivityDateAndType(orgId, table, dealActivityIdColumn, whereClause, activitySummary);
                activitySummary.setNextActivityDate(getNextActivityDate(orgId, table, dealActivityIdColumn, whereClause));
                activitySummary.setDoneActivities(getDealActivityCount(orgId, entityId, ActivityFilterCriteria.COMPLETED_ACTIVITIES, false));
                activitySummary.setUpcomingActivities(getDealActivityCount(orgId, entityId, ActivityFilterCriteria.UPCOMING_ACTIVITIES, true));
                activitySummary.setPendingActivities(getPendingActivitiesCount(orgId, table, dealActivityIdColumn, whereClause));
                activitySummary.setTotalActivities(getDealActivityCount(orgId, entityId));
            }
        }

        return activitySummary;
    }

    /**
     * <p>
     * Loads the last activity type and date into activity summary
     * </p>
     *
     * @param orgId                  Represents the organisation ID
     * @param activityTable          Represents the activity table.
     * @param activitySourceIdColumn Represents the activity source id column
     * @param whereClause            Represents the where clause by which the data to be fetched.
     * @param activitySummary        Represents the {@link ActivitySummary}
     */
    private void loadLastActivityDateAndType(final Long orgId, final Table activityTable, final Column activitySourceIdColumn, final WhereClause whereClause,
                                             final ActivitySummary activitySummary) {
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column startTime = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column ativityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);

        selectQuery.addSelectClause(List.of(ativityTypeIdColumn, startTime, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME)));
        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getPKColumn(), activitySourceIdColumn),
                new JoinClause(ativityTypeIdColumn, activityTypeTable.getPKColumn())));
        selectQuery.setWhereClause(whereClause.and(Criteria.le(activityTable.getColumn(OMACACTIVITY.START_TIME),
                Instant.now().toEpochMilli())).and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));
        selectQuery.setLimitClause(new LimitClause(1));
        selectQuery.addOrderByClause(new OrderByClause(startTime, OrderByClause.OrderType.DESCENDING));
        final DataContainer lastActivityContainer = orgDataStore(orgId).get(selectQuery);

        if (!lastActivityContainer.isEmpty()) {
            final Row row = lastActivityContainer.getRow(activityTable);
            final Row activityTypeRow = lastActivityContainer.getRow(activityTypeManager.getTable(), (Long) row.get(OMACACTIVITY.TYPE_ID));

            if (Objects.nonNull(activityTypeRow)) {
                activitySummary.setLastActivityType(activityTypeRow.get(OMACACTIVITYTYPE.NAME));
            }
            activitySummary.setLastActivityDate(new Date(((DateTime) row.get(OMACACTIVITY.START_TIME)).getTimeStamp()));
        }
    }

    /**
     * <p>
     * Gets the pending activities count for an activity
     * </p>
     *
     * @param orgId         Represents an organization id.
     * @param activityTable Represents the activity table
     * @param column        Represents the source related activity column
     * @param whereClause   Represents the where clause by which the data to be fetched.
     * @return The count of pending activities
     */
    private Long getPendingActivitiesCount(final Long orgId, final Table activityTable, final Column column, final WhereClause whereClause) {
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.addJoinClause(List.of(new JoinClause(activityTable.getPKColumn(), column)));
        countQuery.setWhereClause(whereClause.and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the next activity date for an entity.
     *
     * @param orgId                  Refers the organization id.
     * @param activityTable          Refers the activity table.
     * @param activitySourceIdColumn Refers the activity source id column
     * @param whereClause            Refers the where clause by which the data to be fetched.
     * @return The start date of upcoming activity.
     */
    private Date getNextActivityDate(final Long orgId, final Table activityTable,
                                     final Column activitySourceIdColumn, final WhereClause whereClause) {
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Column startTime = activityTable.getColumn(OMACACTIVITY.START_TIME);

        selectQuery.addJoinClause(new JoinClause(activityTable.getPKColumn(), activitySourceIdColumn));
        selectQuery.addSelectClause(startTime);
        selectQuery.setWhereClause(whereClause.and(Criteria.gt(activityTable.getColumn(OMACACTIVITY.START_TIME),
                Instant.now().toEpochMilli())).and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), false)));
        selectQuery.addOrderByClause(new OrderByClause(startTime, ASCENDING));
        selectQuery.setLimitClause(new LimitClause(1));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(activityTable);
        final DateTime dateTime = Objects.isNull(row) ? null : row.get(OMACACTIVITY.START_TIME);

        return Objects.isNull(dateTime) ? null : new Date(dateTime.getTimeStamp());
    }

    /**
     * Retrieves the last completed activity date and type.
     *
     * @param orgId               Refers the organization id.
     * @param activityTable       Refers the activity table.
     * @param activitySourceTable Refers the source related activity table.
     * @param whereClause         Refers the where clause by which the data to be fetched.
     * @return The last completed activity date and type.
     */
    private Map<String, Object> getLastCompletedActivityDateAndType(final Long orgId, final Table activityTable,
                                                                    final Table activitySourceTable, final WhereClause whereClause) {
        final Table typeTable = activityTypeManager.getTable();
        final Column modifiedTime = activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME);
        final SelectQuery selectQuery = SelectQuery.get(typeTable);

        selectQuery.setWhereClause(whereClause.and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), true)));
        selectQuery.addJoinClause(DmlUtil.getJoins(typeTable, activityTable, activitySourceTable));
        selectQuery.addSelectClause(List.of(modifiedTime, typeTable.getColumn(OMACACTIVITYTYPE.NAME)));
        selectQuery.addOrderByClause(new OrderByClause(modifiedTime, OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(1));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row typeRow = dataContainer.getRow(typeTable);
        final Row row = dataContainer.getRow(activityTable);

        return Objects.nonNull(row) && Objects.nonNull(typeRow) ? Map.of(ActivityHolderFields.ACTIVITY_DATE.getName(), row.get(OMACACTIVITY.MODIFIED_TIME),
                ActivityHolderFields.ACTIVITY_TYPE.getName(), typeRow.get(OMACACTIVITYTYPE.NAME)) : Map.of();
    }

    /**
     * <p>
     * Gets the user activity based on the given user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param isDone A flag is represents the activity is done or not
     * @param start  Represents the starting index of activity to be fetched
     * @param limit  Represents the maximum number of activity to be fetched
     * @return The collection of activities for the given user id
     */
    public Collection<ActivityHolder> getUserActivities(final Long orgId, final Long userId, final boolean isDone,
                                                        final int start, final int limit) {
        final Table activityTable = getTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final Table companyTable = companyManager.getTable();
        final Table contactTable = contactManager.getTable();
        final Table contactActivityTable = getContactActivityTable();
        final Table dealTable = getDealManager().getTable();
        final Table noteTable = sourceNoteManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Column activityPkColumn = activityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = LEFT;
        final SelectQuery innerSelectQuery = SelectQuery.get(activityTable);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
        final Column typeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column monetaryColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);

        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.addSelectClause(activityPkColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.CREATOR_ID), userId)).
                and(Criteria.eq(isDoneColumn, isDone)));
        selectQuery.addJoinClause(List.of(new JoinClause(typeIdColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityPkColumn, dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealTable.getPKColumn(), leftJoin),
                new JoinClause(monetaryColumn, monetaryTable.getPKColumn(), leftJoin),
                new JoinClause(pipelineStageIdColumn, pipelineStageTable.getPKColumn(), leftJoin),
                new JoinClause(activityPkColumn, noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(noteActivityTable.getColumn(MACNOTEACTIVITY.NOTE_ID), noteTable.getPKColumn(), leftJoin),
                new JoinClause(activityPkColumn, companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyTable.getPKColumn(), leftJoin),
                new JoinClause(activityPkColumn, contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), contactTable.getPKColumn(), leftJoin)));

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.START_TIME), isDoneColumn,
                activityTable.getColumn(OMACACTIVITY.CREATOR_ID), typeIdColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), contactTable.getColumn(MCOCONTACT.ORG_ID),
                noteTable.getColumn(OMNTNOTE.CONTENT), pipelineStageIdColumn, dealTable.getColumn(MDODEAL.TITLE), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), monetaryColumn,
                contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.FIRST_NAME), companyTable.getColumn(MCOCOMPANY.NAME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityTable.getColumn(OMACACTIVITY.STATUS), contactTable.getColumn(MCOCONTACT.OWNER_ID),
                activityTable.getColumn(OMACACTIVITY.ORG_ID), contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
        selectQuery.addSelectClause(dealActivityTable, noteActivityTable, companyActivityTable, contactActivityTable, monetaryTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(activityPkColumn, innerSelectQuery)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        appendDataContainer(orgId, dataContainer);
        return dataContainer.getRows(activityTable).map(row -> convertActivityRowToModel(row, dataContainer)).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts the row to {@link ActivityHolder} details
     * </p>
     *
     * @param row           Represents the activity row
     * @param dataContainer A container that holds the user defined field values and system values
     * @return The {@link ActivityHolder} details
     */
    private ActivityHolder convertActivityRowToModel(final Row row, final DataContainer dataContainer) {
        return convertActivityRowToModel(row, dataContainer, Collections.emptyMap());
    }

    /**
     * <p>
     * Converts the row to {@link ActivityHolder} details
     * </p>
     *
     * @param row             Represents the activity row
     * @param dataContainer   A container that holds the user defined field values and system values
     * @param rowsByTableName Represents the Rows from data container grouped by table name
     * @return the converted activity model
     */
    private ActivityHolder convertActivityRowToModel(final Row row, final DataContainer dataContainer,
                                                     final Map<String, Map<Long, Row>> rowsByTableName) {
        final ActivityHolder activityHolder = new ActivityHolder();
        final Activity activity = new Activity();
        final Long orgId = row.getOrgId();
        final Long activityId = row.getPKValue();
        final Object status = row.get(OMACACTIVITY.STATUS);

        activity.setOrgId(orgId);
        activity.setId(activityId);
        activity.setDone(row.get(OMACACTIVITY.IS_DONE));
        activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
        activity.setDescription(row.get(OMACACTIVITY.DESCRIPTION));
        activity.setTitle(row.get(OMACACTIVITY.TITLE));
        activity.setStatus(Objects.isNull(status) ? null : Status.get((int) status));
        activity.setCreatedTime(row.get(OMACACTIVITY.CREATED_TIME));
        activity.setUpdatedTime(row.get(OMACACTIVITY.MODIFIED_TIME));
        loadCheckInOutRecordIntoActivity(row, activity);

        activityHolder.setActivity(activity);

        final Row companyActivityRow = getRow(dataContainer, getCompanyActivityTable(),
                MACCOMPANYACTIVITY.ACTIVITY_ID, activityId, rowsByTableName);

        if (Objects.nonNull(companyActivityRow)) {
            final Row companyRow = getRow(dataContainer, companyManager.getTable(),
                    MCOCOMPANY.ID, companyActivityRow.get(MACCOMPANYACTIVITY.COMPANY_ID), rowsByTableName);

            if (Objects.nonNull(companyRow)) {
                activityHolder.setCompany(companyManager.convertRowToPartialModel(companyRow));
            }
        }
        final Row dealActivityRow = getRow(dataContainer, getDealActivityTable(),
                MACDEALACTIVITY.ACTIVITY_ID, activityId, rowsByTableName);

        if (Objects.nonNull(dealActivityRow)) {
            final Row dealRow = getRow(dataContainer, getDealTable(),
                    MDODEAL.ID, dealActivityRow.get(MACDEALACTIVITY.DEAL_ID), rowsByTableName);

            if (Objects.nonNull(dealRow)) {
                final Deal deal = new Deal();

                deal.setId(dealRow.getPKValue());
                deal.setTitle(dealRow.get(MDODEAL.TITLE));
                deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
                final Row monetaryRow = getRow(dataContainer, monetaryManager.getTable(),
                        OMETMONETARY.ID, dealRow.get(MDODEAL.MONETARY_ID), rowsByTableName);

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
                }
                final Row pipelineStageRow = getRow(dataContainer, pipelineStageManager.getTable(),
                        MDOPIPELINESTAGE.ID, dealRow.get(MDODEAL.PIPELINE_STAGE_ID), rowsByTableName);

                if (Objects.nonNull(pipelineStageRow)) {
                    deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
                }
                activityHolder.setDeal(deal);
            }
        }
        final Row noteActivityRow = getRow(dataContainer, activityManager.getNoteActivityTable(),
                MACNOTEACTIVITY.ACTIVITY_ID, activityId, rowsByTableName);

        if (Objects.nonNull(noteActivityRow)) {
            final Row noteRow = getRow(dataContainer, noteManager.getTable(),
                    OMNTNOTE.ID, noteActivityRow.get(MACNOTEACTIVITY.NOTE_ID), rowsByTableName);

            if (Objects.nonNull(noteRow)) {
                final Note note = new Note();

                note.setOrgId(orgId);
                note.setContent(noteRow.get(OMNTNOTE.CONTENT));
                note.setId(noteRow.getPKValue());
                activity.setNote(note);
            }
        }
        final Row activityTypeRow = getRow(dataContainer, activityTypeManager.getTable(),
                OMACACTIVITYTYPE.ID, row.get(OMACACTIVITY.TYPE_ID), rowsByTableName);

        if (Objects.nonNull(activityTypeRow)) {
            final ActivityType activityType = new ActivityType();

            activityType.setName(activityTypeRow.get(OMACACTIVITYTYPE.NAME));
            activityType.setActivityIcon(iconService.get(activityTypeRow.get(OMACACTIVITYTYPE.ICON_ID)));
            activityType.setId(activityTypeRow.getPKValue());
            activity.setType(activityType);
        }
        activityHolder.setContacts(convertDataToModels(orgId, activityId, dataContainer, rowsByTableName));
        final Collection<Long> userIds = new HashSet<>();
        final Long assigneeId = row.get(OMACACTIVITY.ASSIGNEE_ID);

        if (Objects.nonNull(assigneeId)) {
            activity.setAssigneeId(assigneeId);
            userIds.add(assigneeId);
        }
        final Long creatorId = row.get(OMACACTIVITY.CREATOR_ID);

        if (Objects.nonNull(creatorId)) {
            activity.setCreatorId(creatorId);
            userIds.add(creatorId);
        }
        final Long modifiedById = row.get(OMACACTIVITY.MODIFIED_BY);

        if (Objects.nonNull(modifiedById)) {
            activity.setModifiedById(modifiedById);
            userIds.add(modifiedById);
        }
        activityHolder.setUserIds(userIds);

        return activityHolder;
    }

    /**
     * <p>
     * Gets the row from the provided table for the specified column with ID
     * </p>
     *
     * @param container       Represents the data container
     * @param table           Represents the table of the row
     * @param columnName      Represents the name of the column
     * @param id              Represents the ID
     * @param rowsByTableName Represents the rows grouped by the table name
     * @return the row if present otherwise null
     */
    private Row getRow(final DataContainer container, final Table table,
                       final String columnName, final Long id,
                       final Map<String, Map<Long, Row>> rowsByTableName) {

        if (Objects.isNull(id)) {
            return null;
        }
        final Column column = table.getColumn(columnName);

        return rowsByTableName.isEmpty() ?
                container.getRow(table, new WhereClause(Criteria.eq(column, id))) :
                getRowById(rowsByTableName.get(table.getName()), column, id);
    }

    /**
     * <p>
     * Gets the row by mapping id
     * </p>
     *
     * @param rows   Represents the collection of rows
     * @param column Represents the PK column or mapper ID column
     * @param id     Represents the ID
     * @return the row if present otherwise null
     */
    private Row getRowById(final Map<Long, Row> rows, final Column column, final Long id) {

        if (Objects.isNull(rows) || rows.isEmpty()) {
            return null;
        }

        return column.isPkColumn() ? rows.get(id) : rows.values().stream()
                .filter(row -> id.equals(row.get(column))).findFirst().orElseGet(() -> null);
    }

    /**
     * <p>
     * Gets the user activity count
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param isDone flag that indicates whether the activity is isDone or not
     * @return The number of activity count by user
     */
    public Long getUserActivityCount(final Long orgId, final Long userId, final boolean isDone) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.CREATOR_ID), userId))
                .and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.IS_DONE), isDone)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the activity events into {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param syncId          Represents the sync id
     * @param activityHolders Represents the {@link Collection} of {@link ActivityHolder}
     */
    public void loadActivityEventIntoContainer(final long orgId, final long userId, final Long syncId, final Collection<ActivityHolder> activityHolders) {
        final Collection<Activity> activities = activityHolders.stream().map(ActivityHolder::getActivity).collect(Collectors.toUnmodifiableSet());
        final Table externalEventsTable = getCalendarExternalEventsTable();
        final Table activityTable = getTable();
        final Table guestTable = getActivityGuestTable();
        final Table emailTable = getEmailTable();
        final Column externalEventIdColumn = externalEventsTable.getColumn(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID);
        final Column guestActivityIdColumn = guestTable.getColumn(MACACTIVITYGUEST.ACTIVITY_ID);
        final Column guestEmailIdColumn = guestTable.getColumn(MACACTIVITYGUEST.EMAIL_ID);
        final Column activityPkColumn = activityTable.getPKColumn();
        final Column emailPkColumn = emailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final JoinClause.JoinType leftJoin = LEFT;
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        selectQuery.addJoinClause(List.of(new JoinClause(activityPkColumn, externalEventsTable.getColumn(OMCALEXTERNALEVENTS.ACTIVITY_ID)),
                new JoinClause(activityPkColumn, guestActivityIdColumn, leftJoin),
                new JoinClause(guestEmailIdColumn, emailPkColumn, leftJoin)));
        selectQuery.addSelectClause(externalEventsTable, activityTable);
        selectQuery.addSelectClause(List.of(emailPkColumn, guestActivityIdColumn, emailTable.getColumn(OMETEMAIL.VALUE),
                guestEmailIdColumn, guestTable.getColumn(MACACTIVITYGUEST.CONTACT_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(externalEventIdColumn, activities.stream().map(Activity::getEventId)
                .collect(Collectors.toUnmodifiableSet()))).and(Criteria.eq(activityTable.getColumn(OMACACTIVITY.CREATOR_ID), userId)));

        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        for (final ActivityHolder activityHolder : activityHolders) {
            loadActivityEventIntoContainer(orgId, syncId, activityHolder, dataContainer, targetContainer);
        }

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the activity events into {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param syncId          Represents the sync id
     * @param activityHolder  Represents the {@link ActivityHolder}
     * @param dataContainer   An entity container which contains the rows of existing records
     * @param targetContainer A container in which all the new/updated rows will be added
     */
    public void loadActivityEventIntoContainer(final long orgId, final Long syncId, final ActivityHolder activityHolder,
                                               final DataContainer dataContainer, final DataContainer targetContainer) {
        final Activity activity = activityHolder.getActivity();
        final Table externalEventsTable = getCalendarExternalEventsTable();
        final Table activityTable = getTable();
        final Column eventIdColumn = externalEventsTable.getColumn(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID);
        final Column activityPkColumn = activityTable.getPKColumn();
        final String eventId = activity.getEventId();
        final Row row = dataContainer.getRow(externalEventsTable, new WhereClause(Criteria.eq(eventIdColumn, eventId)));
        final boolean isCreateEvent = Objects.isNull(row);
        final boolean hasActivityRow;

        if (activity.isActiveEvent() && Objects.nonNull(row)) {
            targetContainer.deleteRow(dataContainer.getRow(activityTable,
                    new WhereClause(Criteria.eq(activityPkColumn, row.get(OMCALEXTERNALEVENTS.ACTIVITY_ID)))));
        } else {
            final Row eventRow;
            final Row activityRow;

            if (isCreateEvent) {
                eventRow = new Row(externalEventsTable);
                activityRow = new Row(activityTable);
                hasActivityRow = true;

                eventRow.setOrgId(orgId);
                eventRow.set(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID, eventId);
                eventRow.set(OMCALEXTERNALEVENTS.REFERENCE_ID, activity.getReferenceId());
                eventRow.set(OMCALEXTERNALEVENTS.ACTIVITY_ID, activityRow.getPKValue());
                eventRow.set(OMCALEXTERNALEVENTS.CALENDAR_SYNC_ID, syncId);
            } else {
                eventRow = row;
                activityRow = dataContainer.getRow(activityTable,
                        new WhereClause(Criteria.eq(activityPkColumn, eventRow.get(OMCALEXTERNALEVENTS.ACTIVITY_ID))));
                hasActivityRow = Objects.nonNull(activityRow);

                if (hasActivityRow) {
                    final ActivityType type = activity.getType();

                    type.setId(activityRow.get(OMACACTIVITY.TYPE_ID));
                    activity.setType(type);
                    activity.setDone(activityRow.get(OMACACTIVITY.IS_DONE));
                    activity.setAssigneeId(activityRow.get(OMACACTIVITY.ASSIGNEE_ID));

                    if (Status.get(activityRow.get(OMACACTIVITY.STATUS)) == COMPLETED) {
                        activity.setStatus(COMPLETED);
                    } else {
                        activity.setStatus(Status.get(activityRow.get(OMACACTIVITY.STATUS)));
                    }

                    activity.setPriority(Priority.get(activityRow.get(OMACACTIVITY.PRIORITY)));
                    activity.setAvailability(Availability.get(activityRow.get(OMACACTIVITY.AVAILABILITY)));
                    activity.setReminder(Reminder.get(activityRow.get(OMACACTIVITY.REMINDER)));
                }
            }

            if (hasActivityRow) {
                activityManager.loadActivityDetailsIntoRow(orgId, activity, activityRow);
                final Collection<ActivityGuest> activityGuests = activityHolder.getActivityGuests();

                if (Objects.nonNull(activityGuests) && !activityGuests.isEmpty()) {
                    final Long activityPkValue = activityRow.getPKValue();

                    loadGuestEmailsIntoContainer(orgId, activityGuests, dataContainer, targetContainer);
                    loadActivityGuestRowIntoContainer(orgId, isCreateEvent, activityPkValue, activityGuests, targetContainer, dataContainer);

                    if (!isCreateEvent) {
                        deleteUnmappedActivityGuest(activityPkValue, orgId, activityGuests, targetContainer);
                    }
                }

                targetContainer.updateRow(eventRow);
                targetContainer.updateRow(activityRow);
            }
        }
    }

    /**
     * <p>
     * Fetches the icon id for the given activity id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param activityId Represents the activity id
     * @return the icon id
     */
    public Long getIconId(final long orgId, final long activityId) {
        if (exists(orgId, activityId)) {
            final Table activityTable = getTable();
            final Table activityTypeTable = Table.get(OMACACTIVITYTYPE.TABLE);
            final SelectQuery selectQuery = SelectQuery.get(activityTypeTable);

            selectQuery.addSelectClause(activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID));
            selectQuery.addJoinClause(new JoinClause(activityTypeTable.getPKColumn(), activityTable.getColumn(OMACACTIVITY.TYPE_ID)));
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityId)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (dataContainer.isEmpty()) {
                return null;
            }
            return dataContainer.getRow(activityTypeTable).get(OMACACTIVITYTYPE.ICON_ID);
        }
        return null;
    }

    /**
     * <p>
     * Loads the guest emails into {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param activityGuests  Represents the {@link Collection} of {@link ActivityGuest}
     * @param dataContainer   An entity container which contains the rows of existing records
     * @param targetContainer A container in which all the new/updated rows will be added
     */
    private void loadGuestEmailsIntoContainer(final long orgId, final Collection<ActivityGuest> activityGuests,
                                              final DataContainer dataContainer, final DataContainer targetContainer) {
        final Collection<Email> emailValues = activityGuests.stream().map(ActivityGuest::getEmail).toList();

        if (!emailValues.isEmpty()) {
            final Table emailTable = getEmailTable();
            final Column emailValueColumn = emailTable.getColumn(OMETEMAIL.VALUE);

            for (final Email email : emailValues) {
                final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailValueColumn, email.getValue())));

                email.setOrgId(orgId);

                if (Objects.nonNull(emailRow)) {
                    email.setId(emailRow.getPKValue());
                } else {
                    emailManager.loadEmailIntoContainer(true, email, targetContainer);
                }
            }
        }
    }

    /**
     * <p>
     * Appends the contact related {@link DataContainer}
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param dataContainer Represents the {@link DataContainer}
     */
    private void appendDataContainer(final Long orgId, final DataContainer dataContainer) {
        final Table contactTable = contactManager.getTable();
        final Collection<Long> contactIds = dataContainer.getRows(contactTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

        if (!contactIds.isEmpty()) {
            final Column contactPkColumn = contactTable.getPKColumn();
            final Table contactEmailTable = contactManager.getContactEmailTable();
            final Table contactPhoneTable = contactManager.getContactPhoneTable();
            final Table contactCompanyTable = contactManager.getContactCompanyTable();
            final Table phoneTable = getPhoneTable();
            final Table emailTable = getEmailTable();
            final Table companyAliasTable = Table.get(companyManager.getTable().getName(), ActivityHolderFields.COMPANY.getName());
            final Column companyPkColumn = companyAliasTable.getPKColumn();
            final JoinClause.JoinType leftJoin = LEFT;
            final SelectQuery selectQuery = SelectQuery.get(contactTable);

            selectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.VALUE), phoneTable.getColumn(OMETPHONE.COUNTRY_ID),
                    phoneTable.getColumn(OMETPHONE.NUMBER), companyAliasTable.getColumn(MCOCOMPANY.NAME), companyAliasTable.getColumn(MCOCOMPANY.WEBSITE), companyPkColumn));
            selectQuery.addSelectClause(contactEmailTable, contactPhoneTable, contactCompanyTable);
            selectQuery.addJoinClause(List.of(
                    new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                    new JoinClause(contactPkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin),
                    new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyPkColumn, leftJoin)));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds)));

            dataContainer.append(orgDataStore(orgId).get(selectQuery));
        }
    }

    /**
     * <p>
     * Converts the data to collection of {@link Contact}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param activityId      Represents the activity id
     * @param dataContainer   Represents the {@link DataContainer}
     * @param rowsByTableName A cache that holds the rows of existing records
     * @return the collection of contacts
     */
    private List<Contact> convertDataToModels(final Long orgId, final Long activityId, final DataContainer dataContainer,
                                              final Map<String, Map<Long, Row>> rowsByTableName) {
        final Table contactTable = getContactTable();
        final Table contactActivityTable = getContactActivityTable();
        final boolean isRowsByTableNameEmpty = rowsByTableName.isEmpty();

        final Collection<Row> contactActivityRows = isRowsByTableNameEmpty ? dataContainer.getRows(contactActivityTable,
                new WhereClause(Criteria.eq(contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), activityId))).toList()
                : rowsByTableName.get(contactActivityTable.getName()).values().stream().filter(row ->
                activityId.equals(row.get(contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID)))).toList();
        final Collection<Long> contactIds = contactActivityRows.stream().map(row ->
                (Long) row.get(MACCONTACTACTIVITY.CONTACT_ID)).collect(Collectors.toSet());
        final Collection<Row> contactRows = isRowsByTableNameEmpty ? dataContainer.getRows(contactTable,
                new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds))).toList() :
                rowsByTableName.get(contactTable.getName()).values().stream().filter(contactRow ->
                        contactIds.contains(contactRow.getPKValue())).toList();

        return contactRows.stream().map(contactRow ->
                        buildContactPreviewData(contactRow, orgId)).collect(Collectors.toList());
    }

    /**
     * <p>
     * Builds the contact
     * </p>
     *
     * @param contactRow Represents the contact row
     * @param orgId      Represents the org ID
     * @return the {@link Contact}
     */
    private Contact buildContactPreviewData(final Row contactRow, final Long orgId) {
        final Contact contact = new Contact();
        final Long contactId = contactRow.getPKValue();

        contact.setId(contactId);
        contact.setOrgId(orgId);
        contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
        contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
        contact.setOwnerId(contactRow.get(MCOCONTACT.OWNER_ID));

        return contact;
    }

    /**
     * <p>
     * Gets the collection of {@link ActivityHolder} based on the given start and end time
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param startTime          Represents the start time of the activities to be fetched
     * @param endTime            Represents the end time if the activities to be fetched
     * @param filterCriteriaList Which contains the collection of {@link FilterCriteria}
     * @param activityTypeIds    Represents the {@link Collection} of activity type ids
     * @return The Collection of {@link ActivityHolder}
     */
    public Collection<ActivityHolder> getActivities(final long orgId, final Long startTime, final Long endTime, final int start, final int limit,
                                                    final Collection<FilterCriteria> filterCriteriaList, final Collection<Long> activityTypeIds) {
        final Collection<ActivityHolder> activityHolders = new ArrayList<>();
        final Table activityTable = getTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column startTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column endTimeColumn = activityTable.getColumn(OMACACTIVITY.END_TIME);
        final Column typeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Column activityPkColumn = activityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = LEFT;
        final Table dealTable = getDealTable();
        final Table companyTable = getCompanyTable();
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column dealActivityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
        final Column companyActivityCompanyIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID);

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), typeIdColumn, startTimeColumn,
                endTimeColumn, activityTable.getColumn(OMACACTIVITY.IS_DONE), activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID),
                activityTable.getColumn(OMACACTIVITY.STATUS), activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), activityTable.getColumn(OMACACTIVITY.ORG_ID),
                activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO),
                activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME), activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO),
                companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID),
                companyActivityCompanyIdColumn, companyActivityIdColumn, dealActivityDealIdColumn, dealActivityIdColumn,
                dealTable.getColumn(MDODEAL.TITLE)));
        selectQuery.addJoinClause(List.of(new JoinClause(typeIdColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityPkColumn, dealActivityIdColumn, leftJoin),
                new JoinClause(dealActivityDealIdColumn, dealTable.getPKColumn(), leftJoin),
                new JoinClause(activityPkColumn, companyActivityIdColumn, leftJoin),
                new JoinClause(companyActivityCompanyIdColumn, companyTable.getPKColumn(), leftJoin)));
        final WhereClause whereClause = new WhereClause(Criteria.between(startTimeColumn, startTime, endTime))
                .or(Criteria.between(endTimeColumn, startTime, endTime)).or(new WhereClause(Criteria.lt(startTimeColumn, startTime))
                        .and(Criteria.gt(endTimeColumn, endTime)));
        selectQuery.setWhereClause(activityTypeIds.isEmpty() ? whereClause : whereClause.and(new WhereClause(Criteria.in(typeIdColumn, activityTypeIds))));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, selectQuery, filterCriteriaList);
        selectQuery.addOrderByClause(new OrderByClause(startTimeColumn, ASCENDING));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(activityTable).forEach(row -> {
            final ActivityHolder activityHolder = getActivityHolder(row.getPKValue(), dataContainer);
            final Activity activity = convertRowToActivity(row, dataContainer);

            loadCheckInOutRecordIntoActivity(row, activity);
            activityHolder.setActivity(activity);
            activityHolder.setUserIds(List.of(activity.getAssignee().id()));

            activityHolders.add(activityHolder);
        });

        return activityHolders;
    }

    /**
     * <p>
     * Converts {@link Row} to {@link Activity}
     * </p>
     *
     * @param row           Represents the {@link Row} of activity
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link Activity}
     */
    private Activity convertRowToActivity(final Row row, final DataContainer dataContainer) {
        final Activity activity = new Activity();
        final Row activityTypeRow = dataContainer.getRow(activityTypeManager.getTable(), (Long) row.get(OMACACTIVITY.TYPE_ID));

        activity.setId(row.getPKValue());
        activity.setOrgId(row.getOrgId());
        activity.setTitle(row.get(OMACACTIVITY.TITLE));
        activity.setStartTime(row.get(OMACACTIVITY.START_TIME));
        activity.setEndTime(row.get(OMACACTIVITY.END_TIME));
        activity.setStatus(Status.get(row.get(OMACACTIVITY.STATUS)));
        activity.setDone(row.get(OMACACTIVITY.IS_DONE));
        activity.setAssignee(userService.getUser(row.getOrgId(), row.get(OMACACTIVITY.ASSIGNEE_ID)));

        if (Objects.nonNull(activityTypeRow)) {
            final ActivityType activityType = new ActivityType();

            activityType.setActivityIcon(iconService.get(activityTypeRow.get(OMACACTIVITYTYPE.ICON_ID)));
            activity.setType(activityType);
        }

        return activity;
    }

    /**
     * <p>
     * Gets the {@link ActivityHolder}
     * </p>
     *
     * @param activityId    Represents the activity id
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link ActivityHolder}
     */
    private ActivityHolder getActivityHolder(final Long activityId, final DataContainer dataContainer) {
        final ActivityHolder activityHolder = new ActivityHolder();
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Row dealActivityRow = dataContainer.getRow(dealActivityTable, new WhereClause(Criteria.eq(dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityId)));
        final Row companyActivityRow = dataContainer.getRow(companyActivityTable, new WhereClause(Criteria.eq(companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), activityId)));

        if (Objects.nonNull(dealActivityRow)) {
            final Row dealRow = dataContainer.getRow(getDealTable(), (Long) dealActivityRow.get(MACDEALACTIVITY.DEAL_ID));

            if (Objects.nonNull(dealRow)) {
                final Deal deal = new Deal();

                deal.setTitle(dealRow.get(MDODEAL.TITLE));
                deal.setId(dealRow.getPKValue());
                activityHolder.setDeal(deal);
            }
        }

        if (Objects.nonNull(companyActivityRow)) {
            final Row companyRow = dataContainer.getRow(getCompanyTable(), (Long) companyActivityRow.get(MACCOMPANYACTIVITY.COMPANY_ID));

            if (Objects.nonNull(companyRow)) {
                final Company company = new Company();

                company.setName(companyRow.get(MCOCOMPANY.NAME));
                company.setId(companyRow.getPKValue());
                company.setOrgId(companyRow.getOrgId());
                activityHolder.setCompany(company);
            }
        }

        return activityHolder;
    }

    /**
     * <p>
     * Gets the activity which is mapped with given source
     * </p>
     *
     * @param orgId                  Represents an organisation id
     * @param entityId               Represents the entity id
     * @param activityFilterCriteria Represents the activity filter criteria
     * @param source                 Represents the {@link Source}
     * @param activityTypeIds        Represents the {@link Collection} of activity type ids
     * @return Collection of {@link Activity}
     */
    public Collection<ActivityHolder> getActivities(final long orgId, final Long entityId, final int start, final int limit,
                                                    final ActivityFilterCriteria activityFilterCriteria, final Source source, final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final DataContainer activityDataContainer = getDataContainer(orgId, entityId,
                new LimitClause(start, limit), activityFilterCriteria, source, activityTypeIds);
        final Column noteActivityIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);
        final Collection<Long> noteIds = activityDataContainer.getRows(noteActivityTable).map(row -> (Long) row.get(MACNOTEACTIVITY.NOTE_ID)).toList();
        final Map<Long, Note> notes = noteIds.isEmpty() ? Collections.emptyMap() : noteManager.getNotes(orgId, noteIds);

        return activityDataContainer.getRows(activityTable).map(activityRow -> {
            final ActivityHolder activityHolder = convertActivityRowToModel(activityRow, activityDataContainer);
            final Row noteActivityRow = activityDataContainer.getRow(noteActivityTable, new WhereClause(Criteria.eq(noteActivityIdColumn, activityHolder.id())));
            final Activity activity = activityHolder.getActivity();

            if (Objects.nonNull(noteActivityRow)) {
                final Long noteId = noteActivityRow.get(MACNOTEACTIVITY.NOTE_ID);

                activity.setNote(notes.get(noteId));
            }
            return activityHolder;
        }).toList();
    }

    /**
     * <p>
     * Gets the activity {@link DataContainer} based on the given parameters
     * </p>
     *
     * @param orgId                  Represents an organisation id
     * @param entityId               Represents the entity id
     * @param limitClause            Represents the {@link LimitClause}
     * @param activityFilterCriteria Represents the {@link ActivityFilterCriteria}
     * @param source                 Represents the {@link Source}
     * @param activityTypeIds        Represents the {@link Collection} of activity type ids
     * @return The {@link DataContainer} which contains the activity data
     */
    private DataContainer getDataContainer(final long orgId, final Long entityId, final LimitClause limitClause,
                                           final ActivityFilterCriteria activityFilterCriteria, final Source source, final Collection<Long> activityTypeIds) {
        final Table activityTable = getTable();
        final Table noteActivityTable = activityManager.getNoteActivityTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);

        if (!activityTypeIds.isEmpty()) {
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria
                    .in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND));
        }
        final Column activityStartTimeColumn = activityTable.getColumn(OMACACTIVITY.START_TIME);
        final Column noteActivityIdColumn = noteActivityTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);

        selectQuery.addSelectClause(getDealActivityTable(), getCompanyActivityTable(), getContactActivityTable(), noteActivityTable);
        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), activityStartTimeColumn,
                activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), activityTable.getColumn(OMACACTIVITY.DESCRIPTION),
                activityTable.getColumn(OMACACTIVITY.ORG_ID), activityTable.getColumn(OMACACTIVITY.IS_DONE),
                activityTable.getColumn(OMACACTIVITY.STATUS), activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO),
                activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO), activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME),
                activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME)));
        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getPKColumn(), noteActivityIdColumn, LEFT)));
        buildSelectQuery(source, entityId, selectQuery, selectQuery.getWhereClause());
        selectQuery.setLimitClause(limitClause);
        final Optional<WhereClause> optionalWhereClause = activityFilterCriteria.getWhereClause();

        optionalWhereClause.ifPresent(criteriaWhereClause -> selectQuery.setWhereClause(WhereClauseUtil
                .connect(selectQuery.getWhereClause(), criteriaWhereClause, Connector.AND)));

        for (final Column column : getOrderByColumns(activityFilterCriteria, activityTable)) {
            selectQuery.addOrderByClause(new OrderByClause(column, activityFilterCriteria.getOrderType()));
        }
        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds the {@link SelectQuery}
     * </p>
     *
     * @param source      Represents the {@link Source}
     * @param entityId    Represents the entity id
     * @param selectQuery Represents the {@link SelectQuery}
     * @param whereClause Represents the {@link WhereClause}
     */
    private void buildSelectQuery(final Source source, final Long entityId, final SelectQuery selectQuery, final WhereClause whereClause) {
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Table companyTable = Table.get(MCOCOMPANY.TABLE);
        final Table contactTable = Table.get(MCOCONTACT.TABLE);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Table dealActivityTable = getDealActivityTable();
        final Table contactActivityTable = getContactActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column dealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);
        final Column companyActivityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);
        final Column companyIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID);
        final Column contactIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID);
        final Column activityPKColumn = getTable().getPKColumn();
        final Column companyNameColumn = companyTable.getColumn(MCOCOMPANY.NAME);
        final Column contactFirstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column contactLastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column dealTitleColumn = dealTable.getColumn(MDODEAL.TITLE);
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column companyOrgIdColumn = companyTable.getColumn(MCOCOMPANY.ORG_ID);
        final Column contactOrgIdColumn = contactTable.getColumn(MCOCONTACT.ORG_ID);

        switch (source) {
            case CONTACT -> {
                selectQuery.addSelectClause(List.of(dealTitleColumn, companyNameColumn, companyOrgIdColumn));
                selectQuery.addJoinClause(List.of(new JoinClause(activityPKColumn, contactActivityIdColumn),
                        new JoinClause(activityPKColumn, dealActivityIdColumn, leftJoin),
                        new JoinClause(dealIdColumn, dealPkColumn, leftJoin),
                        new JoinClause(activityPKColumn, companyActivityIdColumn, leftJoin),
                        new JoinClause(companyIdColumn, companyPkColumn, leftJoin)));
                selectQuery.setWhereClause(WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(contactIdColumn, entityId)), Connector.AND));
            }
            case DEAL -> {
                selectQuery.addSelectClause(List.of(companyNameColumn, contactFirstNameColumn, contactLastNameColumn, companyOrgIdColumn, contactOrgIdColumn));
                selectQuery.addJoinClause(List.of(new JoinClause(activityPKColumn, dealActivityIdColumn),
                        new JoinClause(activityPKColumn, companyActivityIdColumn, leftJoin),
                        new JoinClause(companyIdColumn, companyPkColumn, leftJoin),
                        new JoinClause(activityPKColumn, contactActivityIdColumn, leftJoin),
                        new JoinClause(contactIdColumn, contactPkColumn, leftJoin)));
                selectQuery.setWhereClause(WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(dealIdColumn, entityId)), Connector.AND));
            }
            case COMPANY -> {
                selectQuery.addSelectClause(List.of(dealTitleColumn, contactFirstNameColumn, contactLastNameColumn, contactOrgIdColumn));
                selectQuery.addJoinClause(List.of(new JoinClause(activityPKColumn, companyActivityIdColumn),
                        new JoinClause(activityPKColumn, contactActivityIdColumn, leftJoin),
                        new JoinClause(contactIdColumn, contactPkColumn, leftJoin),
                        new JoinClause(activityPKColumn, dealActivityIdColumn, leftJoin),
                        new JoinClause(dealIdColumn, dealPkColumn, leftJoin)));
                selectQuery.setWhereClause(WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(companyIdColumn, entityId)), Connector.AND));
            }
        }
    }

    /**
     * <p>
     * Fetches activities for a given organization id and a collection of entity IDs based on the specified {@link Source}
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param entityIds Which contains the collection of entity ids
     * @param source    Represents the {@link Source}
     * @return A {@link Map} where the key is the entity id and the value is a collection of activities associated with that entity id
     */
    public Map<Long, Collection<Activity>> fetchActivitiesBySource(final long orgId, final Collection<Long> entityIds, final Source source) {
        final Table activityTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column activityPkColumn = activityTable.getPKColumn();

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.IS_DONE), activityTable.getColumn(OMACACTIVITY.TYPE_ID),
                activityTable.getColumn(OMACACTIVITY.END_TIME), activityTable.getColumn(OMACACTIVITY.START_TIME),
                activityTable.getColumn(OMACACTIVITY.CREATED_TIME), activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME), activityTypeTable.getColumn(OMACACTIVITYTYPE.OWNER_ID),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID)));
        selectQuery.addSelectClause(activityTypeTable);
        selectQuery.addJoinClause(new JoinClause(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeTable.getPKColumn()));

        return switch (source) {
            case CONTACT -> {
                final Table contactActivityTable = getContactActivityTable();
                final Column contactIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID);
                final Column contactActivityIdColumn = contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID);

                selectQuery.addSelectClause(List.of(contactIdColumn, contactActivityIdColumn));
                selectQuery.addJoinClause(new JoinClause(activityPkColumn, contactActivityIdColumn));
                selectQuery.setWhereClause(new WhereClause(Criteria.in(contactIdColumn, entityIds)));
                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

                yield convertActivities(orgId, dataContainer, activityTable, contactActivityTable, entityIds, source);
            }
            case COMPANY -> {
                final Table companyActivityTable = getCompanyActivityTable();
                final Column companyIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID);
                final Column activityIdColumn = companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID);

                selectQuery.addSelectClause(List.of(companyIdColumn, activityIdColumn));
                selectQuery.addJoinClause(new JoinClause(activityPkColumn, activityIdColumn));
                selectQuery.setWhereClause(new WhereClause(Criteria.in(companyIdColumn, entityIds)));
                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

                yield convertActivities(orgId, dataContainer, activityTable, companyActivityTable, entityIds, source);
            }
            case DEAL -> {
                final Table dealActivityTable = getDealActivityTable();
                final Column dealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
                final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);

                selectQuery.addSelectClause(List.of(dealIdColumn, dealActivityIdColumn));
                selectQuery.addJoinClause(new JoinClause(activityPkColumn, dealActivityIdColumn));
                selectQuery.setWhereClause((new WhereClause(Criteria.in(dealIdColumn, entityIds))));
                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

                yield convertActivities(orgId, dataContainer, activityTable, dealActivityTable, entityIds, source);
            }
            default -> Collections.emptyMap();
        };
    }

    /**
     * <p>
     * Converts activities based on the provided {@link DataContainer} and maps them to the corresponding entity ids
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param dataContainer A container which contains the source activities rows
     * @param activityTable Represents the activity table
     * @param sourceTable   Represents the activity source table
     * @param source        Represents the {@link Source}
     * @param entityIds     Which contains the collection of entity ids
     * @return A {@link Map} where the key is the entity ID and the value is a collection of activities associated with that entity id
     */
    private Map<Long, Collection<Activity>> convertActivities(final long orgId, final DataContainer dataContainer,
                                                              final Table activityTable, final Table sourceTable,
                                                              final Collection<Long> entityIds, final Source source) {
        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, Collection<Activity>> entityActivityMap = new HashMap<>();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Column activityPKColumn = activityTable.getPKColumn();
        String entityIdColumn = null;
        String activityIdColumn = null;

        switch (source) {
            case CONTACT -> {
                entityIdColumn = MACCONTACTACTIVITY.CONTACT_ID;
                activityIdColumn = MACCONTACTACTIVITY.ACTIVITY_ID;
            }
            case COMPANY -> {
                entityIdColumn = MACCOMPANYACTIVITY.COMPANY_ID;
                activityIdColumn = MACCOMPANYACTIVITY.ACTIVITY_ID;
            }
            case DEAL -> {
                entityIdColumn = MACDEALACTIVITY.DEAL_ID;
                activityIdColumn = MACDEALACTIVITY.ACTIVITY_ID;
            }
        }

        for (final Long entityId : entityIds) {
            final String sourceActivityIdColumn = activityIdColumn;
            final Collection<Long> activityIds = dataContainer.getRows(sourceTable, new WhereClause(Criteria.eq(sourceTable.getColumn(entityIdColumn), entityId)))
                    .map(row -> (Long) row.get(sourceActivityIdColumn)).toList();
            final Stream<Row> activityRows = dataContainer.getRows(activityTable, new WhereClause(Criteria.in(activityPKColumn, activityIds)));
            final Collection<Activity> activities = new ArrayList<>();

            activityRows.forEach(activityRow -> {
                final Activity activity = new Activity();
                final Row activityTypeRow = dataContainer.getRow(activityTypeTable, (Long) activityRow.get(OMACACTIVITY.TYPE_ID));

                if (Objects.nonNull(activityTypeRow)) {
                    activity.setType(activityTypeManager.convertRowToModel(activityTypeRow));
                }
                activity.setId(activityRow.getPKValue());
                activity.setOrgId(orgId);
                activity.setDone(activityRow.get(OMACACTIVITY.IS_DONE));
                activity.setEndTime(activityRow.get(OMACACTIVITY.END_TIME));
                activity.setStartTime(activityRow.get(OMACACTIVITY.START_TIME));
                activity.setCreatedTime(activityRow.get(OMACACTIVITY.CREATED_TIME));
                activity.setUpdatedTime(activityRow.get(OMACACTIVITY.MODIFIED_TIME));
                activities.add(activity);

            });
            entityActivityMap.put(entityId, activities);
        }

        return entityActivityMap;
    }

    /**
     * <p>
     * Gets the {@link ActivityHolder} matches for the given organization and criteria
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param start           Start value represents the lower bound of timeline to be fetched
     * @param limit           Limit value represents the upper bound of timeline to be fetched
     * @param status          Represents the {@link Status}
     * @param fieldName       Represents the name of the field the sorts needs to apply
     * @param filterCriteria  Represents the {@link FilterCriteria}
     * @param sortType        Represents the {@link SortType}
     * @param activityTypeIds Represents the {@link Collection} of activity type ids
     * @return {@link Collection} of {@link ActivityHolder}
     */
    public Collection<ActivityHolder> getKanbanActivities(final long orgId, final int start, final int limit, final Status status, final String fieldName,
                                                          final Collection<FilterCriteria> filterCriteria, final SortType sortType, final Collection<Long> activityTypeIds) {
        final Table activityTable = activityManager.getTable();
        final Table dealActivityTable = getDealActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Table companyTable = Table.get(MCOCOMPANY.TABLE);

        final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column activityPKColumn = activityTable.getPKColumn();
        final Column activityStatusIdColumn = activityTable.getColumn(OMACACTIVITY.STATUS);

        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final JoinClause.JoinType leftJoin = LEFT;
        WhereClause whereClause = null;

        if (Objects.nonNull(status)) {
            whereClause = new WhereClause(Criteria.eq(activityStatusIdColumn, status.getId()));
        }

        if (!activityTypeIds.isEmpty()) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeIds)), Connector.AND);
        }

        if (Objects.nonNull(whereClause)) {
            selectQuery.setWhereClause(whereClause);
        }

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), activityStatusIdColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME),
                activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), activityTable.getColumn(OMACACTIVITY.START_TIME), activityTable.getColumn(OMACACTIVITY.END_TIME),
                activityTypeIdColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityTable.getColumn(OMACACTIVITY.COMPLETED_TIME),
                activityTable.getColumn(OMACACTIVITY.IS_DONE), activityTable.getColumn(OMACACTIVITY.CHECK_IN_INFO), activityTable.getColumn(OMACACTIVITY.CHECK_OUT_INFO),
                activityTable.getColumn(OMACACTIVITY.CHECKED_IN_TIME), activityTable.getColumn(OMACACTIVITY.CHECKED_OUT_TIME),
                activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE), companyTable.getColumn(MCOCOMPANY.NAME),
                dealTable.getColumn(MDODEAL.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
        selectQuery.addSelectClause(dealActivityTable, companyActivityTable);
        selectQuery.addJoinClause(List.of(new JoinClause(activityTypeIdColumn, activityTypeTable.getPKColumn()),
                new JoinClause(activityPKColumn, dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(activityPKColumn, companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID), companyTable.getPKColumn(), leftJoin),
                new JoinClause(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealTable.getPKColumn(), leftJoin)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        applySortCriteria(fieldName, sortType, selectQuery);

        if (Objects.nonNull(filterCriteria) && !filterCriteria.isEmpty()) {
            filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, selectQuery, filterCriteria);
        }

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(activityTable).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Apply the sort criteria for the given {@link SelectQuery} based on sort type and given field
     * </p>
     *
     * @param field       Represents the field name
     * @param sortType    Represents the {@link SortType}
     * @param selectQuery Represents the {@link SelectQuery}
     */
    private void applySortCriteria(final String field, final SortType sortType, final SelectQuery selectQuery) {
        final Table activityTable = activityManager.getTable();

        final Column column = switch (field) {
            case "title" -> activityTable.getColumn(OMACACTIVITY.TITLE);
            case "status" -> activityTable.getColumn(OMACACTIVITY.STATUS);
            case "availability" -> activityTable.getColumn(OMACACTIVITY.AVAILABILITY);
            case "endTime" -> activityTable.getColumn(OMACACTIVITY.END_TIME);
            case "modifiedTime" -> activityTable.getColumn(OMACACTIVITY.MODIFIED_TIME);
            case "createdTime" -> activityTable.getColumn(OMACACTIVITY.CREATED_TIME);
            case "duration" -> activityTable.getColumn(OMACACTIVITY.DURATION);
            case "completedTime" -> activityTable.getColumn(OMACACTIVITY.COMPLETED_TIME);
            case "lastNotificationTime" -> activityTable.getColumn(OMACACTIVITY.LAST_NOTIFICATION_TIME);
            case "activityType" -> activityTable.getColumn(OMACACTIVITY.TYPE_ID);
            default -> activityTable.getColumn(OMACACTIVITY.START_TIME);
        };

        selectQuery.addSelectClause(column);
        selectQuery.addOrderByClause(new OrderByClause(column, sortType.getOrderType()));
    }

    /**
     * <p>
     * Gets the {@link Collection} of activities mapped for the given {@link Collection} of deal id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param dealIds Represents the {@link Collection} of deal id
     * @return the {@link Map} of activities
     */
    public Map<Long, Collection<Activity>> getDealActivities(final long orgId, final Collection<Long> dealIds) {
        final Table activityTable = activityManager.getTable();
        final Table dealActivityTable = getDealActivityTable();
        final SelectQuery selectQuery = SelectQuery.get(dealActivityTable);

        selectQuery.addJoinClause(new JoinClause(dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), activityTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.START_TIME), activityTable.getColumn(OMACACTIVITY.END_TIME)));
        selectQuery.addSelectClause(dealActivityTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        } else {
            final Map<Long, Collection<Activity>> dealActivities = new HashMap<>();

            dataContainer.getRows(dealActivityTable).forEach(row -> {
                final Row activityRow = dataContainer.getRow(activityTable, (Long) row.get(MACDEALACTIVITY.ACTIVITY_ID));
                final Activity activity = new Activity();

                activity.setStartTime(activityRow.get(OMACACTIVITY.START_TIME));
                activity.setEndTime(activityRow.get(OMACACTIVITY.END_TIME));

                dealActivities.computeIfAbsent(row.get(MACDEALACTIVITY.DEAL_ID), activities -> new ArrayList<>()).add(activity);
            });

            return dealActivities;
        }
    }

    /**
     * Retrieves the total activity count.
     *
     * @param orgId            Represents an organization id.
     * @param dropdownCriteria Represents a dropdown criteria.
     * @return The count of entities.
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMACACTIVITY.TITLE))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Fetches the {@link Activity} for the dropdown actions.
     *
     * @param orgId    Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of companies.
     */
    public Collection<Activity> getActivityDropdownList(final Long orgId, final DropdownLimit limit, final DropdownCriteria criteria) {
        final Table table = getTable();
        final Column titleColumn = table.getColumn(OMACACTIVITY.TITLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(titleColumn);
        selectQuery.setWhereClause(new WhereClause(criteria.build(titleColumn)));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        selectQuery.setLimitClause(limit.getLimitClause());

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final Activity activity = new Activity();

            activity.setOrgId(orgId);
            activity.setId(row.getPKValue());
            activity.setTitle(row.get(OMACACTIVITY.TITLE));

            return activity;
        }).toList();
    }

    /**
     * <p>
     * Checks whether the given user is the activity assignee or not
     * </p>
     *
     * @param orgId         Represents the organisation Id
     * @param userId        Represents the user Id
     * @param activityId    Represents the activity Id
     * @return True if the user is the activity assignee, false otherwise
     */
    public boolean isUserActivityAssignee(final Long orgId, final Long userId, final Long activityId) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getColumn(OMACACTIVITY.ASSIGNEE_ID), userId))
                .and(Criteria.eq(activityTable.getPKColumn(), activityId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Stores the check-in and check-out record
     * </p>
     *
     * @param checkInOutRecord Represents the {@link CheckInOutRecord}
     */
    public void storeCheckInOutRecord(final CheckInOutRecord checkInOutRecord) {
        activityManager.storeCheckInOutRecord(checkInOutRecord);
        loadCheckInCheckOutIntoTimeline(checkInOutRecord);
    }

    /**
     * <p>
     * Loads the {@link CheckInOutRecord} into timeline
     * </p>
     *
     * @param checkInOutRecord Represents the {@link CheckInOutRecord}
     */
    private void loadCheckInCheckOutIntoTimeline(final CheckInOutRecord checkInOutRecord) {
        final Table activityTable = getTable();
        final Long orgId = checkInOutRecord.orgId();
        final Long activityId = checkInOutRecord.getActivityId();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table contactActivityTable = getContactActivityTable();
        final Table companyActivityTable = getCompanyActivityTable();
        final Table dealActivityTable = getDealActivityTable();
        final SelectQuery selectQuery = SelectQuery.get(activityTable);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.TITLE), activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID),
                contactActivityTable.getColumn(MACCONTACTACTIVITY.CONTACT_ID), companyActivityTable.getColumn(MACCOMPANYACTIVITY.COMPANY_ID),
                dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID)));
        selectQuery.addJoinClause(List.of(new JoinClause(activityTable.getColumn(OMACACTIVITY.TYPE_ID), activityTypeTable.getPKColumn()),
                new JoinClause(activityTable.getPKColumn(), contactActivityTable.getColumn(MACCONTACTACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(activityTable.getPKColumn(), companyActivityTable.getColumn(MACCOMPANYACTIVITY.ACTIVITY_ID), leftJoin),
                new JoinClause(activityTable.getPKColumn(), dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row activityRow = dataContainer.getRow(activityTable);
        final Row activityTypeRow = dataContainer.getRow(activityTypeTable);
        final Timeline timeline = buildCheckInOutTimeline(checkInOutRecord, activityRow);

        timeline.setIcon(iconService.get(activityTypeRow.get(OMACACTIVITYTYPE.ICON_ID)));

        loadCheckInOutIntoSourceTimeline(timeline, dataContainer);
    }

    /**
     * <p>
     * Builds the {@link Timeline} for the {@link CheckInOutRecord}
     * </p>
     *
     * @param checkInOutRecord Represents the {@link CheckInOutRecord}
     * @param activityRow      Represent the activity {@link Row}
     * @return The {@link Timeline}
     */
    private Timeline buildCheckInOutTimeline(final CheckInOutRecord checkInOutRecord, final Row activityRow) {
        final Long orgId = checkInOutRecord.orgId();
        final Long activityId = checkInOutRecord.getActivityId();
        final boolean isCheckIn = checkInOutRecord.isCheckIn();
        final String title = activityRow.get(OMACACTIVITY.TITLE);
        final Timeline timeline = new Timeline();
        final JsonObject valueJson = Json.object();
        final FieldTimeline fieldTimeline;

        valueJson.put(ActivityFields.TITLE.getName(), title);
        final String privateNote = activityManager.getPrivateNote(orgId, activityId);

        if (Objects.nonNull(privateNote)) {
            final Note note = new Note();

            note.setContent(privateNote);
            valueJson.put(ActivityFields.NOTE.getName(), note.getTrimmedContent(TIMELINE_NOTE_MAX_LENGTH));
        }

        if (isCheckIn) {
            valueJson.put(ActivityFields.CHECK_IN.getName(), checkInOutRecord.getDataJson());

            fieldTimeline = new FieldTimeline(null, valueJson.toString(), new I18nKey("i18n.activity.display.checkIn"));
        } else {
            final CheckInOutRecord checkInRecord = activityManager.getCheckInRecord(orgId, activityId);

            if (Objects.nonNull(checkInRecord)) {
                final JsonObject checkInJson = checkInRecord.getDataJson();
                final JsonObject checkOutJson = checkInOutRecord.getDataJson();

                valueJson.put(ActivityFields.CHECK_IN.getName(), checkInJson);
                valueJson.put(ActivityFields.CHECK_OUT.getName(), checkOutJson);
                valueJson.put(ActivityFields.TOTAL_TIME_SPENT.getName(), checkInOutRecord.getTime() - checkInRecord.getTime());
            }
            fieldTimeline = new FieldTimeline(null, valueJson.toString(), new I18nKey("i18n.activity.display.checkOut"));
        }
        timeline.setFieldTimeline(fieldTimeline);
        timeline.setTimelineType(isCheckIn ? TimelineType.CHECK_IN : TimelineType.CHECK_OUT);
        timeline.setAction(isCheckIn ? TimelineActionFields.CHECKED_IN : TimelineActionFields.CHECKED_OUT);
        timeline.setOrgId(orgId);
        timeline.setOwnerId(checkInOutRecord.getUserId());
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));

        return timeline;
    }

    /**
     * <p>
     * Loads the {@link CheckInOutRecord} into source timeline
     * </p>
     *
     * @param timeline      Represents the {@link Timeline}
     * @param dataContainer Represents the {@link DataContainer}
     */
    private void loadCheckInOutIntoSourceTimeline(final Timeline timeline, final DataContainer dataContainer) {
        final DataContainer targetContainer = DataContainer.create();
        final Row contactActivityRow = dataContainer.getRow(getContactActivityTable());
        final Row companyActivityRow = dataContainer.getRow(getCompanyActivityTable());
        final Row dealActivityRow = dataContainer.getRow(getDealActivityTable());

        if (Objects.nonNull(contactActivityRow)) {
            timeline.setCategory(Timeline.Category.CONTACT);
            commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
            final Long contactId = contactActivityRow.get(MACCONTACTACTIVITY.CONTACT_ID);

            commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), contactId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                    new Row(commonTimelineManager.getContactTimelineTable()), targetContainer);
        }

        if (Objects.nonNull(companyActivityRow)) {
            timeline.setCategory(Timeline.Category.COMPANY);
            commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
            final Long companyId = companyActivityRow.get(MACCOMPANYACTIVITY.COMPANY_ID);

            commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), companyId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                    new Row(commonTimelineManager.getCompanyTimelineTable()), targetContainer);
        }

        if (Objects.nonNull(dealActivityRow)) {
            timeline.setCategory(Timeline.Category.DEAL);
            commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
            final Long dealId = dealActivityRow.get(MACDEALACTIVITY.DEAL_ID);

            commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), dealId, MTLDEALTIMELINE.DEAL_ID,
                    MTLDEALTIMELINE.TIMELINE_ID, new Row(commonTimelineManager.getDealTimelineTable()), targetContainer);
        }
        orgDataStore(timeline.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Gets the total number of overdue activities by applying the given criteria list.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @return The total number of activities.
     */
    public Long getActivitiesCount(final Long orgId, final Collection<FilterCriteria> criteriaList) {
        final Table activityTable = getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        filterCriteriaHandler.applyCriteria(orgId, Provider.ACTIVITY_LIST, countQuery, criteriaList);

        return orgDataStore(orgId).get(countQuery);
    }
}