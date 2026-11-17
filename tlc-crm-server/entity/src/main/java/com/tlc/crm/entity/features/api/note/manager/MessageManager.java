package com.tlc.crm.entity.features.api.note.manager;

import com.tlc.crm.entity.features.api.note.model.Message;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * Interface for managing the DB operations related to {@link Message} entities.
 *
 * @param <R> the type of Message this manager handles
 * @author Sathish
 * @version 1.0
 */
public interface MessageManager<R extends Message> {

    /**
     * Returns the SQL {@link Table} this manager operates on.
     *
     * @return table instance for message persistence
     */
    Table getTable();

    /**
     * Persists a new message into the underlying data store.
     *
     * @param message the message to be created
     */
    void create(final R message);

    /**
     * Updates an existing message in the data store.
     *
     * @param message the message containing updated data
     */
    void update(final R message);

    /**
     * Deletes a message by ID for the given organization.
     *
     * @param orgId organization context
     * @param id    message ID to be deleted
     */
    void delete(final long orgId, final long id);

    /**
     * Updates the pinned status of a message (e.g., for UI sorting/highlighting).
     *
     * @param orgId    organization context
     * @param id       ID of the message
     * @param isPinned true to pin, false to unpin
     */
    void updatePinStatus(final long orgId, final long id, final boolean isPinned);

    /**
     * Fetches a message by ID for the given organization.
     *
     * @param orgId organization context
     * @param id    message ID to fetch
     * @return message instance, or null if not found
     */
    R get(final Long orgId, final Long id);

    /**
     * Fetches multiple messages by their IDs for the given organization.
     *
     * @param orgId organization context
     * @param ids   collection of message IDs
     * @return collection of messages
     */
    Collection<R> get(final long orgId, final Collection<Long> ids);

    /**
     * Maps a message and its source data into a new data container, typically used
     * when transitioning between temporary and persistent storage or transforming
     * data across layers.
     *
     * @param isCreateEvent   whether this operation is part of a create flow
     * @param message         the message model to populate
     * @param sourceContainer container with source data
     * @param targetContainer container to receive transformed data
     */
    void loadRowIntoContainer(final boolean isCreateEvent, final R message,
                              final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * Converts a database row into a typed message model.
     *
     * @param row           raw SQL row object
     * @param dataContainer contextual data container
     * @return converted message model
     */
    R convertRowToModel(final Row row, final DataContainer dataContainer);
}