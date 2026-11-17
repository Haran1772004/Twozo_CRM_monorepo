package com.tlc.crm.plan.and.billing.internal.listener;

import com.tlc.crm.plan.and.billing.cache.TransactionCompletionStatusCache;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBTRANSACTION;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;

/**
 * A database listener responsible for synchronizing transaction state changes with the {@link TransactionCompletionStatusCache}.
 * <p>
 * This listener intercepts updates to the {@code OMPBTRANSACTION} table. When a row is
 * updated (e.g., via a PayU webhook or internal process), this listener
 * pushes the new completion status into the short-lived cache to ensure
 * immediate consistency for UI redirects and polling services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TransactionRowListener implements RowUpdateListener {

    private final TransactionCompletionStatusCache transactionCompletionStatusCache;

    private TransactionRowListener() {
        transactionCompletionStatusCache = TransactionCompletionStatusCache.getInstance();
    }

    private static final class InstanceHolder {
        private static final TransactionRowListener TRANSACTION_ROW_LISTENER = new TransactionRowListener();
    }

    /**
     * Provides access to the singleton instance of the row listener.
     *
     * @return The active {@link TransactionRowListener} instance.
     */
    public static TransactionRowListener getInstance() {
        return InstanceHolder.TRANSACTION_ROW_LISTENER;
    }

    /**
     * Processes a collection of updated transaction rows and synchronizes
     * their status with the cache.
     * <p>
     * Iterates through each modified {@link Row} in the {@code OMPBTRANSACTION} table,
     * extracting the primary key and the {@code COMPLETION_STATUS} column to
     * update the cache state.
     * </p>
     *
     * @param rows A {@link Collection} of {@link Row} objects provided by the data access layer.
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        rows.forEach(row -> transactionCompletionStatusCache.updateCompletionStatus(
                row.getPKValue(),
                row.get(OMPBTRANSACTION.COMPLETION_STATUS)
        ));
    }
}