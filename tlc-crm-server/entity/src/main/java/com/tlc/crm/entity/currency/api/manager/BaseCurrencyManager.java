package com.tlc.crm.entity.currency.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.sql.resource.OMETBASECURRENCY;
import com.tlc.crm.entity.sql.resource.OMETCURRENCY;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Handles and facilitates db related operations for entities related to {@link BaseCurrency}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractReadableConfigManager
 */
public class BaseCurrencyManager extends AbstractReadableConfigManager<BaseCurrency> {

    private final CurrencyUnitManager currencyUnitManager;

    private static class Instance {
        private static final BaseCurrencyManager INSTANCE = new BaseCurrencyManager();
    }

    private BaseCurrencyManager() {
        this.currencyUnitManager = CurrencyUnitManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link BaseCurrencyManager}
     * </p>
     *
     * @return {@link BaseCurrencyManager}
     */
    public static BaseCurrencyManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Table} of base currency
     */
    @Override
    public Table getTable() {
        return Table.get(OMETBASECURRENCY.TABLE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param row Represents the {@link Row} that holds the information of model
     * @return {@link BaseCurrency}
     */
    @Override
    public BaseCurrency convertRowToModel(final Row row) {
        final BaseCurrency baseCurrency = new BaseCurrency();

        baseCurrency.setId(row.getPKValue());
        baseCurrency.setOrgId(row.getOrgId());
        baseCurrency.setDefaultCurrency(Currency.getCurrencyById(row.get(OMETBASECURRENCY.CURRENCY_ID)));
        baseCurrency.setImmutable(row.get(OMETBASECURRENCY.IMMUTABLE));
        baseCurrency.setDecimalPoint(BaseCurrency.DecimalPoint.get(row.get(OMETBASECURRENCY.DECIMAL_POINT_ID)));

        return baseCurrency;
    }

    /**
     * <p>
     * Gets the {@link BaseCurrency} for the given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link BaseCurrency}
     */
    public BaseCurrency getBaseCurrency(final long orgId) {
        final Table table = getTable();
        final Table currencyTable = Table.get(OMETCURRENCY.TABLE);
        final Column baseCurrencyOrgIdColumn = table.getColumn(OMETBASECURRENCY.ORG_ID);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(baseCurrencyOrgIdColumn,
                currencyTable.getColumn(OMETCURRENCY.ORG_ID), JoinClause.JoinType.LEFT));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));

        return convertRowToModel(row, dataContainer);
    }

    /**
     * <p>
     * Gets the {@link BaseCurrency} for the given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @param start Represents the lower bound of currencies to be fetched
     * @param limit Represents the upper bound of currencies to be fetched
     * @return {@link BaseCurrency}
     */
    public BaseCurrency getBaseCurrency(final long orgId, final int start, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = getSelectQuery(orgId, table);

        selectQuery.setLimitClause(new LimitClause(start, limit));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return convertRowToModel(dataContainer.getRow(table), dataContainer);
    }

    /**
     * <p>
     * Checks whether the base currency exists or not.
     * </p>
     *
     * @param orgId           Represents an organization
     * @param baseCurrencyIds Represents the {@link Collection} of base currency ids
     * @return true, if the base currency exists; otherwise false
     */
    public boolean exists(final long orgId, final Set<Long> baseCurrencyIds) {
        final Table baseCurrencyTable = getTable();
        final CountQuery countQuery = CountQuery.get(baseCurrencyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(baseCurrencyTable.getPKColumn(), baseCurrencyIds)));

        return orgDataStore(orgId).get(countQuery) == baseCurrencyIds.size();
    }

    /**
     * <p>
     * Checks whether the org exists for the base currency.
     * </p>
     *
     * @param orgId Represents an organization
     * @return true, if the org exists for the base currency; otherwise false
     */
    public boolean exists(final long orgId) {
        final Table baseCurrencyTable = getTable();
        final CountQuery countQuery = CountQuery.get(baseCurrencyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.ORG_ID), orgId))
                .and(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the {@link BaseCurrency} based on the provided details.
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param id                  Represents the currency id or decimalPoint id based on the hasCurrency flag
     * @param needPrecisionUpdate A flag that represents whether it needs to update the decimal point or not
     */
    public void updateBaseCurrencyDetails(final long orgId, final short id, final boolean needPrecisionUpdate) {
        final Table baseCurrencyTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(baseCurrencyTable);

        if (needPrecisionUpdate) {
            updateQuery.addUpdateColumn(baseCurrencyTable.getColumn(OMETBASECURRENCY.DECIMAL_POINT_ID), id);
            updateQuery.addUpdateColumn(baseCurrencyTable.getColumn(OMETBASECURRENCY.MODIFIED_TIME), Instant.now().toEpochMilli());
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.ORG_ID), orgId))
                    .and(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));

            orgDataStore(orgId).update(updateQuery);
        } else {
            updateBaseCurrencyIntoContainer(orgId, id, baseCurrencyTable);
        }
    }

    /**
     * <p>
     * Updates the base currency into the target container.
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param currencyId        Represents the currency id
     * @param baseCurrencyTable Represents the base currency table
     */
    private void updateBaseCurrencyIntoContainer(final long orgId, final short currencyId, final Table baseCurrencyTable) {
        final DataContainer dataContainer = orgDataStore(orgId).get(baseCurrencyTable, (WhereClause) null);
        final Row existingActiveRow = dataContainer.getRow(baseCurrencyTable,
                new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));
        final DataContainer targetContainer = DataContainer.create();
        final Row existingCurrencyRow = dataContainer.getRow(baseCurrencyTable,
                new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.CURRENCY_ID), currencyId)));
        final Row row = Objects.isNull(existingCurrencyRow) ? new Row(getTable()) : existingCurrencyRow;
        final long currentTime = Instant.now().toEpochMilli();

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMETBASECURRENCY.CURRENCY_ID, currencyId);
            row.set(OMETBASECURRENCY.IMMUTABLE, false);
        }
        row.set(OMETBASECURRENCY.DECIMAL_POINT_ID, existingActiveRow.get(OMETBASECURRENCY.DECIMAL_POINT_ID));
        row.set(OMETBASECURRENCY.CREATED_TIME, currentTime);
        row.set(OMETBASECURRENCY.MODIFIED_TIME, currentTime);
        row.set(OMETBASECURRENCY.IS_ACTIVE, true);
        existingActiveRow.set(OMETBASECURRENCY.IS_ACTIVE, false);

        targetContainer.updateRow(row);
        targetContainer.updateRow(existingActiveRow);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Creates the {@link BaseCurrency}.
     * </p>
     *
     * @param baseCurrency Represents the {@link BaseCurrency}
     */
    public void create(final BaseCurrency baseCurrency) {
        final DataContainer targetContainer = DataContainer.create();
        final Row row = new Row(getTable());
        final Long orgId = baseCurrency.orgId();
        final long currentTime = Instant.now().toEpochMilli();

        row.setOrgId(orgId);
        row.set(OMETBASECURRENCY.CURRENCY_ID, baseCurrency.getDefaultCurrency().getId());
        row.set(OMETBASECURRENCY.IMMUTABLE, baseCurrency.isImmutable());
        row.set(OMETBASECURRENCY.DECIMAL_POINT_ID, baseCurrency.getDecimalPoint().id());
        row.set(OMETBASECURRENCY.CREATED_TIME, currentTime);
        row.set(OMETBASECURRENCY.MODIFIED_TIME, currentTime);
        row.set(OMETBASECURRENCY.IS_ACTIVE, true);

        baseCurrency.setId(row.getPKValue());
        targetContainer.addNewRow(row);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Loads the base currency information for the given organization into the specified data container.
     *
     * @param orgId           The unique identifier of the organization.
     * @param baseCurrency    The base currency details to be loaded.
     * @param targetContainer The container where the base currency data will be stored.
     */
    public void createBaseCurrency(final long orgId, final BaseCurrency baseCurrency, final DataContainer targetContainer) {
        final Row row = new Row(getTable());
        final long currentTime = Instant.now().toEpochMilli();

        row.setOrgId(orgId);
        row.set(OMETBASECURRENCY.CURRENCY_ID, baseCurrency.getDefaultCurrency().getId());
        row.set(OMETBASECURRENCY.IMMUTABLE, baseCurrency.isImmutable());
        row.set(OMETBASECURRENCY.DECIMAL_POINT_ID, baseCurrency.getDecimalPoint().id());
        row.set(OMETBASECURRENCY.CREATED_TIME, currentTime);
        row.set(OMETBASECURRENCY.MODIFIED_TIME, currentTime);
        row.set(OMETBASECURRENCY.IS_ACTIVE, true);

        baseCurrency.setId(row.getPKValue());
        targetContainer.addNewRow(row);
    }

    /**
     * <p>
     * Checks whether the base currency is immutable or not.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return true, if the base currency is immutable; otherwise false
     */
    public boolean isImmutable(final long orgId) {
        final Table table = getTable();

        return orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMETBASECURRENCY.IMMUTABLE), true))).size(table) > 0;
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} for the base currency.
     * </p>
     *
     * @param baseCurrencyTable Represents the {@link Table} of base currency
     * @return {@link SelectQuery}
     */
    private SelectQuery getSelectQuery(final long orgId, final Table baseCurrencyTable) {
        final Table currencyTable = Table.get(OMETCURRENCY.TABLE);
        final Column baseCurrencyOrgIdColumn = baseCurrencyTable.getColumn(OMETBASECURRENCY.ORG_ID);
        final SelectQuery selectQuery = SelectQuery.get(baseCurrencyTable);

        selectQuery.addJoinClause(new JoinClause(baseCurrencyOrgIdColumn,
                currencyTable.getColumn(OMETCURRENCY.ORG_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(baseCurrencyOrgIdColumn, orgId))
                .and(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));
        selectQuery.addOrderByClause(List.of(new OrderByClause(currencyTable.getColumn(OMETCURRENCY.IS_ENABLED), OrderByClause.OrderType.DESCENDING),
                new OrderByClause(currencyTable.getColumn(OMETCURRENCY.CREATED_TIME), OrderByClause.OrderType.ASCENDING)));

        return selectQuery;
    }

    /**
     * <p>
     * Converts the currency row to {@link BaseCurrency}.
     * </p>
     *
     * @param row           Represents the {@link Row} that holds the information of model
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link BaseCurrency}
     */
    private BaseCurrency convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table currencyTable = Table.get(OMETCURRENCY.TABLE);
        final BaseCurrency baseCurrency = new BaseCurrency();
        final Collection<CurrencyUnit> currencyUnits = new ArrayList<>();

        if (Objects.nonNull(row)) {
            baseCurrency.setId(row.getPKValue());
            baseCurrency.setOrgId(row.getOrgId());
            baseCurrency.setDefaultCurrency(Currency.getCurrencyById(row.get(OMETBASECURRENCY.CURRENCY_ID)));
            baseCurrency.setImmutable(row.get(OMETBASECURRENCY.IMMUTABLE));
            baseCurrency.setDecimalPoint(BaseCurrency.DecimalPoint.get(row.get(OMETBASECURRENCY.DECIMAL_POINT_ID)));

            final Collection<Row> currencyRows = dataContainer.getRows(currencyTable).toList();

            if (!currencyRows.isEmpty()) {

                for (final Row currencyRow : currencyRows) {
                    currencyUnits.add(currencyUnitManager.convertRowToModel(currencyRow));
                }
            }
        }
        baseCurrency.setCurrencyUnits(currencyUnits);

        return baseCurrency;
    }

    /**
     * <p>
     * Checks whether the given currency id already exists or not.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param currencyId Represents the currency id
     * @return true, if the currency exists;otherwise false
     */
    public boolean currencyExists(final long orgId, final short currencyId) {
        final Table baseCurrencyTable = getTable();
        final CountQuery countQuery = CountQuery.get(baseCurrencyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.ORG_ID), orgId))
                .and(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.CURRENCY_ID), currencyId))
                .and(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the in-active base currencies for an given organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link Collection} of inactive currencies
     */
    public Collection<Currency> getInactiveBaseCurrencies(final long orgId) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMETBASECURRENCY.IS_ACTIVE), false)));
        final Collection<Currency> currencies = new HashSet<>();
        final Collection<Row> rows = dataContainer.getRows(table).toList();

        if (!rows.isEmpty()) {

            for (final Row row : rows) {
                currencies.add(Currency.getCurrencyById(row.get(OMETBASECURRENCY.CURRENCY_ID)));
            }
        }

        return currencies;
    }
}
