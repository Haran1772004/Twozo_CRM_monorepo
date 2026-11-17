package com.tlc.crm.auto.assignment.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.auto.assignment.internal.listener.AssignmentRuleRowListener;
import com.tlc.crm.auto.assignment.sql.resource.OMASRASSIGNMENTRULE;
import com.tlc.crm.auto.assignment.cache.AutoAssignmentComparatorCache;
import com.tlc.crm.auto.assignment.internal.async.AutoAssignmentConsumerController;
import com.tlc.crm.auto.assignment.internal.event.AutoAssignmentEventListener;

import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

/**
 * An initializer that initializes the service which needs to be started.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentServiceInitializer implements ServiceInitializer {

    private final AutoAssignmentComparatorCache assignmentComparatorCache;
    private final AutoAssignmentConsumerController assignmentConsumerController;
    private final EventListenerRegistry eventListenerRegistry;
    private final AutoAssignmentEventListener assignmentEventListener;
    private final WritableDataStore writableDataStore;
    private final AssignmentRuleRowListener assignmentRuleRowListener;

    private AutoAssignmentServiceInitializer() {
        this.assignmentComparatorCache = AutoAssignmentComparatorCache.getInstance();
        this.assignmentConsumerController = new AutoAssignmentConsumerController();
        this.eventListenerRegistry = EventListenerRegistry.getInstance();
        this.assignmentEventListener = AutoAssignmentEventListener.getInstance();
        this.writableDataStore = DataStoreProvider.getWritableDataStore();
        this.assignmentRuleRowListener = AssignmentRuleRowListener.getInstance();
    }

    private static final class Instance {
        private static final AutoAssignmentServiceInitializer INITIALIZER = new AutoAssignmentServiceInitializer();
    }

    public static AutoAssignmentServiceInitializer getInstance() {
        return Instance.INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        final Table table = Table.get(OMASRASSIGNMENTRULE.TABLE);

        writableDataStore.registerDeletedEntryListener(table, assignmentRuleRowListener);
        writableDataStore.registerUpdatedEntryListener(table, assignmentRuleRowListener);
        eventListenerRegistry.registerPostEventListener(assignmentEventListener);
        assignmentConsumerController.start(configurations);
        assignmentComparatorCache.load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        assignmentConsumerController.stop();
        assignmentComparatorCache.clear();
    }

}