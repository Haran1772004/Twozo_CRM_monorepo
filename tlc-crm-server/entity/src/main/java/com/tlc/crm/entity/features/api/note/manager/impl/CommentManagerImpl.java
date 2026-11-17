package com.tlc.crm.entity.features.api.note.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.features.api.note.manager.CommentManager;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.internal.status.NoteErrorCodes;
import com.tlc.crm.entity.sql.resource.MNTCOMMENTMENTION;
import com.tlc.crm.entity.sql.resource.OMNTCOMMENT;

import com.tlc.crm.entity.sql.resource.OMNTNOTE;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link Comment} related DB actions
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CommentManagerImpl implements CommentManager {

    private final UserService userService;

    private static class Instance {
        private static final CommentManagerImpl INSTANCE = new CommentManagerImpl();
    }

    private CommentManagerImpl() {
        userService = UserService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CommentManagerImpl} Instance
     * </p>
     *
     * @return {@link CommentManagerImpl}
     */
    public static CommentManagerImpl getInstance() {
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
        return Table.get(OMNTCOMMENT.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getCommentMentionTable() {
        return Table.get(MNTCOMMENTMENTION.TABLE);
    }

    /**
     * <p>
     * Provides the table of the note.
     * </p>
     *
     * @return OMNTNote table
     */
    public Table getNoteTable() {
        return Table.get(OMNTNOTE.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final Comment comment) {
        final DataContainer dataContainer = DataContainer.create();
        final Table noteTable = getNoteTable();
        final OrgDataStore orgDataStore = orgDataStore(comment.orgId());
        final DataContainer sourceContainer = orgDataStore.get(noteTable, Set.of(comment.getNoteId()));

        loadRowIntoContainer(true, comment, sourceContainer, dataContainer);
        orgDataStore.commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Comment comment) {
        final Long orgId = comment.orgId();
        final Table commentTable = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = buildSelectQuery(new WhereClause(Criteria.eq(commentTable.getPKColumn(), comment.id())));
        final DataContainer sourceContainer = orgDataStore.get(selectQuery);
        final DataContainer dataContainer = DataContainer.create();

        loadRowIntoContainer(false, comment, sourceContainer, dataContainer);
        orgDataStore.commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final long orgId, final long id) {
        final Table commentTable = getTable();
        final Table noteTable = getNoteTable();
        final SelectQuery selectQuery = SelectQuery.get(commentTable);

        selectQuery.addJoinClause(new JoinClause(commentTable.getColumn(OMNTCOMMENT.NOTE_ID), noteTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(commentTable.getPKColumn(), id)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row commentRow = dataContainer.getRow(commentTable);

        if (Objects.nonNull(commentRow)) {
            final Row noteRow = dataContainer.getRow(noteTable);

            noteRow.set(OMNTNOTE.COMMENTS_COUNT, (Long) noteRow.get(OMNTNOTE.COMMENTS_COUNT) - NumericalConstant.ONE);
            dataContainer.updateRow(noteRow);
            dataContainer.deleteRow(commentRow);

            orgDataStore(orgId).commitChanges(dataContainer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePinStatus(final long orgId, final long id, final boolean isPinned) {
        final Table commentTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(commentTable);

        updateQuery.addUpdateColumn(commentTable.getColumn(OMNTCOMMENT.IS_PINNED), isPinned);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(commentTable.getPKColumn(), id)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comment get(final Long orgId, final Long id) {
        return get(orgId, Set.of(id)).stream().findAny().orElseThrow(() -> ErrorCode.get(NoteErrorCodes.COMMENT_NOT_FOUND, "i18n.comment.not.found"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Comment> get(final long orgId, final Collection<Long> ids) {
        final Table commentTable = getTable();
        final SelectQuery selectQuery = buildSelectQuery(new WhereClause(Criteria.in(commentTable.getPKColumn(), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(commentTable).map(commentRow -> convertRowToModel(commentRow, dataContainer)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Comment> getComments(final long orgId, final Long noteId, final LimitConfig limitConfig) {
        final Table commentTable = getTable();
        final Table commentMentionTable = getCommentMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(commentTable);

        selectQuery.addJoinClause(new JoinClause(commentTable.getPKColumn(), commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID),
                JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(commentTable.getColumn(OMNTCOMMENT.NOTE_ID), noteId)));
        selectQuery.addOrderByClause(new OrderByClause(commentTable.getColumn(OMNTCOMMENT.IS_PINNED), OrderByClause.OrderType.DESCENDING));
        selectQuery.addOrderByClause(new OrderByClause(commentTable.getColumn(OMNTCOMMENT.CREATED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setLimitClause(limitConfig.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(commentTable).map(row -> convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Gets the total comments count for the given note ID
     * </p>
     *
     * @param orgId    Represents an organisation ID
     * @param noteId   Represents the note ID
     * @return total comments count for the given note ID
     */
    public Long getCommentsCount(final long orgId, final Long noteId) {
        final Table commentTable = getTable();
        final CountQuery countQuery = CountQuery.get(commentTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(commentTable.getColumn(OMNTCOMMENT.NOTE_ID), noteId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization ids
     * @param commentIds Which contains the collection of comment ids
     * @return The collection of note ids
     */
    @Override
    public Map<Long, Long> getNoteIds(final Long orgId, final Collection<Long> commentIds) {
        final Table commentTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(commentTable);

        selectQuery.addSelectClause(commentTable.getColumn(OMNTCOMMENT.NOTE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(commentTable.getPKColumn(), commentIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(commentTable).collect(Collectors.toMap(Row::getPKValue, row -> row.get(OMNTCOMMENT.NOTE_ID)));
    }

    /**
     * <p>
     * Fetches comments required for mention email alert.
     * </p>
     *
     * @param orgId      the organization ID
     * @param commentIds collection of comment IDs
     * @return map of comment ID to comment model containing only mention-email fields
     */
    @Override
    public Map<Long, Comment> getComments(final long orgId, final Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table commentTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(commentTable);

        selectQuery.addSelectClause(
                List.of(commentTable.getColumn(OMNTCOMMENT.NOTE_ID),commentTable.getColumn(OMNTCOMMENT.CONTENT),
                        commentTable.getColumn(OMNTCOMMENT.CREATED_BY), commentTable.getColumn(OMNTCOMMENT.UPDATED_BY),
                        commentTable.getColumn(OMNTCOMMENT.ORG_ID), commentTable.getColumn(OMNTCOMMENT.IS_PINNED))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(commentTable.getPKColumn(), commentIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(commentTable).collect(
                Collectors.toMap(Row::getPKValue, row -> convertRowToModel(row, dataContainer))
        );
    }

    /**
     * <p>
     * Retrieves the un-notified mentioned user IDs for the given comment IDs.
     * </p>
     *
     * @param orgId      the organization ID
     * @param commentIds collection of comment IDs
     * @return map of comment ID to unnotified mentioned user IDs
     */
    @Override
    public Map<Long, Collection<Long>> getUnnotifiedMentionedUserIds(final long orgId, final Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table commentMentionTable = getCommentMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(commentMentionTable);

        selectQuery.addSelectClause(List.of(
                commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID),
                commentMentionTable.getColumn(MNTCOMMENTMENTION.USER_ID))
        );
        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID), commentIds))
                .and(Criteria.eq(commentMentionTable.getColumn(MNTCOMMENTMENTION.IS_NOTIFIED), false))
        );
        final Map<Long, Collection<Long>> mentionedUserIdsByCommentId = new HashMap<>();

        orgDataStore(orgId).get(selectQuery).getRows(commentMentionTable).forEach(row ->
                mentionedUserIdsByCommentId.computeIfAbsent(row.get(MNTCOMMENTMENTION.COMMENT_ID),
                                key -> new HashSet<>()).add(row.get(MNTCOMMENTMENTION.USER_ID)));

        return mentionedUserIdsByCommentId;
    }

    /**
     * <p>
     * Marks the given comment mention rows as notified for multiple comments and their respective users
     * </p>
     *
     * @param orgId              the organization ID
     * @param userIdsByCommentId a map of comment ID to the collection of user IDs to mark as notified
     */
    @Override
    public void markMentionedUsersAsNotified(final long orgId, final Map<Long, Collection<Long>> userIdsByCommentId) {
        if (userIdsByCommentId.isEmpty()) {
            return;
        }
        final Table commentMentionTable = getCommentMentionTable();
        final UpdateQuery updateQuery = UpdateQuery.get(commentMentionTable);

        updateQuery.addUpdateColumn(commentMentionTable.getColumn(MNTCOMMENTMENTION.IS_NOTIFIED), true);
        updateQuery.setWhereClause(new WhereClause(
                Criteria.in(commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID), userIdsByCommentId.keySet()))
                .and(Criteria.in(commentMentionTable.getColumn(MNTCOMMENTMENTION.USER_ID),
                        userIdsByCommentId.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())))
        );
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadRowIntoContainer(final boolean isCreateEvent, final Comment comment,
                                     final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table commentTable = getTable();
        final Long id = comment.id();
        final Row commentRow = isCreateEvent ? new Row(commentTable)
                : Optional.ofNullable(sourceContainer.getRow(commentTable, id)).map(Row::new).orElse(null);

        if (!isCreateEvent && Objects.isNull(commentRow)) {
            throw ErrorCode.get(NoteErrorCodes.ENTITY_NOT_FOUND, "Org ID : " + comment.orgId() + ", Entity ID : " + id);
        }
        final Long time = Instant.now().toEpochMilli();

        if (commentRow.isNewRow()) {
            final DateTime createdTime = comment.getCreatedTime();

            comment.setId(commentRow.getPKValue());
            commentRow.setOrgId(comment.orgId());
            commentRow.set(OMNTCOMMENT.CREATED_TIME, Objects.isNull(createdTime) ? time : createdTime);
            commentRow.set(OMNTCOMMENT.CREATED_BY, comment.getCreatedBy().id());
            commentRow.set(OMNTCOMMENT.NOTE_ID, comment.getNoteId());
            loadNoteCommentsCountIntoContainer(comment.getNoteId(), sourceContainer, targetContainer);
        } else {
            final DateTime updatedTime = comment.getUpdatedTime();

            commentRow.set(OMNTCOMMENT.UPDATED_BY, comment.getUpdatedBy().id());
            commentRow.set(OMNTCOMMENT.UPDATED_TIME, Objects.isNull(updatedTime) ? time : updatedTime);
        }
        commentRow.set(OMNTCOMMENT.CONTENT, comment.getContent());
        final Collection<CrmUser> mentionedUsers = comment.getMentionedUsers();

        if (Objects.nonNull(mentionedUsers)) {
            loadCommentMentionIntoContainer(comment.id(), mentionedUsers, sourceContainer, targetContainer);
        }
        comment.setUpdatedTime(new DateTime(time));
        targetContainer.updateRow(commentRow);
    }

    /**
     * <p>
     * Loads the comments count of the note into the {@link DataContainer}.
     * </p>
     *
     * @param noteId          Represents the note ID.
     * @param sourceContainer container with source data
     * @param targetContainer container to receive transformed data
     */
    private void loadNoteCommentsCountIntoContainer(final Long noteId, final DataContainer sourceContainer,
                                                    final DataContainer targetContainer) {
        final Optional<Row> optionalNoteRow = Optional.ofNullable(sourceContainer.getRow(getNoteTable(), noteId));

        optionalNoteRow.ifPresent(noteRow -> {
            noteRow.set(OMNTNOTE.COMMENTS_COUNT, (long) noteRow.get(OMNTNOTE.COMMENTS_COUNT) + 1);
            targetContainer.updateRow(noteRow);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comment convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Comment comment = new Comment();
        final Long commentId = row.getPKValue();
        final Long orgId = row.getOrgId();

        comment.setId(commentId);
        comment.setOrgId(orgId);
        comment.setNoteId(row.get(OMNTCOMMENT.NOTE_ID));
        comment.setContent(row.get(OMNTCOMMENT.CONTENT));
        comment.setCreatedTime(row.get(OMNTCOMMENT.CREATED_TIME));
        comment.setUpdatedTime(row.get(OMNTCOMMENT.UPDATED_TIME));
        comment.setPinned(row.get(OMNTCOMMENT.IS_PINNED));
        final Long createdBy = row.get(OMNTCOMMENT.CREATED_BY);
        final Long updatedBy = row.get(OMNTCOMMENT.UPDATED_BY);
        final boolean createdByIsNotNull = Objects.nonNull(createdBy);
        final boolean updatedByIsNotNull = Objects.nonNull(updatedBy);
        final Collection<Long> userIds = new HashSet<>();

        if (createdByIsNotNull) {
            userIds.add(createdBy);
        }

        if (updatedByIsNotNull) {
            userIds.add(updatedBy);
        }
        final Map<Long, CrmUser> users = userService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

        if (createdByIsNotNull) {
            comment.setCreatedBy(users.get(createdBy));
        }

        if (updatedByIsNotNull) {
            comment.setUpdatedBy(users.get(updatedBy));
        }

        if (Objects.nonNull(dataContainer)) {
            final Table commentMentionTable = getCommentMentionTable();
            final Stream<Row> commentMentionRows = dataContainer.getRows(commentMentionTable,
                    new WhereClause(Criteria.eq(commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID), commentId)));
            final Collection<Long> mentionedUserIds = commentMentionRows.map(commentMentionRow ->
                    (long) commentMentionRow.get(MNTCOMMENTMENTION.USER_ID)).collect(Collectors.toUnmodifiableSet());

            comment.setMentionedUsers(userService.getUsers(orgId, mentionedUserIds));
        }
        return comment;
    }

    /**
     * Synchronizes the mentioned users of a comment by comparing the new list with existing DB records.
     *
     * This method performs a differential update:
     * - Removes any user mention rows that are no longer in the updated list.
     * - Adds new mention rows for users not already associated with the comment.
     *
     * @param commentId        ID of the comment being processed
     * @param mentionedUsers   Current list of mentioned users to persist
     * @param sourceContainer  Container holding existing comment mention rows (used for comparison)
     * @param targetContainer  Container where changes (add/delete) will be staged for commit
     */
    private void loadCommentMentionIntoContainer(final long commentId, final Collection<CrmUser> mentionedUsers,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table commentMentionTable = getCommentMentionTable();
        final Map<Long, CrmUser> mentionedUserMap = mentionedUsers.stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
        final Stream<Row> commentMentionRows = Objects.isNull(sourceContainer) ? Stream.empty() : sourceContainer.getRows(commentMentionTable,
                new WhereClause(Criteria.eq(commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID), commentId)));

        commentMentionRows.forEach(commentMentionRow -> {
            if (Objects.isNull(mentionedUserMap.remove((Long) commentMentionRow.get(MNTCOMMENTMENTION.USER_ID)))) {
                targetContainer.deleteRow(commentMentionRow);
            }
        });

        for (final CrmUser mentionedUser : mentionedUserMap.values()) {
            final Row commentMentionRow = new Row(commentMentionTable);

            commentMentionRow.set(MNTCOMMENTMENTION.COMMENT_ID, commentId);
            commentMentionRow.set(MNTCOMMENTMENTION.USER_ID, mentionedUser.id());
            targetContainer.addNewRow(commentMentionRow);
        }
    }

    /**
     * Constructs a {@link SelectQuery} with a LEFT JOIN between the comment and comment mention tables.
     *
     * @param whereClause filter condition to apply (e.g., by ID, Note ID)
     * @return constructed {@link SelectQuery} object
     */
    private SelectQuery buildSelectQuery(final WhereClause whereClause) {
        final Table commentTable = getTable();
        final Table commentMentionTable = getCommentMentionTable();
        final SelectQuery selectQuery = SelectQuery.get(commentTable);

        selectQuery.addJoinClause(new JoinClause(commentTable.getPKColumn(), commentMentionTable.getColumn(MNTCOMMENTMENTION.COMMENT_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        return selectQuery;
    }
}