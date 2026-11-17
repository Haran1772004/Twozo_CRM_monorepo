package com.tlc.crm.core.list;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.ListFieldsFetcher;
import com.tlc.crm.entity.list.dynamic.container.ListDynamicFieldsFetcherRegistry;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.model.ListFieldsData;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Process the entity list actions in the deal summary and Provides the implementation for the {@link EntitySQLListInterpreter}.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class DealSummarySQLListInterpreter extends EntitySQLListInterpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealSummarySQLListInterpreter.class);

    private final ListDynamicFieldsFetcherRegistry listDynamicFieldsFetcherRegistry;
    private Collection<Long> productDetailIds;
    private Collection<Long> productIds;

    public DealSummarySQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                         final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
        this.listDynamicFieldsFetcherRegistry = ListDynamicFieldsFetcherRegistry.getInstance();
    }

    /**
     * <p>
     * Gets the {@link FieldConfig.FieldEnableType}
     * </p>
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return switch (provider) {
            case CONTACT_LIST -> FieldConfig.FieldEnableType.IS_ENABLED_FOR_DEAL_SUMMARY_CONTACT_LIST;
            case PRODUCT_LIST -> FieldConfig.FieldEnableType.IS_ENABLED_FOR_DEAL_SUMMARY_PRODUCT_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_PROVIDER, "i18n.entity.list.provider.not.found");
        };
    }

    /**
     * <p>
     * Fetches the PK values of the primary table for the give limit
     * </p>
     *
     * @return Collection of PK values.
     */
    @Override
    public Collection<Long> getPkValues() {
        return switch (provider) {
            case PRODUCT_LIST -> {
                final ReadableDataStore orgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
                final SelectQuery selectQuery = SelectQuery.get(primaryTable);
                final List<OrderByClause> orderByClauses = derivedQuery.getOrderByClause();

                selectQuery.addSelectClause(primaryTable.getPKColumn().distinct());
                selectQuery.addJoinClause(derivedQuery.getJoinClause());
                selectQuery.setWhereClause(derivedQuery.getWhereClause());

                for (final OrderByClause orderByClause : orderByClauses) {
                    selectQuery.addSelectClause(orderByClause.getColumn());
                }

                final Table productDetailTable = selectQuery.getTables().stream().filter(table -> table.getName().equals(MDOPRODUCTDETAIL.TABLE))
                        .findFirst().orElseGet(() -> Table.get(MDOPRODUCTDETAIL.TABLE));

                selectQuery.getTables().stream().filter(table -> table.getName().equals(productDetailTable.getName())).findFirst()
                        .ifPresent(table -> selectQuery.addSelectClause(table.getPKColumn()));
                selectQuery.setLimitClause(derivedQuery.getLimitClause());
                selectQuery.addOrderByClause(orderByClauses);

                this.productDetailIds = orgDataStore.get(selectQuery).getRows(productDetailTable).map(Row::getPKValue).collect(Collectors.toCollection(LinkedHashSet::new));
                yield Collections.emptyList();
            }
            default -> super.getPkValues();
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of maps containing the mapped data.
     */
    @Override
    public Collection<Map<String, Object>> getResult() {
        return switch (provider) {
            case PRODUCT_LIST -> {
                final ReadableDataStore secureReadableOrgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
                final Collection<Long> ids = fetchPkValues();
                final Column pkColumn = primaryTable.getPKColumn();
                final Table productDetailTable = Table.get(MDOPRODUCTDETAIL.TABLE);
                final Column productDetailPkColumn = productDetailTable.getPKColumn();

                selectQuery.addSelectClause(productDetailTable.getColumn(MDOPRODUCTDETAIL.ID, ListActionFields.PRODUCT_DETAIL_ID));
                selectQuery.addJoinClause(new JoinClause(primaryTable, pkColumn, productDetailTable, productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), JoinClause.JoinType.INNER));
                selectQuery.setWhereClause(new WhereClause(productDetailIds.isEmpty() ? Criteria.eq(productDetailPkColumn, null) : Criteria.in(productDetailPkColumn, productDetailIds)));
                final List<Map<String, Object>> result = secureReadableOrgDataStore.getData(selectQuery);
                final String productIdKey = primaryField();

                this.productIds = result.stream().map(row -> row.get(productIdKey)).filter(Objects::nonNull)
                        .map(productId -> NumberUtils.parseNumber(productId).longValue()).collect(Collectors.toCollection(LinkedHashSet::new));

                yield result;
            }
            default -> super.getResult();
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return The total number of hits.
     */
    @Override
    public long fetchTotalHits() {
        return switch (provider) {
            case PRODUCT_LIST -> {
                final ReadableDataStore orgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
                final SelectQuery selectQuery = SelectQuery.get(primaryTable);

                selectQuery.addJoinClause(derivedQuery.getJoinClause());
                selectQuery.setWhereClause(derivedQuery.getWhereClause());
                final Table productDetailTable = selectQuery.getTables().stream().filter(table -> table.getName().equals(MDOPRODUCTDETAIL.TABLE))
                        .findFirst().orElseGet(() -> Table.get(MDOPRODUCTDETAIL.TABLE));

                selectQuery.addSelectClause(productDetailTable.getPKColumn());
                final long totalCount = orgDataStore.get(selectQuery).size(productDetailTable);

                this.derivedQuery.setWhereClause(selectQuery.getWhereClause());

                yield totalCount;
            }
            default -> super.fetchTotalHits();
        };

    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For the deal product list, the dynamic fields are fetched using the product ids of the fetched deal product rows
     * </p>
     *
     * @return The {@link Map} of product id to its dynamic fields data
     */
    @Override
    public Map<String, Map<String, Object>> fetchDynamicFields() {
        return switch (provider) {
            case PRODUCT_LIST -> {
                final ListFieldsFetcher<?> dynamicFieldsFetcher = listDynamicFieldsFetcherRegistry.get(provider);

                if (Objects.nonNull(dynamicFieldsFetcher) && Objects.nonNull(productIds) && !productIds.isEmpty() && !dynamicFields.isEmpty()) {
                    final DynamicFieldRequest dynamicFieldRequestData = new DynamicFieldRequest(orgId, productIds, provider, dynamicFields);
                    final long dynamicFieldsFetchStartTime = System.currentTimeMillis();

                    LOGGER.debug("List Action - [{}]: Going to fetch the dynamic fields data from the database, Org ID: {}, User ID: {}, Product IDs: {}", provider, orgId, userId, productIds);
                    final ListFieldsData listFieldsData = dynamicFieldsFetcher.fetchDynamicFieldsData(dynamicFieldRequestData);

                    LOGGER.debug("List Action - [{}]: Dynamic data are fetched from the database in {}ms, Org ID: {}, User ID: {}, Product IDs: {}", provider, System.currentTimeMillis() - dynamicFieldsFetchStartTime, orgId, userId, productIds);

                    if (Objects.nonNull(listFieldsData)) {
                        yield listFieldsData.get();
                    }
                }
                yield Collections.emptyMap();
            }
            default -> super.fetchDynamicFields();
        };
    }
}