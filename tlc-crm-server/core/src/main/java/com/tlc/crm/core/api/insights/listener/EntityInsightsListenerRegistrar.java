package com.tlc.crm.core.api.insights.listener;

import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;

/**
 * <p>
 * Registers DELETE listeners that listens for entity insights.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsListenerRegistrar {

    private EntityInsightsListenerRegistrar() {
    }

    public static void registerAll(final WritableDataStore writableDataStore) {

        final ActivityMappingListener activityMappingListener = ActivityMappingListener.getInstance();

        writableDataStore.registerDeletedEntryListener(Table.get(MACCONTACTACTIVITY.TABLE), activityMappingListener);
        writableDataStore.registerDeletedEntryListener(Table.get(MACCOMPANYACTIVITY.TABLE), activityMappingListener);
        writableDataStore.registerDeletedEntryListener(Table.get(MACDEALACTIVITY.TABLE), activityMappingListener);
        final EmailEntityMappingListener emailMappingListener = EmailEntityMappingListener.getInstance();

        writableDataStore.registerDeletedEntryListener(Table.get(OMECCONTACTCONVERSATIONS.TABLE), emailMappingListener);
        writableDataStore.registerDeletedEntryListener(Table.get(OMECCOMPANYCONVERSATIONS.TABLE), emailMappingListener);
        writableDataStore.registerDeletedEntryListener(Table.get(OMECDEALCONVERSATIONS.TABLE), emailMappingListener);

        writableDataStore.registerDeletedEntryListener(Table.get(MDODEAL.TABLE), DealListener.getInstance());
        final EntityDeleteListener entityDeleteListener = EntityDeleteListener.getInstance();

        writableDataStore.registerDeletedEntryListener(Table.get(MCOCONTACT.TABLE), entityDeleteListener);
        writableDataStore.registerDeletedEntryListener(Table.get(MCOCOMPANY.TABLE), entityDeleteListener);
        writableDataStore.registerDeletedEntryListener(Table.get(MDODEAL.TABLE), entityDeleteListener);
    }
}