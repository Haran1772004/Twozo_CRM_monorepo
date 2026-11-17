package com.tlc.crm.plan.and.billing.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.plan.and.billing.handler.PlanExpirationUpdater;
import com.tlc.crm.plan.and.billing.internal.listener.PayerCreateEventListener;
import com.tlc.crm.plan.and.billing.internal.listener.TransactionRowListener;
import com.tlc.crm.plan.and.billing.internal.listener.UserRowListener;

import com.tlc.crm.plan.and.billing.sql.resource.OMPBTRANSACTION;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * <p>
 * Provides the service for plans and billings module actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SubscriptionServiceInitializer implements ServiceInitializer {

    private final UserRowListener userRowListener;
    private final WritableDataStore writableDataStore;
    private final PayerCreateEventListener payerCreateEventListener;
    private final PlanExpirationUpdater planExpirationUpdater;
    private final TransactionRowListener transactionRowListener;

    private SubscriptionServiceInitializer() {
        userRowListener = UserRowListener.getInstance();
        writableDataStore = SqlServiceProvider.get().getAdminDataStore();
        payerCreateEventListener = new PayerCreateEventListener();
        planExpirationUpdater = PlanExpirationUpdater.getInstance();
        transactionRowListener = TransactionRowListener.getInstance();
    }

    private static final class Instance {
        private static final SubscriptionServiceInitializer SUBSCRIPTION_SERVICE_INITIALIZER = new SubscriptionServiceInitializer();
    }

    public static SubscriptionServiceInitializer getInstance() {
        return Instance.SUBSCRIPTION_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preInitialize(final Configurations configurations) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        payerCreateEventListener.start(configurations);
        planExpirationUpdater.start(configurations);
        writableDataStore.registerNewEntryListener(Table.get(OMCRMUSER.TABLE), userRowListener);
        writableDataStore.registerUpdatedEntryListener(Table.get(OMPBTRANSACTION.TABLE), transactionRowListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        payerCreateEventListener.stop();
        planExpirationUpdater.stop();
    }
}
