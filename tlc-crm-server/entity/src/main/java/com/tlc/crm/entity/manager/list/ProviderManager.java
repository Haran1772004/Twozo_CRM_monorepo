package com.tlc.crm.entity.manager.list;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.sql.resource.METDATAPROVIDER;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link Provider} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ProviderManager {

    private final ReadableDataStore readableDataStore;

    private static class Instance {
        private static final ProviderManager INSTANCE = new ProviderManager();
    }

    private ProviderManager() {
        readableDataStore = DataStoreProvider.getReadableDataStore();
    }

    public static ProviderManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the {@link Table} of the provider details.
     * </p>
     *
     * @return METDataProvider table.
     */
    private Table getTable() {
        return Table.get(METDATAPROVIDER.TABLE);
    }

    /**
     * <p>
     * Fetches all the {@link Provider} entities are available in DB.
     * </p>
     *
     * @return Map of grouped provider.
     */
    public Map<Long, Pair<Long, Provider>> fetchAll() {
        final Table table = getTable();

        return readableDataStore.get(table, (WhereClause) null).getRows(table).collect(Collectors.toMap(row ->
                row.get(METDATAPROVIDER.SOURCE_ID), row -> Pair.of(row.getPKValue(), Provider.valueOf(row.get(METDATAPROVIDER.NAME)))));
    }

    /**
     * <p>
     * Gets the provider entry for the given source id.
     * </p>
     *
     * @param sourceId Represents the source id
     * @return The {@link Map.Entry} containing the key as provider id value as {@link Provider}.
     */
    public Pair<Long, Provider> fetchProvider(final Long sourceId) {
        final Table table = getTable();
        final Row row = readableDataStore.get(table,
                new WhereClause(Criteria.eq(table.getColumn(METDATAPROVIDER.SOURCE_ID), sourceId))).getRow(table);

        return Pair.of(row.getPKValue(), Provider.valueOf(row.get(METDATAPROVIDER.NAME)));
    }
}
