package com.tlc.crm.plan.and.billing.cache.service.impl;

import com.tlc.crm.plan.and.billing.cache.resolver.TransactionCompletionStatusResolver;
import com.tlc.crm.plan.and.billing.cache.service.TransactionCompletionStatusService;

/**
 * Implementation of {@link TransactionCompletionStatusService} that provides a
 * read-only interface for checking transaction finalization.
 * <p>
 * This service serves as the public API for UI components and background workers
 * to verify the outcome of a payment. By delegating to the {@link TransactionCompletionStatusResolver},
 * it ensures that the most accurate state is returned, handling the typical
 * asynchronous race conditions inherent in payment gateway integrations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TransactionCompletionStatusServiceImpl implements TransactionCompletionStatusService {

    private final TransactionCompletionStatusResolver statusResolver;

    private TransactionCompletionStatusServiceImpl() {
        statusResolver = TransactionCompletionStatusResolver.getInstance();
    }

    private static final class InstanceHolder {
        private static final TransactionCompletionStatusServiceImpl STATUS_SERVICE = new TransactionCompletionStatusServiceImpl();
    }

    /**
     * Provides access to the singleton instance of the cache implementation.
     *
     * @return The active {@link TransactionCompletionStatusService} instance.
     */
    public static TransactionCompletionStatusServiceImpl getInstance() {
        return InstanceHolder.STATUS_SERVICE;
    }

    /**
     * Retrieves the current completion status for a specific transaction.
     *
     * @param transactionId The unique identifier of the transaction (e.g., PayU TxnID).
     * @return {@code Boolean.TRUE} if the transaction is completed, {@code Boolean.FALSE} if it incomplete, {@code null} if transaction id is not available
     */
    @Override
    public Boolean getCompletionStatus(final long transactionId) {
        return statusResolver.getCompletionStatus(transactionId);
    }
}
