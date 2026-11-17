package com.tlc.marketplace.api.manager;

import com.tlc.marketplace.model.MarketplaceApp;
import com.tlc.marketplace.sql.resource.MKPAPPS;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages marketplace applications which means it provides specific marketplace-related
 * functionality for handling MarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public class MarketplaceManager extends AbstractMarketplaceManager {

    private static class Instance {
        private static final MarketplaceManager INSTANCE = new MarketplaceManager();
    }

    protected MarketplaceManager() {
    }

    /**
     * Gets the {@link MarketplaceManager} instance.
     *
     * @return {@link MarketplaceManager}.
     */
    public static MarketplaceManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@code Table}.
     */
    @Override
    public Table getTable() {
        return Table.get(MKPAPPS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique identifier of the marketplace entity.
     * @return {@code true} if the entity exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(final Long id) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));
        return adminDataStore.get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link MarketplaceApp}.
     */
    @Override
    public Collection<MarketplaceApp> getAll(final int start, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(table.getPKColumn(), null)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(MKPAPPS.NAME)));

        return adminDataStore.get(selectQuery).getRows(table).map(this::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique identifier of the marketplace entity.
     * @return The {@link MarketplaceApp}.
     */
    @Override
    public MarketplaceApp get(final Long id) {
        return Optional.ofNullable(adminDataStore.get(getTable(), id)).map(this::convertRowToModel).orElseGet(() -> null);
    }

    /**
     * {@inheritDoc}
     *
     * @param row Row that holds the information of model.
     * @return The converted entity.
     */
    private MarketplaceApp convertRowToModel(final Row row) {
        final MarketplaceApp marketPlaceApp = new MarketplaceApp();

        marketPlaceApp.setId(row.getPKValue());
        marketPlaceApp.setName(row.get(MKPAPPS.NAME));
        marketPlaceApp.setDisplayName(row.get(MKPAPPS.DISPLAY_NAME));
        marketPlaceApp.setDescription(row.get(MKPAPPS.DESCRIPTION));
        return marketPlaceApp;
    }
}