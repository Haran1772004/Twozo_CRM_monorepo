package com.tlc.crm.entity.currency.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.currency.api.model.CurrencyData;
import com.tlc.crm.entity.internal.status.CurrencyUnitErrorCodes;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.sql.resource.OMETBASECURRENCY;
import com.tlc.crm.entity.sql.resource.OMETCURRENCY;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Handles and facilitates db related operations for entities related to {@link CurrencyUnit}
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractConfigManager
 */
public class CurrencyUnitManager extends AbstractConfigManager<CurrencyUnit> {

    private static final String CURRENCY_ID = "CURRENCY_ID";

    private final EntityManagerHolder entityManagerHolder;

    private static class Instance {
        private static final CurrencyUnitManager INSTANCE = new CurrencyUnitManager();
    }

    private CurrencyUnitManager() {
        this.entityManagerHolder = EntityManagerHolder.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CurrencyUnitManager}.
     * </p>
     *
     * @return {@link CurrencyUnitManager}
     */
    public static CurrencyUnitManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Table} of currency
     */
    @Override
    public Table getTable() {
        return Table.get(OMETCURRENCY.TABLE);
    }

    /**
     * <p>
     * Gets the base currency table.
     * </p>
     *
     * @return {@link Table} of base currency
     */
    public Table getBaseCurrencyTable() {
        return Table.get(OMETBASECURRENCY.TABLE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId         Represents an organisation id.
     * @param currencyUnits Represents the {@link Collection} of {@link CurrencyUnit}
     * @return updated records
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<CurrencyUnit> currencyUnits) {
        final DataContainer existingCurrencyContainer = fetchCurrencyDataContainer(orgId, currencyUnits.stream().map(CurrencyUnit::id).toList());
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final ProcessOutcome processOutcome = new ProcessOutcome();

        for (final CurrencyUnit currencyUnit : currencyUnits) {
            final Long currencyId = currencyUnit.id();

            try {
                final DataContainer targetContainer = DataContainer.create();

                loadCurrencyUnitIntoContainer(false, currencyUnit, existingCurrencyContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(currencyId);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Currency update failed Org ID : {} ID : {} reason : {}", orgId, currencyId, errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), CommonErrorCodes.UPDATE_ACTION_FAILED.name(), currencyId.toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @param ids   Represents the {@link Collection} of currency ids
     * @return {@link Collection} of {@link CurrencyUnit}
     */
    @Override
    public Collection<CurrencyUnit> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = fetchCurrencyDataContainer(orgId, ids);
        final Collection<Row> rows = dataContainer.isEmpty() ? Collections.emptyList() : dataContainer.getRows(getTable()).toList();

        if (rows.isEmpty()) {
            throw ErrorCode.get(CurrencyUnitErrorCodes.CURRENCY_NOT_FOUND, "i18n.currency.unit.not.found");
        }

        return rows.stream().map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @param ids   Represents the {@link Collection} of currency ids
     * @return {@link Collection} of {@link CurrencyUnit}
     */
    @Override
    public Collection<CurrencyUnit> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table currencyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(currencyTable);

        selectQuery.addSelectClause(List.of(currencyTable.getPKColumn(), currencyTable.getColumn(OMETCURRENCY.CURRENCY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(currencyTable.getPKColumn(), ids)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> currencyRows = dataContainer.getRows(currencyTable);

        return Objects.isNull(currencyRows) ? Collections.emptyList() : currencyRows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param currencyUnit Represents the {@link CurrencyUnit}
     * @return {@link CurrencyUnit}
     */
    @Override
    public CurrencyUnit create(final CurrencyUnit currencyUnit) {
        final DataContainer targetContainer = DataContainer.create();

        loadCurrencyUnitIntoContainer(currencyUnit, targetContainer);
        orgDataStore(currencyUnit.orgId()).commitChanges(targetContainer);

        return currencyUnit;
    }

    /**
     * <p>
     * Checks if any currency unit ID from the provided collection is associated with the organization's settings;
     * returns true if found, false otherwise.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param currencyIds Represents the {@link Collection} of currency ids
     * @return {@code true} if all specified currency unit exist within the given organization; otherwise, {@code false}.
     */
    public boolean exists(final long orgId, final Collection<Long> currencyIds) {
        final Table currencyTable = getTable();
        final CountQuery countQuery = CountQuery.get(currencyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(currencyTable.getPKColumn(), currencyIds)));

        return orgDataStore(orgId).get(countQuery) == currencyIds.size();
    }

    /**
     * <p>
     * Checks the existence of specified currency IDs within the context of a given organization with respective currency unit.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param currencyIds Represents the {@link Collection} of currency ids
     * @return {@code true} if all specified currencies exist within the given organization; otherwise, {@code false}.
     */
    public boolean isCurrencyExists(final long orgId, final Set<Long> currencyIds) {
        final Table currencyTable = getTable();
        final CountQuery countQuery = CountQuery.get(currencyTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(currencyTable.getColumn(OMETCURRENCY.CURRENCY_ID), currencyIds)));

        return orgDataStore(orgId).get(countQuery) == currencyIds.size();
    }

    /**
     * <p>
     * Fetches the {@link CurrencyUnit} {@link DataContainer}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of currency ids
     * @return {@link DataContainer}
     */
    private DataContainer fetchCurrencyDataContainer(final Long orgId, final Collection<Long> ids) {
        final Table currencyTable = getTable();

        return orgDataStore(orgId).get(currencyTable, new WhereClause(Criteria.in(currencyTable.getPKColumn(), ids)));
    }

    /**
     * <p>
     * Gets the currency count for the given organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the currency count
     */
    public long getCurrencyCount(final long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * <p>
     * Loads the {@link CurrencyUnit} into {@link DataContainer}
     * </p>
     *
     * @param isCreateEvent             A flag which represents the event is created or updated
     * @param currencyUnit              Represents the {@link CurrencyUnit}
     * @param existingCurrencyContainer Holds the existing currency {@link DataContainer}
     * @param targetContainer           Holds the updated target {@link DataContainer}
     */
    public void loadCurrencyUnitIntoContainer(final boolean isCreateEvent, final CurrencyUnit currencyUnit,
                                              final DataContainer existingCurrencyContainer, final DataContainer targetContainer) {
        final Table currencyTable = getTable();
        final Row row = new Row(currencyTable);
        final Long orgId = currencyUnit.orgId();
        final long currentTime = Instant.now().toEpochMilli();

        currencyUnit.setId(row.getPKValue());
        row.setOrgId(orgId);
        row.set(OMETCURRENCY.CURRENCY_ID, currencyUnit.getCurrency().getId());
        row.set(OMETCURRENCY.CREATED_TIME, currentTime);
        row.set(OMETCURRENCY.SYMBOL, currencyUnit.getSymbol());
        row.set(OMETCURRENCY.IS_ENABLED, currencyUnit.isEnabled());
        row.set(OMETCURRENCY.CONVERSION_TYPE, currencyUnit.getConversionType().id());
        row.set(OMETCURRENCY.MODIFIED_TIME, currentTime);

        targetContainer.addNewRow(row);

        if (Objects.nonNull(existingCurrencyContainer)) {
            final Row baseCurrencyRow = existingCurrencyContainer.getRow(getBaseCurrencyTable());

            if (Objects.nonNull(baseCurrencyRow)) {
                baseCurrencyRow.set(OMETBASECURRENCY.IMMUTABLE, true);

                targetContainer.updateRow(baseCurrencyRow);
            }
        }
    }

    /**
     * <p>
     * Updates the status for the {@link CurrencyUnit} for the given currency id.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param currencyId Represents the currency id
     * @param isEnabled  A flag indicates whether the currency is enabled or not
     */
    public void updateStatus(final long orgId, final long currencyId, final boolean isEnabled) {
        final Table currencyTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(currencyTable);

        updateQuery.addUpdateColumn(currencyTable.getColumn(OMETCURRENCY.IS_ENABLED), isEnabled);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(currencyTable.getPKColumn(), currencyId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Converts the currency row to {@link CurrencyUnit}
     * </p>
     *
     * @param currencyRow Represents the {@link Row} of {@link CurrencyUnit}
     * @return {@link CurrencyUnit}
     */
    public CurrencyUnit convertRowToModel(final Row currencyRow) {
        final CurrencyUnit currencyUnit = new CurrencyUnit();

        currencyUnit.setCurrency(Currency.getCurrencyById(currencyRow.get(OMETBASECURRENCY.CURRENCY_ID)));
        currencyUnit.setEnabled(currencyRow.get(OMETCURRENCY.IS_ENABLED));
        currencyUnit.setConversionType(CurrencyUnit.ConversionType.get(currencyRow.get(OMETCURRENCY.CONVERSION_TYPE)));
        currencyUnit.setSymbol(currencyRow.get(OMETCURRENCY.SYMBOL));
        currencyUnit.setOrgId(currencyRow.getOrgId());
        currencyUnit.setId(currencyRow.getPKValue());

        return currencyUnit;
    }

    /**
     * <p>
     * Loads the {@link CurrencyUnit} into {@link DataContainer}.
     * </p>
     *
     * @param currencyUnit    Represents the {@link CurrencyUnit}
     * @param targetContainer Holds the updated records into the target {@link DataContainer}
     */
    private void loadCurrencyUnitIntoContainer(final CurrencyUnit currencyUnit, final DataContainer targetContainer) {
        final Table baseCurrencyTable = getBaseCurrencyTable();
        final DataContainer existingContainer = orgDataStore(currencyUnit.orgId()).get(baseCurrencyTable,
                new WhereClause(Criteria.eq(baseCurrencyTable.getColumn(OMETBASECURRENCY.IS_ACTIVE), true)));

        loadCurrencyUnitIntoContainer(true, currencyUnit, existingContainer, targetContainer);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link CurrencyData} for the given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link CurrencyData}
     */
    public Collection<CurrencyData> getCurrencies(final long orgId) {
        final Table currencyTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(currencyTable);

        selectQuery.addOrderByClause(List.of(new OrderByClause(currencyTable.getColumn(OMETCURRENCY.CREATED_TIME), OrderByClause.OrderType.ASCENDING)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(currencyTable).map(row -> new CurrencyData(false, row.get(OMETCURRENCY.IS_ENABLED),
                Currency.getCurrencyById(row.get(OMETCURRENCY.CURRENCY_ID)))).toList();
    }

    /**
     * <p>
     * Checks whether the given currency is exists in priceTag.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param currency Represents the {@link Currency}
     * @return true, if the currency exists in priceTag; otherwise false
     */
    public boolean isCurrencyInPriceTag(final long orgId, final Currency currency) {
        final Table priceTagTable = entityManagerHolder.getTable(StoreType.PRICE_TAGS);
        final CountQuery countQuery = CountQuery.get(priceTagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getColumn(CURRENCY_ID), currency.getId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}
