package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.Email.MessageCategory;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the services to load the email related data into the database.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ThreadManager {

    static ThreadManager getInstance() {
        return ThreadManagerImpl.getInstance();
    }

    /**
     * <p>
     * Creates the {@link Thread}.
     * </p>
     *
     * @param thread Represents the {@link Thread}
     */
    void create(final Thread thread);

    /**
     * <p>
     * Gets the {@link Collection} of {@link Thread}.
     * </p>
     *
     * @param syncEmailId        Represents the sync email id
     * @param startTime          Represents the start time
     * @param endTime            Represents the end time
     * @param additionalCriteria Represents the additional where clause
     * @param limitClause        Represents the limit clause.
     * @return {@link Collection} of {@link Thread}
     */
    Collection<Thread> getThreads(final Long syncEmailId, final long startTime, final long endTime, final WhereClause additionalCriteria,
                                  final LimitClause limitClause);

    /**
     * <p>
     * Gets the total count of conversations based on the given parameters.
     * </p>
     *
     * @param syncEmailId Represents the sync email id
     * @param startTime   Represents the start time
     * @param endTime     Represents the end time
     * @param whereClause Represents the additional where clause
     * @return Count of conversations as a {@code long}
     */
    long getTotalRecords(final long syncEmailId, final long startTime, final long endTime, final WhereClause whereClause);

    /**
     * <p>
     * Updates the state of mail based on thread (active, read)
     * </p>
     *
     * @param state               Represents the {@link Email.State}
     * @param stateTransitionInfo Represents the message id as key and value has graph id for reply mapping
     */
    void updateConversation(final Email.State state, final Collection<EmailStateTransitionInfo> stateTransitionInfo);

    /**
     * Loads or updates a thread row in the target container based on the provided email and conversation metadata.
     *
     * @param emailAddress           Represents the email address.
     * @param thread                 Represents the {@link Thread}
     * @param existingEmailContainer An optional {@link DataContainer} containing existing rows for the thread (can be null).
     * @param targetContainer        The {@link DataContainer} where the thread row will be updated or created.
     */
    void loadThreadIntoContainer(final EmailAddress emailAddress, final Thread thread,
                                 final DataContainer existingEmailContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Loads or updates a thread row in the target container based on the provided reference addresses.
     * </p>
     *
     * @param emailAddress       Represents the email address
     * @param thread             Represents the {@link Thread}
     * @param referenceAddresses Represents the {@link Collection} of reference address
     * @param targetContainer    The {@link DataContainer} where the thread row will be updated or created.
     */
    void loadThreadBasedOnReferences(final EmailAddress emailAddress, final Thread thread, final Collection<String> referenceAddresses,
                                     final DataContainer targetContainer);

    /**
     * <p>
     * Gets the thread id fo the given references.
     * </p>
     *
     * @param emailId              Represents the synced email id
     * @param conversationThreadId Represents the conversation thread id
     * @param referenceAddresses   Represents the {@link Collection} of reference addresses
     * @return thread id
     */
    Optional<Long> getThreadId(final Long emailId, final String conversationThreadId, final Collection<String> referenceAddresses);

    /**
     * <p>
     * Gets the {@link DataContainer} for the given thread id.
     * </p>
     *
     * @param threadId Represents the thread id
     * @return {@link DataContainer}
     */
    DataContainer getDataContainer(final Long threadId);

    /**
     * <p>
     * Updates the message state for the given message id.
     * </p>
     *
     * @param messageId       Represents the message id
     * @param targetContainer The {@link DataContainer} where the state of message has been updated
     */
    void updateMessageState(final String messageId, final DataContainer targetContainer);

    /**
     * <p>
     * Gets the optional thread id from the provided conversation id
     * </p>
     *
     * @param conversationId Represents the conversation id
     * @return the optional of thread id
     */
    Optional<Long> getThreadId(final Long conversationId);

    /**
     * <p>
     * Retrieves a collection of threads corresponding to the given message IDs.
     * </p>
     *
     * @param emailId             Represents the synced email id.
     * @param requiredPartialData Flag indicating whether only partial thread data is required
     * @param messageIds          A collection of message IDs for which threads are to be fetched
     * @return A {@link Collection} of {@link Thread} objects corresponding to the provided message IDs
     */
    Collection<Thread> getThreadsByEmailId(final Long emailId, final boolean requiredPartialData, final Collection<String> messageIds);

    /**
     * <p>
     * Updates the labels of the specified Gmail threads.
     * </p>
     *
     * @param threads A collection of {@link Thread} objects whose labels are to be updated
     */
    void updateThreadLabels(final Collection<Thread> threads);

    /**
     * <p>
     * Gets the {@link DataContainer} for the given thread id.
     * </p>
     *
     * @param syncedEmailId Represents the synced email id
     * @param messageId     Represents the message id
     * @return {@link DataContainer}
     */
    DataContainer getContainerByMessageId(final Long syncedEmailId, final String messageId);

    /**
     * <p>
     * Retrieves the {@link Email.MessageCategory} of a given email by matching its metadata
     * against the external message and conversation tables.
     * </p>
     *
     * @param emailId Represent the email ID.
     * @param email   The {@link Email} whose message category is to be determined.
     * @return An {@link Optional} containing the {@link Email.MessageCategory} if found, or empty if not.
     */
    Optional<MessageCategory> getMessageCategory(final Long emailId, final Email email);

    /**
     * <p>
     * Deletes the messages corresponding to the given collection of message IDs.
     * </p>
     *
     * @param ids A collection of unique message IDs to be deleted.
     */
    void deleteMessages(final Collection<Long> ids);
}