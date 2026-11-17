package com.tlc.crm.plan.and.billing.cache;

import com.tlc.crm.plan.and.billing.cache.impl.TransactionCompletionStatusCacheImpl;

/**
 * Interface for a thread-safe cache used to track and manage the completion
 * status of financial transactions.
 * <p>
 * This cache acts as a temporary state store to prevent redundant processing
 * or to handle asynchronous payment notifications (webhooks) by checking
 * whether a transaction ID has already been marked as completed.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TransactionCompletionStatusCache {

    /**
     * Provides access to the singleton instance of the cache implementation.
     *
     * @return The active {@link TransactionCompletionStatusCache} instance.
     */
    static TransactionCompletionStatusCache getInstance() {
        return TransactionCompletionStatusCacheImpl.getInstance();
    }

    /**
     * Retrieves the current completion status for a specific transaction.
     *
     * @param transactionId The unique identifier of the transaction (e.g., PayU TxnID).
     * @return {@code Boolean.TRUE} if the transaction is completed, @code Boolean.FALSE} if it incomplete, or {@code null} if no record exists in the cache.
     */
    Boolean getCompletionStatus(final long transactionId);

    /**
     * Updates or inserts the completion status for a specific transaction ID.
     *
     * @param transactionId    The unique identifier for the transaction.
     * @param completionStatus The status to persist ({@code true} for success, {@code false} for failure/pending).
     */
    void updateCompletionStatus(final long transactionId, final boolean completionStatus);
}
