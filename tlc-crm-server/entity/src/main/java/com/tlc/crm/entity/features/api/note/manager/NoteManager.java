package com.tlc.crm.entity.features.api.note.manager;

import com.tlc.crm.entity.features.api.note.manager.impl.NoteManagerImpl;
import com.tlc.crm.entity.features.api.note.model.Note;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Manager interface for handling database operations related to {@link Note} entities.
 *
 * @author AswiniN
 * @version 1.0
 */
public interface NoteManager extends MessageManager<Note> {

    /**
     * Returns a singleton instance of the {@link NoteManager}.
     *
     * @return singleton instance of NoteManager
     */
    static NoteManager getInstance() {
        return NoteManagerImpl.getInstance();
    }

    /**
     * Fetches a map of Note objects keyed by their IDs for the given organization.
     * Useful for bulk operations like list views, batch updates, etc.
     *
     * @param orgId   the organization ID
     * @param noteIds collection of note IDs to fetch
     * @return map of note ID to Note object
     */
    Map<Long, Note> getNotes(final long orgId, final Collection<Long> noteIds);

    /**
     * <p>
     * Retrieves the un-notified mentioned user IDs for the given note IDs.
     * </p>
     *
     * @param orgId   the organization ID
     * @param noteIds collection of note IDs
     * @return map of note ID to unnotified mentioned user IDs
     */
    Map<Long, Collection<Long>> getUnnotifiedMentionedUserIds(final long orgId, final Collection<Long> noteIds);

    /**
     * <p>
     * Marks the given note mention rows as notified for multiple notes and their respective users.
     * </p>
     *
     * @param orgId                  the organization ID
     * @param userIdsByNoteId        a map of note ID to the collection of user IDs to mark as notified
     */
    void markMentionedUsersAsNotified(final long orgId, final Map<Long, Collection<Long>> userIdsByNoteId);

    /**
     * <p>
     * Retrieves a map of note IDs to their content for the specified note IDs.
     * </p>
     *
     * @param orgId   the organization ID
     * @param noteIds collection of note IDs to fetch content for
     * @return map of note ID to its content
     */
    Map<Long, String> getNoteContent(final long orgId, final Collection<Long> noteIds);

    /**
     * Converts a raw SQL {@link Row} to a partially populated {@link Note} object.
     * Partial conversion avoids unnecessary joins or field loading for lightweight use cases
     * like summary cards or note previews.
     *
     * @param noteRow SQL row representing a Note
     * @return a lightweight Note instance
     */
    Note convertRowToPartialNote(final Row noteRow);

    /**
     * Builds a dynamic SQL {@link SelectQuery} for the notes table based on a provided filter.
     * Used for advanced search, filtering, and pagination purposes.
     *
     * @param whereClause dynamic filter criteria
     * @return constructed SelectQuery instance
     */
    SelectQuery buildSelectQuery(final WhereClause whereClause);

    /**
     * <p>
     * Loads the note preview data such as content into the provided container
     * </p>
     *
     * @param orgId         Represents the organization ID
     * @param ids           Represents the collection of note IDs to fetch preview data for
     * @param dataContainer Represents the container where the fetched data will be appended
     */
    void loadNotePreviewDataIntoContainer(final long orgId, final Collection<Long> ids,
                                          final DataContainer dataContainer);
}