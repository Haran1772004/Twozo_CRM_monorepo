package com.tlc.crm.core.features.timeline.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.timeline.store.ConversableTimelineManagerStore;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.features.invoice.timeline.InvoiceTimelineManager;
import com.tlc.crm.core.notification.email.timeline.impl.EmailTimelineProvider;

import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.timeline.model.EntityTimeline;
import com.tlc.crm.entity.timeline.model.TimelineManager;
import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.entity.timeline.model.ActivityTimeline;
import com.tlc.crm.entity.timeline.model.AttachmentTimeline;
import com.tlc.crm.entity.timeline.model.CompanyTimeline;
import com.tlc.crm.entity.timeline.model.ContactTimeline;
import com.tlc.crm.entity.timeline.model.DealTimeline;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMFICOMPANYFILE;
import com.tlc.crm.core.sql.resource.OMFICONTACTFILE;
import com.tlc.crm.core.sql.resource.OMFIDEALFILE;
import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;
import com.tlc.crm.core.sql.resource.MTLACTIVITYTIMELINE;
import com.tlc.crm.core.sql.resource.MTLNOTESTIMELINE;
import com.tlc.crm.core.sql.resource.MWFCONTACTWEBFORM;
import com.tlc.crm.core.sql.resource.MWFDEALWEBFORM;

import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.timeline.model.EmailTimeline;
import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.InvoiceTimeline;
import com.tlc.crm.entity.timeline.model.NotesTimeline;
import com.tlc.crm.entity.timeline.model.WebFormTimeline;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.sql.resource.MACNOTEACTIVITY;
import com.tlc.crm.entity.sql.resource.MTLATTACHMENTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLFIELDVALUETIMELINE;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.entity.sql.resource.OMTLTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.Timeline.Category;
import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.sql.resource.OMWFWEBFORM;
import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Timeline} related DB actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ReadableTimelineManager extends AbstractReadableConfigManager<Timeline> implements TimelineManager {

    private static final short TIMELINE_NOTE_MAX_LENGTH = 200;
    private static final Collection<String> UPDATE_ACTION_FIELDS = List.of(TimelineActionFields.UPDATED, TimelineActionFields.CHANGED,
            TimelineActionFields.EDITED, TimelineActionFields.MOVED, TimelineActionFields.FOLLOWERS_ADDED,
            TimelineActionFields.FOLLOWERS_REMOVED, TimelineActionFields.REASSIGNED);

    private final CompanyManager companyManager;
    private final CommonTimelineManager commonTimelineManager;
    private final IconService iconService;
    private final WebFormManager webFormManager;
    private final EmailTimelineProvider emailTimelineProvider;
    private final InvoiceTimelineManager invoiceTimelineManager;
    private final ConversableTimelineManagerStore conversableTimelineManagerStore;

    private static class Instance {
        private static final ReadableTimelineManager INSTANCE = new ReadableTimelineManager();
    }

    private ReadableTimelineManager() {
        companyManager = CompanyManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        iconService = IconService.getInstance();
        webFormManager = WebFormManager.getInstance();
        emailTimelineProvider = EmailTimelineProvider.getInstance();
        invoiceTimelineManager = InvoiceTimelineManager.getInstance();
        conversableTimelineManagerStore =  ConversableTimelineManagerStore.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ReadableTimelineManager} Instance
     * </p>
     *
     * @return {@link ReadableTimelineManager}
     */
    public static ReadableTimelineManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of timeline
     */
    @Override
    public Table getTable() {
        return Table.get(OMTLTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the activity timeline table
     * </p>
     *
     * @return the {@link Table} of activity timeline
     */
    public Table getActivityTimelineTable() {
        return Table.get(MTLACTIVITYTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the notes timeline table
     * </p>
     *
     * @return the {@link Table} of notes timeline
     */
    public Table getNotesTimelineTable() {
        return Table.get(MTLNOTESTIMELINE.TABLE);
    }

    /**
     * <p>
     * Gets the attachment timeline table
     * </p>
     *
     * @return the {@link Table} of attachment timeline
     */
    public Table getAttachmentTimelineTable() {
        return Table.get(MTLATTACHMENTTIMELINE.TABLE);
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
     * Gets the note alias table
     * </p>
     *
     * @return The {@link Table} of note
     */
    public Table getNoteAliasTable() {
        return Table.get(Table.get(OMNTNOTE.TABLE).getName(), ActivityFields.ACTIVITY_NOTE.getName());
    }

    /**
     * <p>
     * Retrieves the timelines mapped for given contact id
     * </p>
     *
     * @param orgId      Represents organization id
     * @param contactId  Represents contact id
     * @param start      Start value represents the lower bound of timeline to be fetched
     * @param limit      Limit value represents the upper bound of timeline to be fetched
     * @param categories Which holds the {@link Set} of category
     * @return the {@link Collection} of Timeline
     */
    public Collection<Timeline> getContactTimeline(final Long orgId, final Long contactId, final int start,
                                                   final int limit, final Set<Category> categories) {
        final Collection<Timeline> contactTimelines = new ArrayList<>();
        final Optional<DataContainer> dataContainer = getContactTimelineContainer(orgId, contactId, categories, start, limit);

        if (dataContainer.isPresent()) {
            final DataContainer contactTimelineContainer = dataContainer.get();

            if (categories.isEmpty()) {
                loadTimelines(contactTimelines, Deal.ENTITY_TYPE, contactTimelineContainer);
            }

            loadSourceTimelines(orgId, contactTimelines, categories, Contact.ENTITY_TYPE, contactTimelineContainer);
        }

        return contactTimelines;
    }

    /**
     * <p>
     * Retrieves the timelines mapped for given deal id
     * </p>
     *
     * @param orgId      Represents organization id
     * @param dealId     Represents deal id
     * @param start      Start value represents the lower bound of timeline to be fetched
     * @param limit      Limit value represents the upper bound of timeline to be fetched
     * @param categories Which holds the {@link Set} of category
     * @return the {@link Collection} of Timeline
     */
    public Collection<Timeline> getDealTimeline(final Long orgId, final Long dealId, final int start, final int limit,
                                                final Set<Category> categories) {
        final Collection<Timeline> dealTimelines = new ArrayList<>();
        final Optional<DataContainer> dataContainer = getDealTimelineContainer(orgId, dealId, categories, start, limit);

        dataContainer.ifPresent(container -> loadSourceTimelines(orgId, dealTimelines, categories, Deal.ENTITY_TYPE, container));

        return dealTimelines;
    }

    /**
     * <p>
     * Retrieves the timelines mapped for given company id
     * </p>
     *
     * @param orgId      Represents organization id
     * @param companyId  Represents company id
     * @param start      Start value represents the lower bound of timeline to be fetched
     * @param limit      Limit value represents the upper bound of timeline to be fetched
     * @param categories Which holds the {@link Set} of category
     * @return the {@link Collection} of Timeline
     */
    public Collection<Timeline> getCompanyTimeline(final Long orgId, final Long companyId, final int start,
                                                   final int limit, final Set<Category> categories) {
        final Collection<Timeline> companyTimelines = new ArrayList<>();
        final Optional<DataContainer> dataContainer = getCompanyTimelineContainer(orgId, companyId, categories, start, limit);

        if (dataContainer.isPresent()) {
            final DataContainer companyTimelineContainer = dataContainer.get();

            if (categories.isEmpty()) {
                loadTimelines(companyTimelines, Deal.ENTITY_TYPE, companyTimelineContainer);
            }

            loadSourceTimelines(orgId, companyTimelines, categories, Company.ENTITY_TYPE, companyTimelineContainer);
        }

        return companyTimelines;
    }

    /**
     * <p>
     * Loads the timelines for the given source id
     * </p>
     *
     * @param timelines     Holds the {@link Collection} of timeline entity
     * @param categories    Holds the {@link Set} of category
     * @param entityName    Represents the name of the entity
     * @param dataContainer Which contains the rows of timeline related records
     */
    private void loadSourceTimelines(final long orgId, final Collection<Timeline> timelines, final Set<Category> categories,
                                     final String entityName, final DataContainer dataContainer) {
        if (categories.isEmpty()) {
            final Map<Category, Set<Long>> categoryTimelineIds = groupTimelineIdsByCategory(dataContainer);
            final Set<Long> emailTimelineIds = categoryTimelineIds.get(Category.EMAILS);
            final Set<Long> invoiceTimelineIds = categoryTimelineIds.get(Category.INVOICE);
            final Set<Long> callTimelineIds = categoryTimelineIds.get(Category.CALLS);

            if (Objects.nonNull(emailTimelineIds) && !emailTimelineIds.isEmpty()) {
                final Map<Long, EntityTimelines> entityTimelines = emailTimelineProvider.get(orgId, emailTimelineIds);

                if (!entityTimelines.isEmpty()) {
                    loadEmailTimelines(entityName, timelines, dataContainer, entityTimelines);
                }
            }

            if (Objects.nonNull(invoiceTimelineIds) && !invoiceTimelineIds.isEmpty()) {
                final Map<Long, EntityTimelines> entityTimelines = invoiceTimelineManager.get(orgId, invoiceTimelineIds);

                if (!entityTimelines.isEmpty()) {
                    loadInvoiceTimelines(timelines, dataContainer, entityTimelines);
                }
            }

            if (Objects.nonNull(callTimelineIds) && !callTimelineIds.isEmpty()) {
                loadCallTimeline(orgId, dataContainer, timelines, callTimelineIds);
            }
            loadTimelines(timelines, entityName, dataContainer);
            loadActivityTimelines(timelines, dataContainer);
            loadFileTimelines(timelines, dataContainer);
            loadNoteTimelines(timelines, dataContainer);
            loadChatTimelines(timelines, dataContainer);
        } else {
            loadFilteredTimeline(entityName, orgId, timelines, categories, dataContainer);
        }
    }

    /**
     * <p>
     * Builds the select query to retrieve the timelines.
     * </p>
     *
     * @param categories Which holds the {@link Set} of category
     */
    private SelectQuery buildSelectQuery(final Set<Category> categories) {
        final Table timelineTable = getTable();
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(timelineTable);

        selectQuery.addJoinClause(new JoinClause(timelinePKColumn, fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(timelineTable, fieldValueTimelineTable);

        return selectQuery;
    }

    /**
     * <p>
     * Loads the timeline for any update action related to the source
     * </p>
     *
     * @param timelines     Which hold the {@link Collection} of timeline entity
     * @param dataContainer Which contains the rows of existing records
     */
    private void loadUpdatedTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Stream<Row> timelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.ACTION), UPDATE_ACTION_FIELDS)));

        loadSourceTimelines(timelines, timelineRows, dataContainer);
    }

    /**
     * <p>
     * Loads the timeline based on category
     * </p>
     *
     * @param timelines     Which hold the {@link Collection} of timeline entity
     * @param category      Which represents category for filter
     * @param dataContainer Which contains the rows of existing records
     */
    private void loadTimelines(final Collection<Timeline> timelines, final String category, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Stream<Row> timelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), category)));

        loadSourceTimelines(timelines, timelineRows, dataContainer);
    }

    /**
     * <p>
     * Loads the source related timeline in collection of timelines
     * </p>
     *
     * @param timelines     A list which hold the {@link Collection} of timeline entity
     * @param timelineRows  Rows holds the values of the timeline table
     * @param dataContainer Which contains the rows of timeline records
     */
    private void loadSourceTimelines(final Collection<Timeline> timelines, final Stream<Row> timelineRows,
                                     final DataContainer dataContainer) {
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Column timelineIdColumn = fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID);
        final Table contactTable = Table.get(MCOCONTACT.TABLE);

        timelineRows.forEach(timelineRow -> {
            final Timeline timeline = convertRowToModel(timelineRow);
            boolean isEntityRowPresent = false;

            switch (timeline.getCategory()) {
                case CONTACT -> {
                    final Table contactTimelineTable = getContactTimelineTable();
                    final String action = timelineRow.get(OMTLTIMELINE.ACTION);
                    final Row contactTimelineRow = dataContainer.getRow(contactTimelineTable, new WhereClause(Criteria
                            .eq(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID), timeline.id())));

                    if (Objects.nonNull(contactTimelineRow)) {

                        if (TimelineActionFields.SUBMITTED.equals(action)) {
                            final Table webFormTable = webFormManager.getTable();
                            final Table contactWebFormTable = Table.get(MWFCONTACTWEBFORM.TABLE);
                            final Row contactWebFormRow = dataContainer.getRow(contactWebFormTable,
                                    new WhereClause(Criteria.eq(contactWebFormTable.getColumn(MWFCONTACTWEBFORM.CONTACT_ID),
                                            contactTimelineRow.get(MTLCONTACTTIMELINE.CONTACT_ID))));

                            if (Objects.nonNull(contactWebFormRow)) {
                                final Row webFormRow = dataContainer.getRow(webFormTable, (Long) contactWebFormRow.get(MWFCONTACTWEBFORM.WEB_FORM_ID));
                                final WebFormTimeline webFormTimeline = new WebFormTimeline();

                                webFormTimeline.setId(webFormRow.getPKValue());
                                webFormTimeline.setName(webFormRow.get(OMWFWEBFORM.NAME));
                                timeline.setEntityTimeline(webFormTimeline);
                            }
                        }

                        final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria
                                .eq(contactTable.getPKColumn(), contactTimelineRow.get(MTLCONTACTTIMELINE.CONTACT_ID))));

                        if (Objects.nonNull(contactRow)) {
                            final Contact contact = new Contact();
                            final ContactTimeline contactTimeline = new ContactTimeline();

                            contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                            contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
                            contactTimeline.setName(contact.getFullName());

                            isEntityRowPresent = true;
                            timeline.setEntityTimeline(contactTimeline);
                        }
                    }
                }
                case DEAL -> {
                    final Table dealTimelineTable = getDealTimelineTable();
                    final Table dealTable = DealManager.getInstance().getTable();
                    final Row dealTimelineRow = dataContainer.getRow(dealTimelineTable,
                            new WhereClause(Criteria.eq(dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID), timeline.id())));
                    final String action = timelineRow.get(OMTLTIMELINE.ACTION);

                    if (Objects.nonNull(dealTimelineRow)) {

                        if (TimelineActionFields.SUBMITTED.equals(action)) {
                            final Table webFormTable = webFormManager.getTable();
                            final Table dealWebFormTable = Table.get(MWFDEALWEBFORM.TABLE);
                            final Row dealWebFormRow = dataContainer.getRow(dealWebFormTable,
                                    new WhereClause(Criteria.eq(dealWebFormTable.getColumn(MWFDEALWEBFORM.DEAL_ID),
                                            dealTimelineRow.get(MTLDEALTIMELINE.DEAL_ID))));

                            if (Objects.nonNull(dealWebFormRow)) {
                                final Row webFormRow = dataContainer.getRow(webFormTable, (Long) dealWebFormRow.get(MWFDEALWEBFORM.WEB_FORM_ID));
                                final WebFormTimeline webFormTimeline = new WebFormTimeline();

                                webFormTimeline.setId(webFormRow.getPKValue());
                                webFormTimeline.setName(webFormRow.get(OMWFWEBFORM.NAME));
                                timeline.setEntityTimeline(webFormTimeline);
                            }
                        }

                        final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(),
                                dealTimelineRow.get(MTLDEALTIMELINE.DEAL_ID))));

                        if (Objects.nonNull(dealRow)) {
                            final DealTimeline dealTimeline = new DealTimeline();

                            dealTimeline.setTitle(dealRow.get(MDODEAL.TITLE));
                            dealTimeline.setId(dealRow.getPKValue());

                            isEntityRowPresent = true;
                            timeline.setEntityTimeline(dealTimeline);
                        }
                    }
                }
                case COMPANY -> {
                    final Table companyTable = companyManager.getTable();
                    final Table companyTimelineTable = getCompanyTimelineTable();
                    final Row companyTimelineRow = dataContainer.getRow(companyTimelineTable,
                            new WhereClause(Criteria.eq(companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID), timeline.id())));

                    if (Objects.nonNull(companyTimelineRow)) {
                        final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                                companyTimelineRow.get(MTLCOMPANYTIMELINE.COMPANY_ID))));

                        if (Objects.nonNull(companyRow)) {
                            final CompanyTimeline companyTimeline = new CompanyTimeline();

                            companyTimeline.setName(companyRow.get(MCOCOMPANY.NAME));

                            isEntityRowPresent = true;
                            timeline.setEntityTimeline(companyTimeline);
                        }
                    }
                }
            }

            if (!timeline.getTimelineType().isEntityDetailsNeeded() || isEntityRowPresent) {
                final Collection<Row> fieldValueTimelineRows = dataContainer.getRows(fieldValueTimelineTable,
                        new WhereClause(Criteria.eq(timelineIdColumn, timeline.id()))).toList();

                if (!fieldValueTimelineRows.isEmpty()) {
                    setFieldTimelineRowToModel(timeline, fieldValueTimelineRows);
                }
                timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

                timelines.add(timeline);
            }
        });
    }

    /**
     * <p>
     * Sets the {@link FieldTimeline} rows to model
     * </p>
     *
     * @param timeline               Represents the {@link Timeline}
     * @param fieldValueTimelineRows Represents the {@link Stream} of field timeline rows
     */
    private void setFieldTimelineRowToModel(final Timeline timeline, final Collection<Row> fieldValueTimelineRows) {
        final Collection<String> oldValues = new ArrayList<>();
        final Collection<String> newValues = new ArrayList<>();
        I18nKey fieldName = null;

        for (final Row fieldValueTimelineRow : fieldValueTimelineRows) {
            final String oldValue = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.OLD_VALUE);
            final String newValue = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE);
            fieldName = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.FIELD_NAME);

            switch (timeline.getAction()) {
                case TimelineActionFields.REASSIGNED -> {

                    newValues.add(newValue);
                    oldValues.add(oldValue);

                    timeline.addUserId(Long.parseLong(newValue));
                    timeline.addUserId(Long.parseLong(oldValue));
                }
                case TimelineActionFields.FOLLOWERS_ADDED -> processFollowers(newValue, newValues, TimelineActionFields.FOLLOWERS_ADDED, timeline);
                case TimelineActionFields.FOLLOWERS_REMOVED -> processFollowers(newValue, newValues, TimelineActionFields.FOLLOWERS_REMOVED, timeline);
                case TimelineActionFields.RELATED_CONTACTS_ADDED -> processRelatedContacts(newValue, newValues, TimelineActionFields.RELATED_CONTACTS_ADDED);
                case TimelineActionFields.RELATED_CONTACTS_REMOVED -> processRelatedContacts(newValue, newValues, TimelineActionFields.RELATED_CONTACTS_REMOVED);
                case TimelineActionFields.PRODUCTS_ADDED -> processProducts(newValue, newValues, TimelineActionFields.PRODUCTS_ADDED);
                case TimelineActionFields.PRODUCTS_REMOVED -> processProducts(newValue, newValues, TimelineActionFields.PRODUCTS_REMOVED);
                default -> {
                    newValues.add(newValue);
                    oldValues.add(oldValue);
                }
            }
        }

        timeline.setFieldTimeline(new FieldTimeline(oldValues, newValues, fieldName));
    }

    /**
     * <p>
     * Process the followers from the field value rows and adds to the timeline.
     * </p>
     *
     * @param newValue    The JSON string containing follower details
     * @param newValues   Represents the {@link Collection} of new values
     * @param actionField The key to extract the follower array (FOLLOWERS_ADDED or FOLLOWERS_REMOVED)
     * @param timeline    Represents the {@link Timeline}
     */
    private void processFollowers(final String newValue, final Collection<String> newValues, final String actionField, final Timeline timeline) {
        final JsonArray array = Json.object(newValue).optJsonArray(actionField);

        newValues.remove(newValue);

        if (Objects.nonNull(array)) {
            for (int index = 0; index < array.size(); index++) {
                final long followerId = array.getLong(index);

                timeline.addUserId(followerId);
                newValues.add(String.valueOf(followerId));
            }
        }
    }

    /**
     * <p>
     * Loads the notes related timeline in collection of timelines
     * </p>
     *
     * @param timelines     List which hold the Collection of timeline entity
     * @param dataContainer Which contains the rows of timeline related records
     */
    private void loadNoteTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Stream<Row> notesTimelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Category.NOTES.name())));

        timelines.addAll(notesTimelineRows.map(noteTimelineRow -> {
            final Timeline timeline = convertRowToModel(noteTimelineRow);
            final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable,
                    new WhereClause(Criteria.eq(fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), timeline.id())));

            if (Objects.nonNull(fieldValueTimelineRow)) {
                final NotesTimeline notesTimeline = new NotesTimeline();

                notesTimeline.setNotes(fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE));
                timeline.setIcon(iconService.get(noteTimelineRow.get(OMTLTIMELINE.ICON_ID)));
                timeline.setEntityTimeline(notesTimeline);
            }

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Loads the chat related timelines to the collection of timelines
     * </p>
     *
     * @param timelines     Represents the collection of timelines
     * @param dataContainer Represents the container that contains the rows of timeline related records
     */
    private void loadChatTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Stream<Row> chatTimelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Category.CHATS.name())));

        timelines.addAll(chatTimelineRows.map(chatTimelineRow -> {
            final Timeline timeline = convertRowToModel(chatTimelineRow);
            final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable,
                    new WhereClause(Criteria.eq(fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), timeline.id())));

            timeline.setFieldTimeline(new FieldTimeline(fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE)));

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Loads the activity related timeline in collection of timelines
     * </p>
     *
     * @param timelines     A list which hold the Collection of timeline entity
     * @param dataContainer Which contains the rows of timeline related records
     */
    private void loadActivityTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Table activityTimelineTable = getActivityTimelineTable();
        final Table activityTable = ActivityManager.getInstance().getTable();
        final Table noteAliasTable = getNoteAliasTable();
        final Table activityNoteTable = Table.get(MACNOTEACTIVITY.TABLE);
        final Stream<Row> timelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Category.ACTIVITY.name())));

        timelines.addAll(timelineRows.map(timelineRow -> {
            final Timeline timeline = convertRowToModel(timelineRow);
            final Row activityTimelineRow = dataContainer.getRow(activityTimelineTable,
                    new WhereClause(Criteria.eq(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID),
                            timeline.id())));

            timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

            if (Objects.nonNull(activityTimelineRow)) {
                final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
                final Column timelineIdColumn = fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID);
                final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable,
                        new WhereClause(Criteria.eq(timelineIdColumn, timeline.id())));

                if (Objects.nonNull(fieldValueTimelineRow)) {
                    final String oldValue = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.OLD_VALUE);
                    final I18nKey fieldName = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.FIELD_NAME);
                    final String newValue = fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE);
                    final ActivityTimeline activityTimeline = new ActivityTimeline();

                    if (fieldName.getKey().equals("i18n.activity.note")) {
                        activityTimeline.setActivityNote(newValue);
                    } else {
                        timeline.setFieldTimeline(new FieldTimeline(Optional.ofNullable(oldValue).map(List::of)
                                .orElseGet(Collections::emptyList), newValue, fieldName));
                        final Row activityRow = dataContainer.getRow(activityTable,
                                new WhereClause(Criteria.eq(activityTable.getPKColumn(), activityTimelineRow.get(MTLACTIVITYTIMELINE.ACTIVITY_ID))));

                        if (Objects.nonNull(activityRow)) {
                            final Row activityNoteRow = dataContainer.getRow(activityNoteTable,
                                    new WhereClause(Criteria.eq(activityNoteTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID), activityRow.getPKValue())));

                            if (Objects.nonNull(activityNoteRow) && dataContainer.size(noteAliasTable) > 0) {
                                final Row noteRow = dataContainer.getRow(noteAliasTable,
                                        new WhereClause(Criteria.eq(noteAliasTable.getPKColumn(), activityNoteRow.get(MACNOTEACTIVITY.NOTE_ID))));

                                if (Objects.nonNull(noteRow)) {
                                    final Note note = new Note();

                                    note.setOrgId(noteRow.getOrgId());
                                    note.setContent(noteRow.get(OMNTNOTE.CONTENT));
                                    activityTimeline.setActivityNote(note.getTrimmedContent(TIMELINE_NOTE_MAX_LENGTH));
                                }
                            }
                        }
                    }
                    timeline.setEntityTimeline(activityTimeline);
                }
            }

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Loads the file related timeline in collection of timelines
     * </p>
     *
     * @param timelines     A list which hold the Collection of timeline entity
     * @param dataContainer Which contains the rows of timeline related records
     */
    private void loadFileTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer) {
        final Table timelineTable = getTable();
        final Table attachmentTimelineTable = getAttachmentTimelineTable();
        final Stream<Row> timelineRows = dataContainer.getRows(timelineTable,
                new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), Category.FILE.name())));

        timelines.addAll(timelineRows.map(timelineRow -> {
            final Timeline timeline = convertRowToModel(timelineRow);
            final Row attachmentRow = dataContainer.getRow(attachmentTimelineTable,
                    new WhereClause(Criteria.eq(attachmentTimelineTable.getColumn(MTLATTACHMENTTIMELINE.TIMELINE_ID), timeline.id())));

            timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

            if (Objects.nonNull(attachmentRow)) {
                final Long attachmentId = attachmentRow.get(MTLATTACHMENTTIMELINE.ATTACHMENT_ID);
                final AttachmentTimeline attachmentTimeline = new AttachmentTimeline();

                attachmentTimeline.setId(attachmentId);
                timeline.setEntityTimeline(attachmentTimeline);
            }

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Loads the filter timelines based on category.
     * </p>
     *
     * @param timelines     A list which hold the Collection of timeline entity
     * @param categories    Holds the {@link Set} of category
     * @param dataContainer Which contains the rows of timeline records
     */
    private void loadFilteredTimeline(final String entityName, final long orgId, final Collection<Timeline> timelines, final Set<Category> categories,
                                      final DataContainer dataContainer) {
        final Map<Category, Set<Long>> categoryTimelineIds = groupTimelineIdsByCategory(dataContainer);

        for (final Category category : categories) {
            switch (category) {
                case NOTES -> loadNoteTimelines(timelines, dataContainer);
                case ACTIVITY -> loadActivityTimelines(timelines, dataContainer);
                case FILE -> loadFileTimelines(timelines, dataContainer);
                case UPDATES -> loadUpdatedTimelines(timelines, dataContainer);
                case EMAILS -> {
                    final Set<Long> emailTimelineIds = categoryTimelineIds.get(Category.EMAILS);

                    if (Objects.nonNull(emailTimelineIds) && !emailTimelineIds.isEmpty()) {
                        final Map<Long, EntityTimelines> entityTimelines = emailTimelineProvider.get(orgId, emailTimelineIds);

                        if (!entityTimelines.isEmpty()) {
                            loadEmailTimelines(entityName, timelines, dataContainer, entityTimelines);
                        }
                    }
                }
                case INVOICE -> {
                    final Set<Long> invoiceTimelineIds = categoryTimelineIds.get(Category.INVOICE);

                    if (Objects.nonNull(invoiceTimelineIds) && !invoiceTimelineIds.isEmpty()) {
                        final Map<Long, EntityTimelines> entityTimelines = invoiceTimelineManager.get(orgId, invoiceTimelineIds);

                        if (!entityTimelines.isEmpty()) {
                            loadInvoiceTimelines(timelines, dataContainer, entityTimelines);
                        }
                    }
                }
                case CALLS -> {
                    final Set<Long> callTimelineIds = categoryTimelineIds.get(Category.CALLS);

                    if (Objects.nonNull(callTimelineIds) && !callTimelineIds.isEmpty()) {
                        loadCallTimeline(orgId, dataContainer, timelines, callTimelineIds);
                    }
                }
                case CHATS -> loadChatTimelines(timelines, dataContainer);
            }
        }
    }

    /**
     * <p>
     * Retrieves the number of timeline rows mapped for given contact id
     * </p>
     *
     * @param orgId     Represents Organization id
     * @param contactId Represents Contact id
     * @return the {@link Long} of contact timeline count
     */
    public Long getContactTimelineCount(final Long orgId, final Long contactId) {
        final Table contactTimelineTable = getContactTimelineTable();
        final CountQuery countQuery = CountQuery.get(contactTimelineTable);

        countQuery.addJoinClause(new JoinClause(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID), commonTimelineManager.getFieldValueTimelineTable()
                .getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), JoinClause.JoinType.LEFT));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID), contactId)));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of timeline rows for given deal id
     * </p>
     *
     * @param orgId  Represents Organization id
     * @param dealId Represents Deal id
     * @return the {@link Long} of deal timeline count
     */
    public Long getDealTimelineCount(final Long orgId, final Long dealId) {
        final Table dealTimelineTable = getDealTimelineTable();
        final Table timelineTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTimelineTable);
        final Column timelineIdColumn = dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID);

        countQuery.addJoinClause(List.of(new JoinClause(timelineIdColumn, timelineTable.getPKColumn()),
                new JoinClause(timelineIdColumn, commonTimelineManager.getFieldValueTimelineTable()
                        .getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), JoinClause.JoinType.LEFT)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID), dealId))
                .and(Criteria.notIn(timelineTable.getColumn(OMTLTIMELINE.TYPE), List.of(TimelineType.DEAL_ASSOCIATED_WITH_CONTACT.getId(), TimelineType.DEAL_ASSOCIATED_WITH_COMPANY.getId()))));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of timeline rows for given company id
     * </p>
     *
     * @param orgId     Represents Organization id
     * @param companyId Represents company id
     * @return the {@link Long} of company timeline count
     */
    public Long getCompanyTimelineCount(final Long orgId, final Long companyId) {
        final Table companyTimelineTable = getCompanyTimelineTable();
        final CountQuery countQuery = CountQuery.get(companyTimelineTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyTimelineTable
                .getColumn(MTLCOMPANYTIMELINE.COMPANY_ID), companyId)));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of timeline rows for given contact id based on filter
     * </p>
     *
     * @param orgId      Represents Organization id
     * @param contactId  Represents contact id
     * @param categories which holds {@link Set} of category for filter
     * @return the {@link Long} of contact filter timeline count
     */
    public Long getContactTimelineCount(final Long orgId, final Long contactId, final Set<Category> categories) {
        long timelineFilterCount = 0;
        final Table contactTimelineTable = getContactTimelineTable();

        for (final Category category : categories) {
            timelineFilterCount += category == Category.UPDATES ? getContactUpdatesCount(orgId, contactId) : switch (category) {
                case NOTES, ACTIVITY, FILE, EMAILS, CHATS, INVOICE -> getTimelineCountByCategory(orgId, contactId, contactTimelineTable, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID, category);
                default -> 0;
            };
        }

        return timelineFilterCount;
    }

    /**
     * <p>
     * Gets the update action count mapped for given contact id.
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param contactId Represents the id of the contact
     * @return {@link Long} of contact updates filter count
     */
    private Long getContactUpdatesCount(final Long orgId, final Long contactId) {
        final Table contactTimelineTable = getContactTimelineTable();
        final Table timelineTable = commonTimelineManager.getTable();
        final CountQuery countQuery = CountQuery.get(timelineTable);

        countQuery.addJoinClause(new JoinClause(timelineTable.getPKColumn(),
                contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID), contactId))
                .and(new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.ACTION), UPDATE_ACTION_FIELDS))));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of timeline rows for given deal id based on filter
     * </p>
     *
     * @param orgId      Represents Organization id
     * @param dealId     Represents deal id
     * @param categories which holds {@link Set} of category for filter
     * @return the {@link Long} of deal filter timeline count
     */
    public Long getDealTimelineCount(final Long orgId, final Long dealId, final Set<Category> categories) {
        final Table dealTimelineTable = getDealTimelineTable();
        long timelineFilterCount = 0;

        for (final Category category : categories) {
            timelineFilterCount += switch (category) {
                case NOTES -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.NOTES);
                case ACTIVITY -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.ACTIVITY);
                case FILE -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.FILE);
                case UPDATES -> getDealUpdatesCount(orgId, dealId);
                case EMAILS -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.EMAILS);
                case INVOICE -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.INVOICE);
                case CHATS -> getTimelineCountByCategory(orgId, dealId, dealTimelineTable, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID, Category.CHATS);
                default -> 0;
            };
        }

        return timelineFilterCount;
    }

    /**
     * <p>
     * Retrieves the number of timeline rows for given company id based on category for filter
     * </p>
     *
     * @param orgId      Represents Organization id
     * @param companyId  Represents company id
     * @param categories which holds {@link Set} of category for filter
     * @return the {@link Long} of company filter timeline count
     */
    public Long getCompanyTimelineCount(final Long orgId, final Long companyId, final Set<Category> categories) {
        long timelineFilterCount = 0;
        final Table companyTimelineTable = getCompanyTimelineTable();

        for (final Category category : categories) {
            timelineFilterCount += switch (category) {
                case ACTIVITY -> getTimelineCountByCategory(orgId, companyId, companyTimelineTable, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID, Category.ACTIVITY);
                case NOTES -> getTimelineCountByCategory(orgId, companyId, companyTimelineTable, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID, Category.NOTES);
                case FILE -> getTimelineCountByCategory(orgId, companyId, companyTimelineTable, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID, Category.FILE);
                case EMAILS -> getTimelineCountByCategory(orgId, companyId, companyTimelineTable, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID, Category.EMAILS);
                case INVOICE -> getTimelineCountByCategory(orgId, companyId, companyTimelineTable, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID, Category.INVOICE);
                case UPDATES -> getCompanyUpdatesCount(orgId, companyId);
                default -> 0;
            };
        }

        return timelineFilterCount;
    }

    /**
     * <p>
     * Gets the update action count mapped for given deal id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param dealId Represents the deal id
     * @return {@link Long} of deal updates filter count
     */
    private Long getDealUpdatesCount(final Long orgId, final Long dealId) {
        final Table dealTimelineTable = getDealTimelineTable();
        final Table timelineTable = commonTimelineManager.getTable();
        final CountQuery countQuery = CountQuery.get(timelineTable);

        countQuery.addJoinClause(new JoinClause(timelineTable.getPKColumn(), dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID), dealId))
                .and(new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.ACTION), UPDATE_ACTION_FIELDS))));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the update action count mapped for given deal id.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param companyId Represents the company id
     * @return {@link Long} of deal updates filter count
     */
    private Long getCompanyUpdatesCount(final long orgId, final Long companyId) {
        final Table companyTimelineTable = getCompanyTimelineTable();
        final Table timelineTable = commonTimelineManager.getTable();
        final CountQuery countQuery = CountQuery.get(timelineTable);

        countQuery.addJoinClause(new JoinClause(timelineTable.getPKColumn(), companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID), companyId))
                .and(new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.ACTION), UPDATE_ACTION_FIELDS))));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param timelineRow Timeline row to be converted
     * @return {@link Timeline}
     */
    @Override
    public Timeline convertRowToModel(final Row timelineRow) {
        final Timeline timeline = new Timeline();

        timeline.setCategory(Category.get(timelineRow.get(OMTLTIMELINE.CATEGORY)));
        timeline.setOrgId(timelineRow.getOrgId());
        timeline.setAuditedTime(timelineRow.get(OMTLTIMELINE.AUDITED_TIME));
        timeline.setAction(timelineRow.get(OMTLTIMELINE.ACTION));
        timeline.setOwnerId(timelineRow.get(OMTLTIMELINE.OWNER_ID));
        timeline.setId(timelineRow.getPKValue());
        timeline.setTimelineType(TimelineType.valueOf((int) timelineRow.get(OMTLTIMELINE.TYPE)));
        timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

        return timeline;
    }

    /**
     * <p>
     * Gets the contact timeline for the given contact id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactId  Represents the contact id
     * @param categories Represents the {@link Set} of {@link Category}
     * @param start      Represents the starting index of contact timeline to be fetched
     * @param limit      Represents the maximum number of contact timeline to be fetched
     * @return contact timeline {@link DataContainer}
     */
    private Optional<DataContainer> getContactTimelineContainer(final long orgId, final Long contactId, final Set<Category> categories, final int start, final int limit) {
        final Table timelineTable = getTable();
        final Table contactTimelineTable = getContactTimelineTable();
        final Column contactTimelineColumn = contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID);
        final Column contactIdColumn = contactTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID);
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final SelectQuery query = SelectQuery.get(timelineTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(contactIdColumn, contactId));

        query.addSelectClause(timelineTable.getPKColumn());
        query.addJoinClause(List.of(new JoinClause(timelinePKColumn, contactTimelineColumn)));
        query.setWhereClause(categories.isEmpty() ? whereClause : whereClause.and(getWhereClause(categories)));
        query.setLimitClause(new LimitClause(start, limit));
        query.addOrderByClause(new OrderByClause(timelineTable.getColumn(OMTLTIMELINE.AUDITED_TIME),
                OrderByClause.OrderType.DESCENDING));
        final Collection<Long> timelineIds = secureOrgDataStore(orgId).get(query).getRows(timelineTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

        if (!timelineIds.isEmpty()) {
            return Optional.ofNullable(buildTimelineContainer(orgId, contactId, categories, Source.CONTACT, timelineIds));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Gets the where clause based on the provided collection of {@link Category}
     * </p>
     *
     * @param categories Represents the timeline categories
     * @return the where clause criteria
     */
    private WhereClause getWhereClause(final Set<Category> categories) {
        final Table timelineTable = getTable();
        final Set<Category> allowedCategories = new HashSet<>(categories);
        WhereClause updateCategoryCriteria = null;
        WhereClause otherCategoryCriteria = null;

        if (allowedCategories.remove(Category.UPDATES)) {
            updateCategoryCriteria = new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.ACTION), UPDATE_ACTION_FIELDS));
        }

        if (!allowedCategories.isEmpty()) {
            otherCategoryCriteria = new WhereClause(Criteria.in(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), categories));
        }

        return WhereClauseUtil.connect(updateCategoryCriteria, otherCategoryCriteria, Connector.OR);
    }

    /**
     * <p>
     * Gets the deal timeline for the given deal id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param dealId     Represents the deal id
     * @param categories Represents the {@link Set} of {@link Category}
     * @param start      Represents the starting index of deal timeline to be fetched
     * @param limit      Represents the maximum number of deal timeline to be fetched
     * @return deal timeline {@link DataContainer}
     */
    private Optional<DataContainer> getDealTimelineContainer(final long orgId, final Long dealId, final Set<Category> categories, final int start, final int limit) {
        final Table timelineTable = getTable();
        final Table dealTimelineTable = getDealTimelineTable();
        final Column dealTimelineColumn = dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID);
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final SelectQuery query = SelectQuery.get(timelineTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID), dealId));

        query.addSelectClause(timelineTable.getPKColumn());
        query.addJoinClause(List.of(new JoinClause(timelinePKColumn, dealTimelineColumn)));
        query.setWhereClause(categories.isEmpty() ? whereClause : whereClause.and(getWhereClause(categories)));
        query.setLimitClause(new LimitClause(start, limit));
        query.addOrderByClause(new OrderByClause(timelineTable.getColumn(OMTLTIMELINE.AUDITED_TIME),
                OrderByClause.OrderType.DESCENDING));
        final Collection<Long> timelineIds = secureOrgDataStore(orgId).get(query).getRows(timelineTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

        if (!timelineIds.isEmpty()) {
            return Optional.ofNullable(buildTimelineContainer(orgId, dealId, categories, Source.DEAL, timelineIds));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Gets the company timeline {@link DataContainer} for the given company id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param companyId  Represents the company id
     * @param categories Represents the {@link Set} of {@link Category}
     * @param start      Represents the starting index of company timeline to be fetched
     * @param limit      Represents the maximum number of company timeline to be fetched
     * @return company timeline {@link DataContainer}
     */
    private Optional<DataContainer> getCompanyTimelineContainer(final long orgId, final Long companyId,
                                                                final Set<Category> categories, final int start, final int limit) {
        final Table timelineTable = getTable();
        final Table companyTimelineTable = getCompanyTimelineTable();
        final Column companyTimelineColumn = companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID);
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final SelectQuery query = SelectQuery.get(timelineTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID), companyId));

        query.addSelectClause(timelineTable.getPKColumn());
        query.addJoinClause(List.of(new JoinClause(timelinePKColumn, companyTimelineColumn)));
        query.setWhereClause(categories.isEmpty() ? whereClause : whereClause.and(getWhereClause(categories)));
        query.setLimitClause(new LimitClause(start, limit));
        query.addOrderByClause(new OrderByClause(timelineTable.getColumn(OMTLTIMELINE.AUDITED_TIME),
                OrderByClause.OrderType.DESCENDING));
        final Collection<Long> timelineIds = secureOrgDataStore(orgId).get(query).getRows(timelineTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());

        if (!timelineIds.isEmpty()) {
            return Optional.ofNullable(buildTimelineContainer(orgId, companyId, categories, Source.COMPANY, timelineIds));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Builds the query with an existing entity container.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param entityId      Represents the entity id
     * @param categories    Represents the timeline categories
     * @param source        Represents the {@link Source}
     * @param timelineIds   Represents the collection of timeline ids
     * @return {@link DataContainer}
     */
    private DataContainer buildTimelineContainer(final long orgId, final Long entityId, final Set<Category> categories,
                                                 final Source source, final Collection<Long> timelineIds ) {
        final boolean isCategoryEmpty = categories.isEmpty();
        final SelectQuery selectQuery = buildSelectQuery(categories);
        final Table activityNoteTable = Table.get(MACNOTEACTIVITY.TABLE);
        final Table dealTimelineTable = Table.get(MTLDEALTIMELINE.TABLE);
        final Table timelineTable = getTable();

        Table entityNoteTable = null;
        Table entityActivityTable = null;
        Table entityFileTable = null;
        Table entityTimelineTable = null;
        Table entityWebFormTable = null;

        final Column timelinePKColumn = timelineTable.getPKColumn();
        Column timelineEntityIdColumn = null;

        final String timelineIdKey = TimelineFields.TIMELINE_ID.name();
        String entityIdKey = null;

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        switch (source) {
            case DEAL -> {
                entityNoteTable = Table.get(OMNTDEALNOTE.TABLE);
                entityActivityTable = Table.get(MACDEALACTIVITY.TABLE);
                entityTimelineTable = Table.get(MTLDEALTIMELINE.TABLE);
                entityFileTable = Table.get(OMFIDEALFILE.TABLE);
                entityWebFormTable = Table.get(MWFDEALWEBFORM.TABLE);

                timelineEntityIdColumn = entityTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID);

                entityIdKey = DealFields.DEALID;
            }
            case CONTACT -> {
                final Table contactTable = ContactManager.getInstance().getTable();

                entityNoteTable = Table.get(OMNTCONTACTNOTE.TABLE);
                entityActivityTable = Table.get(MACCONTACTACTIVITY.TABLE);
                entityTimelineTable = Table.get(MTLCONTACTTIMELINE.TABLE);
                entityFileTable = Table.get(OMFICONTACTFILE.TABLE);
                entityWebFormTable = Table.get(MWFCONTACTWEBFORM.TABLE);

                final Column contactPKColumn = contactTable.getPKColumn();
                final Column timelineIdColumn = entityTimelineTable.getColumn(timelineIdKey);

                timelineEntityIdColumn = entityTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID);

                entityIdKey = TimelineFields.CONTACT_ID.name();

                selectQuery.addJoinClause(List.of(new JoinClause(timelinePKColumn, timelineIdColumn),
                        new JoinClause(timelineEntityIdColumn, contactPKColumn)));
                selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), timelineEntityIdColumn,
                        contactTable.getColumn(MCOCONTACT.LAST_NAME), timelineIdColumn));
            }
            case COMPANY -> {
                final Table companyTable = Table.get(MCOCOMPANY.TABLE);

                entityTimelineTable = Table.get(MTLCOMPANYTIMELINE.TABLE);
                entityActivityTable = Table.get(MACCOMPANYACTIVITY.TABLE);
                entityFileTable = Table.get(OMFICOMPANYFILE.TABLE);
                entityNoteTable = Table.get(OMNTCOMPANYNOTE.TABLE);

                timelineEntityIdColumn = entityTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID);
                final Column companyPKColumn = companyTable.getPKColumn();
                final Column companyTimelineColumn = entityTimelineTable.getColumn(timelineIdKey);

                entityIdKey = TimelineFields.COMPANY_ID.name();

                selectQuery.addJoinClause(List.of(new JoinClause(timelinePKColumn, companyTimelineColumn),
                        new JoinClause(timelineEntityIdColumn, companyPKColumn)));
                selectQuery.addSelectClause(List.of(companyPKColumn, companyTable.getColumn(MCOCOMPANY.NAME), companyTimelineColumn, timelineEntityIdColumn));
            }
        }

        if (source == Source.DEAL || isDealTimelinePresent(orgId, timelineIds)) {
            final Column timelineIdColumn = dealTimelineTable.getColumn(timelineIdKey);
            final Column timelineDealColumn = dealTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID);
            final Table dealTable = Table.get(MDODEAL.TABLE);

            selectQuery.addJoinClause(List.of(new JoinClause(timelinePKColumn, timelineIdColumn, leftJoin),
                    new JoinClause(timelineDealColumn, dealTable.getPKColumn(), leftJoin)));
            selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), timelineDealColumn, timelineIdColumn));
        }
        final WhereClause whereClause = new WhereClause(Criteria.eq(timelineEntityIdColumn, entityId))
                .and(Criteria.in(timelinePKColumn, timelineIds));

        selectQuery.setWhereClause(Source.DEAL == source ? whereClause.and(Criteria.notIn(timelineTable
                .getColumn(OMTLTIMELINE.TYPE), List.of(TimelineType.DEAL_ASSOCIATED_WITH_CONTACT.getId(),
                TimelineType.DEAL_ASSOCIATED_WITH_COMPANY.getId()))) : whereClause);
        final DataContainer dataContainer = secureOrgDataStore(orgId).get(selectQuery);

        if (Objects.nonNull(entityNoteTable) && (isCategoryEmpty || categories.contains(Category.NOTES)) && isNoteTimelinePresent(orgId, timelineIds)) {
            final SelectQuery noteTimelineQuery = SelectQuery.get(timelineTable);
            final Table noteTable = Table.get(OMNTNOTE.TABLE);
            final Table notesTimelineTable = getNotesTimelineTable();
            final Column notesTimelineColumn = notesTimelineTable.getColumn(MTLNOTESTIMELINE.TIMELINE_ID);

            noteTimelineQuery.addJoinClause(List.of(new JoinClause(timelinePKColumn, notesTimelineColumn, leftJoin),
                    new JoinClause(notesTimelineTable.getColumn(MTLNOTESTIMELINE.NOTES_ID), noteTable.getPKColumn(), leftJoin)));
            noteTimelineQuery.addSelectClause(noteTable.getColumn(OMNTNOTE.CONTENT));
            noteTimelineQuery.addSelectClause(notesTimelineTable);
            noteTimelineQuery.setWhereClause(new WhereClause(Criteria.in(timelinePKColumn, timelineIds)));
            dataContainer.append(secureOrgDataStore(orgId).get(noteTimelineQuery));
        }

        if (Objects.nonNull(entityActivityTable) && (isCategoryEmpty || categories.contains(Category.ACTIVITY)) && isActivityTimelinePresent(orgId, timelineIds)) {
            final Table activityTimelineTable = getActivityTimelineTable();
            final Column activityTimelineColumn = activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID);
            final SelectQuery activityTimelineQuery = SelectQuery.get(timelineTable);
            final Table activityTable = Table.get(OMACACTIVITY.TABLE);
            final Column activityPkColumn = activityTable.getPKColumn();

            activityTimelineQuery.addJoinClause(List.of(new JoinClause(timelinePKColumn, activityTimelineColumn, leftJoin),
                    new JoinClause(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.ACTIVITY_ID), activityPkColumn, leftJoin)));
            activityTimelineQuery.addSelectClause(List.of(activityTable.getColumn(OMACACTIVITY.STATUS), activityTable.getColumn(OMACACTIVITY.TITLE),
                    activityTable.getColumn(OMACACTIVITY.IS_DONE), activityTable.getColumn(OMACACTIVITY.TYPE_ID)));
            activityTimelineQuery.addSelectClause(activityTimelineTable);

            if (Objects.nonNull(activityNoteTable) && isActivityNoteTimelinePresent(orgId, timelineIds)) {
                final Table noteAliasTable = getNoteAliasTable();
                final Column activityNoteIdColumn = activityNoteTable.getColumn(MACNOTEACTIVITY.NOTE_ID);
                final Column noteAliasPkColumn = noteAliasTable.getPKColumn();
                final Column noteActivityIdColumn = activityNoteTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID);

                activityTimelineQuery.addJoinClause(List.of(new JoinClause(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.ACTIVITY_ID),
                                activityNoteTable.getColumn(TimelineFields.ACTIVITY_ID.name()), leftJoin),
                        new JoinClause(activityNoteIdColumn, noteAliasPkColumn, leftJoin)));
                activityTimelineQuery.addSelectClause(List.of(noteAliasTable.getColumn(OMNTNOTE.CONTENT), noteAliasPkColumn,
                        noteActivityIdColumn, activityNoteIdColumn));
            }
            activityTimelineQuery.setWhereClause(new WhereClause(Criteria.in(timelinePKColumn, timelineIds)));
            dataContainer.append(secureOrgDataStore(orgId).get(activityTimelineQuery));
        }

        if (Objects.nonNull(entityFileTable) && (isCategoryEmpty || categories.contains(Category.FILE)) && isAttachmentTimelinePresent(orgId, timelineIds)) {
            final SelectQuery fileTimelineQuery = SelectQuery.get(timelineTable);
            final Table attachmentTimelineTable = getAttachmentTimelineTable();
            final Column attachmentTimelineColumn = attachmentTimelineTable.getColumn(MTLATTACHMENTTIMELINE.TIMELINE_ID);

            fileTimelineQuery.addJoinClause(List.of( new JoinClause(timelinePKColumn, attachmentTimelineColumn, leftJoin)));
            fileTimelineQuery.addSelectClause(attachmentTimelineTable);

            fileTimelineQuery.setWhereClause(new WhereClause(Criteria.in(timelinePKColumn, timelineIds)));
            dataContainer.append(secureOrgDataStore(orgId).get(fileTimelineQuery));
        }

        if (Objects.nonNull(entityTimelineTable) && Objects.nonNull(entityWebFormTable) && isWebFormTimelinePresent(orgId, entityWebFormTable, entityIdKey, entityId)) {
            final SelectQuery webformQuery = SelectQuery.get(entityTimelineTable);
            final Table webFormTable = webFormManager.getTable();
            final Column webFormIdColumn = entityWebFormTable.getColumn(TimelineFields.WEB_FORM_ID.name());

            webformQuery.addJoinClause(List.of(new JoinClause(timelineEntityIdColumn, entityWebFormTable.getColumn(entityIdKey), leftJoin),
                    new JoinClause(webFormIdColumn, webFormTable.getPKColumn(), leftJoin)));
            webformQuery.addSelectClause(List.of(webFormIdColumn, entityWebFormTable.getColumn(entityIdKey),
                    webFormTable.getColumn(OMWFWEBFORM.NAME)));
            webformQuery.setWhereClause(new WhereClause(Criteria.in(entityTimelineTable.getColumn(timelineIdKey), timelineIds)));
            dataContainer.append(secureOrgDataStore(orgId).get(webformQuery));
        }

        return dataContainer;
    }

    /**
     * <p>
     * Checks whether the entity has deal timeline
     * </p>
     *
     * @param orgId       Represents the org id
     * @param timelineIds Represents the collection of timeline ids
     * @return true, if the entity has deal timeline, false otherwise
     */
    private boolean isDealTimelinePresent(final Long orgId, final Collection<Long> timelineIds) {
        final Table dealTimelineTable = Table.get(MTLDEALTIMELINE.TABLE);
        final CountQuery countQuery = CountQuery.get(dealTimelineTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID), timelineIds)));

        return secureOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks whether the entity has note timeline
     * </p>
     *
     * @param orgId       Represents the org id
     * @param timelineIds Represents the collection of timeline ids
     * @return true, if the entity has note timeline, false otherwise
     */
    private boolean isNoteTimelinePresent(final Long orgId, final Collection<Long> timelineIds) {
        final Table notesTimelineTable = getNotesTimelineTable();
        final CountQuery countQuery = CountQuery.get(notesTimelineTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(notesTimelineTable.getColumn(MTLNOTESTIMELINE.TIMELINE_ID), timelineIds)));

        return secureOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks whether the entity has attachment timeline
     * </p>
     *
     * @param orgId       Represents the org id
     * @param timelineIds Represents the collection of timeline ids
     * @return true, if the entity has attachment timeline, false otherwise
     */
    private boolean isAttachmentTimelinePresent(final Long orgId, final Collection<Long> timelineIds) {
        final Table attachmentTimelineTable = getAttachmentTimelineTable();
        final CountQuery countQuery = CountQuery.get(attachmentTimelineTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(attachmentTimelineTable.getColumn(MTLATTACHMENTTIMELINE.TIMELINE_ID), timelineIds)));

        return secureOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Checks whether the entity has activity timeline
     * </p>
     *
     * @param orgId       Represents the org id
     * @param timelineIds Represents the collection of timeline ids
     * @return true, if the entity has activity timeline, false otherwise
     */
    private boolean isActivityTimelinePresent(final Long orgId, final Collection<Long> timelineIds) {
        final Table activityTimelineTable = getActivityTimelineTable();
        final CountQuery countQuery = CountQuery.get(activityTimelineTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID), timelineIds)));

        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the entity has activity note timeline
     * </p>
     *
     * @param orgId       Represents the org id
     * @param timelineIds Represents the collection of timeline ids
     * @return true, if the entity has activity note timeline, false otherwise
     */
    private boolean isActivityNoteTimelinePresent(final Long orgId, final Collection<Long> timelineIds) {
        final Table activityTimelineTable = getActivityTimelineTable();
        final Table activityNoteTable = Table.get(MACNOTEACTIVITY.TABLE);
        final CountQuery countQuery = CountQuery.get(activityTimelineTable);

        countQuery.addJoinClause(new JoinClause(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.ACTIVITY_ID), activityNoteTable.getColumn(MACNOTEACTIVITY.ACTIVITY_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.in(activityTimelineTable.getColumn(MTLACTIVITYTIMELINE.TIMELINE_ID), timelineIds)));

        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the entity has webform timeline
     * </p>
     *
     * @param orgId              Represents the org id
     * @param entityWebformTable Represents the entity webform table
     * @param entityIdKey        Represents the entity id key
     * @param entityId           Represents the entity id
     * @return true, if the entity has webform timeline, false otherwise
     */
    private boolean isWebFormTimelinePresent(final Long orgId, final Table entityWebformTable, final String entityIdKey, final Long entityId) {
        final CountQuery countQuery = CountQuery.get(entityWebformTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(entityWebformTable.getColumn(entityIdKey), entityId)));

        return secureOrgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the email count mapped for given company id.
     * </p>
     *
     * @param orgId               Represents an organization id.
     * @param entityId            Represents the id of the entity.
     * @param entityTimelineTable Represents the entity timeline table
     * @param entityIdKey         Represents entity id column value.
     * @param timelineIdKey       Represents the timeline id column value.
     * @return {@link Long} of contact updates filter count.
     */
    private Long getTimelineCountByCategory(final Long orgId, final Long entityId, final Table entityTimelineTable, final String entityIdKey, final String timelineIdKey, final Category category) {
        final Table timelineTable = commonTimelineManager.getTable();
        final CountQuery countQuery = CountQuery.get(timelineTable);

        countQuery.addJoinClause(new JoinClause(timelineTable.getPKColumn(), entityTimelineTable.getColumn(timelineIdKey)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(entityTimelineTable.getColumn(entityIdKey), entityId))
                .and(new WhereClause(Criteria.eq(timelineTable.getColumn(OMTLTIMELINE.CATEGORY), category))));

        return secureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the email related timeline rows in a {@link Collection} of timeline.
     * </p>
     *
     * @param entityName      Represents the entity name
     * @param timelines       A list which holds the {@link Collection} of timeline entity
     * @param dataContainer   Represents the {@link DataContainer} Which contains the rows of timeline records
     * @param entityTimelines Holds {@link Collection} of entity timelines record
     */
    private void loadEmailTimelines(final String entityName, final Collection<Timeline> timelines,
                                    final DataContainer dataContainer, final Map<Long, EntityTimelines> entityTimelines) {
        final Collection<Row> emailTimelineRows = dataContainer.getRows(getTable()).filter(row ->
                Timeline.isCategoryEquals(Category.EMAILS, row.get(OMTLTIMELINE.CATEGORY))).collect(Collectors.toSet());

        timelines.addAll(emailTimelineRows.stream().map(timelineRow -> {
            final Timeline timeline = convertRowToModel(timelineRow);
            final Long timelineId = timeline.id();
            final EntityTimelines entityTimeline = entityTimelines.get(timelineId);

            if (Objects.nonNull(entityTimeline)) {
                setEmailTimelineRecipients(entityName, timeline, entityTimeline.get(EmailTimeline.TYPE), timelineId, dataContainer);
            }

            timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Loads invoice related timelines into the collection of timelines.
     * </p>
     *
     * @param timelines       Holds the collection of timeline entities.
     * @param dataContainer   Holds the rows of timeline related records.
     * @param entityTimelines Holds invoice timeline entity details keyed by timeline id.
     */
    private void loadInvoiceTimelines(final Collection<Timeline> timelines, final DataContainer dataContainer,
                                      final Map<Long, EntityTimelines> entityTimelines) {
        final Collection<Row> invoiceTimelineRows = dataContainer.getRows(getTable()).filter(row ->
                Timeline.isCategoryEquals(Category.INVOICE, row.get(OMTLTIMELINE.CATEGORY))).collect(Collectors.toSet());
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Column fieldValueTimelineIdColumn = fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID);

        timelines.addAll(invoiceTimelineRows.stream().map(timelineRow -> {
            final Timeline timeline = convertRowToModel(timelineRow);
            final EntityTimelines entityTimeline = entityTimelines.get(timeline.id());

            if (TimelineType.INVOICE_STATUS_UPDATED == timeline.getTimelineType()) {
                final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable,
                        new WhereClause(Criteria.eq(fieldValueTimelineIdColumn, timeline.id())));

                if (Objects.nonNull(fieldValueTimelineRow)) {
                    setFieldTimelineRowToModel(timeline, List.of(fieldValueTimelineRow));
                }
            }

            if (Objects.nonNull(entityTimeline)) {
                timeline.setEntityTimeline(entityTimeline.get(InvoiceTimeline.TYPE));
            }

            timeline.setIcon(iconService.get(timelineRow.get(OMTLTIMELINE.ICON_ID)));

            return timeline;
        }).toList());
    }

    /**
     * <p>
     * Set the recipients for email timeline
     * </p>
     *
     * @param entityName    Represents the entity name
     * @param emailTimeline Represents the {@link EmailTimeline}
     * @param timeline      Represents the {@link Timeline}
     * @param timelineId    Represents the timeline id
     * @param dataContainer Which contains the rows of timeline records
     */
    private void setEmailTimelineRecipients(final String entityName, final Timeline timeline, final EmailTimeline emailTimeline, final Long timelineId,
                                            final DataContainer dataContainer) {
        final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable, new WhereClause(Criteria.eq(fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), timelineId)));

        if (Objects.nonNull(fieldValueTimelineRow)) {
            final JsonObject jsonObject = Json.object((String) fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE));
            final JsonObject toJson = jsonObject.getJsonObject(TimelineFields.TO.getName());

            if (Objects.equals(TimelineFields.OUTBOUND.getName(), jsonObject.get(TimelineFields.TYPE.getName()))) {
                setToName(entityName, toJson, emailTimeline, dataContainer);
            } else {
                final Object fromJson = jsonObject.get(TimelineFields.FROM.getName());
                final String syncedEmailAddress = jsonObject.optString(TimelineFields.SYNCED_ADDRESS.getName(), null);

                if (fromJson instanceof final String fromAddress) {
                    emailTimeline.setFromName(fromAddress);
                } else if (fromJson instanceof final Map<?, ?> contactJson) {
                    emailTimeline.setFromName((String) contactJson.get(TimelineFields.CONTACT_NAME.getName()));
                }
                setToName(entityName, toJson, emailTimeline, dataContainer);

                final Collection<String> toName = emailTimeline.getToName();

                toName.remove(syncedEmailAddress);
            }

            timeline.setEntityTimeline(emailTimeline);
        }
    }

    /**
     * <p>
     * Sets the to name for the {@link EmailTimeline}
     * </p>
     *
     * @param entityName    Represents the entity name
     * @param toJson        Represents the {@link JsonObject} for to address
     * @param emailTimeline Represents the {@link EmailTimeline}
     * @param dataContainer Represents the {@link DataContainer}
     */
    private void setToName(final String entityName, final JsonObject toJson, final EmailTimeline emailTimeline, final DataContainer dataContainer) {
        final Row contactRow = dataContainer.getRow(Table.get(MCOCONTACT.TABLE));
        final LinkedList<String> toName = new LinkedList<>();
        final JsonArray contactsArray = toJson.getJsonArray(TimelineFields.CONTACTS.getName());
        final JsonArray addressArray = toJson.getJsonArray(TimelineFields.EMAIL_ADDRESS.getName());
        final boolean isContactEntity = Objects.equals(entityName.toLowerCase(), TimelineFields.CONTACT.getName());
        final Long contactId = isContactEntity ? contactRow.getPKValue() : null;

        for (int index = 0; index < contactsArray.size(); index++) {
            final JsonObject contactJson = contactsArray.getJsonObject(index);
            final String contactName = contactJson.getString(TimelineFields.CONTACT_NAME.getName());

            if (isContactEntity && Objects.equals(contactId, contactJson.getLong(TimelineFields.CONTACT_ID.getName()))) {
                toName.addFirst(contactName);
            } else {
                toName.add(contactName);
            }
        }

        for (int index = 0; index < addressArray.size(); index++) {
            toName.add(addressArray.getString(index));
        }

        emailTimeline.setToName(toName);
    }

    /**
     * <p>
     * Process the related contacts from the field value rows and adds to the timeline
     * </p>
     *
     * @param newValue    The JSON string containing related contacts details
     * @param newValues   Represents the {@link Collection} of new values
     * @param actionField The key to extract the related Contacts array (RELATED_CONTACTS_ADDED or RELATED_CONTACTS_REMOVED)
     */
    private void processRelatedContacts(final String newValue, final Collection<String> newValues, final String actionField) {
        final JsonArray jsonArray = Json.object(newValue).optJsonArray(actionField);

        if (Objects.nonNull(jsonArray)) {
            final int size = jsonArray.size();

            for (int index = 0; index < size; index++) {
                newValues.add(jsonArray.getString(index));
            }
        }
    }

    /**
     * <p>
     * Process the products from the field value rows and adds them to the timeline
     * </p>
     *
     * @param newValue    Represents the JSON string containing product details
     * @param newValues   Represents the {@link Collection} of new values
     * @param actionField Represents the key to extract the product array
     */
    private void processProducts(final String newValue, final Collection<String> newValues, final String actionField) {
        final JsonArray fieldValueArray = Json.object(newValue).optJsonArray(actionField);

        if (Objects.nonNull(fieldValueArray)) {
            final int size = fieldValueArray.size();

            for (int index = 0; index < size; index++) {
                newValues.add(fieldValueArray.getString(index));
            }
        }
    }

    /**
     * <p>
     * Groups all timeline IDs by their corresponding {@link Category}
     * </p>
     *
     * @param dataContainer The container providing access to timeline table rows
     * @return A {@link Map} of {@link Category} to a set of timeline IDs
     */
    private Map<Category, Set<Long>> groupTimelineIdsByCategory(final DataContainer dataContainer) {
        final Collection<Row> rows = dataContainer.getRows(getTable()).collect(Collectors.toSet());

        return rows.stream().collect(Collectors.groupingBy(row -> Category.get(row.get(OMTLTIMELINE.CATEGORY)),
                Collectors.mapping(Row::getPKValue, Collectors.toUnmodifiableSet())));
    }

    /**
     * <p>
     * Loads and assigns {@link EntityTimeline} objects for timelines of type {@link Category}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param dataContainer   Which contains the rows of timeline related records
     * @param timelines       Which contains the collection of {@link Timeline}
     * @param callTimelineIds Which contains the collection of timeline ids
     */
    private void loadCallTimeline(final long orgId, final DataContainer dataContainer, final Collection<Timeline> timelines,
                                  final Collection<Long> callTimelineIds) {
        conversableTimelineManagerStore.get(Category.CALLS).ifPresent(timelineCategoryManager -> {
            final Map<Long, EntityTimeline> entityTimelines = timelineCategoryManager.get(orgId, callTimelineIds);
            final Table timelineTable = getTable();
            final Stream<Row> timelineRows = dataContainer.getRows(timelineTable, new WhereClause(Criteria.in(timelineTable.getPKColumn(), callTimelineIds)));

            timelines.addAll(timelineRows.map(timelineRow -> {
                final Timeline timeline = convertRowToModel(timelineRow);
                final Long callTimelineId = timeline.id();

                if (entityTimelines.containsKey(callTimelineId)) {
                    timeline.setEntityTimeline(entityTimelines.get(callTimelineId));
                }

                if (Objects.equals(timeline.getTimelineType(), TimelineType.TRANSFERRED_CALL)) {
                    final Table fieldValueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
                    final Row fieldValueTimelineRow = dataContainer.getRow(fieldValueTimelineTable,
                            new WhereClause(Criteria.eq(fieldValueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), timeline.id())));

                    if (Objects.nonNull(fieldValueTimelineRow)) {
                        timeline.setFieldTimeline(new FieldTimeline(fieldValueTimelineRow.get(MTLFIELDVALUETIMELINE.NEW_VALUE)));
                    }
                }

                return timeline;
            }).toList());
        });
    }
}