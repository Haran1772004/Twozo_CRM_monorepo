package com.tlc.crm.entity.forecastcategory.manager.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.DefaultForecastCategory;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manager class for handling {@link ForecastCategory} related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public class ForecastCategoryManagerImpl extends AbstractConfigManager<ForecastCategory> implements ForecastCategoryManager {

    private ForecastCategoryManagerImpl() {
    }

    private static class InstanceHolder {
        private static final ForecastCategoryManagerImpl INSTANCE = new ForecastCategoryManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link ForecastCategoryManagerImpl}
     * </p>
     *
     * @return The {@link ForecastCategoryManagerImpl}
     */
    public static ForecastCategoryManagerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The total count of forecast category records
     */
    @Override
    public long getTotalRecords(final long orgId) {
        return orgDataStore(orgId).get(CountQuery.get(getTable()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param limitConfig Represents the {@link LimitConfig}
     * @return A collection of {@link ForecastCategory}
     */
    @Override
    public Collection<ForecastCategory> get(final long orgId, final LimitConfig limitConfig) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMQFFORECASTCATEGORY.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
        selectQuery.setLimitClause(new LimitClause(limitConfig.start(), limitConfig.limit()));
        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertRowIntoForecastCategory).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the forecast category id
     */
    @Override
    public void updateDefaultCategory(final long orgId, final long id) {
        final Table table = getTable();
        final UpdateQuery unsetDefaultCategoryQuery = UpdateQuery.get(table);
        final UpdateQuery updateDefaultCategoryQuery = UpdateQuery.get(table);

        unsetDefaultCategoryQuery.addUpdateColumn(table.getColumn(OMQFFORECASTCATEGORY.IS_DEFAULT), false);
        unsetDefaultCategoryQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMQFFORECASTCATEGORY.IS_DEFAULT), true)));

        updateDefaultCategoryQuery.addUpdateColumn(table.getColumn(OMQFFORECASTCATEGORY.IS_DEFAULT), true);
        updateDefaultCategoryQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        orgDataStore(orgId).update(List.of(unsetDefaultCategoryQuery, updateDefaultCategoryQuery));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The default {@link ForecastCategory}
     */
    @Override
    public ForecastCategory getDefaultCategory(final Long orgId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMQFFORECASTCATEGORY.IS_DEFAULT), true)));
        final Row defaultCategoryRow = orgDataStore(orgId).get(selectQuery).getRow(table);

        return convertRowIntoForecastCategory(defaultCategoryRow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param names The collection of names to verify for existence
     * @return true if at least one name exists in the organization, otherwise false
     */
    @Override
    public boolean nameAlreadyExists(final long orgId, final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMQFFORECASTCATEGORY.NAME), names)));
        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of ids
     * @param names The collection of names to verify for existence
     * @return true if at least one name exists in the organization, otherwise false
     */
    @Override
    public boolean nameAlreadyExists(final long orgId, final Collection<Long> ids, final Collection<String> names) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMQFFORECASTCATEGORY.NAME), names))
                .and(Criteria.notIn(table.getPKColumn(), ids)));
        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return The commited forecast category id
     */
    @Override
    public Optional<Long> getCommitedCategoryId(final long orgId) {
        final Table forecastCategoryTable = getTable();
        final SelectQuery forecastCategorySelectQuery = SelectQuery.get(forecastCategoryTable);

        forecastCategorySelectQuery.addSelectClause(forecastCategoryTable.getPKColumn());
        forecastCategorySelectQuery.setWhereClause(new WhereClause(Criteria.eq(forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME),
                DefaultForecastCategory.COMMITTED.getName())));
        final Row row = orgDataStore(orgId).get(forecastCategorySelectQuery).getRow(forecastCategoryTable);

        return Optional.ofNullable(row).map(Row::getPKValue);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @return A collection of {@link ForecastCategory}
     */
    @Override
    public Collection<ForecastCategory> getAll(final long orgId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMQFFORECASTCATEGORY.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertRowIntoForecastCategory).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId the unique identifier of the organization
     * @param id    the unique identifier of the forecast category
     * @return an {@link Optional} containing the forecast category name if found,
     * otherwise {@link Optional#empty()}
     */
    @Override
    public Optional<String> getName(long orgId, long id) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMQFFORECASTCATEGORY.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        return Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(table)).map(row -> row.get(OMQFFORECASTCATEGORY.NAME));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param targetUserId  The ID of the user who will receive the records
     * @param sourceUserIds A collection of user IDs whose records will be transferred
     */
    @Override
    public void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {
        final Table table = getTable();
        final UpdateQuery createdByUpdatedQuery = UpdateQuery.get(table);
        final UpdateQuery updatedByUpdatedQuery = UpdateQuery.get(table);
        final Column createdByColumn = table.getColumn(OMQFFORECASTCATEGORY.CREATED_BY);
        final Column updatedByColumn = table.getColumn(OMQFFORECASTCATEGORY.UPDATED_BY);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        createdByUpdatedQuery.addUpdateColumn(createdByColumn, null);
        updatedByUpdatedQuery.addUpdateColumn(updatedByColumn, targetUserId);

        for (final Collection<Long> batchedUserIds : JavaUtil.splitCollection(sourceUserIds, NumericalConstant.TEN)) {
            createdByUpdatedQuery.setWhereClause(new WhereClause(Criteria.in(createdByColumn, batchedUserIds)));
            updatedByUpdatedQuery.setWhereClause(new WhereClause(Criteria.in(updatedByColumn, batchedUserIds)));

            orgDataStore.executeActions(List.of(createdByUpdatedQuery, updatedByUpdatedQuery));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The forecast category table
     */
    @Override
    public Table getTable() {
        return Table.get(OMQFFORECASTCATEGORY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents an organization id
     * @param forecastCategories Which contains the collection of {@link ForecastCategory}
     * @return The {@link ProcessOutcome}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<ForecastCategory> forecastCategories) {
        final Table table = getTable();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer sourceContainer = orgDataStore.get(table, forecastCategories.stream().map(ForecastCategory::id).collect(Collectors.toSet()));

        for (final ForecastCategory forecastCategory : forecastCategories) {

            try {
                final DataContainer targetContainer = DataContainer.create();

                loadForecastCategoryIntoContainer(false, forecastCategory, sourceContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(forecastCategory.id());
            } catch (final Exception exception) {
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.common.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, forecastCategory.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.common.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of forecast category ids
     * @return A collection of {@link ForecastCategory}
     */
    @Override
    public Collection<ForecastCategory> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertRowIntoForecastCategory).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of forecast category ids
     * @return A collection of {@link ForecastCategory}
     */
    @Override
    public Collection<ForecastCategory> partialGet(final Long orgId, final Collection<Long> ids) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param forecastCategory Represents the {@link ForecastCategory}
     * @return The {@link ForecastCategory}
     */
    @Override
    public ForecastCategory create(final ForecastCategory forecastCategory) {
        final DataContainer targetContainer = DataContainer.create();

        loadForecastCategoryIntoContainer(true, forecastCategory, null, targetContainer);
        orgDataStore(forecastCategory.orgId()).commitChanges(targetContainer);
        return forecastCategory;
    }

    /**
     * <p>
     * Loads the {@link ForecastCategory} details into the target container
     * </p>
     *
     * @param isCreateEvent    A flag which indicates whether the action is related to create or update event
     * @param forecastCategory Represents the {@link ForecastCategory}
     * @param sourceContainer  Holds the field values of existing {@link ForecastCategory}
     * @param targetContainer  Holds the updated information of field values
     */
    private void loadForecastCategoryIntoContainer(final boolean isCreateEvent, final ForecastCategory forecastCategory, final DataContainer sourceContainer,
                                                   final DataContainer targetContainer) {
        final Table table = getTable();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), forecastCategory.id())));

        if (row.isNewRow()) {
            row.setOrgId(forecastCategory.orgId());

            forecastCategory.setId(row.getPKValue());
            row.set(OMQFFORECASTCATEGORY.DISPLAY_NAME, forecastCategory.getDisplayName());
            row.set(OMQFFORECASTCATEGORY.CREATED_TIME, Instant.now().toEpochMilli());
            row.set(OMQFFORECASTCATEGORY.CONFIG, forecastCategory.buildConfigJson());
            row.set(OMQFFORECASTCATEGORY.CREATED_BY, forecastCategory.getCreatedBy().id());
        }
        row.set(OMQFFORECASTCATEGORY.UPDATED_TIME, Instant.now().toEpochMilli());
        row.set(OMQFFORECASTCATEGORY.UPDATED_BY, forecastCategory.getUpdatedBy().id());
        row.set(OMQFFORECASTCATEGORY.NAME, forecastCategory.getName());
        row.set(OMQFFORECASTCATEGORY.DESCRIPTION, forecastCategory.getDescription());
        row.set(OMQFFORECASTCATEGORY.IS_DEFAULT, forecastCategory.isDefault());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts the row to {@link ForecastCategory}
     * </p>
     *
     * @param row Represents the {@link ForecastCategory} row
     * @return The {@link ForecastCategory}
     */
    private ForecastCategory convertRowIntoForecastCategory(final Row row) {
        final ForecastCategory forecastCategory = new ForecastCategory(Json.object((String) row.get(OMQFFORECASTCATEGORY.CONFIG)));

        forecastCategory.setId(row.getPKValue());
        forecastCategory.setOrgId(row.getOrgId());
        forecastCategory.setName(row.get(OMQFFORECASTCATEGORY.NAME));
        forecastCategory.setDisplayName(row.get(OMQFFORECASTCATEGORY.DISPLAY_NAME));
        forecastCategory.setDescription(row.get(OMQFFORECASTCATEGORY.DESCRIPTION));
        forecastCategory.setDefault(row.get(OMQFFORECASTCATEGORY.IS_DEFAULT));

        return forecastCategory;
    }
}
