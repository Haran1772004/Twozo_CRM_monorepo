package com.tlc.crm.plan.and.billing.cache.service;

import com.tlc.crm.plan.and.billing.cache.service.impl.TransactionCompletionStatusServiceImpl;

/**
 * Service interface for querying the completion status of payment transactions.
 * <p>
 * This service provides a high-level abstraction over the transaction cache,
 * allowing other modules to verify if a payment process has successfully
 * finished. It is particularly useful for verifying transaction states
 * during UI redirects or background verification processes.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TransactionCompletionStatusService {

    /**
     * Retrieves the singleton instance of the service implementation.
     *
     * @return The active {@link TransactionCompletionStatusService} instance.
     */
    static TransactionCompletionStatusService getInstance() {
        return TransactionCompletionStatusServiceImpl.getInstance();
    }

    /**
     * Checks the processing status of a specific transaction by its unique ID.
     * <p>
     * Based on the PayU integration flow (e.g., Transaction ID: {@code 5703_3753_1764837621514}),
     * this method determines if the backend has finalized the payment record.
     * </p>
     *
     * @param transactionId The unique identifier of the transaction to query.
     * @return {@code Boolean.TRUE} if the transaction is completed, {@code Boolean.FALSE} if it incomplete, {@code null} if transaction id is not available.
     */
    Boolean getCompletionStatus(final long transactionId);
}
