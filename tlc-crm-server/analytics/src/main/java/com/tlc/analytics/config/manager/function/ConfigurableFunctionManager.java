package com.tlc.analytics.config.manager.function;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANACONFIGURABLEFUNCTIONDETAILS;
import com.tlc.analytics.sql.resource.ANAPARENTCONFIGURABLEFUNCTIONDETAILS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Column;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * The {@link  ConfigurableFunctionManager} manages the configuration details for configurable functions
 * for  the analytics
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Accessors( fluent = true )
@RequiredArgsConstructor
public class ConfigurableFunctionManager extends AbstractAnalyticsConfigManager<ConfigurableFunctionDetail>
{

    private static class Instance
    {
        private static final ConfigurableFunctionManager INSTANCE = new ConfigurableFunctionManager();
    }

    public static ConfigurableFunctionManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    @Override
    protected @NonNull Class<ConfigurableFunctionDetail> getModelClass()
    {
        return ConfigurableFunctionDetail.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable()
    {
        return Table.get( ANACONFIGURABLEFUNCTIONDETAILS.TABLE );
    }

    /**
     * Fetches all {@link ConfigurableFunctionDetail} from the database.
     *
     * @return {@link Collection} of {@link ConfigurableFunctionDetail}, never null
     */
    public Collection<ConfigurableFunctionDetail> fetchAll() {
        final Table table = Table.get(ANACONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<ConfigurableFunctionDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildConfigurableFunction(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ConfigurableFunctionDetail from DB", result.size());
        return result;
    }

    /**
     * Fetches all {@link ParentConfigurableFunctionDetail} from the database.
     *
     * @return {@link Collection} of {@link ParentConfigurableFunctionDetail}, never null
     */
    public Collection<ParentConfigurableFunctionDetail> fetchAllParent() {
        final Table table = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildParentBaseQuery(null, null));
        final Collection<ParentConfigurableFunctionDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildParentConfigurableFunction(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ParentConfigurableFunctionDetail from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link ConfigurableFunctionDetail} from the database for the given column ID.
     *
     * @param columnId Represents the column identifier
     * @return {@link Collection} of {@link ConfigurableFunctionDetail}, never null
     */
    public Collection<ConfigurableFunctionDetail> fetchByColumnId(final Long columnId) {
        final Table table = Table.get(ANACONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID), columnId));
        final Collection<ConfigurableFunctionDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildConfigurableFunction(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ConfigurableFunctionDetail from DB for columnId [{}]", result.size(), columnId);
        return result;
    }

    /**
     * Fetches a single {@link ConfigurableFunctionDetail} from the database for the given ID.
     *
     * @param functionId Represents the entity identifier
     * @return {@link ConfigurableFunctionDetail} or null if not found
     */
    public ConfigurableFunctionDetail fetchByFunctionId(final Long functionId) {
        final Table table = Table.get(ANACONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getPKColumn(), functionId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildConfigurableFunction(row);
            }
        }

        LOGGER.info("CRM Analytics : No ConfigurableFunctionDetail found in DB for functionId [{}]", functionId);
        return null;
    }

    /**
     * Fetches the {@link Collection} of {@link ParentConfigurableFunctionDetail} from the database for the given column function ID.
     *
     * @param columnFunctionId Represents the column function identifier
     * @return {@link Collection} of {@link ParentConfigurableFunctionDetail}, never null
     */
    public Collection<ParentConfigurableFunctionDetail> fetchParentByColumnFunctionId(final Long columnFunctionId) {
        final Table table = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildParentBaseQuery(table.getColumn(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID), columnFunctionId));
        final Collection<ParentConfigurableFunctionDetail> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildParentConfigurableFunction(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] ParentConfigurableFunctionDetail from DB for columnFunctionId [{}]", result.size(), columnFunctionId);
        return result;
    }

    /**
     * Fetches a single {@link ParentConfigurableFunctionDetail} from the database for the given ID.
     *
     * @param parentFunctionId Represents the parent function identifier
     * @return {@link ParentConfigurableFunctionDetail} or null if not found
     */
    public ParentConfigurableFunctionDetail fetchParentByFunctionId(final Long parentFunctionId) {
        final Table table = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildParentBaseQuery(table.getPKColumn(), parentFunctionId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildParentConfigurableFunction(row);
            }
        }

        LOGGER.info("CRM Analytics : No ParentConfigurableFunctionDetail found in DB for parentFunctionId [{}]", parentFunctionId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the configurable function details table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(ANACONFIGURABLEFUNCTIONDETAILS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the parent configurable function details table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildParentBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link ConfigurableFunctionDetail} instance from the given row data.
     * </p>
     *
     * @param row The database row containing configurable function data
     * @return The constructed {@link ConfigurableFunctionDetail}
     */
    private ConfigurableFunctionDetail buildConfigurableFunction(final Row row) {
        return ConfigurableFunctionDetail.builder()
                .id(row.getPKValue())
                .columnId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.COLUMN_ID))
                .finalFunction(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FINAL_FUNCTION))
                .functionTypeId(row.get(ANACONFIGURABLEFUNCTIONDETAILS.FUNCTION_TYPE_ID))
                .build();
    }

    /**
     * <p>
     * Builds a {@link ParentConfigurableFunctionDetail} instance from the given row data.
     * </p>
     *
     * @param row The database row containing parent configurable function data
     * @return The constructed {@link ParentConfigurableFunctionDetail}
     */
    private ParentConfigurableFunctionDetail buildParentConfigurableFunction(final Row row) {
        return ParentConfigurableFunctionDetail.builder()
                .id(row.getPKValue())
                .columnFunctionId(row.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.COLUMN_FUNCTION_ID))
                .parentColumnId(row.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.PARENT_COLUMN_ID))
                .index(row.get(ANAPARENTCONFIGURABLEFUNCTIONDETAILS.INDEX))
                .build();
    }

    /**
     * Returns the {@link ReadableDataStore} instance for executing read queries.
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }
}
