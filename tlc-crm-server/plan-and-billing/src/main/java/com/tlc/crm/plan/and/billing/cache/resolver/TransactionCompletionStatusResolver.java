package com.tlc.crm.plan.and.billing.cache.resolver;

import com.tlc.crm.plan.and.billing.cache.resolver.impl.TransactionCompletionStatusResolverImpl;

/**
 * Interface responsible for resolving the final processing state of a transaction.
 * <p>
 * While services and caches provide raw data, the resolver acts as the definitive
 * logic layer to determine if a transaction (identified by a unique ID, such as
 * {@code 5703_3753_1764837621514} seen in the PayU checkout) has reached a
 * terminal state (Success or Failure).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TransactionCompletionStatusResolver {

    /**
     * Retrieves the singleton instance of the resolver implementation.
     *
     * @return The active {@link TransactionCompletionStatusResolver} instance.
     */
    static TransactionCompletionStatusResolver getInstance() {
        return TransactionCompletionStatusResolverImpl.getInstance();
    }

    /**
     * Evaluates and returns the final completion status for a specific transaction.
     * <p>
     * This method typically orchestrates a lookup across the {@code TransactionCompletionStatusCache}
     * and potentially the persistent database to resolve race conditions between
     * asynchronous webhooks and synchronous user redirects.
     * </p>
     *
     * @param transactionId The unique identifier of the transaction to resolve.
     * @return {@code Boolean.TRUE} if the transaction is completed, {@code Boolean.FALSE} if it incomplete, {@code null} resolves if no record exists in the cache.
     */
    Boolean getCompletionStatus(final long transactionId);
}
