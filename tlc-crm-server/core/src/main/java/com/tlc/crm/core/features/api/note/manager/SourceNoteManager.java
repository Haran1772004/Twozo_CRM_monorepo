package com.tlc.crm.core.features.api.note.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.audit.EntityNameService;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;
import com.tlc.crm.core.features.api.note.model.RecentNote;
import com.tlc.crm.core.features.timeline.manager.ReadableTimelineManager;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MTLNOTESTIMELINE;
import com.tlc.crm.core.sql.resource.OMNTCOMPANYNOTE;
import com.tlc.crm.core.sql.resource.OMNTCONTACTNOTE;
import com.tlc.crm.core.sql.resource.OMNTDEALNOTE;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.fields.models.StoreType.NOTE;

/**
 * <p>
 * Implements the {@link Note} related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class SourceNoteManager extends AbstractConfigManager<NoteHolder> {

    private static final I18nKey NOTE_KEY = new I18nKey("i18n.note.display.note");
    private static final short TIMELINE_NOTE_MAX_LENGTH = 200;
    private static final short RECENT_NOTE_MAX_LENGTH = 25;
    private static final String ELLIPSIS = "...";

    private final CommonTimelineManager commonTimelineManager;
    private final ReadableTimelineManager readableTimelineManager;
    private final NoteManager noteManager;
    private final AuditLogger auditLogger;
    private final ModificationHandler modificationHandler;

    private static class Instance {
        private static final SourceNoteManager INSTANCE = new SourceNoteManager();
    }

    private SourceNoteManager() {
        commonTimelineManager = CommonTimelineManager.getInstance();
        readableTimelineManager = ReadableTimelineManager.getInstance();
        noteManager = NoteManager.getInstance();
        auditLogger = AuditLogger.getInstance();
        modificationHandler = ModificationHandler.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SourceNoteManager} Instance
     * </p>
     *
     * @return {@link SourceNoteManager}
     */
    public static SourceNoteManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of note
     */
    @Override
    public Table getTable() {
        return noteManager.getTable();
    }

    /**
     * <p>
     * Gets the contact note table
     * </p>
     *
     * @return the {@link Table} of contact note
     */
    public Table getContactNoteTable() {
        return Table.get(OMNTCONTACTNOTE.TABLE);
    }

    /**
     * <p>
     * Gets the deal note table
     * </p>
     *
     * @return the {@link Table} of deal note
     */
    public Table getDealNoteTable() {
        return Table.get(OMNTDEALNOTE.TABLE);
    }

    /**
     * <p>
     * Gets the company note table
     * </p>
     *
     * @return the {@link Table} of company note
     */
    public Table getCompanyNoteTable() {
        return Table.get(OMNTCOMPANYNOTE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Organization Id
     * @param ids   Collection of Ids
     * @return Collection of {@link Note}
     */
    @Override
    public Collection<NoteHolder> get(final Long orgId, final Collection<Long> ids) {
        return noteManager.get(orgId, ids).stream().map(NoteHolder::new).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<NoteHolder> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param noteHolder An entity that holds the note attributes
     * @return {@link Note}
     */
    @Override
    public NoteHolder create(final NoteHolder noteHolder) {
        final DataContainer targetContainer = DataContainer.create();
        final Long orgId = noteHolder.orgId();
        final Long entityId = noteHolder.getEntityId();
        final String entityName = EntityNameService.getInstance().fetchEntityName(orgId, entityId, noteHolder.getSource());

        loadNoteIntoContainer(true, noteHolder, true, null, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        auditLogger.log(orgId, List.of(new AuditLogData(noteHolder.getNote().getCreatedBy().id(), entityId, entityName,
                AuditSource.get(noteHolder.getSource().name()), AuditAction.UPDATED, new AuditAttribute(NOTE_KEY, null, null, AuditAction.ADDED, NOTE))));

        return noteHolder;
    }

    /**
     * <p>
     * Constructs select query for Note
     * </p>
     *
     * @return {@link SelectQuery}
     */
    private SelectQuery buildSelectQueryForNote(final Collection<NoteHolder> noteHolders) {
        final Table noteTable = getTable();
        final Table contactNoteTable = getContactNoteTable();
        final Table dealNoteTable = getDealNoteTable();
        final Table companyNoteTable = getCompanyNoteTable();
        final Column noteId = noteTable.getPKColumn();
        final Column contactNoteColumn = contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID);
        final Column companyNoteColumn = companyNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID);
        final SelectQuery selectQuery = noteManager.buildSelectQuery(null);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Source source = noteHolders.stream().filter(Objects::nonNull).map(NoteHolder::getSource).findFirst().get();

        switch (source) {
            case DEAL -> {
                selectQuery.addJoinClause(List.of(new JoinClause(noteId, dealNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID)),
                        new JoinClause(noteId, contactNoteColumn), new JoinClause(noteId, companyNoteColumn, leftJoin)));
                selectQuery.addSelectClause(dealNoteTable, contactNoteTable, companyNoteTable);
            }
            case CONTACT -> {
                selectQuery.addJoinClause(List.of(new JoinClause(noteId, contactNoteColumn),
                        new JoinClause(noteId, companyNoteColumn, leftJoin)));
                selectQuery.addSelectClause(contactNoteTable, companyNoteTable);
            }
            case COMPANY -> {
                selectQuery.addJoinClause(new JoinClause(noteId, companyNoteColumn));
                selectQuery.addSelectClause(companyNoteTable);
            }
        }

        return selectQuery;
    }

    /**
     * <p>
     * Loads the note details into the target container
     * </p>
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create or update event
     * @param noteHolder         An entity that holds the note attributes
     * @param isTimelineRequired Represents the timeline is required or not
     * @param sourceContainer    Holds the user defined and system field values of existing note
     * @param targetContainer    Holds the updated information of field values provided by user
     */
    public void loadNoteIntoContainer(final boolean isCreateEvent, final NoteHolder noteHolder, final boolean isTimelineRequired,
                                      final DataContainer sourceContainer, final DataContainer targetContainer) {

        noteManager.loadRowIntoContainer(isCreateEvent, noteHolder.getNote(), sourceContainer, targetContainer);

        if (isTimelineRequired) {
            loadNotesTimeline(isCreateEvent, noteHolder, buildNotesTimeline(isCreateEvent, noteHolder.getNote()), targetContainer, sourceContainer);
        }

        if (isCreateEvent) {

            switch (noteHolder.getSource()) {
                case DEAL -> loadDealNoteRowIntoContainer(noteHolder, targetContainer);
                case COMPANY -> loadCompanyNoteRowIntoContainer(noteHolder, targetContainer);
                case CONTACT -> loadContactNoteRowIntoContainer(noteHolder, targetContainer);
            }
        }
    }

    /**
     * Gets the most recent note of an entity to update.
     *
     * @param orgId  Refers the organization id.
     * @param noteId Refers the note id.
     * @return The collection of {@link RecentNote}.
     */
    private Collection<RecentNote> getRecentNotes(final long orgId, final long noteId) {
        final Collection<RecentNote> recentNotes = new ArrayList<>();
        final BiConsumer<Long, Source> recentNoteCollector = (entityId, source) -> {
            final Table noteTable = getTable();
            final SelectQuery selectQuery = SelectQuery.get(noteTable);
            final WhereClause whereClause = new WhereClause(Criteria.notEq(noteTable.getPKColumn(), noteId));

            switch (source) {
                case DEAL -> {
                    final Table dealNoteTable = getDealNoteTable();

                    selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, dealNoteTable));
                    selectQuery.setWhereClause(whereClause.and(Criteria.eq(dealNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID), entityId)));
                }
                case CONTACT -> {
                    final Table contactNoteTable = getContactNoteTable();

                    selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, contactNoteTable));
                    selectQuery.setWhereClause(whereClause.and(Criteria.eq(contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID), entityId)));
                }
                case COMPANY -> {
                    final Table companyTable = getCompanyNoteTable();

                    selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, companyTable));
                    selectQuery.setWhereClause(whereClause.and(Criteria.eq(companyTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID), entityId)));
                }
            }
            selectQuery.addOrderByClause(new OrderByClause(noteTable.getColumn(OMNTNOTE.UPDATED_TIME), OrderByClause.OrderType.DESCENDING));
            selectQuery.setLimitClause(new LimitClause(1));

            final Row row = orgDataStore(orgId).get(selectQuery).getRow(noteTable);

            if (Objects.isNull(row)) {
                recentNotes.add(new RecentNote(orgId, entityId, null, source));
            } else {
                final NoteHolder noteHolder = this.convertRowToModel(row, null);

                recentNotes.add(new RecentNote(orgId, entityId, noteHolder.getNote().getTrimmedContent(RECENT_NOTE_MAX_LENGTH), source));
            }
        };

        final Row companyNoteRow = getCompanyNoteRow(orgId, noteId);
        final Row contactNoteRow = getContactNoteRow(orgId, noteId);
        final Row dealNoteTow = getDealNoteRow(orgId, noteId);

        if (Objects.nonNull(companyNoteRow)) {
            recentNoteCollector.accept(companyNoteRow.get(OMNTCOMPANYNOTE.COMPANY_ID), Source.COMPANY);
        }

        if (Objects.nonNull(contactNoteRow)) {
            recentNoteCollector.accept(contactNoteRow.get(OMNTCONTACTNOTE.CONTACT_ID), Source.CONTACT);
        }

        if (Objects.nonNull(dealNoteTow)) {
            recentNoteCollector.accept(dealNoteTow.get(OMNTDEALNOTE.DEAL_ID), Source.DEAL);
        }

        return recentNotes;
    }

    /**
     * Updates the recent note of an entity for the given note.
     *
     * @param note Refers the {@link Note}.
     */
    public final void updateRecentNote(final Note note) {
        final Long orgId = note.orgId();
        final Long noteId = note.id();
        final String trimmedContent = note.getTrimmedContent(RECENT_NOTE_MAX_LENGTH);
        final String recentNote = note.getContentBody().length() > RECENT_NOTE_MAX_LENGTH ? StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, trimmedContent, ELLIPSIS) : trimmedContent;
        final Row companyNoteRow = getCompanyNoteRow(orgId, noteId);
        final Row contactNoteRow = getContactNoteRow(orgId, noteId);
        final Row dealNoteRow = getDealNoteRow(orgId, noteId);
        final Collection<RecentNote> recentNotes = new HashSet<>();

        if (Objects.nonNull(companyNoteRow)) {
            recentNotes.add(new RecentNote(orgId, companyNoteRow.get(OMNTCOMPANYNOTE.COMPANY_ID), recentNote, Source.COMPANY));
        }

        if (Objects.nonNull(contactNoteRow)) {
            recentNotes.add(new RecentNote(orgId, contactNoteRow.get(OMNTCONTACTNOTE.CONTACT_ID), recentNote, Source.CONTACT));
        }

        if (Objects.nonNull(dealNoteRow)) {
            recentNotes.add(new RecentNote(orgId, dealNoteRow.get(OMNTDEALNOTE.DEAL_ID), recentNote, Source.DEAL));
        }

        updateEntityRecentNote(orgId, recentNotes);
    }

    /**
     * Returns the {@link Row} corresponding to the mapped note ID in the company notes.
     *
     * @param orgId  Refers the organization id.
     * @param noteId Refers the note id.
     * @return The note row which is related with the company.
     */
    private Row getCompanyNoteRow(final long orgId, final long noteId) {
        final Table companyNoteTable = getCompanyNoteTable();

        return orgDataStore(orgId).get(companyNoteTable, new WhereClause(Criteria.eq(companyNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID), noteId)),
                List.of(companyNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID))).getRow(companyNoteTable);
    }

    /**
     * Returns the {@link Row} corresponding to the mapped note ID in the contact notes.
     *
     * @param orgId  Refers the organization id.
     * @param noteId Refers the note id.
     * @return The note row which is related with the contact.
     */
    private Row getContactNoteRow(final long orgId, final long noteId) {
        final Table contactNoteTable = getContactNoteTable();

        return orgDataStore(orgId).get(contactNoteTable, new WhereClause(Criteria.eq(contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID), noteId)),
                List.of(contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID))).getRow(contactNoteTable);
    }

    /**
     * Returns the {@link Row} corresponding to the mapped note ID in the deal notes.
     *
     * @param orgId  Refers the organization id.
     * @param noteId Refers the note id.
     * @return The note row which is related with the deal.
     */
    private Row getDealNoteRow(final long orgId, final long noteId) {
        final Table dealNoteTable = getDealNoteTable();

        return orgDataStore(orgId).get(dealNoteTable, new WhereClause(Criteria.eq(dealNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID), noteId)),
                List.of(dealNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID))).getRow(dealNoteTable);
    }

    /**
     * Updates the collection of recent notes details of an entity.
     *
     * @param orgId       Refers the organization id.
     * @param recentNotes Refers the collection of {@link RecentNote} details.
     */
    public final void updateEntityRecentNote(final long orgId, final Collection<RecentNote> recentNotes) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final RecentNote recentNote : recentNotes) {
            final Table table = switch (recentNote.source()) {
                case COMPANY -> Table.get(MCOCOMPANY.TABLE);
                case CONTACT -> Table.get(MCOCONTACT.TABLE);
                case DEAL -> Table.get(MDODEAL.TABLE);
            };
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(MCOCOMPANY.RECENT_NOTE), recentNote.content());
            updateQuery.addUpdateColumn(table.getColumn(MCOCOMPANY.MODIFIED_TIME), Instant.now().toEpochMilli());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), recentNote.entityId())));

            updateQueries.add(updateQuery);
        }

        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * <p>
     * Loads the deal note row into container
     * </p>
     *
     * @param noteHolder      An entity that holds the note attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDealNoteRowIntoContainer(final NoteHolder noteHolder, final DataContainer targetContainer) {
        final Long entityId = noteHolder.getEntityId();
        final Row dealNoteRow = new Row(getDealNoteTable());

        dealNoteRow.set(OMNTDEALNOTE.DEAL_ID, entityId);
        dealNoteRow.set(OMNTDEALNOTE.NOTE_ID, noteHolder.id());
        targetContainer.addNewRow(dealNoteRow);
        final Map<Source, Long> relatedIds = noteHolder.getRelatedSourceIds();
        final boolean isRelatedSourceIdsEmpty = Objects.isNull(relatedIds) || relatedIds.isEmpty();

        if (!isRelatedSourceIdsEmpty && relatedIds.containsKey(Source.CONTACT)) {
            loadContactNoteRowIntoContainer(noteHolder, targetContainer);
        } else if (!isRelatedSourceIdsEmpty && relatedIds.containsKey(Source.COMPANY)) {
            loadCompanyNoteRowIntoContainer(noteHolder, targetContainer);
        }
        modificationHandler.loadModificationDetailsIntoContainer(noteHolder.orgId(), noteHolder.getNote().getCreatedBy().id(), List.of(entityId),
                com.tlc.crm.entity.fields.source.Source.DEAL, targetContainer);
    }

    /**
     * <p>
     * Loads the contact note row into container
     * </p>
     *
     * @param noteHolder      An entity that holds the note attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadContactNoteRowIntoContainer(final NoteHolder noteHolder, final DataContainer targetContainer) {
        final Map<Source, Long> relatedIds = noteHolder.getRelatedSourceIds();
        final Long entityId = noteHolder.getEntityId();
        final Long sourceContactId = Objects.isNull(relatedIds) ? null : relatedIds.get(Source.CONTACT);
        final Long contactId = Objects.isNull(sourceContactId) ? entityId : sourceContactId;
        final Row contactNoteRow = new Row(getContactNoteTable());

        contactNoteRow.set(OMNTCONTACTNOTE.NOTE_ID, noteHolder.id());
        contactNoteRow.set(OMNTCONTACTNOTE.CONTACT_ID, contactId);
        targetContainer.addNewRow(contactNoteRow);

        if (Objects.nonNull(relatedIds) && relatedIds.containsKey(Source.COMPANY)) {
            loadCompanyNoteRowIntoContainer(noteHolder, targetContainer);
        }
        modificationHandler.loadModificationDetailsIntoContainer(noteHolder.orgId(), noteHolder.getNote().getCreatedBy().id(),
                List.of(contactId), com.tlc.crm.entity.fields.source.Source.CONTACT, targetContainer);
    }

    /**
     * <p>
     * Loads the company note row into container
     * </p>
     *
     * @param noteHolder      An entity that holds the note attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadCompanyNoteRowIntoContainer(final NoteHolder noteHolder, final DataContainer targetContainer) {
        final Map<Source, Long> relatedIds = noteHolder.getRelatedSourceIds();
        final Long companyId = Objects.nonNull(relatedIds) ? relatedIds.get(Source.COMPANY) : noteHolder.getEntityId();

        if (Objects.nonNull(companyId)) {
            final Row companyNoteRow = new Row(getCompanyNoteTable());

            companyNoteRow.set(OMNTCOMPANYNOTE.COMPANY_ID, companyId);
            companyNoteRow.set(OMNTCOMPANYNOTE.NOTE_ID, noteHolder.id());
            modificationHandler.loadModificationDetailsIntoContainer(noteHolder.orgId(), noteHolder.getNote().getCreatedBy().id(),
                    List.of(companyId), com.tlc.crm.entity.fields.source.Source.COMPANY, targetContainer);
            targetContainer.addNewRow(companyNoteRow);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Organization id
     * @param noteHolders Collection of notes that have to be updated
     * @return Updated status
     */
    public ProcessOutcome update(final Long orgId, final Collection<NoteHolder> noteHolders) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Collection<Long> ids = noteHolders.stream().map(NoteHolder::getNote).map(Note::id).filter(Objects::nonNull).toList();
        final SelectQuery selectQuery = buildSelectQueryForNote(noteHolders);
        final Column notePkColumn = getTable().getPKColumn();

        selectQuery.setWhereClause(new WhereClause(Criteria.in(notePkColumn, ids)));
        final DataContainer entityContainer = orgDataStore.get(selectQuery);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.UPDATED;
        final AuditAttribute auditAttribute = new AuditAttribute(NOTE_KEY, null, null, AuditAction.UPDATED, NOTE);

        for (final NoteHolder noteHolder : noteHolders) {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadNoteIntoContainer(false, noteHolder, true, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                final Long entityId = noteHolder.getEntityId();
                final String entityName = EntityNameService.getInstance().fetchEntityName(orgId, entityId, noteHolder.getSource());

                processOutcome.addProcessedRecord(noteHolder.id());
                auditLogDataList.add(new AuditLogData(noteHolder.getNote().getCreatedBy().id(), entityId, entityName, AuditSource.get(noteHolder.getSource().name()), auditAction, auditAttribute));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Note update failed Org ID : {} ID : {} reason : {}", orgId, noteHolder.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, noteHolder.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts row into {@link Note}
     * </p>
     *
     * @param row           Note row to be converted
     * @param dataContainer
     * @return {@link Note}
     */
    private NoteHolder convertRowToModel(final Row row, final DataContainer dataContainer) {
        return new NoteHolder(noteManager.convertRowToModel(row, dataContainer));
    }

    /**
     * <p>
     * Retrieves the number of note rows for given contact id
     * </p>
     *
     * @param orgId     Organization Id
     * @param contactId Contact Id
     * @return the {@link Long} of contact note count
     */
    public Long getContactNotesCount(final Long orgId, final Long contactId) {
        final Table contactNoteTable = getContactNoteTable();
        final CountQuery countQuery = CountQuery.get(contactNoteTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(getContactNoteTable().getColumn(OMNTCONTACTNOTE.CONTACT_ID),
                contactId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of note rows for given contact id
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param contactId            Represents the contact id
     * @param notesTimelineTable   Represents the notes timeline table
     * @param contactTimelineTable Represents the contact timeline table
     * @return the {@link Long} of contact note count
     */
    public Long getContactNotesCount(final Long orgId, final Long contactId, final Table notesTimelineTable, final Table contactTimelineTable) {
        final Table contactNoteTable = getContactNoteTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTimelineTable);
        final SelectQuery innerQuery = SelectQuery.get(contactNoteTable);
        final Column contactIdColumn = contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID);

        innerQuery.addJoinClause(new JoinClause(contactIdColumn, contactTimelineTable.getColumn(MTLCONTACTTIMELINE.CONTACT_ID)));
        innerQuery.addSelectClause(contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID));
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(contactIdColumn, contactId)));

        selectQuery.addJoinClause(new JoinClause(contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID), notesTimelineTable.getColumn(MTLNOTESTIMELINE.TIMELINE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(notesTimelineTable.getColumn(MTLNOTESTIMELINE.NOTES_ID), innerQuery)));

        return (long) orgDataStore(orgId).get(selectQuery).size(contactTimelineTable);
    }

    /**
     * <p>
     * Retrieves the number of note rows for given deal id
     * </p>
     *
     * @param orgId  Organization Id
     * @param dealId Deal Id
     * @return {@link Long} of deal note count
     */
    public Long getDealNotesCount(final Long orgId, final Long dealId) {
        final Table dealNoteTable = getDealNoteTable();
        final CountQuery countQuery = CountQuery.get(dealNoteTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(getDealNoteTable().getColumn(OMNTDEALNOTE.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the number of note rows for given company id
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param companyId Represents the company id
     * @return {@link Long} of company note count
     */
    public Long getCompanyNotesCount(final Long orgId, final Long companyId) {
        final Table companyNoteTable = getCompanyNoteTable();
        final CountQuery countQuery = CountQuery.get(companyNoteTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(companyNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID),
                companyId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the source notes for given entity id
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param sourceName Represents the source
     * @param entityId   Represents the entity id
     * @param start      Start value represents the lower bound of notes to be fetched
     * @param limit      Limit value represents the upper bound of notes to be fetched
     * @return the {@link Collection} of notes
     */
    public Collection<NoteHolder> getSourceNotes(final Long orgId, final Source sourceName, final Long entityId,
                                                 final int start, final int limit) {
        final Table noteTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);

        switch (sourceName) {
            case DEAL -> {
                final Table dealNoteTable = getDealNoteTable();

                selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, dealNoteTable));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(getDealNoteTable().getColumn(OMNTDEALNOTE.DEAL_ID),
                        entityId)));
            }
            case CONTACT -> {
                final Table contactNoteTable = getContactNoteTable();

                selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, contactNoteTable));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(getContactNoteTable().getColumn(OMNTCONTACTNOTE.CONTACT_ID),
                        entityId)));
            }
            case COMPANY -> {
                final Table companyTable = getCompanyNoteTable();

                selectQuery.addJoinClause(DmlUtil.getJoins(noteTable, companyTable));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(companyTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID),
                        entityId)));
            }
        }
        selectQuery.addOrderByClause(new OrderByClause(noteTable.getColumn(OMNTNOTE.IS_PINNED), OrderByClause.OrderType.DESCENDING));
        selectQuery.addOrderByClause(new OrderByClause(noteTable.getColumn(OMNTNOTE.CREATED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> noteRows = dataContainer.getRows(noteTable);

        return noteRows.map(noteRow -> convertRowToModel(noteRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Checks whether the given note IDs are exists in the deal
     * </p>
     *
     * @param orgId   Represents the organisation id
     * @param noteIds Represents the collection of note ids
     * @param dealId  Represents the deal entity id
     * @return true if all the deal note are exists
     */
    public boolean isDealNoteIdsExists(final Long orgId, final Collection<Long> noteIds, final Long dealId) {
        final Table dealNoteTable = getDealNoteTable();
        final CountQuery dealsCountQuery = CountQuery.get(dealNoteTable);

        dealsCountQuery.setWhereClause(new WhereClause(Criteria.in(dealNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID), noteIds))
                .and(Criteria.eq(dealNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(dealsCountQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the given note IDs are exists in the contact
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param noteIds   Represents the collection of note ids
     * @param contactId Represents the contact entity id
     * @return true if all the contact note are exists
     */
    public boolean isContactNoteIdsExists(final Long orgId, final Collection<Long> noteIds, final Long contactId) {
        final Table contactNoteTable = getContactNoteTable();
        final CountQuery contactCountQuery = CountQuery.get(contactNoteTable);

        contactCountQuery.setWhereClause(new WhereClause(Criteria.in(contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID),
                noteIds)).and(Criteria.eq(contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID), contactId)));

        return orgDataStore(orgId).get(contactCountQuery) > 0;
    }

    /**
     * <p>
     * Checks whether the given note IDs are exists in the company
     * </p>
     *
     * @param orgId     Represents the Organisation id
     * @param noteIds   Represents the collection of note ids
     * @param companyId Represent the company entity id
     * @return true if all the company note are exists
     */
    public boolean isCompanyNoteIdsExists(final Long orgId, final Collection<Long> noteIds, final Long companyId) {
        final Table companyNoteTable = getCompanyNoteTable();
        final CountQuery companyCountQuery = CountQuery.get(companyNoteTable);

        companyCountQuery.setWhereClause(new WhereClause(Criteria.in(companyNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID),
                noteIds)).and(Criteria.eq(companyNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID), companyId)));

        return orgDataStore(orgId).get(companyCountQuery) > 0;
    }

    /**
     * <p>
     * Builds the timeline when any action performed related to notes.
     * </p>
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event.
     * @param note          An entity that holds the note attributes
     * @return {@link Timeline}
     */
    private Timeline buildNotesTimeline(final boolean isCreateEvent, final Note note) {
        final Timeline timeline = new Timeline();

        if (isCreateEvent) {
            timeline.setAction(TimelineActionFields.ADDED);
            timeline.setTimelineType(TimelineType.NOTES_ADDED);
        } else {
            timeline.setAction(TimelineActionFields.EDITED);
            timeline.setTimelineType(TimelineType.NOTES_EDITED);
        }
        timeline.setFieldTimeline(new FieldTimeline(Collections.emptyList(), note.getTrimmedContent(TIMELINE_NOTE_MAX_LENGTH), null));
        timeline.setAuditedTime(note.getUpdatedTime());
        timeline.setOrgId(note.orgId());
        timeline.setOwnerId(note.getCreatedBy().id());
        timeline.setCategory(Timeline.Category.NOTES);

        return timeline;
    }

    /**
     * <p>
     * Loads the notes related timeline
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param noteHolder      An entity that holds the note attributes
     * @param timeline        An entity that holds the timeline attributes
     * @param targetContainer Holds the updated information of field values provided by user
     * @param sourceContainer Holds the field values of existing notes
     */
    private void loadNotesTimeline(final boolean isCreateEvent, final NoteHolder noteHolder, final Timeline timeline,
                                   final DataContainer targetContainer, final DataContainer sourceContainer) {
        final Row notesTimelineRow = new Row(readableTimelineManager.getNotesTimelineTable());

        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        final Long timelineId = timeline.id();

        notesTimelineRow.set(MTLNOTESTIMELINE.NOTES_ID, noteHolder.id());
        notesTimelineRow.set(MTLNOTESTIMELINE.TIMELINE_ID, timelineId);
        targetContainer.addNewRow(notesTimelineRow);
        final Map<Source, Long> sourceEntityIdMap = new HashMap<>();

        if (isCreateEvent) {
            final Map<Source, Long> relatedSourceIds = noteHolder.getRelatedSourceIds();
            sourceEntityIdMap.put(noteHolder.getSource(), noteHolder.getEntityId());

            if (Objects.nonNull(relatedSourceIds)) {
                sourceEntityIdMap.putAll(relatedSourceIds);
            }
        } else {
            final Row companyNoteRow = sourceContainer.size(getCompanyNoteTable()) > 0 ? sourceContainer.getRow(getCompanyNoteTable()) : null;
            final Row contactNoteRow = sourceContainer.size(getContactNoteTable()) > 0 ? sourceContainer.getRow(getContactNoteTable()) : null;
            final Row dealNoteRow = sourceContainer.size(getDealNoteTable()) > 0 ? sourceContainer.getRow(getDealNoteTable()) : null;

            if (Objects.nonNull(companyNoteRow)) {
                sourceEntityIdMap.put(Source.COMPANY, companyNoteRow.get(OMNTCOMPANYNOTE.COMPANY_ID));
            }

            if (Objects.nonNull(contactNoteRow)) {
                sourceEntityIdMap.put(Source.CONTACT, contactNoteRow.get(OMNTCONTACTNOTE.CONTACT_ID));
            }

            if (Objects.nonNull(dealNoteRow)) {
                sourceEntityIdMap.put(Source.DEAL, dealNoteRow.get(OMNTDEALNOTE.DEAL_ID));
            }
        }

        for (final Map.Entry<Source, Long> sourceIdEntry : sourceEntityIdMap.entrySet()) {
            final Long entityId = sourceIdEntry.getValue();

            switch (sourceIdEntry.getKey()) {
                case DEAL -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId,
                        MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                        new Row(readableTimelineManager.getDealTimelineTable()), targetContainer);

                case CONTACT -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId,
                        MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                        new Row(readableTimelineManager.getContactTimelineTable()), targetContainer);

                case COMPANY -> commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, entityId,
                        MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                        new Row(readableTimelineManager.getCompanyTimelineTable()), targetContainer);
            }
        }
    }

    /**
     * <p>
     * Delete the row for given notes
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param noteHolders Represents the collection of notes
     */
    @Override
    public void delete(final Long orgId, final Collection<NoteHolder> noteHolders) {
        final Table timelineTable = commonTimelineManager.getTable();
        final Table noteTimelineTable = readableTimelineManager.getNotesTimelineTable();
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final Collection<Long> noteIds = new HashSet<>();
        final SelectQuery selectQuery = SelectQuery.get(timelineTable);
        final Collection<RecentNote> recentNotes = new HashSet<>();
        final AuditAttribute auditAttribute = new AuditAttribute(NOTE_KEY, null, null, AuditAction.DELETED, NOTE);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditAction auditAction = AuditAction.DELETED;
        final Map<com.tlc.crm.entity.fields.source.Source, Collection<Long>> entityIdMap = getEntityIds(orgId, noteHolders.stream().map(NoteHolder::id).toList());

        for (final NoteHolder noteHolder : noteHolders) {
            final Long id = noteHolder.id();
            final Pair<AuditCategory, Pair<Long, String>> entityDetails = getEntityDetails(orgId, id);

            if (Objects.nonNull(entityDetails)) {
                final Pair<Long, String> entityDetailsPair = entityDetails.getValue();

                noteIds.add(id);
                auditLogDataList.add(new AuditLogData(noteHolder.getNote().getCreatedBy().id(), entityDetailsPair.getKey(), entityDetailsPair.getValue(),
                        AuditSource.get(entityDetails.getKey().getName()), auditAction, auditAttribute));
            }
            recentNotes.addAll(getRecentNotes(orgId, id));
        }

        selectQuery.addJoinClause(new JoinClause(timelinePKColumn, noteTimelineTable.getColumn(MTLNOTESTIMELINE.TIMELINE_ID), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(timelinePKColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(noteTimelineTable.getColumn(MTLNOTESTIMELINE.NOTES_ID), noteIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Table noteTable = getTable();
            final DataContainer targetContainer = DataContainer.create();
            final Stream<Row> timelineRows = dataContainer.getRows(timelineTable);

            for (final Long noteId : noteIds) {
                targetContainer.deleteRow(new Row(noteTable, noteId));
            }
            timelineRows.forEach(targetContainer::deleteRow);

            if (!entityIdMap.isEmpty()) {
                final Long userId = noteHolders.stream().map(NoteHolder::getNote).map(note -> note.getCreatedBy().id()).findFirst().get();

                entityIdMap.forEach(((source, entityIds) ->
                        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, entityIds, source, targetContainer)));
            }
            orgDataStore(orgId).commitChanges(targetContainer);
            updateEntityRecentNote(orgId, recentNotes);

            if (!auditLogDataList.isEmpty()) {
                auditLogger.log(orgId, auditLogDataList);
            }
        }
    }

    /**
     * <p>
     * Gets the source and entity ids based on the note ids
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param noteHolderIds Which contains the collection of {@link NoteHolder}
     * @return The source as a key and collection of entity ids as a value
     */
    private Map<com.tlc.crm.entity.fields.source.Source, Collection<Long>> getEntityIds(final long orgId, final Collection<Long> noteHolderIds) {
        final Map<com.tlc.crm.entity.fields.source.Source, Collection<Long>> entityIds = new HashMap<>();
        final Collection<Long> contactIds = getEntityIds(orgId, noteHolderIds, getContactNoteTable(), OMNTCONTACTNOTE.CONTACT_ID,
                OMNTCONTACTNOTE.NOTE_ID);
        final Collection<Long> companyIds = getEntityIds(orgId, noteHolderIds, getCompanyNoteTable(), OMNTCOMPANYNOTE.COMPANY_ID,
                OMNTCOMPANYNOTE.NOTE_ID);
        final Collection<Long> dealIds = getEntityIds(orgId, noteHolderIds, getDealNoteTable(), OMNTDEALNOTE.DEAL_ID,
                OMNTDEALNOTE.NOTE_ID);

        if (!contactIds.isEmpty()) {
            entityIds.put(com.tlc.crm.entity.fields.source.Source.CONTACT, contactIds);
        }

        if (!companyIds.isEmpty()) {
            entityIds.put(com.tlc.crm.entity.fields.source.Source.COMPANY, companyIds);
        }

        if (!dealIds.isEmpty()) {
            entityIds.put(com.tlc.crm.entity.fields.source.Source.DEAL, dealIds);
        }

        return entityIds;
    }

    /**
     * <p>
     * Gets the entity ids based on the note ids
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param noteHolderIds      Which contains the collection of {@link NoteHolder}
     * @param table              Represents the source table
     * @param entityIdColumnName Represents the entity id column name
     * @param noteIdColumnName   Represents the note id column name
     * @return The collection of entity ids
     */
    private Collection<Long> getEntityIds(final long orgId, final Collection<Long> noteHolderIds, final Table table,
                                          final String entityIdColumnName, final String noteIdColumnName) {
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(entityIdColumnName));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(noteIdColumnName), noteHolderIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(row -> (Long) row.get(entityIdColumnName)).toList();
    }

    /**
     * <p>
     * Fetches the entity details of the given note id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param noteId Represents the note id.
     * @return Entity details.
     */
    public Pair<AuditCategory, Pair<Long, String>> getEntityDetails(final Long orgId, final Long noteId) {
        final Table noteTable = getTable();
        final Table contactTable = ContactManager.getInstance().getTable();
        final Table companyTable = CompanyManager.getInstance().getTable();
        final Table dealTable = DealManager.getInstance().getTable();
        final Table companyNoteTable = getCompanyNoteTable();
        final Table contactNoteTable = getContactNoteTable();
        final Table dealNoteTable = getDealNoteTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);
        final Column notePkColumn = noteTable.getPKColumn();
        final Column companyIdColumn = companyNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID);
        final Column contactIdColumn = contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID);
        final Column dealIdColumn = dealNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(notePkColumn, noteId)));
        selectQuery.addSelectClause(List.of(companyIdColumn, contactIdColumn, dealIdColumn,
                companyTable.getColumn(MCOCOMPANY.NAME), contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), dealTable.getColumn(MDODEAL.TITLE)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(notePkColumn, companyNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID), leftJoin),
                new JoinClause(companyIdColumn, companyTable.getPKColumn(), leftJoin),
                new JoinClause(notePkColumn, contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID), leftJoin),
                new JoinClause(contactIdColumn, contactTable.getPKColumn(), leftJoin),
                new JoinClause(notePkColumn, dealNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID), leftJoin),
                new JoinClause(dealIdColumn, dealTable.getPKColumn(), leftJoin)
        ));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row contactNoteRow = dataContainer.getRow(contactNoteTable);
        final Row companyNoteRow = dataContainer.getRow(companyNoteTable);
        final Row dealNoteRow = dataContainer.getRow(dealNoteTable);

        if (Objects.isNull(dealNoteRow)) {
            if (Objects.isNull(contactNoteRow)) {
                if (Objects.isNull(companyNoteRow)) {
                    return null;
                }
                final Long companyId = companyNoteRow.get(OMNTCOMPANYNOTE.COMPANY_ID);
                final Row companyRow = dataContainer.getRow(companyTable, companyId);

                return Pair.of(AuditCategory.COMPANY, Pair.of(companyId, companyRow.get(MCOCOMPANY.NAME)));
            }
            final Long contactId = contactNoteRow.get(OMNTCONTACTNOTE.CONTACT_ID);
            final Row contactRow = dataContainer.getRow(contactTable, contactId);

            return Pair.of(AuditCategory.CONTACT, Pair.of(contactId,
                    String.format("%s %s", contactRow.get(MCOCONTACT.FIRST_NAME), contactRow.get(MCOCONTACT.LAST_NAME))));
        }
        final Long dealId = dealNoteRow.get(OMNTDEALNOTE.DEAL_ID);
        final Row dealRow = dataContainer.getRow(dealTable, dealId);

        return Pair.of(AuditCategory.DEAL, Pair.of(dealId, dealRow.get(MDODEAL.TITLE)));
    }

    /**
     * <p>
     * Fetches the entity details of the given note ids.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param noteIds Represents the collection of note ids.
     * @return Entity details.
     */
    public Map<Long, Map<Source, Entity>> getEntities(final Long orgId, final Collection<Long> noteIds) {
        final Table contactTable = ContactManager.getInstance().getTable();
        final Table companyTable = CompanyManager.getInstance().getTable();
        final Table dealTable = DealManager.getInstance().getTable();
        final Table companyNoteTable = getCompanyNoteTable();
        final Table contactNoteTable = getContactNoteTable();
        final Table dealNoteTable = getDealNoteTable();
        final Map<Long, Map<Source, Entity>> entities = new HashMap<>(noteIds.size());
        final SelectQuery contactNoteSelectQuery = SelectQuery.get(contactNoteTable);
        final SelectQuery dealNoteSelectQuery = SelectQuery.get(dealNoteTable);
        final SelectQuery companyNoteSelectQuery = SelectQuery.get(companyNoteTable);

        contactNoteSelectQuery.addSelectClause(contactNoteTable);
        contactNoteSelectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        contactNoteSelectQuery.addJoinClause(new JoinClause(contactNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID), contactTable.getPKColumn()));
        contactNoteSelectQuery.setWhereClause(new WhereClause(Criteria.in(contactNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID), noteIds)));

        companyNoteSelectQuery.addSelectClause(companyNoteTable);
        companyNoteSelectQuery.addSelectClause(companyTable.getColumn(MCOCOMPANY.NAME));
        companyNoteSelectQuery.addJoinClause(new JoinClause(companyNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID), companyTable.getPKColumn()));
        companyNoteSelectQuery.setWhereClause(new WhereClause(Criteria.in(companyNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID), noteIds)));

        dealNoteSelectQuery.addSelectClause(dealNoteTable);
        dealNoteSelectQuery.addSelectClause(dealTable.getColumn(MDODEAL.TITLE));
        dealNoteSelectQuery.addJoinClause(new JoinClause(dealNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID), dealTable.getPKColumn()));
        dealNoteSelectQuery.setWhereClause(new WhereClause(Criteria.in(dealNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID), noteIds)));

        final DataContainer dealNoteContainer = orgDataStore(orgId).get(dealNoteSelectQuery);
        final DataContainer contactNoteContainer = orgDataStore(orgId).get(contactNoteSelectQuery);
        final DataContainer companyNoteContainer = orgDataStore(orgId).get(companyNoteSelectQuery);

        contactNoteContainer.getRows(contactNoteTable).forEach(row -> {
            final Row contactRow = contactNoteContainer.getRow(contactTable, (Long) row.get(OMNTCONTACTNOTE.CONTACT_ID));

            if (Objects.nonNull(contactRow)) {
                final Contact contact = new Contact();

                contact.setId(contactRow.getPKValue());
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

                entities.computeIfAbsent(row.get(OMNTCONTACTNOTE.NOTE_ID), noteId -> new HashMap<>()).put(Source.CONTACT, contact);
            }
        });

        companyNoteContainer.getRows(companyNoteTable).forEach(row -> {
            final Row companyRow = companyNoteContainer.getRow(companyTable, (Long) row.get(OMNTCOMPANYNOTE.COMPANY_ID));

            if (Objects.nonNull(companyRow)) {
                final Company company = new Company();

                company.setId(companyRow.getPKValue());
                company.setName(companyRow.get(MCOCOMPANY.NAME));

                entities.computeIfAbsent(row.get(OMNTCOMPANYNOTE.NOTE_ID), noteId -> new HashMap<>()).put(Source.COMPANY, company);
            }
        });

        dealNoteContainer.getRows(dealNoteTable).forEach(row -> {
            final Row dealRow = dealNoteContainer.getRow(dealTable, (Long) row.get(OMNTDEALNOTE.DEAL_ID));

            if (Objects.nonNull(dealRow)) {
                final Deal deal = new Deal();

                deal.setId(dealRow.getPKValue());
                deal.setTitle(dealRow.get(MDODEAL.TITLE));

                entities.computeIfAbsent(row.get(OMNTDEALNOTE.NOTE_ID), noteId -> new HashMap<>()).put(Source.DEAL, deal);
            }
        });
        return entities;
    }

    /**
     * <p>
     * Fetches the total number of pinned notes for the given entity based on the provided {@link NoteHolder}
     * </p>
     *
     * @param noteHolder Represents the {@link NoteHolder}
     * @return the total number of pinned notes for the entity
     */
    public long getTotalPinnedNotesCount(final NoteHolder noteHolder) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final SourceNoteMapping sourceNoteMapping = getSourceNoteMapping(noteHolder.getSource());

        countQuery.addJoinClause(new JoinClause(table.getPKColumn(), sourceNoteMapping.noteIdColumn));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMNTNOTE.IS_PINNED), true))
                .and(Criteria.eq(sourceNoteMapping.entityIdColumn, noteHolder.getEntityId())));

        return orgDataStore(noteHolder.orgId()).get(countQuery);
    }

    /**
     * <p>
     * Fetches a list of recently pinned notes for the given entity, limited by the specified count
     * </p>
     *
     * @param noteHolder Represents the {@link NoteHolder}
     * @param limit      The maximum number of recently pinned notes
     * @return A collection of {@link NoteHolder} representing the pinned notes
     */
    public Collection<NoteHolder> getRecentlyPinnedNotes(final NoteHolder noteHolder, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final SourceNoteMapping sourceNoteMapping = getSourceNoteMapping(noteHolder.getSource());

        selectQuery.addSelectClause(table);
        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), sourceNoteMapping.noteIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMNTNOTE.IS_PINNED), true))
                .and(Criteria.eq(sourceNoteMapping.entityIdColumn, noteHolder.getEntityId())));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMNTNOTE.UPDATED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(new LimitClause(limit));
        final DataContainer dataContainer = orgDataStore(noteHolder.orgId()).get(selectQuery);

        return dataContainer.getRows(table).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Fetches the source note mapping for the given note source type
     * </p>
     *
     * @param source The note source type for which the mapping is required
     * @return a {@link SourceNoteMapping} containing table and column mappings
     */
    private SourceNoteMapping getSourceNoteMapping(final NoteHolder.Source source) {
        final Table sourceNoteTable;
        final Column entityIdColumn;
        final Column noteIdColumn;

        switch (source) {
            case DEAL -> {
                sourceNoteTable = getDealNoteTable();
                entityIdColumn = sourceNoteTable.getColumn(OMNTDEALNOTE.DEAL_ID);
                noteIdColumn = sourceNoteTable.getColumn(OMNTDEALNOTE.NOTE_ID);
            }
            case COMPANY -> {
                sourceNoteTable = getCompanyNoteTable();
                entityIdColumn = sourceNoteTable.getColumn(OMNTCOMPANYNOTE.COMPANY_ID);
                noteIdColumn = sourceNoteTable.getColumn(OMNTCOMPANYNOTE.NOTE_ID);
            }
            case CONTACT -> {
                sourceNoteTable = getContactNoteTable();
                entityIdColumn = sourceNoteTable.getColumn(OMNTCONTACTNOTE.CONTACT_ID);
                noteIdColumn = sourceNoteTable.getColumn(OMNTCONTACTNOTE.NOTE_ID);
            }
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_SOURCE);
        }
        return new SourceNoteMapping(sourceNoteTable, entityIdColumn, noteIdColumn);
    }

    /**
     * Represents the mapping details required to join the main note table with the
     * appropriate source-specific note mapping table
     *
     * @param sourceNoteTable The source-specific note mapping table
     * @param entityIdColumn  The column representing the entity ID
     * @param noteIdColumn    The column representing the note ID in the source mapping table
     * @author kishore J
     * @version 1.0
     */
    private record SourceNoteMapping(Table sourceNoteTable, Column entityIdColumn, Column noteIdColumn) {
    }
}