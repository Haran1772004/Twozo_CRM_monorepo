package com.tlc.crm.entity.features.api.note.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.features.api.note.manager.CommentManager;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.internal.status.NoteErrorCodes;
import com.tlc.crm.entity.sql.resource.MNTNOTEMENTION;
import com.tlc.crm.entity.sql.resource.OMNTCOMMENT;
import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Note} related DB actions
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NoteManagerImpl implements NoteManager {

    private final CommentManager commentManager;
    private final UserService userService;

    private static class Instance {
        private static final NoteManagerImpl INSTANCE = new NoteManagerImpl();
    }

    private NoteManagerImpl() {
        commentManager = CommentManager.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link NoteManagerImpl} Instance
     * </p>
     *
     * @return {@link NoteManagerImpl}
     */
    public static NoteManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the orgDataStore
     *
     * @param orgId Represents the organization
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMNTNOTE.TABLE);
    }

    public Table getCommentTable() {
        return commentManager.getTable();
    }

    public Table getNoteMentionTable() {
        return Table.get(MNTNOTEMENTION.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Note note) {
        final DataContainer dataContainer = DataContainer.create();

        loadRowIntoContainer(true, note, null, dataContainer);
        orgDataStore(note.orgId()).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Note note) {
        final Long orgId = note.orgId();
        final Table noteTable = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = buildSelectQuery(new WhereClause(Criteria.eq(noteTable.getPKColumn(), note.id())));
        final DataContainer sourceContainer = orgDataStore.get(selectQuery);
        final DataContainer dataContainer = DataContainer.create();

        loadRowIntoContainer(false, note, sourceContainer, dataContainer);
        orgDataStore.commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final long orgId, final long id) {
        orgDataStore(orgId).delete(getTable(), id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePinStatus(final long orgId, final long id, final boolean isPinned) {
        final Table noteTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(noteTable);

        updateQuery.addUpdateColumn(noteTable.getColumn(OMNTNOTE.IS_PINNED), isPinned);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(noteTable.getPKColumn(), id)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Note get(final Long orgId, final Long id) {
        return get(orgId, Set.of(id)).stream().findAny().orElseThrow(() -> ErrorCode.get(NoteErrorCodes.NOTE_NOT_FOUND, "i18n.note.not.found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Note> get(final long orgId, final Collection<Long> ids) {
        final Table noteTable = getTable();
        final SelectQuery selectQuery = buildSelectQuery(new WhereClause(Criteria.in(noteTable.getPKColumn(), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> noteRows = dataContainer.getRows(noteTable);

        return noteRows.map(noteRow -> convertRowToModel(noteRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, Note> getNotes(final long orgId, final Collection<Long> noteIds) {
        final Table noteTable = getTable();
        final Table noteMentionTable = getNoteMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);

        selectQuery.addSelectClause(List.of(
                noteTable.getColumn(OMNTNOTE.CONTENT), noteTable.getColumn(OMNTNOTE.CREATED_BY),
                noteTable.getColumn(OMNTNOTE.UPDATED_BY), noteTable.getColumn(OMNTNOTE.ORG_ID))
        );
        selectQuery.addSelectClause(noteMentionTable);
        selectQuery.addJoinClause(new JoinClause(noteTable.getPKColumn(), noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(noteTable.getPKColumn(), noteIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(noteTable).collect(Collectors.toMap(Row::getPKValue, row -> convertRowToModel(row, dataContainer)));
    }

    /**
     * <p>
     * Retrieves the not-yet-notified mentioned user IDs for the given note IDs.
     * </p>
     *
     * @param orgId   the organization ID
     * @param noteIds collection of note IDs
     * @return map of note ID to unnotified mentioned user IDs
     */
    @Override
    public Map<Long, Collection<Long>> getUnnotifiedMentionedUserIds(final long orgId, final Collection<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table noteMentionTable = getNoteMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(noteMentionTable);

        selectQuery.addSelectClause(List.of(
                noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), noteMentionTable.getColumn(MNTNOTEMENTION.USER_ID))
        );
        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), noteIds))
                .and(Criteria.eq(noteMentionTable.getColumn(MNTNOTEMENTION.IS_NOTIFIED), false))
        );
        final Map<Long, Collection<Long>> mentionedUserIdsByNoteId = new HashMap<>();

        orgDataStore(orgId).get(selectQuery).getRows(noteMentionTable).forEach(row ->
                mentionedUserIdsByNoteId.computeIfAbsent(row.get(MNTNOTEMENTION.NOTE_ID),
                        key -> new HashSet<>()).add(row.get(MNTNOTEMENTION.USER_ID))
        );

        return mentionedUserIdsByNoteId;
    }

    /**
     * <p>
     * Marks the given note mention rows as notified for multiple notes and their respective users
     * in a reduced number of DB hits.
     * </p>
     *
     * @param orgId                  the organization ID
     * @param userIdsByNoteId        a map of note ID to the collection of user IDs to mark as notified
     */
    @Override
    public void markMentionedUsersAsNotified(final long orgId, final Map<Long, Collection<Long>> userIdsByNoteId) {
        if (userIdsByNoteId.isEmpty()) {
            return;
        }
        final Table noteMentionTable = getNoteMentionTable();
        final Collection<Long> noteIds = userIdsByNoteId.keySet();
        final Collection<Long> userIds = userIdsByNoteId.values()
                .stream().flatMap(Collection::stream).collect(Collectors.toSet());
        final UpdateQuery updateQuery = UpdateQuery.get(noteMentionTable);

        updateQuery.addUpdateColumn(noteMentionTable.getColumn(MNTNOTEMENTION.IS_NOTIFIED), true);
        updateQuery.setWhereClause(new WhereClause(
                Criteria.in(noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), noteIds))
                .and(Criteria.in(noteMentionTable.getColumn(MNTNOTEMENTION.USER_ID), userIds))
        );
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Retrieves a map of note IDs to their content for the specified note IDs.
     * This method is optimized for fetching only the content field when the full Note object is not required.
     * </p>
     *
     * @param orgId   the organization ID
     * @param noteIds collection of note IDs to fetch content for
     * @return map of note ID to its content
     */
    @Override
    public Map<Long, String> getNoteContent(final long orgId, final Collection<Long> noteIds) {
        if (noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table noteTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);

        selectQuery.addSelectClause(List.of(noteTable.getColumn(OMNTNOTE.CONTENT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(noteTable.getPKColumn(), noteIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, String> noteContentByIds = new HashMap<>();

        if (!dataContainer.isEmpty()) {
            dataContainer.getRows(noteTable).forEach(row -> {
                noteContentByIds.put(row.getPKValue(), row.get(OMNTNOTE.CONTENT));
            });
        }

        return noteContentByIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadRowIntoContainer(final boolean isCreateEvent, final Note note,
                                      final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table noteTable = getTable();
        final Long id = note.id();
        final Row noteRow = isCreateEvent ? new Row(noteTable)
                : Optional.ofNullable(sourceContainer.getRow(noteTable, id)).map(Row::new).orElse(null);

        if (!isCreateEvent && Objects.isNull(noteRow)) {
            throw ErrorCode.get(NoteErrorCodes.ENTITY_NOT_FOUND, "Org ID : " + note.orgId() + ", Entity ID : " + id);
        }
        final Long time = Instant.now().toEpochMilli();
        final Long commentsCount = note.getCommentsCount();
        final DateTime createdTime = note.getCreatedTime();

        if (noteRow.isNewRow()) {
            note.setId(noteRow.getPKValue());
            noteRow.setOrgId(note.orgId());
            noteRow.set(OMNTNOTE.CREATED_TIME, Objects.isNull(createdTime) ? time : createdTime);
            noteRow.set(OMNTNOTE.CREATED_BY, note.getCreatedBy().id());

            if (Objects.isNull(commentsCount)) {
                noteRow.set(OMNTNOTE.COMMENTS_COUNT, 0);
            }
        } else {
            final DateTime updatedTime = note.getUpdatedTime();
            final CrmUser updatedBy = note.getUpdatedBy();

            if (Objects.nonNull(updatedBy)) {
                noteRow.set(OMNTNOTE.UPDATED_BY, updatedBy.id());
            }
            noteRow.set(OMNTNOTE.UPDATED_TIME, Objects.isNull(updatedTime) ? time : updatedTime);

            if (Objects.nonNull(createdTime)) {
                noteRow.setIfModified(OMNTNOTE.CREATED_TIME, createdTime);
            }
        }
        noteRow.set(OMNTNOTE.CONTENT, note.getContent());

        if (Objects.nonNull(commentsCount)) {
            noteRow.set(OMNTNOTE.COMMENTS_COUNT, commentsCount);
        }
        final Collection<CrmUser> mentionedUsers = note.getMentionedUsers();

        if (Objects.nonNull(mentionedUsers)) {
            loadNoteMentionIntoContainer(note.id(), mentionedUsers, sourceContainer, targetContainer);
        }
        note.setUpdatedTime(new DateTime(time));
        targetContainer.updateRow(noteRow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Note convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Note note = new Note();
        final Long noteId = row.getPKValue();
        final Long orgId = row.getOrgId();

        note.setId(noteId);
        note.setOrgId(orgId);
        note.setContent(row.get(OMNTNOTE.CONTENT));
        note.setCreatedTime(row.get(OMNTNOTE.CREATED_TIME));
        note.setUpdatedTime(row.get(OMNTNOTE.UPDATED_TIME));
        final Long createdBy = row.get(OMNTNOTE.CREATED_BY);
        final Long updatedBy = row.get(OMNTNOTE.UPDATED_BY);
        final boolean createdByIsNotNull = Objects.nonNull(createdBy);
        final boolean updatedByIsNotNull = Objects.nonNull(updatedBy);
        final Collection<Long> userIds = new HashSet<>();
        final Boolean isPinned = row.get(OMNTNOTE.IS_PINNED);
        final Long commentsCount = row.get(OMNTNOTE.COMMENTS_COUNT);

        if (Objects.nonNull(commentsCount)) {
            note.setCommentsCount(commentsCount);
        }

        if (Objects.nonNull(isPinned)) {
            note.setPinned(isPinned);
        }

        if (createdByIsNotNull) {
            userIds.add(createdBy);
        }

        if (updatedByIsNotNull) {
            userIds.add(updatedBy);
        }
        final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

        if (createdByIsNotNull) {
            note.setCreatedBy(users.get(createdBy));
        }

        if (updatedByIsNotNull) {
            note.setUpdatedBy(users.get(updatedBy));
        }

        if (Objects.nonNull(dataContainer)) {
            final Table noteMentionTable = getNoteMentionTable();
            final Table commentTable = getCommentTable();
            final Stream<Row> noteMentionRows = dataContainer.getRows(noteMentionTable,
                    new WhereClause(Criteria.eq(noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), noteId)));
            final Stream<Row> commentRows = dataContainer.getRows(commentTable,
                    new WhereClause(Criteria.eq(commentTable.getColumn(OMNTCOMMENT.NOTE_ID), noteId)));
            final Collection<Long> mentionedUserIds = noteMentionRows.map(noteMentionRow ->
                    (long) noteMentionRow.get(MNTNOTEMENTION.USER_ID)).collect(Collectors.toUnmodifiableSet());

            note.setMentionedUsers(userService.getUsers(orgId, mentionedUserIds));
            note.setComments(commentRows.map(commentRow -> commentManager.convertRowToModel(commentRow, dataContainer)).toList());
        }
        return note;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Note convertRowToPartialNote(final Row noteRow) {
        return convertRowToModel(noteRow, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SelectQuery buildSelectQuery(final WhereClause whereClause) {
        final Table noteTable = getTable();
        final Table noteMentionTable = getNoteMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);

        selectQuery.addSelectClause(noteTable, noteMentionTable);
        selectQuery.addJoinClause(new JoinClause(noteTable.getPKColumn(), noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);

        return selectQuery;
    }

    /**
     * <p>
     * Loads the note preview data such as content into the provided container
     * </p>
     *
     * @param orgId         Represents the organization ID
     * @param ids           Represents the collection of note IDs to fetch preview data for
     * @param dataContainer Represents the container where the fetched data will be appended
     */
    public void loadNotePreviewDataIntoContainer(final long orgId, final Collection<Long> ids,
                                                 final DataContainer dataContainer) {
        final Table noteTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(noteTable);

        selectQuery.addSelectClause(noteTable.getColumn(OMNTNOTE.CONTENT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(noteTable.getPKColumn(), ids)));

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * Synchronizes the mentioned users for a note by comparing the provided list of mentions
     * with existing records in the data store.
     * This method ensures mention consistency by:
     * - Deleting mention rows no longer present in the updated list
     * - Adding new mention rows that were not previously stored
     *
     * @param noteId           ID of the note being updated
     * @param mentionedUsers   current list of users mentioned in the note
     * @param sourceContainer  container with existing note mention data (for comparison)
     * @param targetContainer  container where changes (add/delete) will be staged for persistence
     */
    private void loadNoteMentionIntoContainer(final long noteId, final Collection<CrmUser> mentionedUsers,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table noteMentionTable = getNoteMentionTable();
        final Map<Long, CrmUser> mentionedUserMap = mentionedUsers.stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
        final Stream<Row> noteMentionRows = Objects.isNull(sourceContainer) ? Stream.empty() : sourceContainer.getRows(noteMentionTable,
                new WhereClause(Criteria.eq(noteMentionTable.getColumn(MNTNOTEMENTION.NOTE_ID), noteId)));

        noteMentionRows.forEach(noteMentionRow -> {
            if (Objects.isNull(mentionedUserMap.remove((Long) noteMentionRow.get(MNTNOTEMENTION.USER_ID)))) {
                targetContainer.deleteRow(noteMentionRow);
            }
        });

        for (final CrmUser mentionedUser : mentionedUserMap.values()) {
            final Row noteMentionRow = new Row(noteMentionTable);

            noteMentionRow.set(MNTNOTEMENTION.NOTE_ID, noteId);
            noteMentionRow.set(MNTNOTEMENTION.USER_ID, mentionedUser.id());
            targetContainer.addNewRow(noteMentionRow);
        }
    }
}