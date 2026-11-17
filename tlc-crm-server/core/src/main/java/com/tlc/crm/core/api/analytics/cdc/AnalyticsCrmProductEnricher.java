package com.tlc.crm.core.api.analytics.cdc;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.table.TableDetail;
import com.tlc.analytics.sql.resource.OACDIMDEALLINEITEMS;
import com.tlc.analytics.sql.resource.OACDIMPRODUCT;
import com.tlc.analytics.sql.resource.OACFACTDEALPRODUCT;

import com.tlc.cdc.core.enrichment.EnrichedRecord;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete implementation of analytics enricher for Product and Deal Line Item entities.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AnalyticsCrmProductEnricher extends AbstractAnalyticsCdcEnricher {

    private final ProductManager productManager;
    private final ProductDetailsHolderManager productDetailsHolderManager;

    private AnalyticsCrmProductEnricher() {
        this.productManager = ProductManager.getInstance();
        this.productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
    }

    private static class Instance {
        private static final AnalyticsCrmProductEnricher COMPANY_ENRICHER = new AnalyticsCrmProductEnricher();
    }

    static AnalyticsCrmProductEnricher getInstance() {
        return Instance.COMPANY_ENRICHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param groupedRecords Map of JSON objects to be enriched which is grouped by table.
     * @return Collection of enriched records.
     */
    @Override
    public Collection<EnrichedRecord> toEnrichedRecords(final Map<String, Collection<JsonObject>> groupedRecords) {
        final Collection<TableDetail> tableDetails = metaHandleService.getTableDetails(getModuleId(groupedRecords.keySet()));
        final Collection<EnrichedRecord> enrichedRecords = new HashSet<>();

        for (final TableDetail chTableDetails : tableDetails) {
            switch (chTableDetails.getTableName()) {
                case OACDIMPRODUCT.TABLE -> buildDimensionProduct(chTableDetails, groupedRecords, enrichedRecords);
                case OACDIMDEALLINEITEMS.TABLE ->
                        buildDimensionDealLineItems(chTableDetails, groupedRecords, enrichedRecords);
            }
        }

        return enrichedRecords;
    }

    /**
     * Builds dimension product data from the given JSON objects.
     *
     * @param chTableDetails  ClickHouse table details for dimension product.
     * @param groupedRecords  Map of JSON objects containing product data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void buildDimensionProduct(final TableDetail chTableDetails, final Map<String, Collection<JsonObject>> groupedRecords, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = groupedRecords.get(MPOPRODUCT.TABLE_NAME.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject productJson : records) {
                final String operation = productJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = productJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMPRODUCT.TABLE), productJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MPOPRODUCT.ID)) : Json.object();

                if (isDeleteEvent) {
                    jsonObject.put(OACDIMPRODUCT.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMPRODUCT.IS_DELETED, true);

                    deleteDealProduct(jsonObject.getLong(MPOPRODUCT.ID), enrichedRecords, transactionId);
                } else {
                    final JsonObject cdcJson = productJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final long orgId = cdcJson.getLong(MPOPRODUCT.ORG_ID);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final Product product = productManager.get(orgId, cdcJson.getLong(MPOPRODUCT.ID));

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMPRODUCT.ID -> jsonObject.put(columnName, cdcJson.getLong(MPOPRODUCT.ID));
                                case OACDIMPRODUCT.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMPRODUCT.NAME ->
                                        jsonObject.put(columnName, cdcJson.getString(MPOPRODUCT.NAME));
                                case OACDIMPRODUCT.VALID_TILL -> {
                                    final Long validTill = product.getValidTill();

                                    if (Objects.nonNull(validTill)) {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(product.getValidTill()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMPRODUCT.SALES_OWNER ->
                                        jsonObject.put(columnName, cdcJson.getLong(MPOPRODUCT.OWNER_ID));
                                case OACDIMPRODUCT.CREATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MPOPRODUCT.CREATED_BY));
                                case OACDIMPRODUCT.CREATED_AT -> {
                                    final DateTime createdTime = product.getCreatedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(createdTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMPRODUCT.UPDATED_BY ->
                                        jsonObject.put(columnName, cdcJson.getLong(MPOPRODUCT.UPDATED_BY));
                                case OACDIMPRODUCT.UPDATED_AT -> {
                                    final DateTime updatedTime = product.getModifiedTime();
                                    jsonObject.put(columnName, TemporalServiceProvider.get(updatedTime.getTimeStamp()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMPRODUCT.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }
                        buildCustomFields(product, jsonObject, Source.PRODUCT);
                    });
                }

                jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
            }
        }
    }

    /**
     * Builds dimension deal line items data from the given JSON objects.
     *
     * @param chTableDetails  ClickHouse table details for dimension deal line items.
     * @param jsonObjects     Map of JSON objects containing deal line items data.
     * @param enrichedRecords Collection of enriched records to be updated.
     */
    private void buildDimensionDealLineItems(final TableDetail chTableDetails,
                                             final Map<String, Collection<JsonObject>> jsonObjects, final Collection<EnrichedRecord> enrichedRecords) {
        final Collection<JsonObject> records = jsonObjects.get(MDOPRODUCTDETAIL.TABLE.toLowerCase());

        if (Objects.nonNull(records) && !records.isEmpty()) {

            for (final JsonObject productDetailJson : records) {
                final String operation = productDetailJson.getString(AnalyticsCdcKeys.OPERATION);
                final long transactionId = productDetailJson.getJsonObject(AnalyticsCdcKeys.SOURCE).getLong(AnalyticsCdcKeys.TXN_ID);
                final boolean isDeleteEvent = Objects.equals(AnalyticsCdcKeys.DELETE_OPERATION, operation);
                final JsonObject jsonObject = isDeleteEvent ? getRowJson(tableResolver.resolve(OACDIMDEALLINEITEMS.TABLE), productDetailJson.getJsonObject(AnalyticsCdcKeys.BEFORE).getLong(MDOPRODUCTDETAIL.ID)) : Json.object();

                if (isDeleteEvent) {
                    jsonObject.put(OACDIMDEALLINEITEMS.UPDATED_AT, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                    jsonObject.put(OACDIMDEALLINEITEMS.IS_DELETED, true);
                    jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());

                    enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
                } else {
                    final JsonObject cdcJson = productDetailJson.getJsonObject(AnalyticsCdcKeys.AFTER);
                    final Long productDetailsHolderId = cdcJson.getLong(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
                    final long orgId = getOrgIdFromProductDetailsHolder(productDetailsHolderId);

                    secureProcess(orgId, userService.getAdminUserId(orgId), () -> {
                        final Long dealId = getDealIdByProductDetailsHolderId(orgId, productDetailsHolderId);

                        if (Objects.isNull(dealId)) {
                            throw new IllegalStateException("Deal ID not found for ProductDetailsHolder ID: " + productDetailsHolderId);
                        }

                        final ProductDetailsHolder productDetailsHolder = productDetailsHolderManager.getLinkedProductList(orgId, dealId, false);
                        final Long productDetailId = cdcJson.getLong(MDOPRODUCTDETAIL.ID);
                        final ProductDetail productDetail = productDetailsHolder.getProductDetails().stream()
                                .filter(detail -> Objects.equals(detail.id(), productDetailId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Product detail not found for ID: " + productDetailId));
                        final Product product = productManager.get(orgId, productDetail.getProduct().id());

                        for (final ColumnDetail chColumnDetails : metaHandleService.getColumnDetails(chTableDetails.id())) {
                            final String columnName = chColumnDetails.getColumnName();

                            switch (columnName) {
                                case OACDIMDEALLINEITEMS.ID -> jsonObject.put(columnName, productDetail.id());
                                case OACDIMDEALLINEITEMS.ORG_ID -> jsonObject.put(columnName, orgId);
                                case OACDIMDEALLINEITEMS.PRODUCT_NAME -> jsonObject.put(columnName, product.getName());
                                case OACDIMDEALLINEITEMS.DEAL_ID ->
                                        jsonObject.put(columnName, productDetailsHolder.getDealId());
                                case OACDIMDEALLINEITEMS.ACTIVE_STATUS -> {
                                    final Option activeStatus = product.getActiveStatus();

                                    if (Objects.nonNull(activeStatus)) {
                                        jsonObject.put(columnName, activeStatus.getOption());
                                    }
                                }
                                case OACDIMDEALLINEITEMS.DISCOUNT ->
                                        jsonObject.put(columnName, productDetail.getDiscount());
                                case OACDIMDEALLINEITEMS.CREATED_AT -> {
                                    final String existingCreatedAt = getExistingDealLineItemCreatedAt(productDetailsHolder.getDealId(), product.id());

                                    if (Objects.nonNull(existingCreatedAt)) {
                                        jsonObject.put(columnName, existingCreatedAt);
                                    } else {
                                        jsonObject.put(columnName, TemporalServiceProvider.get(Instant.now().toEpochMilli())
                                                .formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                    }
                                }
                                case OACDIMDEALLINEITEMS.UPDATED_AT -> {
                                    jsonObject.put(columnName, TemporalServiceProvider.get(Instant.now().toEpochMilli()).formatUtcToDateTime(TimezoneInfo.UTC, DATE_TIME_FORMATTER));
                                }
                                case OACDIMDEALLINEITEMS.UNIT_PRICE -> {
                                    jsonObject.put(columnName, productDetail.getPrice());
                                }
                                case OACDIMDEALLINEITEMS.PRODUCT_CATEGORY -> {
                                    final Option category = product.getCategory();

                                    if (Objects.nonNull(category)) {
                                        jsonObject.put(columnName, category.getOption());
                                    }
                                }
                                case OACDIMDEALLINEITEMS.QUANTITY ->
                                        jsonObject.put(columnName, productDetail.getQuantity());
                                case OACDIMDEALLINEITEMS.TOTAL_PRICE_WITHOUT_TAX -> {
                                    jsonObject.put(columnName, productDetail.getAmount());
                                }
                                case OACDIMDEALLINEITEMS.TOTAL_PRICE_IN_BASE_CURRENCY_WITHOUT_TAX -> {
                                    final BigDecimal amount = BigDecimal.valueOf(productDetail.getAmount());
                                    final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId, baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency());

                                    jsonObject.put(columnName, Objects.isNull(exchangeRate) ? amount : amount.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
                                }
                                case OACDIMDEALLINEITEMS.PRODUCT_ID -> jsonObject.put(columnName, product.id());
                                case OACDIMPRODUCT.IS_DELETED -> jsonObject.put(columnName, false);
                            }
                        }

                        jsonObject.put(AnalyticsCdcKeys.TOPIC, chTableDetails.getTableName().toLowerCase());
                        enrichedRecords.add(new EnrichedRecord(transactionId, jsonObject));
                    });
                }
            }
        }
    }

    /**
     * Deletes deal product records associated with the given product ID and adds enriched records for deletion.
     *
     * @param productId       The ID of the product being deleted.
     * @param enrichedRecords Collection of enriched records to be updated with deletion records.
     * @param transactionId   The transaction ID associated with the delete event.
     */
    private void deleteDealProduct(final long productId, final Collection<EnrichedRecord> enrichedRecords, final long transactionId) {
        final Table dealProductTable = tableResolver.resolve(OACFACTDEALPRODUCT.TABLE);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealProductTable.getColumn(OACFACTDEALPRODUCT.PRODUCT_ID), productId));

        if (exists(dealProductTable, whereClause)) {
            final Collection<JsonObject> contactDeals = getRowsJson(dealProductTable, whereClause);

            for (final JsonObject contactDealJson : contactDeals) {
                contactDealJson.put(OACFACTDEALPRODUCT.IS_DELETED, true);
                contactDealJson.put(AnalyticsCdcKeys.TOPIC, OACFACTDEALPRODUCT.TABLE.toLowerCase());
                enrichedRecords.add(new EnrichedRecord(transactionId, contactDealJson));
            }
        }
    }

    private Long getDealIdByProductDetailsHolderId(final Long orgId, final Long productDetailsHolderId) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsHolderTable.getPKColumn(), productDetailsHolderId)));

        final DataContainer dataContainer = DataStoreProvider.getSecureOrgDataStore(orgId).get(selectQuery);
        final com.tlc.sql.common.api.Row row = dataContainer.getRow(productDetailsHolderTable);

        return Objects.nonNull(row) ? row.get(OMDOPRODUCTDETAILSHOLDER.DEAL_ID) : null;
    }

    /**
     * Retrieves the organization ID from the product details holder table using the given holder ID.
     *
     * @param productDetailsHolderId the ID of the product details holder
     * @return the organization ID
     * @throws IllegalStateException if the product details holder is not found
     */
    private long getOrgIdFromProductDetailsHolder(final Long productDetailsHolderId) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsHolderTable.getPKColumn(), productDetailsHolderId)));

        final DataContainer dataContainer = DataStoreProvider.getAdminDataStore().get(selectQuery);
        final com.tlc.sql.common.api.Row row = dataContainer.getRow(productDetailsHolderTable);

        if (Objects.isNull(row)) {
            throw new IllegalStateException("ProductDetailsHolder not found for ID: " + productDetailsHolderId);
        }

        return row.get(OMDOPRODUCTDETAILSHOLDER.ORG_ID);
    }
}

