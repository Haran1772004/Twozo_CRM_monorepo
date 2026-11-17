package com.tlc.crm.core.features.api.quota.manager.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTSUMMARYDEAL;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAWONDEAL;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Manager contract for handling {@link UserQuota}
 *
 * @author kishore J
 * @version 1.0
 */
public class UserQuotaManager extends AbstractQuotaManager<UserQuota> {

    private final DealManager dealManager;

    private UserQuotaManager() {
        dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final UserQuotaManager INSTANCE = new UserQuotaManager();
    }

    /**
     * <p>
     * Gets the {@link UserQuotaManager}
     * </p>
     *
     * @return The {@link UserQuotaManager}
     */
    public static UserQuotaManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The user quota table
     */
    @Override
    public Table getTable() {
        return Table.get(OMQFUSERQUOTA.TABLE);
    }

    /**
     * Gets the user quota won deals table
     *
     * @return The user quota won deals table
     */
    private Table getUserQuotaWonDealTable() {
        return Table.get(OMQFUSERQUOTAWONDEAL.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userQuotas Which contains the collection {@link UserQuota}
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<UserQuota> userQuotas) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Set<Long> ids = userQuotas.stream().map(Quota::id).collect(Collectors.toSet());
        final DataContainer sourceContainer = getDataContainer(orgId, ids);

        for (final UserQuota userQuota : userQuotas) {
            final Long id = userQuota.id();
            final DataContainer targetContainer = DataContainer.create();

            loadQuotaIntoContainer(false, userQuota, targetContainer, sourceContainer);

            try {
                getOrgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(id);
            } catch (final ErrorCode errorCode) {
                failedEntities.add(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_FAILED, id.toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains collection of quota ids
     * @return The collection of {@link UserQuota}
     */
    @Override
    public Collection<UserQuota> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer quotaDataContainer = getDataContainer(orgId, ids);
        final Table quotaTable = getQuotaTable();
        final Table userQuotaTable = getTable();
        final Column quotaIdColumn = userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID);

        return quotaDataContainer.getRows(quotaTable).map(row -> {
            final UserQuota userQuota = new UserQuota();

            loadQuotaFromRow(row, quotaDataContainer, userQuota);
            final Row userQuotaRow = quotaDataContainer.getRow(userQuotaTable, new WhereClause(Criteria.eq(quotaIdColumn, userQuota.id())));

            if (Objects.nonNull(userQuotaRow)) {
                final Long userId = userQuotaRow.get(OMQFUSERQUOTA.USER_ID);

                userQuota.setOrgId(orgId);
                userQuota.setUserQuotaId(userQuotaRow.getPKValue());
                userQuota.setCrmUsers(List.of(userService.getUser(orgId, userId)));
                userQuota.setUserIds(List.of(userId));
            }
            return userQuota;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains collection of quota ids
     * @return The collection of {@link UserQuota}
     */
    @Override
    public Collection<UserQuota> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param userQuota Represents the {@link UserQuota}
     * @return The {@link Quota}
     */
    @Override
    public UserQuota create(final UserQuota userQuota) {
        final DataContainer targetContainer = DataContainer.create();
        final Collection<CrmUser> crmUsers = userQuota.getCrmUsers();
        final OrgDataStore orgDataStore = getOrgDataStore(userQuota.orgId());

        for (final CrmUser crmUser : crmUsers) {
            loadQuotaIntoContainer(true, userQuota, targetContainer, null);
            loadUserQuotaIntoContainer(userQuota, crmUser.id(), targetContainer);

            orgDataStore.commitChanges(targetContainer);
        }
        return userQuota;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of quota ids
     * @return The {@link Quota} data container
     */
    @Override
    protected DataContainer getDataContainer(final long orgId, final Collection<Long> ids) {
        final Table table = getQuotaTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Table userQuotaTable = getTable();
        final Column quotaPkColumn = table.getPKColumn();

        selectQuery.addSelectClause(table, userQuotaTable);
        selectQuery.addJoinClause(new JoinClause(quotaPkColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(quotaPkColumn, ids)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        loadDataContainer(orgId, ids, dataContainer);
        return dataContainer;
    }

    /**
     * <p>
     * Recalculates the attainment (won value or won count) for a given UserQuota.
     * </p>
     *
     * @param orgId   the organization identifier
     * @param quotaId the user quota identifier
     */
    public void recalculateAttainment(final long orgId, final long quotaId) {
        final Table userQuotaTable = getTable();
        final Table userQuotaWonDealTable = getUserQuotaWonDealTable();
        final DataContainer targetContainer = DataContainer.create();
        final Row userQuotaRow = getUserQuotaWonDealsDataContainer(orgId, new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId)))
                .getRow(userQuotaTable);

        if (Objects.isNull(userQuotaRow)) {
            return;
        }
        final UserQuota userQuota = get(orgId, quotaId);
        final Collection<Long> userIds = userQuota.getCrmUsers()
                .stream().map(TlcModel::id).collect(Collectors.toSet());
        final Collection<Long> pipelineIds = Optional.ofNullable(userQuota.getPipelines())
                .map(pipelines -> pipelines.stream().map(Pipeline::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final Collection<Long> territoryIds = Optional.ofNullable(userQuota.getTerritories())
                .map(territories -> territories.stream().map(Territory::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final Collection<Long> productIds = Optional.ofNullable(userQuota.getProducts())
                .map(products -> products.stream().map(Product::id).collect(Collectors.toSet())).orElseGet(Collections::emptySet);
        final Collection<Long> existingDealIds = getDealIds(orgId, quotaId);
        final Collection<Long> dealIds = dealManager.getDealIds(orgId,
                userIds, userQuota.getStartTime().getTimeStamp(), userQuota.getEndTime().getTimeStamp(),
                pipelineIds, territoryIds, productIds, existingDealIds);

        userQuotaRow.set(OMQFUSERQUOTA.ACTION_TIME, Instant.now().toEpochMilli());
        final Long userQuotaId = userQuotaRow.getPKValue();

        if (dealIds.isEmpty()) {
            targetContainer.updateRow(userQuotaRow);
            getOrgDataStore(orgId).commitChanges(targetContainer);
            return;
        }
        final QuotaMetric quotaMetric = userQuota.getQuotaMetric();

        if (quotaMetric.isDealValue()) {
            final Map<Long, Monetary> dealValueMap = dealManager.getDealValuesInBaseCurrency(orgId, dealIds)
                    .stream().collect(Collectors.toMap(TlcModel::id, Deal::getDealValueInBaseCurrency));
            double total = Optional.ofNullable((Double) userQuotaRow.get(OMQFUSERQUOTA.WON)).orElse(0.0);

            for (final Long dealId : dealIds) {
                final Monetary dealValue = dealValueMap.get(dealId);

                if (Objects.nonNull(dealValue)) {
                    total += dealValue.getValue().doubleValue();
                    final Row row = new Row(userQuotaWonDealTable);

                    row.setOrgId(orgId);
                    row.set(OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID, userQuotaId);
                    row.set(OMQFUSERQUOTAWONDEAL.DEAL_ID, dealId);
                    targetContainer.addNewRow(row);
                }
            }
            userQuotaRow.set(OMQFUSERQUOTA.WON, total);
        } else {
            final Long existingCount = Optional.ofNullable((Long) userQuotaRow.get(OMQFUSERQUOTA.WON)).orElse(0L);

            userQuotaRow.set(OMQFUSERQUOTA.WON, existingCount + dealIds.size());

            for (final Long dealId : dealIds) {
                final Row row = new Row(userQuotaWonDealTable);

                row.setOrgId(orgId);
                row.set(OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID, userQuotaId);
                row.set(OMQFUSERQUOTAWONDEAL.DEAL_ID, dealId);
                targetContainer.addNewRow(row);
            }
        }
        targetContainer.updateRow(userQuotaRow);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Checks whether the specified quota has been recalculated within the given time threshold.
     * </p>
     *
     * @param orgId   the organization ID
     * @param quotaId the quota ID
     * @param time    the threshold time in milliseconds (e.g., current time minus 3 hours)
     * @return true if the quota was recalculated after the given time; otherwise false
     */
    public boolean isQuotaRecentlyRecalculated(final long orgId, final long quotaId, final Long time) {
        final Table userQuotaTable = getTable();
        final CountQuery countQuery = CountQuery.get(userQuotaTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.ge(userQuotaTable.getColumn(OMQFUSERQUOTA.ACTION_TIME), time)));

        return getOrgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Changes the won deals for a user quota when the quota metric changes.
     * This method recalculates the attainment (won value or won count) based on the new quota metric type.
     * </p>
     *
     * @param orgId       the organization identifier
     * @param quotaId     the user quota identifier
     * @param quotaMetric the new quota metric to apply
     */
    public void changeQuotaMetricData(final long orgId, final long quotaId, final QuotaMetric quotaMetric) {
        final Table userQuotaTable = Table.get(OMQFUSERQUOTA.TABLE);
        final Table userQuotaForecastSummaryTable = Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final DataContainer sourceContainer = getUserQuotaWonDealsDataContainer(orgId,
                new WhereClause(Criteria.eq(userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), quotaId)));
        final DataContainer targetContainer = DataContainer.create();
        final Row userQuotaRow = sourceContainer.getRow(userQuotaTable);

        if (Objects.isNull(userQuotaRow)) {
            return;
        }
        final Collection<Long> existingDealIds = getDealIds(orgId, quotaId);

        if (existingDealIds.isEmpty()) {
            return;
        }

        if (quotaMetric.isDealValue()) {
            final Map<Long, Monetary> dealValueMap = dealManager.getDealValuesInBaseCurrency(orgId, existingDealIds)
                    .stream().collect(Collectors.toMap(TlcModel::id, Deal::getDealValueInBaseCurrency));
            double total = 0.0;

            for (final Long dealId : existingDealIds) {
                final Monetary dealValue = dealValueMap.get(dealId);

                if (Objects.nonNull(dealValue)) {
                    total += dealValue.getValue().doubleValue();
                }
            }
            userQuotaRow.set(OMQFUSERQUOTA.WON, total);
        } else {
            userQuotaRow.set(OMQFUSERQUOTA.WON, (long) existingDealIds.size());
        }
        final Long userQuotaId = userQuotaRow.getPKValue();
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final DataContainer forecastSummaryContainer = orgDataStore.get(userQuotaForecastSummaryTable,
                new WhereClause(Criteria.eq(userQuotaForecastSummaryTable.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID), userQuotaId)));
        final Set<Long> summaryIds = forecastSummaryContainer.getRows(userQuotaForecastSummaryTable)
                .map(row -> (Long) row.get(OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID))
                .collect(Collectors.toSet());

        if (!summaryIds.isEmpty()) {
            final Table forecastSummaryDealTable = Table.get(OMQFQUOTAFORECASTSUMMARYDEAL.TABLE);
            final SelectQuery forecastSummarySelectQuery = SelectQuery.get(forecastSummaryTable);
            final Column summaryPkColumn = forecastSummaryTable.getPKColumn();
            final Column summaryIdColumn = forecastSummaryDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID);

            forecastSummarySelectQuery.addSelectClause(forecastSummaryTable, forecastSummaryDealTable);
            forecastSummarySelectQuery.addJoinClause(new JoinClause(summaryPkColumn, summaryIdColumn));
            forecastSummarySelectQuery.setWhereClause(new WhereClause(Criteria.in(summaryPkColumn, summaryIds)));
            final DataContainer forcastSummaryDatacontainer = orgDataStore.get(forecastSummarySelectQuery);
            final Map<Long, Monetary> dealValueMap = quotaMetric.isDealValue()
                    ? getDealValueMap(orgId, forcastSummaryDatacontainer, forecastSummaryDealTable)
                    : Collections.emptyMap();

            forcastSummaryDatacontainer.indexRows(summaryIdColumn);

            forcastSummaryDatacontainer.getRows(forecastSummaryTable).forEach(forecastSummaryRow -> {
                final Long summaryId = forecastSummaryRow.getPKValue();
                final AtomicReference<Double> withoutWonValue = new AtomicReference<>(0.0);
                final AtomicLong withoutWonCount = new AtomicLong(0L);

                forcastSummaryDatacontainer.getIndexedRows(summaryIdColumn, summaryId)
                        .forEach(dealRow -> {
                            final Long dealId = dealRow.get(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID);

                            if (quotaMetric.isDealValue()) {
                                final Monetary dealValue = dealValueMap.get(dealId);

                                if (Objects.nonNull(dealValue)) {
                                    final double value = dealValue.getValue().doubleValue();
                                    withoutWonValue.set(withoutWonValue.get() + value);
                                }
                            } else {
                                withoutWonCount.incrementAndGet();
                            }
                        });

                if (quotaMetric.isDealValue()) {
                    forecastSummaryRow.set(OMQFFORECASTSUMMARY.WITHOUT_WON, withoutWonValue.get());
                } else {
                    forecastSummaryRow.set(OMQFFORECASTSUMMARY.WITHOUT_WON, withoutWonCount.get());
                }
                targetContainer.updateRow(forecastSummaryRow);
            });
        }
        targetContainer.updateRow(userQuotaRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Fetches deal values in base currency for deals present in the forecast summary data container.
     * </p>
     *
     * @param orgId                       the organization identifier
     * @param forcastSummaryDatacontainer the data container with forecast summary deal data
     * @param forecastSummaryDealTable    the forecast summary deal table
     * @return a map of deal ID to Monetary value
     */
    private Map<Long, Monetary> getDealValueMap(final Long orgId, final DataContainer forcastSummaryDatacontainer,
                                                final Table forecastSummaryDealTable) {
        final Set<Long> dealIds = forcastSummaryDatacontainer.getRows(forecastSummaryDealTable)
                .map(row -> (Long) row.get(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID))
                .collect(Collectors.toSet());

        if (dealIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dealManager.getDealValuesInBaseCurrency(orgId, dealIds)
                .stream().collect(Collectors.toMap(TlcModel::id, Deal::getDealValueInBaseCurrency));
    }

    /**
     * Retrieves all deal IDs already associated with the given UserQuota.
     *
     * @param orgId the organization identifier
     * @param id    the user quota identifier
     * @return a collection of deal IDs linked to the UserQuota
     */
    private Collection<Long> getDealIds(final Long orgId, final Long id) {
        final Table userQuotaWonDealsTable = getUserQuotaWonDealTable();
        final Table table = getTable();
        final DataContainer dataContainer = getUserQuotaWonDealsDataContainer(orgId,
                new WhereClause(Criteria.eq(table.getColumn(OMQFUSERQUOTA.QUOTA_ID), id)));

        return dataContainer.getRows(userQuotaWonDealsTable).map(row -> (Long) row.get(OMQFUSERQUOTAWONDEAL.DEAL_ID))
                .collect(Collectors.toSet());
    }

    /**
     * Fetches a DataContainer containing UserQuota and its associated
     * UserQuotaWonDeal rows based on the provided where clause.
     *
     * @param orgId       the organization identifier
     * @param whereClause the filtering condition for the query
     * @return a populated DataContainer containing matching records
     */
    private DataContainer getUserQuotaWonDealsDataContainer(final Long orgId, final WhereClause whereClause) {
        final Table userQuotaWonDealsTable = getUserQuotaWonDealTable();
        final Table table = getTable();
        final Table quotaTable = getQuotaTable();
        final SelectQuery selectQuery = SelectQuery.get(quotaTable);

        selectQuery.addJoinClause(List.of(new JoinClause(quotaTable.getPKColumn(), table.getColumn(OMQFUSERQUOTA.QUOTA_ID)),
                new JoinClause(table.getPKColumn(), userQuotaWonDealsTable.getColumn(OMQFUSERQUOTAWONDEAL.USER_QUOTA_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addSelectClause(table, userQuotaWonDealsTable);
        selectQuery.setWhereClause(whereClause);

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the user quota of the given {@link UserQuota} into the target container.
     * </p>
     *
     * @param userQuota       Represents the {@link UserQuota}
     * @param userId          Represents an user id
     * @param targetContainer The container to load data into
     */
    private void loadUserQuotaIntoContainer(final UserQuota userQuota, final Long userId, final DataContainer targetContainer) {
        final Table userQuotaTable = getTable();
        final Row row = new Row(userQuotaTable);

        row.set(OMQFUSERQUOTA.USER_ID, userId);
        row.set(OMQFUSERQUOTA.QUOTA_ID, userQuota.id());
        row.setOrgId(userQuota.orgId());

        targetContainer.addNewRow(row);
    }
}
