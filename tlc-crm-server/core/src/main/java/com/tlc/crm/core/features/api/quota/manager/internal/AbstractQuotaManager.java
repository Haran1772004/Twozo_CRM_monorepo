package com.tlc.crm.core.features.api.quota.manager.internal;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaTimePeriod;
import com.tlc.crm.core.features.api.quota.model.QuotaTimeRange;
import com.tlc.crm.core.features.api.quota.model.QuotaType;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.core.sql.resource.OMQFPIPELINEQUOTA;
import com.tlc.crm.core.sql.resource.OMQFPRODUCTQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTATERRITORY;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Manager contract for handling {@link Quota}
 *
 * @param <Q> the concrete {@link Quota} type managed by this manager
 * @author kishore J
 * @version 1.0
 */
public abstract class AbstractQuotaManager<Q extends Quota> extends AbstractConfigManager<Q> implements ConfigManager<Q> {

    protected final UserService userService;
    private final TerritoryManager territoryManager;
    private final PipelineManager pipelineManager;
    private final ProductManager productManager;
    private final VariationManager variationManager;

    protected AbstractQuotaManager() {
        territoryManager = TerritoryManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        productManager = ProductManager.getInstance();
        variationManager = VariationManager.getInstance();
        userService = UserService.getInstance();
    }

    /**
     * <p>
     * Gets the quota table
     * </p>
     *
     * @return The quota table
     */
    protected Table getQuotaTable() {
        return Table.get(OMQFQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the pipeline quota table
     * </p>
     *
     * @return The pipeline quota table
     */
    private Table getPipelineQuotaTable() {
        return Table.get(OMQFPIPELINEQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the product quota table
     * </p>
     *
     * @return The product quota table
     */
    private Table getProductQuotaTable() {
        return Table.get(OMQFPRODUCTQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the territory quota table
     * </p>
     *
     * @return The territory quota table
     */
    private Table getTerritoryQuotaTable() {
        return Table.get(OMQFQUOTATERRITORY.TABLE);
    }

    /**
     * <p>
     * Loads the quota of the given quota into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param quota           Represents the {@link Quota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    protected void loadQuotaIntoContainer(final boolean isCreateEvent, final Q quota, final DataContainer targetContainer, final DataContainer sourceContainer) {
        final Table quotaTable = getQuotaTable();
        final Row quotaRow = isCreateEvent ? new Row(quotaTable) : sourceContainer.getRow(quotaTable, quota.id());

        if (quotaRow.isNewRow()) {
            quota.setId(quotaRow.getPKValue());
            quotaRow.setOrgId(quota.orgId());
            quotaRow.set(OMQFQUOTA.CREATED_TIME, Instant.now().toEpochMilli());
            quotaRow.set(OMQFQUOTA.CREATED_BY, quota.getCreatedBy().id());
        }
        quotaRow.set(OMQFQUOTA.START_TIME, quota.getStartTime());
        quotaRow.set(OMQFQUOTA.END_TIME, quota.getEndTime());
        quotaRow.set(OMQFQUOTA.UPDATED_TIME, Instant.now().toEpochMilli());
        quotaRow.set(OMQFQUOTA.UPDATED_BY, quota.getUpdatedBy().id());
        quotaRow.set(OMQFQUOTA.TIME_PERIOD, quota.getQuotaTimePeriod().getId());
        quotaRow.set(OMQFQUOTA.TIME_RANGE, quota.getQuotaTimeRange().getId());
        quotaRow.set(OMQFQUOTA.TYPE, quota.getQuotaType().getId());
        quotaRow.set(OMQFQUOTA.TARGET, quota.getTarget());
        quotaRow.set(OMQFQUOTA.METRIC, quota.getQuotaMetric().getId());

        loadQuotaTerritoryIntoContainer(isCreateEvent, quota, targetContainer, sourceContainer);
        loadQuotaPipelineIntoContainer(isCreateEvent, quota, targetContainer, sourceContainer);
        loadQuotaProductIntoContainer(isCreateEvent, quota, targetContainer, sourceContainer);
        loadQuotaProductVariationIntoContainer(isCreateEvent, quota, targetContainer, sourceContainer);

        targetContainer.updateRow(quotaRow);
    }

    /**
     * <p>
     * Loads the territories of the given quota into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param quota           Represents the {@link Quota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    private void loadQuotaTerritoryIntoContainer(final boolean isCreateEvent, final Q quota, final DataContainer targetContainer,
                                                 final DataContainer sourceContainer) {
        final Collection<Territory> territories = quota.getTerritories();
        final Table territoryQuotaTable = getTerritoryQuotaTable();
        final Long id = quota.id();

        if (Objects.isNull(territories)) {

            if (!isCreateEvent) {
                final Stream<Row> quotaTerritoryRows = sourceContainer.getRows(territoryQuotaTable,
                        new WhereClause(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), id)));

                quotaTerritoryRows.forEach(targetContainer::deleteRow);
            }
            return;
        }
        final Long orgId = quota.orgId();
        final Set<Long> territoryIds = new HashSet<>(territories.size());

        for (final Territory territory : territories) {
            final Long territoryId = territory.id();
            final Row territoryRow;

            if (isCreateEvent) {
                territoryRow = new Row(territoryQuotaTable);
            } else {
                final Row existingTerritoryRow = sourceContainer.getRow(territoryQuotaTable, new WhereClause(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), id))
                        .and(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.TERRITORY_ID), territoryId)));

                territoryRow = Objects.isNull(existingTerritoryRow) ? new Row(territoryQuotaTable) : existingTerritoryRow;
            }

            if (territoryRow.isNewRow()) {
                territoryRow.setOrgId(orgId);
            }
            territoryRow.set(OMQFQUOTATERRITORY.TERRITORY_ID, territoryId);
            territoryRow.set(OMQFQUOTATERRITORY.QUOTA_ID, id);
            targetContainer.updateRow(territoryRow);
            territoryIds.add(territoryId);
        }

        if (!isCreateEvent && !territoryIds.isEmpty()) {
            final Stream<Row> quotaTerritoryRows = sourceContainer.getRows(territoryQuotaTable,
                    new WhereClause(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), id))
                            .and(Criteria.notIn(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.TERRITORY_ID), territoryIds)));

            quotaTerritoryRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the pipelines of the given quota into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param quota           Represents the {@link Quota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    private void loadQuotaPipelineIntoContainer(final boolean isCreateEvent, final Q quota, final DataContainer targetContainer,
                                                final DataContainer sourceContainer) {
        final Collection<Pipeline> pipelines = quota.getPipelines();
        final Table pipelineQuotaTable = getPipelineQuotaTable();
        final Long id = quota.id();

        if (Objects.isNull(pipelines)) {

            if (!isCreateEvent) {
                final Stream<Row> quotaPipelineRows = sourceContainer.getRows(pipelineQuotaTable,
                        new WhereClause(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), id)));

                quotaPipelineRows.forEach(targetContainer::deleteRow);
            }
            return;
        }
        final Long orgId = quota.orgId();
        final Set<Long> pipelineIds = new HashSet<>(pipelines.size());

        for (final Pipeline pipeline : pipelines) {
            final Long pipelineId = pipeline.id();
            final Row pipelineRow;

            if (isCreateEvent) {
                pipelineRow = new Row(pipelineQuotaTable);
            } else {
                final Row existingPipelineRow = sourceContainer.getRow(pipelineQuotaTable, new WhereClause(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), id))
                        .and(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.PIPELINE_ID), pipelineId)));

                pipelineRow = Objects.isNull(existingPipelineRow) ? new Row(pipelineQuotaTable) : existingPipelineRow;
            }

            if (pipelineRow.isNewRow()) {
                pipelineRow.setOrgId(orgId);
            }
            pipelineRow.set(OMQFPIPELINEQUOTA.PIPELINE_ID, pipelineId);
            pipelineRow.set(OMQFPIPELINEQUOTA.QUOTA_ID, id);
            targetContainer.updateRow(pipelineRow);
            pipelineIds.add(pipelineId);
        }

        if (!isCreateEvent && !pipelineIds.isEmpty()) {
            final Stream<Row> quotaPipelineRows = sourceContainer.getRows(pipelineQuotaTable,
                    new WhereClause(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), id))
                            .and(Criteria.notIn(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.PIPELINE_ID), pipelineIds)));

            quotaPipelineRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the products of the given quota into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether this is a create or update operation
     * @param quota           Represents the {@link Quota}
     * @param targetContainer The container to load data into
     * @param sourceContainer The container for existing data
     */
    private void loadQuotaProductIntoContainer(final boolean isCreateEvent, final Quota quota, final DataContainer targetContainer,
                                               final DataContainer sourceContainer) {
        final Collection<Product> products = quota.getProducts();

        if (Objects.isNull(products) && isCreateEvent) {
            return;
        }
        final Table productQuotaTable = getProductQuotaTable();
        final Long id = quota.id();
        final Long orgId = quota.orgId();
        final WhereClause quotaIdCriteria = new WhereClause(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), id));
        final Collection<Long> productIds = new HashSet<>();

        if (Objects.nonNull(products)) {

            for (final Product product : products) {
                final Long productId = product.id();
                final Row productRow;

                if (isCreateEvent) {
                    productRow = new Row(productQuotaTable);
                } else {
                    final WhereClause existingProductRowCriteria = quotaIdCriteria
                            .and(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.PRODUCT_ID), productId));
                    final Row existingProductRow = sourceContainer.getRow(productQuotaTable, existingProductRowCriteria);

                    productRow = Objects.isNull(existingProductRow) ? new Row(productQuotaTable) : existingProductRow;
                }

                if (productRow.isNewRow()) {
                    productRow.setOrgId(orgId);
                }
                productRow.set(OMQFPRODUCTQUOTA.PRODUCT_ID, productId);
                productRow.set(OMQFPRODUCTQUOTA.VARIATION_ID, null);
                productRow.set(OMQFPRODUCTQUOTA.QUOTA_ID, id);
                targetContainer.updateRow(productRow);
                productIds.add(productId);
            }
        }

        if (!isCreateEvent) {
            final WhereClause productRowCriteria = quotaIdCriteria.and(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), null));
            final WhereClause staleProductRowCriteria = productIds.isEmpty() ? productRowCriteria
                    : productRowCriteria.and(Criteria.notIn(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.PRODUCT_ID), productIds));
            final Stream<Row> quotaProductRows = sourceContainer.getRows(productQuotaTable, staleProductRowCriteria);

            quotaProductRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * Loads the selected product variations of the given quota into the target container.
     * Whole-product rows are handled separately.
     *
     * @param isCreateEvent   whether this is a create or update operation
     * @param quota            the quota
     * @param targetContainer  the target container
     * @param sourceContainer  the source container containing existing data
     */
    private void loadQuotaProductVariationIntoContainer(final boolean isCreateEvent, final Quota quota, final DataContainer targetContainer,
                                                        final DataContainer sourceContainer) {
        final Collection<Variation> variations = quota.getVariations();

        if (Objects.isNull(variations) && isCreateEvent) {
            return;
        }
        final Table productQuotaTable = getProductQuotaTable();
        final Long id = quota.id();
        final Long orgId = quota.orgId();
        final WhereClause quotaIdCriteria = new WhereClause(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), id));
        final Collection<Long> variationIds = new HashSet<>();

        if (Objects.nonNull(variations)) {

            for (final Variation variation : variations) {
                final Long variationId = variation.id();
                final Long productId = variation.getProductId();

                if (Objects.isNull(variationId) || Objects.isNull(productId)) {
                    continue;
                }
                final Row variationRow;

                if (isCreateEvent) {
                    variationRow = new Row(productQuotaTable);
                } else {
                    final WhereClause existingVariationRowCriteria = quotaIdCriteria
                            .and(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), variationId));
                    final Row existingVariationRow = sourceContainer.getRow(productQuotaTable, existingVariationRowCriteria);

                    variationRow = Objects.isNull(existingVariationRow) ? new Row(productQuotaTable) : existingVariationRow;
                }

                if (variationRow.isNewRow()) {
                    variationRow.setOrgId(orgId);
                }
                variationRow.set(OMQFPRODUCTQUOTA.PRODUCT_ID, productId);
                variationRow.set(OMQFPRODUCTQUOTA.VARIATION_ID, variationId);
                variationRow.set(OMQFPRODUCTQUOTA.QUOTA_ID, id);
                targetContainer.updateRow(variationRow);
                variationIds.add(variationId);
            }
        }

        if (!isCreateEvent) {
            final WhereClause variationRowCriteria = quotaIdCriteria.and(Criteria.notEq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), null));
            final WhereClause staleVariationRowCriteria = variationIds.isEmpty() ? variationRowCriteria
                    : variationRowCriteria.and(Criteria.notIn(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), variationIds));
            final Stream<Row> quotaVariationRows = sourceContainer.getRows(productQuotaTable, staleVariationRowCriteria);

            quotaVariationRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the given quota model using values from the provided row.
     * </p>
     *
     * @param row           Represents the quota row
     * @param dataContainer A container that holds the quota related data
     * @param quota         Represents the {@link Quota}
     */
    protected void loadQuotaFromRow(final Row row, final DataContainer dataContainer, final Q quota) {
        final Table pipelineQuotaTable = getPipelineQuotaTable();
        final Table productQuotaTable = getProductQuotaTable();
        final Table territoryQuotaTable = getTerritoryQuotaTable();
        final Long quotaId = row.getPKValue();

        quota.setId(quotaId);
        quota.setOrgId(row.getOrgId());
        quota.setOrgId(row.getOrgId());
        quota.setQuotaMetric(QuotaMetric.getById(row.get(OMQFQUOTA.METRIC)));
        quota.setQuotaType(QuotaType.getById(row.get(OMQFQUOTA.TYPE)));
        quota.setQuotaTimePeriod(QuotaTimePeriod.getById(row.get(OMQFQUOTA.TIME_PERIOD)));
        quota.setQuotaTimeRange(QuotaTimeRange.getById(row.get(OMQFQUOTA.TIME_RANGE)));
        quota.setStartTime(row.get(OMQFQUOTA.START_TIME));
        quota.setEndTime(row.get(OMQFQUOTA.END_TIME));
        quota.setTarget(row.get(OMQFQUOTA.TARGET));
        final Table territoryTable = territoryManager.getTable();
        final Column territoryPkColumn = territoryTable.getPKColumn();

        if (NumericalConstant.ZERO < dataContainer.size(pipelineQuotaTable)) {
            final Table pipelineTable = pipelineManager.getTable();
            final Column pipelinePkColumn = pipelineTable.getPKColumn();
            final Stream<Row> pipelineQuotaRows = dataContainer.getRows(pipelineQuotaTable,
                    new WhereClause(Criteria.eq(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID), quotaId)));

            final Collection<Pipeline> pipelines = pipelineQuotaRows.map(pipelineQuotaRow -> {
                final Long pipelineId = pipelineQuotaRow.get(OMQFPIPELINEQUOTA.PIPELINE_ID);
                final Row pipelineRow = dataContainer.getRow(pipelineTable, new WhereClause(Criteria.eq(pipelinePkColumn, pipelineId)));
                final Pipeline pipeline = new Pipeline();

                pipeline.setId(pipelineId);
                pipeline.setName(pipelineRow.get(MDOPIPELINE.NAME));
                return pipeline;
            }).toList();

            quota.setPipelines(pipelines);
        }

        if (NumericalConstant.ZERO < dataContainer.size(productQuotaTable)) {
            final Table productTable = productManager.getTable();
            final Column productPkColumn = productTable.getPKColumn();
            final Table variationTable = variationManager.getTable();
            final Column variationPkColumn = variationTable.getPKColumn();
            final Stream<Row> quotaProductRows = dataContainer.getRows(productQuotaTable,
                    new WhereClause(Criteria.eq(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID), quotaId)));
            final Collection<Product> products = new ArrayList<>();
            final Collection<Variation> variations = new ArrayList<>();

            quotaProductRows.forEach(quotaProductRow -> {
                final Long variationId = quotaProductRow.get(OMQFPRODUCTQUOTA.VARIATION_ID);

                if (Objects.isNull(variationId)) {
                    final Long productId = quotaProductRow.get(OMQFPRODUCTQUOTA.PRODUCT_ID);
                    final Row productRow = dataContainer.getRow(productTable, new WhereClause(Criteria.eq(productPkColumn, productId)));
                    final Product product = new Product();

                    product.setId(productId);
                    product.setName(productRow.get(MPOPRODUCT.NAME));
                    products.add(product);
                } else {
                    final Row variationRow = dataContainer.getRow(variationTable, new WhereClause(Criteria.eq(variationPkColumn, variationId)));
                    final Variation variation = new Variation();

                    variation.setId(variationId);
                    variation.setProductId(quotaProductRow.get(OMQFPRODUCTQUOTA.PRODUCT_ID));

                    if (Objects.nonNull(variationRow)) {
                        variation.setName(variationRow.get(OMPOVARIATION.NAME));
                    }
                    variations.add(variation);
                }
            });

            quota.setProducts(products);
            quota.setVariations(variations);
        }

        if (NumericalConstant.ZERO < dataContainer.size(territoryQuotaTable)) {
            final Stream<Row> territoryQuotaRows = dataContainer.getRows(territoryQuotaTable,
                    new WhereClause(Criteria.eq(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID), quotaId)));

            final Collection<Territory> territories = territoryQuotaRows.map(territoryQuotaRow -> {
                final Long territoryId = territoryQuotaRow.get(OMQFQUOTATERRITORY.TERRITORY_ID);
                final Row territoryRow = dataContainer.getRow(territoryTable, new WhereClause(Criteria.eq(territoryPkColumn, territoryId)));
                final Territory territory = new Territory();

                territory.setId(territoryId);
                territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));

                return territory;
            }).toList();

            quota.setTerritories(territories);
        }
    }

    /**
     * <p>
     * Loads and appends additional quota-related data into the given
     * {@link DataContainer}.
     * </p>
     *
     * @param orgId         The organization identifier
     * @param ids           The collection of quota identifiers for which
     *                      related data should be loaded
     * @param dataContainer The data container to be enriched with additional
     *                      quota-related information
     */
    protected void loadDataContainer(final long orgId, final Collection<Long> ids, final DataContainer dataContainer) {
        if (!dataContainer.isEmpty()) {
            final OrgDataStore orgDataStore = getOrgDataStore(orgId);
            final Table table = getQuotaTable();
            final Column quotaPkColumn = table.getPKColumn();
            final Table territoryQuotaTable = getTerritoryQuotaTable();
            final Table pipelineQuotaTable = getPipelineQuotaTable();
            final Table productQuotaTable = getProductQuotaTable();
            final Table territoryTable = territoryManager.getTable();
            final Table pipelineTable = pipelineManager.getTable();
            final Table productTable = productManager.getTable();
            final Table variationTable = variationManager.getTable();
            final SelectQuery quotaTerritoryQuery = SelectQuery.get(table);
            final SelectQuery quotaProductQuery = SelectQuery.get(table);
            final SelectQuery quotaPipelineQuery = SelectQuery.get(table);
            final WhereClause quotaIdCriteria = new WhereClause(Criteria.in(quotaPkColumn, ids));

            quotaTerritoryQuery.addSelectClause(territoryQuotaTable);
            quotaTerritoryQuery.addSelectClause(territoryTable.getColumn(OMCRMTERRITORY.NAME));
            quotaTerritoryQuery.addJoinClause(List.of(new JoinClause(quotaPkColumn, territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.QUOTA_ID)),
                    new JoinClause(territoryQuotaTable.getColumn(OMQFQUOTATERRITORY.TERRITORY_ID), territoryTable.getPKColumn())));
            quotaTerritoryQuery.setWhereClause(quotaIdCriteria);

            quotaProductQuery.addSelectClause(productQuotaTable);
            quotaProductQuery.addSelectClause(productTable.getColumn(MPOPRODUCT.NAME));
            quotaProductQuery.addSelectClause(variationTable.getColumn(OMPOVARIATION.NAME));
            quotaProductQuery.addJoinClause(List.of(new JoinClause(quotaPkColumn, productQuotaTable.getColumn(OMQFPRODUCTQUOTA.QUOTA_ID)),
                    new JoinClause(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.PRODUCT_ID), productTable.getPKColumn()),
                    new JoinClause(productQuotaTable.getColumn(OMQFPRODUCTQUOTA.VARIATION_ID), variationTable.getPKColumn(), JoinClause.JoinType.LEFT)));
            quotaProductQuery.setWhereClause(quotaIdCriteria);

            quotaPipelineQuery.addSelectClause(pipelineQuotaTable);
            quotaPipelineQuery.addSelectClause(pipelineTable.getColumn(MDOPIPELINE.NAME));
            quotaPipelineQuery.addJoinClause(List.of(new JoinClause(quotaPkColumn, pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.QUOTA_ID)),
                    new JoinClause(pipelineQuotaTable.getColumn(OMQFPIPELINEQUOTA.PIPELINE_ID), pipelineTable.getPKColumn())));
            quotaPipelineQuery.setWhereClause(quotaIdCriteria);

            dataContainer.append(orgDataStore.get(quotaTerritoryQuery));
            dataContainer.append(orgDataStore.get(quotaProductQuery));
            dataContainer.append(orgDataStore.get(quotaPipelineQuery));
        }
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the specified organization ID.
     * </p>
     *
     * @param orgId Represents the organization ID for which to retrieve the data store
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link Quota} data container for the given org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of quota ids
     * @return The {@link Quota} data container
     */
    protected abstract DataContainer getDataContainer(final long orgId, final Collection<Long> ids);
}
