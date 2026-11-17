package com.tlc.crm.plan.and.billing.cache.resolver.impl;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.cache.TransactionCompletionStatusCache;
import com.tlc.crm.plan.and.billing.cache.resolver.TransactionCompletionStatusResolver;

import java.util.Objects;

/**
 * Implementation of {@link TransactionCompletionStatusResolver} that manages the
 * final determination of a transaction's state.
 * <p>
 * This class serves as the primary evaluation point for transaction completion.
 * As seen in the PayU checkout flow (Transaction ID: {@code 5703_3753_1764837621514}),
 * this resolver is tasked with identifying if a terminal state (Success/Failure)
 * has been reached based on the data available in the {@link TransactionCompletionStatusCache}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TransactionCompletionStatusResolverImpl implements TransactionCompletionStatusResolver {

    private final TransactionCompletionStatusCache statusCache;
    private final OrgPlanManager orgPlanManager;

    private TransactionCompletionStatusResolverImpl() {
        statusCache = TransactionCompletionStatusCache.getInstance();
        orgPlanManager = OrgPlanManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final TransactionCompletionStatusResolverImpl STATUS_RESOLVER = new TransactionCompletionStatusResolverImpl();
    }

    /**
     * Provides access to the singleton instance of the cache implementation.
     *
     * @return The active {@link TransactionCompletionStatusResolver} instance.
     */
    public static TransactionCompletionStatusResolverImpl getInstance() {
        return InstanceHolder.STATUS_RESOLVER;
    }

    /**
     * Retrieves the current completion status for a specific transaction.
     *
     * @param transactionId The unique identifier of the transaction (e.g., PayU TxnID).
     * @return {@code Boolean.TRUE} if the transaction is completed, {@code Boolean.FALSE} if it incomplete, {@code null} resolves if no record exists in the cache.
     */
    @Override
    public Boolean getCompletionStatus(final long transactionId) {
        final Boolean cachedCompletionStatus = statusCache.getCompletionStatus(transactionId);

        if (Objects.isNull(cachedCompletionStatus)) {
            final Boolean completionStatus = orgPlanManager.getTransactionCompletionStatus(transactionId);

            if (Objects.isNull(completionStatus)) {
                return null;
            }
            statusCache.updateCompletionStatus(transactionId, completionStatus);
            return completionStatus;
        }
        return cachedCompletionStatus;
    }
}
