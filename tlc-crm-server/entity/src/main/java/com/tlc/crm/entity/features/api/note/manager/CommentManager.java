package com.tlc.crm.entity.features.api.note.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.features.api.note.manager.impl.CommentManagerImpl;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for managing database operations related to {@link Comment} entities in the CRM system.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CommentManager extends MessageManager<Comment> {

    /**
     * Provides the singleton instance of the CommentManager.
     *
     * @return singleton instance of {@link CommentManager}
     */
    static CommentManager getInstance() {
        return CommentManagerImpl.getInstance();
    }

    /**
     * Returns the SQL table instance used to store and manage mentions within comments.
     * This table usually maps a comment ID to multiple mentioned user IDs.
     *
     * @return SQL {@link Table} that manages comment mentions
     */
    Table getCommentMentionTable();

    /**
     * Retrieves a paginated list of comments associated with a specific note ID.
     *
     * @param orgId        the organization ID
     * @param noteId       the note to fetch comments for
     * @param limitConfig  pagination and offset config (start, limit)
     * @return collection of {@link Comment} objects matching the criteria
     */
    Collection<Comment> getComments(final long orgId, final Long noteId, final LimitConfig limitConfig);

    /**
     * <p>
     * Gets the total comments count for the given note ID
     * </p>
     *
     * @param orgId    Represents an organisation ID
     * @param noteId   Represents the note ID
     * @return total comments count for the given note ID
     */
    Long getCommentsCount(final long orgId, final Long noteId);

    /**
     * <p>
     * Gets the note ids for given comment ids
     * </p>
     *
     * @param orgId      Represents an organization ids
     * @param commentIds Which contains the collection of comment ids
     * @return The collection of note ids
     */
    Map<Long, Long> getNoteIds(final Long orgId, final Collection<Long> commentIds);

    /**
     * <p>
     * Fetches comments required for mention email alert.
     * </p>
     *
     * @param orgId      the organization ID
     * @param commentIds collection of comment IDs
     * @return map of comment ID to comment model containing only mention-email fields
     */
    Map<Long, Comment> getComments(final long orgId, final Collection<Long> commentIds);

    /**
     * <p>
     * Retrieves the un-notified mentioned user IDs for the given comment IDs.
     * </p>
     *
     * @param orgId      the organization ID
     * @param commentIds collection of comment IDs
     * @return map of comment ID to unnotified mentioned user IDs
     */
    Map<Long, Collection<Long>> getUnnotifiedMentionedUserIds(final long orgId, final Collection<Long> commentIds);

    /**
     * <p>
     * Marks the given comment mention rows as notified for multiple comments and their respective users.
     * </p>
     *
     * @param orgId              the organization ID
     * @param userIdsByCommentId a map of comment ID to the collection of user IDs to mark as notified
     */
    void markMentionedUsersAsNotified(final long orgId, final Map<Long, Collection<Long>> userIdsByCommentId);
}