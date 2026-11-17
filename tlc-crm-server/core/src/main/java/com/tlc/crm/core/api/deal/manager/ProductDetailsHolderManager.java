package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;
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
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link ProductDetailsHolder} related DB actions
 *
 * @author karthickV
 * @version 1.0
 */
public class ProductDetailsHolderManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ProductDetailsHolderManager.class);

    private final ProductManager productManager;
    private final MonetaryManager monetaryManager;
    private final IndexDataPublisher indexDataPublisher;
    private final AuditLogger auditLogger;

    private ProductDetailsHolderManager() {
        productManager = ProductManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
        auditLogger = AuditLogger.getInstance();
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    private static class Instance {
        private static final ProductDetailsHolderManager INSTANCE = new ProductDetailsHolderManager();
    }

    /**
     * Gets the {@link ProductDetailsHolderManager} instance
     *
     * @return the {@link ProductDetailsHolderManager} instance
     */
    public static ProductDetailsHolderManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the product details holder table
     *
     * @return the {@link Table} of product details holder
     */
    public Table getTable() {
        return Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
    }

    /**
     * <p>
     * Gets {@link ProductDetail} table
     * </p>
     *
     * @return the {@link Table} of deal
     */
    public Table getProductDetailTable() {
        return Table.get(MDOPRODUCTDETAIL.TABLE);
    }

    /**
     * Gets the {@link ProductDetailsHolder} by given deal id
     *
     * @param orgId It represents organization id
     * @param id    Collection of product details holder id
     * @return Collection of {@link ProductDetailsHolder}
     */
    public ProductDetailsHolder get(final Long orgId, final Long id) {
        final Table productDetailsHolderTable = getTable();
        final DataContainer dataContainer = getDataContainer(orgId, new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), id)));
        final Row row = dataContainer.getRow(productDetailsHolderTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_DETAILS_HOLDER_NOT_FOUND,
                    String.format("Org ID : %d, Entity ID : %s", orgId, id));
        } else {
            return convertRowToModel(row, dataContainer);
        }
    }

    /**
     * Gets the {@link ProductDetailsHolder} by given product details holder id.
     *
     * @param orgId                  It represents organization id.
     * @param productDetailsHolderId It represents the product details holder id.
     * @param productDetailId        It refers the product detail id.
     * @return {@link ProductDetailsHolder}
     */
    public ProductDetailsHolder productDetailsHolder(final Long orgId, final Long productDetailsHolderId, final Long productDetailId) {
        final Table productDetailsHolderTable = getTable();
        final Table productDetailsTable = getProductDetailTable();
        final DataContainer dataContainer = getDataContainer(orgId,
                new WhereClause(Criteria.eq(productDetailsHolderTable.getPKColumn(), productDetailsHolderId))
                        .and(Criteria.eq(productDetailsTable.getPKColumn(), productDetailId)));
        final Row row = dataContainer.getRow(productDetailsHolderTable);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_DETAILS_HOLDER_NOT_FOUND);
        } else {
            return convertRowToModel(row, dataContainer);
        }
    }

    /**
     * Updates the given entities
     *
     * @param orgId                Organization id
     * @param productDetailsHolder An entity that holds the product details holder attributes
     * @return Updated status
     */
    public ProcessOutcome update(final Long orgId, final long userId, final ProductDetailsHolder productDetailsHolder) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = getDataContainer(orgId, new WhereClause(Criteria.eq(getTable()
                .getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), productDetailsHolder.getDealId())));

        try {
            final DataContainer targetContainer = DataContainer.create();

            loadProductDetailsHolderIntoContainer(false, productDetailsHolder, entityContainer, targetContainer);
            orgDataStore.commitChanges(targetContainer);
            processOutcome.addProcessedRecord(productDetailsHolder.id());

            auditEntry(userId, productDetailsHolder, targetContainer);
        } catch (ErrorCode error) {
            LOGGER.error("Update action: Product details holder update failed Org ID : {} ID : {} reason : {}", orgId, productDetailsHolder.id(), error.getMessage(), error);
            failedEntities.add(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, productDetailsHolder.id().toString()));
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!processOutcome.getProcessedRecords().isEmpty()) {
            addProductDetailsIntoDeal(productDetailsHolder);
        }

        return processOutcome;
    }

    /**
     * <p>
     * Checks whether any product is associated with the given deal within the specified organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return {@code true} if at least one product is linked to the deal; {@code false} otherwise
     */
    public boolean isProductLinkedToDeal(final Long orgId, final Long dealId) {
        final Table productDetailsHolderTable = getTable();
        final Table productDetailsTable = getProductDetailTable();
        final Table productTable = productManager.getTable();
        final Column productDetailsHolderPkColumn = productDetailsHolderTable.getPKColumn();
        final CountQuery countQuery = CountQuery.get(productDetailsHolderTable);

        countQuery.addJoinClause(List.of(new JoinClause(productDetailsHolderPkColumn,
                        productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID)),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productTable.getPKColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsHolderTable
                .getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }


    /**
     * <p>
     * Loads the product timeline into container
     * </p>
     *
     * @param orgId                Represents the org id
     * @param dealId               Represents the deal id
     * @param newProductIds        Represents the new product ids
     * @param unlinkedProductIds   Represents the unkinked product ids
     * @param newVariationIds      Refers the variation ids of a product detail.
     * @param unlinkedVariationIds Refers the variation ids of a product detail that are to be deleted.
     * @param targetContainer      Represents the target container
     */
    private void loadProductTimelineIntoContainer(final Long orgId, final Long dealId, final Collection<Long> newProductIds, final Collection<Long> unlinkedProductIds,
                                                  final Map<Long, Long> newVariationIds, final Map<Long, Long> unlinkedVariationIds, final DataContainer targetContainer) {
        final Timeline.Category category = Timeline.Category.DEAL;
        final DealManager dealManager = DealManager.getInstance();
        final Long userId = LoggedInUserInfo.getUser().id();

        if (!newProductIds.isEmpty()) {
            dealManager.loadDealTimelineRowIntoContainer(buildTimeline(orgId, userId, TimelineActionFields.PRODUCTS_ADDED,
                    newProductIds, category, TimelineType.DEAL_PRODUCTS_ADDED, newVariationIds, targetContainer), dealId, targetContainer);
        }

        if (!unlinkedProductIds.isEmpty()) {
            dealManager.loadDealTimelineRowIntoContainer(buildTimeline(orgId, userId, TimelineActionFields.PRODUCTS_REMOVED,
                    unlinkedProductIds, category, TimelineType.DEAL_PRODUCTS_REMOVED, unlinkedVariationIds, targetContainer), dealId, targetContainer);
        }
    }

    /**
     * <p>
     * Builds the {@link Timeline} for the products
     * </p>
     *
     * @param orgId        Represents an org id
     * @param userId       Represents an user id
     * @param action       Represents the timeline action
     * @param category     Represents the {@link Timeline.Category}
     * @param timelineType Represents the {@link TimelineType}
     * @param variationIds Refers the variation ids of a product detail.
     * @return The {@link Timeline}
     */
    private Timeline buildTimeline(final Long orgId, final Long userId, final String action, final Collection<Long> productIds,
                                   final Timeline.Category category, final TimelineType timelineType, final Map<Long, Long> variationIds,
                                   final DataContainer targetContainer) {
        final Timeline timeline = new Timeline();
        final Collection<String> productNames = new ArrayList<>();

        timeline.setOrgId(orgId);
        timeline.setOwnerId(userId);
        timeline.setAction(action);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setCategory(category);
        timeline.setTimelineType(timelineType);

        for (final Collection<Long> batchedProductIds : JavaUtil.splitCollection(productIds, NumericalConstant.FIFTY)) {
            productNames.addAll(productManager.getProductNames(orgId, batchedProductIds, variationIds, targetContainer));
        }

        timeline.setFieldTimeline(new FieldTimeline(null, Json.object().put(action, Json.array(productNames)).toString(),
                new I18nKey("i18n.timeline.source.product")));

        return timeline;
    }

    /**
     * <p>
     * Stores the product create log into the ES storage.
     * </p>
     *
     * @param productDetailsHolder Holds the details of the products.
     * @param targetContainer      Holds the updated information of user defined and system field values.
     */
    private void auditEntry(final Long userId, final ProductDetailsHolder productDetailsHolder, final DataContainer targetContainer) {
        final Collection<AuditLogData> auditLogData = new ArrayList<>();
        final AuditSource auditSource = AuditSource.PRODUCT;
        final AuditAction auditAction = AuditAction.ADDED;
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails)) {

            for (final ProductDetail productDetail : productDetails) {
                final Product product = productDetail.getProduct();
                final Row productRow = targetContainer.getRow(productManager.getTable(), product.id());

                if (Objects.nonNull(productRow) && productRow.isNewRow()) {
                    auditLogData.add(new AuditLogData(userId, product.id(), product, auditSource, auditAction));
                }
            }
            auditLogger.log(productDetailsHolder.orgId(), auditLogData);
        }
    }

    /**
     * <p>
     * Adds Product details to the mapped Deal
     * </p>
     *
     * @param productDetailsHolder An entity that holds the attributes of product details holder.
     */
    private void addProductDetailsIntoDeal(final ProductDetailsHolder productDetailsHolder) {
        final Deal deal = new Deal();
        final DealValue dealValue = new DealValue();
        final Long orgId = productDetailsHolder.orgId();
        BigDecimal value = BigDecimal.ZERO;
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails)) {

            for (final ProductDetail productDetail : productDetails) {
                final boolean isTaxInclusive = productDetailsHolder.isTaxInclusive();
                final double tax = productDetail.getTax();
                final double amount = productDetail.getAmount();
                final double totalAmount = isTaxInclusive ? (amount + (amount * tax / 100)) : amount;

                value = value.add(BigDecimal.valueOf(totalAmount));
            }
        }
        deal.setId(productDetailsHolder.getDealId());
        deal.setOrgId(orgId);
        deal.setProductDetailsHolder(productDetailsHolder);
        dealValue.setValue(value);
        deal.setDealValue(dealValue);
        indexDataPublisher.publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
    }

    /**
     * Gets the data container for product details holder.
     *
     * @param orgId       It represents organization Id
     * @param whereClause Represents the criteria to fetch the rows from the given table
     * @return {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final WhereClause whereClause) {
        final Table productDetailsHolderTable = getTable();
        final Table productDetailsTable = getProductDetailTable();
        final Table productTable = productManager.getTable();
        final Table variationTable = VariationManager.getInstance().getTable();
        final Column productDetailsHolderPkColumn = productDetailsHolderTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(productDetailsHolderPkColumn,
                        productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productTable.getPKColumn(), leftJoin),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(whereClause);
        selectQuery.addSelectClause(productDetailsHolderTable, productDetailsTable, productTable, variationTable);

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the product details holder into the container
     * </p>
     *
     * @param isCreateEvent        A flag which indicates whether the action is related to created event or update event
     * @param productDetailsHolder An entity that holds the product details holder attributes
     * @param entityContainer      Holds the user defined and system field values of existing product details holder
     * @param targetContainer      Holds the updated information of user defined and system field values
     */
    public void loadProductDetailsHolderIntoContainer(final boolean isCreateEvent, final ProductDetailsHolder productDetailsHolder,
                                                      final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table productDetailsHolderTable = getTable();
        final Long dealId = productDetailsHolder.getDealId();
        final Row row = Objects.isNull(entityContainer) ? null : entityContainer.getRow(productDetailsHolderTable,
                new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));
        final Long id = isCreateEvent ? null : Objects.isNull(row) ? null : row.getPKValue();
        final Long orgId = productDetailsHolder.orgId();
        final Row productDetailsHolderRow = Objects.isNull(id) ? new Row(productDetailsHolderTable) :
                entityContainer.getRow(productDetailsHolderTable, id);
        final Collection<Long> newProductIds = new ArrayList<>();
        final Collection<Long> unlinkedProductIds = new ArrayList<>();

        if (!isCreateEvent && Objects.isNull(productDetailsHolderRow)) {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_DETAILS_HOLDER_NOT_FOUND, String.format("Org ID : %d, Entity ID : %d", orgId, id));
        }

        if (productDetailsHolderRow.isNewRow()) {
            productDetailsHolderRow.setOrgId(orgId);
        }
        final Map<Long, Long> newVariationIds = new HashMap<>();
        final Map<Long, Long> unlinkedVariationIds = new HashMap<>();

        productDetailsHolder.setId(productDetailsHolderRow.getPKValue());
        productDetailsHolderRow.set(OMDOPRODUCTDETAILSHOLDER.DEAL_ID, dealId);
        productDetailsHolderRow.set(OMDOPRODUCTDETAILSHOLDER.TAX_INCLUSIVE, productDetailsHolder.isTaxInclusive());
        productDetailsHolderRow.set(OMDOPRODUCTDETAILSHOLDER.CURRENCY_ID, productDetailsHolder.getCurrency().getId());

        targetContainer.updateRow(productDetailsHolderRow);
        loadProductDetailsIntoContainer(isCreateEvent, orgId, productDetailsHolder, productDetailsHolderRow, entityContainer, targetContainer, newProductIds, newVariationIds);

        if (!isCreateEvent) {
            updateDealValue(dealId, orgId, productDetailsHolder, targetContainer);
        }

        if (!productDetailsHolderRow.isNewRow()) {
            removeUnmappedProducts(id, productDetailsHolder.getProductDetails(), entityContainer, targetContainer, unlinkedProductIds, unlinkedVariationIds);
        }
        loadProductTimelineIntoContainer(orgId, productDetailsHolder.getDealId(), newProductIds, unlinkedProductIds, newVariationIds, unlinkedVariationIds, targetContainer);
    }

    /**
     * Update the deal value based on the linked product
     *
     * @param dealId               represents the deal id
     * @param orgId                represents the organization id
     * @param productDetailsHolder An entity that holds the product details holder attributes
     * @param targetContainer      Holds the updated information of user defined and system field values
     */
    private void updateDealValue(final Long dealId, final Long orgId, final ProductDetailsHolder productDetailsHolder,
                                 final DataContainer targetContainer) {
        final Table dealTable = DealManager.getInstance().getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);

        selectQuery.addJoinClause(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));
        selectQuery.addSelectClause(List.of(monetaryIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID)));

        final Row monetaryRow = orgDataStore(orgId).get(selectQuery).getRow(monetaryTable);

        if (Objects.nonNull(monetaryRow)) {
            monetaryRow.set(OMETMONETARY.VALUE, productDetailsHolder.getTotalAmount(productDetailsHolder));
            monetaryRow.set(OMETMONETARY.CURRENCY_ID, productDetailsHolder.getCurrency().getId());

            targetContainer.updateRow(monetaryRow);
        }
    }

    /**
     * <p>
     * Loads the {@link ProductDetail} into product detail container
     * </p>
     *
     * @param productDetailsHolder    An entity that holds the product details holder attributes
     * @param productDetailsHolderRow It represents the row of a product details holder table
     * @param targetContainer         Holds the updated information of user defined and system field values
     * @param newProductIds           Represents the newly added product ids
     * @param variationIds            Refers the variation ids of a product detail.
     */
    protected void loadProductDetailsIntoContainer(final boolean isCreateEvent, final long orgId, final ProductDetailsHolder productDetailsHolder,
                                                   final Row productDetailsHolderRow, final DataContainer entityContainer,
                                                   final DataContainer targetContainer, final Collection<Long> newProductIds,
                                                   final Map<Long, Long> variationIds) {
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails)) {
            productDetails.forEach(productDetail -> {
                final Long id = productDetail.id();
                final Long variationId = productDetail.getVariationId();
                final Table productDetailTable = getProductDetailTable();
                final Row productDetailsRow;

                if (isCreateEvent || Objects.isNull(id)) {
                    if (Objects.nonNull(variationId)) {
                        productDetailsRow = Objects.requireNonNullElse(entityContainer.getRow(productDetailTable, new WhereClause(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationId))
                                .and(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), productDetailsHolder.id()))), new Row(productDetailTable));
                    } else {
                        productDetailsRow = new Row(productDetailTable);
                    }
                } else {
                    if (Objects.nonNull(variationId)) {
                        productDetailsRow = entityContainer.getRow(productDetailTable, new WhereClause(Criteria.eq(productDetailTable.getPKColumn(), id))
                                .and(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationId)));
                    } else {
                        productDetailsRow = entityContainer.getRow(productDetailTable, id);
                    }
                }
                if (!isCreateEvent && Objects.isNull(productDetailsRow)) {
                    throw ErrorCode.get(DealErrorCodes.PRODUCT_DETAILS_NOT_FOUND, String.format("ID : %d", id));
                }

                if (productDetailsRow.isNewRow()) {
                    productDetail.setId(productDetailsRow.getPKValue());
                    productDetailsRow.setOrgId(orgId);
                }

                if (productDetailsHolder.isTaxInclusive()) {
                    productDetailsRow.set(MDOPRODUCTDETAIL.TAX, productDetail.getTax());
                }

                productDetail.setId(productDetailsRow.getPKValue());
                productDetailsRow.set(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID, productDetailsHolderRow.getPKValue());
                productDetailsRow.set(MDOPRODUCTDETAIL.QUANTITY, productDetail.getQuantity());
                productDetailsRow.set(MDOPRODUCTDETAIL.DISCOUNT, productDetail.getDiscount());
                productDetailsRow.set(MDOPRODUCTDETAIL.AMOUNT, productDetail.getAmount());
                productDetailsRow.set(MDOPRODUCTDETAIL.PRICE, productDetail.getPrice());
                productDetailsRow.set(MDOPRODUCTDETAIL.VARIATION_ID, variationId);
                productDetailsRow.set(MDOPRODUCTDETAIL.COMMENT, productDetail.getComment());
                final Product product = productDetail.getProduct();

                if (Objects.nonNull(product) && productDetailsRow.isNewRow()) {
                    final Long productId = product.id();

                    if (Objects.isNull(productId)) {
                        productManager.loadProductIntoContainer(true, product, targetContainer);
                        newProductIds.add(product.id());
                    } else {
                        newProductIds.add(productId);
                    }
                }

                if (Objects.nonNull(product)) {
                    productDetailsRow.set(MDOPRODUCTDETAIL.PRODUCT_ID, product.id());
                }

                if (productDetailsRow.isNewRow()) {
                    variationIds.put(productDetailsRow.getPKValue(), variationId);
                }
                targetContainer.updateRow(productDetailsRow);
            });
        }
    }

    /**
     * <p>
     * Removes the unmapped Products details of the {@link ProductDetailsHolder} in the database
     * </p>
     *
     * @param productDetailsHolderId Product details holder id
     * @param productDetails         An entity that holds the product details attributes
     * @param targetContainer        Holds the updated information of user defined and system field values
     * @param unlinkedProductIds     Represents the unlinked product ids
     * @param variationIds           Refers the variation ids of a product detail.
     */
    private void removeUnmappedProducts(final Long productDetailsHolderId, final Collection<ProductDetail> productDetails,
                                        final DataContainer sourceContainer, final DataContainer targetContainer,
                                        final Collection<Long> unlinkedProductIds, final Map<Long, Long> variationIds) {
        if (Objects.nonNull(productDetails)) {
            final Table productDetailTable = getProductDetailTable();
            final WhereClause whereClause = new WhereClause(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                    productDetailsHolderId)).and(new WhereClause(Criteria.notIn(productDetailTable.getPKColumn(),
                    productDetails.stream().map(ProductDetail::id).collect(Collectors.toUnmodifiableSet()))));
            final Stream<Row> productRows = sourceContainer.getRows(productDetailTable, whereClause);

            if (Objects.nonNull(productRows)) {
                productRows.forEach(productDetailRow -> {
                    unlinkedProductIds.add(productDetailRow.get(MDOPRODUCTDETAIL.PRODUCT_ID));
                    variationIds.put(productDetailRow.getPKValue(), productDetailRow.get(MDOPRODUCTDETAIL.VARIATION_ID));
                    targetContainer.deleteRow(productDetailRow);
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param row product details holder row
     * @return the {@link ProductDetailsHolder} filled with product details holder data
     */
    protected ProductDetailsHolder convertRowToModel(final Row row, final DataContainer dataContainer) {
        final ProductDetailsHolder productDetailsHolder = new ProductDetailsHolder();
        final Table productDetailsTable = getProductDetailTable();

        if (Objects.nonNull(row)) {
            final Stream<Row> productDetailRows = dataContainer.getRows(productDetailsTable,
                    new WhereClause(Criteria.eq(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                            row.getPKValue())));

            productDetailsHolder.setId(row.getPKValue());
            productDetailsHolder.setOrgId(row.getOrgId());
            productDetailsHolder.setDealId(row.get(OMDOPRODUCTDETAILSHOLDER.DEAL_ID));
            productDetailsHolder.setTaxInclusive(row.get(OMDOPRODUCTDETAILSHOLDER.TAX_INCLUSIVE));
            final Short currencyId = row.get(OMDOPRODUCTDETAILSHOLDER.CURRENCY_ID);

            if (Objects.nonNull(currencyId)) {
                productDetailsHolder.setCurrency(Currency.getCurrencyById(currencyId));
            }

            if (Objects.nonNull(productDetailRows)) {
                productDetailRows.forEach(productDetailRow -> productDetailsHolder
                        .addProductDetail(convertRowToProductDetail(productDetailRow, dataContainer)));
            }
        }

        return productDetailsHolder;
    }

    /**
     * <p>
     * Converts {@link Row} into {@link ProductDetail}
     * </p>
     *
     * @param row           Represents the row of a product details table
     * @param dataContainer Holds the existing records of the productDetail table
     * @return {@link ProductDetail}
     */
    private ProductDetail convertRowToProductDetail(final Row row, final DataContainer dataContainer) {
        final ProductDetail productDetail = new ProductDetail();
        final Table productTable = productManager.getTable();
        final Long productId = row.get(MDOPRODUCTDETAIL.PRODUCT_ID);

        productDetail.setId(row.getPKValue());
        productDetail.setOrgId(row.getOrgId());
        productDetail.setTax(row.get(MDOPRODUCTDETAIL.TAX));
        productDetail.setPrice(row.get(MDOPRODUCTDETAIL.PRICE));
        productDetail.setAmount(row.get(MDOPRODUCTDETAIL.AMOUNT));
        productDetail.setDiscount(row.get(MDOPRODUCTDETAIL.DISCOUNT));
        productDetail.setQuantity(row.get(MDOPRODUCTDETAIL.QUANTITY));
        productDetail.setProductDetailsHolderId(row.get(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID));
        final Row productRow = dataContainer.getRow(productTable,
                new WhereClause(Criteria.eq(productTable.getPKColumn(), productId)));
        final Long variationId = row.get(MDOPRODUCTDETAIL.VARIATION_ID);

        if (Objects.nonNull(productRow)) {
            final Product product = new Product();
            final Table variationTable = Table.get(OMPOVARIATION.TABLE_NAME);

            product.setId(productRow.getPKValue());

            if (Objects.nonNull(variationId)) {
                Optional.ofNullable(dataContainer.getRow(variationTable, new WhereClause(Criteria.eq(variationTable.getPKColumn(), variationId))))
                        .ifPresentOrElse(variationRow -> {
                                    final Variation variation = new Variation();
                                    final String variationName = variationRow.get(OMPOVARIATION.NAME);
                                    product.setNameWithVariation(productRow.get(MPOPRODUCT.NAME), variationName);

                                    variation.setId(variationRow.getPKValue());
                                    variation.setName(variationName);
                                    productDetail.setVariation(variation);
                                },
                                () -> product.setName(productRow.get(MPOPRODUCT.NAME)));
            } else {
                product.setName(productRow.get(MPOPRODUCT.NAME));
            }
            productDetail.setProduct(product);
        }
        productDetail.setVariationId(variationId);
        productDetail.setComment(row.get(MDOPRODUCTDETAIL.COMMENT));

        return productDetail;
    }

    /**
     * Gets the linked deal product list
     *
     * @param orgId     Represents the organization id
     * @param dealId    Represents the deal id
     * @param isPartial need partial data means true otherwise false
     * @return {@link Collection} of product details holder
     */
    public ProductDetailsHolder getLinkedProductList(final Long orgId, final Long dealId, final Boolean isPartial) {
        final Table productTable = productManager.getTable();
        final Table productDetailsHolderTable = getTable();
        final Table productDetailsTable = getProductDetailTable();
        final Table variationTable = VariationManager.getInstance().getTable();
        final Table dealTable = DealManager.getInstance().getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column productPkColumn = productTable.getPKColumn();
        final Column productHolderDealIdColumn = productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);
        final Column productDetailsHolderIdColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
        final Column productIdColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID);
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column monetaryCurrencyColumn = monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsHolderIdColumn),
                new JoinClause(productIdColumn, productPkColumn),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(productHolderDealIdColumn, dealTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn(), JoinClause.JoinType.LEFT)));

        if (isPartial) {
            selectQuery.addSelectClause(List.of(productHolderDealIdColumn, productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.ORG_ID),
                    productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.CURRENCY_ID), productDetailsHolderIdColumn,
                    productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRICE), productIdColumn));
        } else {
            selectQuery.addSelectClause(productDetailsHolderTable, productDetailsTable);
            selectQuery.addSelectClause(List.of(variationTable.getColumn(OMPOVARIATION.NAME), variationTable.getColumn(OMPOVARIATION.ENTITY_ID)));
        }
        selectQuery.addSelectClause(productTable.getColumn(MPOPRODUCT.NAME));
        selectQuery.addSelectClause(monetaryCurrencyColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(productHolderDealIdColumn, dealId)));

        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = entityContainer.getRow(productDetailsHolderTable);
        final ProductDetailsHolder productDetailsHolder = isPartial ? convertPartialRowToModel(row, entityContainer) : convertRowToModel(row, entityContainer);

        if (Objects.nonNull(productDetailsHolder)) {
            final Row monetaryRow = entityContainer.getRow(monetaryTable);
            final Short currencyId = Objects.isNull(monetaryRow) ? null : monetaryRow.get(OMETMONETARY.CURRENCY_ID);

            if (Objects.nonNull(currencyId)) {
                productDetailsHolder.setCurrency(Currency.getCurrencyById(currencyId));
            }
        }

        return productDetailsHolder;
    }

    /**
     * <p>
     * Retrieves the variation names for the products linked to a deal
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param dealId           Represents the deal id
     * @param productDetailIds Represents the collection of product ids to filter
     * @return A {@link Map} containing product entity id as key and variation name as value
     */
    public Map<Long, String> getProductVariationName(final long orgId, final long dealId, final Collection<Long> productDetailIds) {
        final Table productDetailsHolderTable = getTable();
        final Table productDetailsTable = getProductDetailTable();
        final Table variationTable = VariationManager.getInstance().getTable();
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId));

        selectQuery.addSelectClause(List.of(variationTable.getColumn(OMPOVARIATION.NAME), productDetailsTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID)));
        selectQuery.addJoinClause(List.of(new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID)),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationTable.getPKColumn(), JoinClause.JoinType.LEFT)));

        if (productDetailIds.isEmpty()) {
            selectQuery.setWhereClause(whereClause);
        } else {
            selectQuery.setWhereClause(whereClause.and(Criteria.in(productDetailsTable.getPKColumn(), productDetailIds)));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, String> variationMap = new HashMap<>();

        dataContainer.getRows(productDetailsTable).forEach(row -> {
            final Long variationId = row.get(MDOPRODUCTDETAIL.VARIATION_ID);

            if (Objects.nonNull(variationId)) {
                final Row variationRow = dataContainer.getRow(variationTable, variationId);

                variationMap.put(row.getPKValue(), variationRow.get(OMPOVARIATION.NAME));
            }
        });
        return variationMap;
    }

    /**
     * <p>
     * Converts {@link Row} to {@link ProductDetailsHolder}
     * </p>
     *
     * @param row           Represents the row of a product details holder table
     * @param dataContainer Holds the existing records of the productDetail table
     * @return {@link ProductDetail}
     */
    private ProductDetailsHolder convertPartialRowToModel(final Row row, final DataContainer dataContainer) {
        final ProductDetailsHolder productDetailsHolder = new ProductDetailsHolder();

        if (Objects.nonNull(row)) {
            final Table productDetailsTable = getProductDetailTable();
            final Short currencyId = row.get(OMDOPRODUCTDETAILSHOLDER.CURRENCY_ID);
            final Stream<Row> productDetailRows = dataContainer.getRows(productDetailsTable,
                    new WhereClause(Criteria.eq(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                            row.getPKValue())));

            productDetailsHolder.setOrgId(row.getOrgId());

            if (Objects.nonNull(currencyId)) {
                productDetailsHolder.setCurrency(Currency.getCurrencyById(currencyId));
            }

            if (Objects.nonNull(productDetailRows)) {
                productDetailRows.forEach(productDetailRow -> productDetailsHolder
                        .addProductDetail(convertRowToPartialProductDetail(productDetailRow, dataContainer)));
            }
        }

        return productDetailsHolder;
    }

    /**
     * <p>
     * Converts {@link Row} to {@link ProductDetail}
     * </p>
     * @param productDetailRow Represents the row of a product details table
     * @param dataContainer    Holds the existing records of the productDetail table
     * @return {@link ProductDetail}
     */
    private ProductDetail convertRowToPartialProductDetail(final Row productDetailRow, final DataContainer dataContainer) {
        final ProductDetail productDetail = new ProductDetail();
        final Table productTable = productManager.getTable();
        final Long productId = productDetailRow.get(MDOPRODUCTDETAIL.PRODUCT_ID);
        final Row productRow = dataContainer.getRow(productTable,
                new WhereClause(Criteria.eq(productTable.getPKColumn(), productId)));

        productDetail.setPrice(productDetailRow.get(MDOPRODUCTDETAIL.PRICE));

        if (Objects.nonNull(productRow)) {
            productDetail.setProduct(productManager.convertPartialRowToModel(productRow));
        }
        return productDetail;
    }

    /**
     * <p>
     * Checks whether the deal id are exists or not
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return If deal id is exists it will return true, otherwise it will return false
     */
    public boolean exists(final Long orgId, final Long dealId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches the deal values for the given product ids
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param productId Which contains the collection of product ids
     * @return A {@link Map} contains key as deal id value as deal product amount
     */
    public Map<Long, Double> fetchDealValuesByProductIds(final long orgId, final Collection<Long> productId) {
        return fetchDealValues(orgId, productId, getProductDetailTable().getColumn(MDOPRODUCTDETAIL.PRODUCT_ID));
    }

    /**
     * <p>
     * Fetches the deal values for the given variation ids
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param variationId Which contains the collection of variation ids
     * @return A {@link Map} contains key as deal id value as deal product amount
     */
    public Map<Long, Double> fetchDealValuesByVariationIds(final long orgId, final Collection<Long> variationId) {
        return fetchDealValues(orgId, variationId, getProductDetailTable().getColumn(MDOPRODUCTDETAIL.VARIATION_ID));
    }

    /**
     * <p>
     * Fetches the deal values for the given linked product detail references
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param entityId Which contains the collection of linked entity ids
     * @param entityIdColumn Represents the linked field in {@link MDOPRODUCTDETAIL}
     * @return A {@link Map} contains key as deal id value as deal product amount
     */
    private Map<Long, Double> fetchDealValues(final long orgId, final Collection<Long> entityId, final Column entityIdColumn) {
        if (Objects.isNull(entityId) || entityId.isEmpty()) {
            return Collections.emptyMap();
        }

        final Table produtcDetailHolderTable = getTable();
        final Table productDetailTable = getProductDetailTable();
        final Column productDetailIdColumn = productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
        final SelectQuery productDetailSelectQuery = SelectQuery.get(produtcDetailHolderTable);

        productDetailSelectQuery.addSelectClause(List.of(produtcDetailHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID),
                produtcDetailHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.TAX_INCLUSIVE),
                productDetailIdColumn, productDetailTable.getColumn(MDOPRODUCTDETAIL.AMOUNT),
                productDetailTable.getColumn(MDOPRODUCTDETAIL.TAX)));
        productDetailSelectQuery.addJoinClause(new JoinClause(produtcDetailHolderTable.getPKColumn(), productDetailIdColumn));
        productDetailSelectQuery.setWhereClause(new WhereClause(Criteria.in(entityIdColumn, entityId)));

        final DataContainer productDetailDataContainer = orgDataStore(orgId).get(productDetailSelectQuery);
        final Map<Long, Double> values = new HashMap<>();

        if (!productDetailDataContainer.isEmpty()) {
            productDetailDataContainer.getRows(produtcDetailHolderTable).forEach(row -> {
                final boolean isTaxInclusive = row.get(OMDOPRODUCTDETAILSHOLDER.TAX_INCLUSIVE);
                final Stream<Row> productDetailRows = productDetailDataContainer.getRows(productDetailTable,
                        new WhereClause(Criteria.eq(productDetailIdColumn,
                                row.getPKValue())));
                final double value = productDetailRows.map(productDetailRow -> {
                    final double amount = productDetailRow.get(MDOPRODUCTDETAIL.AMOUNT);
                    final double tax = productDetailRow.get(MDOPRODUCTDETAIL.TAX);

                    return isTaxInclusive ? (amount + (amount * tax / 100)) : amount;
                }).reduce(0.0, Double::sum);

                values.put(row.get(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), value);
            });
        }

        return values;
    }
}