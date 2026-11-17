package com.tlc.crm.core.api.product.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.model.ProductDealHolder;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.sql.resource.MPODROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MPOPRICETAG;
import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.entity.DefaultFieldsDataLoader;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;

import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Product} related DB actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ProductManager extends EntityConfigManager<Product> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductManager.class);

    private final EntityLoader entityLoader;
    private final EntityHandler entityHandler;
    private final VariationManager variationManager;
    private final DefaultFieldsDataLoader defaultFieldsDataLoader;
    private final UserService userService;
    private final OptionService optionService;
    private final BaseCurrencyService baseCurrencyService;
    private final AuditLogger auditLogger;
    private final ModificationHandler modificationHandler;

    private static class Instance {
        private static final ProductManager INSTANCE = new ProductManager();
    }

    private ProductManager() {
        entityLoader = EntityLoader.getInstance();
        entityHandler = EntityHandler.getInstance();
        variationManager = VariationManager.getInstance();
        defaultFieldsDataLoader = DefaultFieldsDataLoader.getInstance();
        userService = UserService.getInstance();
        optionService = OptionService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        auditLogger = AuditLogger.getInstance();
        modificationHandler = ModificationHandler.getInstance();
    }

    public static ProductManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(MPOPRODUCT.TABLE_NAME);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EntityHandler}
     */
    @Override
    protected EntityHandler getEntityHandler() {
        return entityHandler;
    }

    /**
     * <p>
     * Gets the product dropdown table
     * </p>
     *
     * @return The product dropdown table
     */
    public Table getProductDropdownTable() {
        return Table.get(MPODROPDOWNFIELDS.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Product} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Product ids
     * @return Collection of {@link Product}
     */
    @Override
    public Collection<Product> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer entityContainer = fetchDataContainer(orgId, false, ids);

        return entityContainer.getRows(getTable()).map(row -> convertRowToModel(row, entityContainer)).toList();
    }

    /**
     * <p>
     * Fetches the {@link Product}
     * </p>
     *
     * @param orgId Represents the Organization id
     * @param id    Represents the Product id
     * @return The {@link Product}
     */
    public Product get(final Long orgId, final Long id, final boolean isUpdateEvent) {
        final DataContainer entityContainer = fetchDataContainer(orgId, isUpdateEvent, List.of(id));

        return entityContainer.getRows(getTable()).map(row -> convertRowToModel(row, entityContainer)).findFirst().orElseGet(null);
    }

    /**
     * Fetches the {@link DataContainer} that holds the products data
     *
     * @param orgId       Represents the organisation ID
     * @param productIds  Which contains the collection of product IDs
     * @return {@link DataContainer} that holds the products data
     */
    public DataContainer fetchDataContainer(final Long orgId, final Collection<Long> productIds) {
        return fetchDataContainer(orgId, true, productIds);
    }

    /**
     * Fetches the {@link DataContainer} that holds the products data
     *
     * @param orgId      Represents the organisation ID
     * @param productIds Which contains the collection of product IDs
     * @return {@link DataContainer} that holds the products data
     */
    public DataContainer fetchDataContainer(final Long orgId, final boolean isUpdateEvent,
                                            final Collection<Long> productIds) {
        if (isEntityIdCollectionEmpty(productIds)) {
            return DataContainer.create();
        }
        final Collection<FieldInfo> fields = getProductFields(orgId);
        final Table productTable = getTable();
        final Column productPkColumn = productTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(productTable);

        selectQuery.addSelectClause(productTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(productPkColumn, productIds)));

        if (isUpdateEvent) {
            final Table variationTable = variationManager.getTable();
            final Table priceTagTable = variationManager.getPriceTagTable();
            final Column priceTagProductIdColumn = priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID);
            final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

            selectQuery.addSelectClause(priceTagTable, variationTable);
            selectQuery.addJoinClause(List.of(new JoinClause(productPkColumn, priceTagProductIdColumn, leftJoin),
                    new JoinClause(productPkColumn, variationTable.getColumn(OMPOVARIATION.ENTITY_ID), leftJoin)));
        }
        return getEntityContainer(orgId, productIds, selectQuery, fields);
    }

    /**
     * <p>
     * Saves the attributes of {@link Product} into the database
     * </p>
     *
     * @param product An entity that holds the product attributes which have to be saved into database
     */
    @Override
    public Product create(Product product) {
        final long orgId = product.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadProductIntoContainer(true, product, targetContainer);
        orgDataStore(product.orgId()).commitChanges(targetContainer);

        auditLogger.log(orgId, List.of(new AuditLogData(product.getCreatedBy().id(), product.id(), product, AuditSource.PRODUCT, AuditAction.ADDED)));

        return product;
    }

    /**
     * <p>
     * Updates the given products
     * </p>
     *
     * @param orgId    Organization id
     * @param products Collection of products that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Product> products) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<FieldInfo> fields = getProductFields(orgId);
        final Collection<Long> ids = products.stream().map(Product::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = fetchDataContainer(orgId, true, ids);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditSource auditSource = AuditSource.PRODUCT;
        final AuditAction auditAction = AuditAction.UPDATED;

        products.forEach(product -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadProductIntoContainer(false, fields, product, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(product.id());
                final Product oldProduct = convertRowToModel(entityContainer.getRow(getTable()));

                auditLogDataList.add(new AuditLogData(product.getUpdatedBy().id(), product.id(), oldProduct, product, auditSource, auditAction));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Product update failed Org ID : {} ID : {} reason : {}", orgId, product.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, product.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * <p>
     * Converts the product row into {@link Product} details
     * </p>
     *
     * @param row Represents the product row
     * @return The {@link Product} details
     */
    private Product convertRowToModel(final Row row) {
        final Product product = new Product();
        final long orgId = row.getOrgId();

        product.setOrgId(orgId);
        product.setId(row.getPKValue());
        product.setName(row.get(MPOPRODUCT.NAME));
        product.setUpdatedBy(userService.getUser(orgId, row.get(MPOPRODUCT.UPDATED_BY)));

        return product;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void delete(final Long orgId, final Collection<Product> products) {
        final Set<Long> ids = new HashSet<>();
        final AuditSource auditSource = AuditSource.PRODUCT;
        final AuditAction auditAction = AuditAction.DELETED;
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final DataContainer targetContainer = DataContainer.create();
        final Table table = getTable();

        for (final Product product : products) {
            final Long id = product.id();

            if (Objects.nonNull(id)) {
                ids.add(id);
            }
            auditLogDataList.add(new AuditLogData(product.getUpdatedBy().id(), product.id(), product, auditSource, auditAction));
        }

        for (final Long productId : ids) {
            targetContainer.deleteRow(new Row(table, productId));
        }
        DealManager.getInstance().updateDealValues(orgId, ids, targetContainer);

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<Column> getTerritoryColumn() {
        return Optional.empty();
    }

    /**
     * <p>
     * Fetches the user defined entity of an organization along with system entity
     * </p>
     *
     * @param orgId Organization id
     * @return Collection of entity
     */
    private Collection<FieldInfo> getProductFields(Long orgId) {
        return secureFieldService.getFieldsBySource(orgId, Product.ENTITY_TYPE);
    }

    /**
     * <p>
     * Loads the product details into the target container
     * </p>
     *
     * @param product         An entity that holds the product attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadProductIntoContainer(final boolean isCreateEvent, final Product product, final DataContainer targetContainer) {
        final Long orgId = product.orgId();
        final Long productId = product.id();
        final Collection<FieldInfo> fields = getProductFields(orgId);
        final DataContainer entityContainer = isCreateEvent ? null
                : entityLoader.getEntityContainerByIds(orgId, getTable(), fields, List.of(productId));

        loadProductIntoContainer(isCreateEvent, fields, product, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the product details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param fields          Product entity that have to be loaded into the container
     * @param product         An entity that holds the product attributes
     * @param sourceContainer Holds the user defined and system field values of existing product
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadProductIntoContainer(final boolean isCreateEvent, final Collection<FieldInfo> fields,
                                         final Product product, final DataContainer sourceContainer,
                                         final DataContainer targetContainer) {
        final Table productTable = getTable();
        final Long productId = product.id();
        final Variation defaultVariation = product.getDefaultVariation();
        final Row productRow = isCreateEvent ? new Row(productTable)
                : Optional.ofNullable(sourceContainer.getRow(productTable, productId)).map(Row::new).orElse(null);
        final Long orgId = product.orgId();
        final long currentTime = Instant.now().toEpochMilli();
        final DateTime createdTime = product.getCreatedTime();

        if (!isCreateEvent && Objects.isNull(productRow)) {
            LOGGER.error("Product not found for the ORG ID: {}, ID: {}", orgId, productId);
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "Product not found");
        }

        if (productRow.isNewRow()) {
            productRow.setOrgId(orgId);
            product.setId(productRow.getPKValue());
            product.setEntityState(Entity.State.NEW);
            productRow.set(MPOPRODUCT.CREATED_TIME, Objects.isNull(createdTime) ? currentTime : createdTime);
            productRow.set(MPOPRODUCT.CREATED_BY, product.getCreatedBy().id());
            productRow.set(MPOPRODUCT.LAST_ASSIGNED_AT, currentTime);
        } else {
            product.setEntityState(Entity.State.EXISTING);

            if (Objects.nonNull(createdTime)) {
                productRow.setIfModified(MPOPRODUCT.CREATED_TIME, createdTime);
            }
        }

        if (!isCreateEvent) {
            if (product.hasSalesOwner() && !Objects.equals(productRow.get(MPOPRODUCT.OWNER_ID), product.getSalesOwner().id())) {
                productRow.set(MPOPRODUCT.LAST_ASSIGNED_AT, currentTime);
            }
        }

        loadProductRowIntoContainer(isCreateEvent, product, productRow, targetContainer, sourceContainer);
        defaultFieldsDataLoader.loadData(new DataLoaderValues<>(isCreateEvent, productTable, product, fields, sourceContainer, targetContainer));
        entityLoader.buildEntity(isCreateEvent, product, fields, productTable, sourceContainer, targetContainer);

        if (!isCreateEvent && Objects.nonNull(defaultVariation)) {
            deleteUnmappedPriceTags(productId, defaultVariation.id(), defaultVariation.getPriceTags(), sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Removes the unmapped price tags details
     * </p>
     *
     * @param productId       Represents the product id
     * @param variationId     Represents the variant id
     * @param priceTags       An entity that holds the price tags attributes
     * @param sourceContainer Holds the user defined and system field values of existing product
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedPriceTags(final Long productId, final Long variationId, final Collection<PriceTag> priceTags,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table priceTagTable = variationManager.getPriceTagTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID),
                productId)).and(new WhereClause(Criteria.notIn(priceTagTable.getPKColumn(),
                priceTags.stream().map(PriceTag::id).collect(Collectors.toUnmodifiableSet())))).and(
                Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), variationId));
        final Stream<Row> priceTagRows = sourceContainer.getRows(priceTagTable, whereClause);

        if (Objects.nonNull(priceTagRows)) {
            priceTagRows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the related entities of product table into the data container
     * </p>
     *
     * @param product       An entity that holds the product attributes
     * @param row           Represents the row of a product table
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    private void loadProductRowIntoContainer(final boolean isCreateEvent, final Product product, final Row row, final DataContainer dataContainer,
                                             final DataContainer sourceContainer) {
        final Variation variation = product.getDefaultVariation();
        final Long productId = product.id();
        final CrmUser salesOwner = product.getSalesOwner();
        final CrmUser updatedBy = product.getUpdatedBy();
        final long currentTime = Instant.now().toEpochMilli();

        if (Objects.nonNull(variation)) {
            variation.setProductId(productId);

            if (!isCreateEvent) {
                variationManager.loadVariationIntoContainer(false, variation, sourceContainer, dataContainer);
            } else {
                variationManager.loadVariationIntoContainer(true, variation, dataContainer);
            }
        }
        row.set(MPOPRODUCT.MODIFIED_TIME, currentTime);
        row.set(MPOPRODUCT.NAME, product.getName());
        row.set(MPOPRODUCT.DESCRIPTION, product.getDescription());
        row.set(MPOPRODUCT.PRODUCT_CODE, product.getProductCode());
        row.set(MPOPRODUCT.SKU_NUMBER, product.getSkuNumber());
        row.set(MPOPRODUCT.VALID_TILL, product.getValidTill());
        row.set(MPOPRODUCT.QUANTITY_ORDERED, product.getQuantityOrdered());
        row.set(MPOPRODUCT.QUANTITY_IN_STOCK, product.getQuantityInStock());
        row.set(MPOPRODUCT.OWNER_ID, salesOwner.id());
        row.set(MPOPRODUCT.UPDATED_BY, updatedBy.id());
        dataContainer.updateRow(row);
    }

    /**
     * <p>
     * Converts row into {@link Product}
     * </p>
     *
     * @param row Product row to be converted
     * @return {@link Product}
     */
    public Product convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Product product = new Product();
        final Long orgId = row.getOrgId();
        final Long productId = row.getPKValue();
        final Variation variation = variationManager.getDefaultProductVariation(orgId, productId);
        final Long createdById = row.get(MPOPRODUCT.CREATED_BY);
        final Long updatedById = row.get(MPOPRODUCT.UPDATED_BY);
        final Long salesOwnerId = row.get(MPOPRODUCT.OWNER_ID);
        final Collection<Long> userIds = new HashSet<>();
        final String categoryKey = ProductFields.CATEGORY;
        final String activeStatusKey = ProductFields.ACTIVE_STATUS;
        final String typeKey = ProductFields.TYPE;
        final String taxableKey = ProductFields.TAXABLE;
        final Map<String, FieldInfo> productFields = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE,
                categoryKey, activeStatusKey, typeKey, taxableKey);
        final Table productDropdownTable = getProductDropdownTable();
        final Long activeStatusId = productFields.get(activeStatusKey).id();
        final Long typeId = productFields.get(typeKey).id();
        final Long categoryId = productFields.get(categoryKey).id();
        final Long taxableId = productFields.get(taxableKey).id();
        final Map<Long, Consumer<Option>> options = new HashMap<>();

        if (Objects.nonNull(variation)) {
            product.setDefaultVariation(variation);
        }

        product.setId(productId);
        product.setOrgId(orgId);
        product.setName(row.get(MPOPRODUCT.NAME));
        product.setDescription(row.get(MPOPRODUCT.DESCRIPTION));
        product.setProductCode(row.get(MPOPRODUCT.PRODUCT_CODE));
        product.setSkuNumber(row.get(MPOPRODUCT.SKU_NUMBER));
        product.setCreatedTime(row.get(MPOPRODUCT.CREATED_TIME));
        product.setModifiedTime(row.get(MPOPRODUCT.MODIFIED_TIME));
        product.setValidTill(row.get(MPOPRODUCT.VALID_TILL));
        product.setTaxable(row.get(MPOPRODUCT.TAXABLE));
        product.setQuantityOrdered(row.get(MPOPRODUCT.QUANTITY_ORDERED));
        product.setQuantityInStock(row.get(MPOPRODUCT.QUANTITY_IN_STOCK));
        entityLoader.setEntityValues(product, getTable(), dataContainer);
        final ValueHolder valueHolder = product.getValueHolder();

        if (Objects.nonNull(valueHolder)) {
            userIds.addAll(valueHolder.getUserValues().stream().map(CrmUser::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(createdById)) {
            product.setCreatedBy(userService.getUser(orgId, createdById));
        }

        if (Objects.nonNull(updatedById)) {
            product.setUpdatedBy(userService.getUser(orgId, updatedById));
        }

        if (Objects.nonNull(salesOwnerId)) {
            product.setSalesOwner(userService.getUser(orgId, salesOwnerId));
        }

        product.setLastAssignedAt(row.get(MPOPRODUCT.LAST_ASSIGNED_AT));
        userIds.add(createdById);
        userIds.add(updatedById);
        userIds.add(salesOwnerId);
        product.setUserIds(userIds);
        product.setBaseCurrencyPriceTags(variationManager.getBaseCurrencyPriceTags(orgId, productId));

        options.put(activeStatusId, product::setActiveStatus);
        options.put(typeId, product::setType);
        options.put(categoryId, product::setCategory);
        options.put(taxableId, product::setTaxable);

        dataContainer.getRows(productDropdownTable).forEach(optionRow -> {
            final Option option = optionService.get(orgId, optionRow.get(MPODROPDOWNFIELDS.FIELD_ID),
                    optionRow.get(MPODROPDOWNFIELDS.OPTION_ID));
            final Long fieldId = option.getFieldId();

            options.getOrDefault(fieldId, (field) -> {
            }).accept(option);
        });

        return product;
    }

    /**
     * Fetches the {@link Product} for the dropdown actions.
     *
     * @param orgId    Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of products.
     */
    public Collection<Product> getProductDropdownList(final Long orgId, final DropdownLimit limit, final DropdownCriteria criteria) {
        return getProductDropdownList(orgId, limit, criteria, false, null, null);
    }

    /**
     * Fetches the {@link Product} for the dropdown actions.
     *
     * @param orgId    Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return Collection of products.
     */
    public Collection<Product> getProductDropdownList(final Long orgId, final DropdownLimit limit, final DropdownCriteria criteria,
                                                      final boolean includeUnitPrice, final Currency currency, final SortConfig sortConfig) {
        final Table productTable = getTable();
        final Column productNameColumn = productTable.getColumn(MPOPRODUCT.NAME);
        final SelectQuery selectQuery = SelectQuery.get(productTable);

        selectQuery.addSelectClause(productNameColumn);
        selectQuery.setWhereClause(new WhereClause(criteria.build(productNameColumn)));
        final Table priceTagTable = includeUnitPrice ? variationManager.getPriceTagTable() : null;

        if (includeUnitPrice && Objects.nonNull(currency)) {
            final Table variationTable = variationManager.getTable();
            final Column productPkColumn = productTable.getPKColumn();
            final SelectQuery priceTagSubQuery = SelectQuery.get(priceTagTable);

            priceTagSubQuery.addSelectClause(priceTagTable);
            priceTagSubQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), currency.getId())));
            final DerivedTable priceTagDerivedTable = DerivedTable.get(priceTagTable.getName(), priceTagTable.getAlias(), priceTagSubQuery);

            selectQuery.addSelectClause(List.of(priceTagDerivedTable.getColumn(MPOPRICETAG.PRICE),
                    priceTagDerivedTable.getColumn(MPOPRICETAG.CURRENCY_ID), priceTagDerivedTable.getColumn(MPOPRICETAG.ENTITY_ID),
                    priceTagTable.getColumn(MPOPRICETAG.TAX), priceTagTable.getColumn(MPOPRICETAG.DISCOUNT)
            ));
            selectQuery.addJoinClause(List.of(
                    new JoinClause(productPkColumn, variationTable.getColumn(OMPOVARIATION.ENTITY_ID), JoinClause.JoinType.LEFT),
                    new JoinClause(variationTable.getPKColumn(), priceTagDerivedTable.getColumn(MPOPRICETAG.VARIATION_ID), JoinClause.JoinType.LEFT)));
        }

        if (Objects.nonNull(sortConfig)) {
            final String fieldName = sortConfig.fieldName();
            final OrderByClause.OrderType orderType = sortConfig.type().getOrderType();

            switch (fieldName) {
                case "name" -> selectQuery.addOrderByClause(new OrderByClause(productNameColumn, orderType));
                case "unitPrice" -> {
                    if (includeUnitPrice) {
                        selectQuery.addOrderByClause(new OrderByClause(priceTagTable.getColumn(MPOPRICETAG.PRICE), orderType));
                    }
                }
                default -> selectQuery.addOrderByClause(new OrderByClause(productTable.getPKColumn()));
            }
        } else {
            selectQuery.addOrderByClause(new OrderByClause(productTable.getPKColumn()));
        }
        selectQuery.setLimitClause(limit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(productTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND);
        } else {
            final List<Product> productList = new ArrayList<>();

            rows.forEach(row -> {
                final Product product = new Product();

                product.setId(row.getPKValue());
                product.setName(row.get(MPOPRODUCT.NAME));

                if (includeUnitPrice) {
                    final Row priceTagRow = dataContainer.getRow(priceTagTable,
                            new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID), row.getPKValue()))
                                    .and(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), currency.getId())));

                    if (Objects.nonNull(priceTagRow)) {
                        final Variation variation = new Variation();
                        final PriceTag priceTag = new PriceTag();

                        priceTag.setTax(priceTagRow.get(MPOPRICETAG.TAX));
                        priceTag.setPrice(priceTagRow.get(MPOPRICETAG.PRICE));
                        priceTag.setDiscount(priceTagRow.get(MPOPRICETAG.DISCOUNT));
                        priceTag.setCurrency(currency);
                        variation.addPriceTag(priceTag);
                        product.setDefaultVariation(variation);
                    }
                }
                productList.add(product);
            });

            return productList;
        }
    }

    /**
     * Gets the variation IDs for the given product IDs.
     *
     * @param orgId      Refers the organization ID.
     * @param productIds Refers the collection of product IDs.
     * @return The variation IDs for the given product IDs.
     */
    public Collection<Long> getVariationProductIds(final Long orgId, final Set<Long> productIds) {
        final Table variationTable = variationManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);
        final Column variationProductIdColumn = variationTable.getColumn(OMPOVARIATION.ENTITY_ID);

        selectQuery.addSelectClause(variationProductIdColumn.distinct());
        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(variationTable.getColumn(OMPOVARIATION.NAME), Variation.DEFAULT))
                .and(Criteria.in(variationProductIdColumn, productIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(variationTable).map(row -> (Long) row.get(OMPOVARIATION.ENTITY_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Product> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * Converts partial Row in to {@link Product}
     *
     * @param row Represents the row of product
     * @return {@link Product}
     */
    public Product convertPartialRowToModel(final Row row) {
        final Product product = new Product();

        product.setId(row.getPKValue());
        product.setName(row.get(MPOPRODUCT.NAME));

        return product;
    }

    /**
     * <p>
     * Updates the given price tag values
     * </p>
     *
     * @param orgId   Organization id
     * @param userId  Represents an user id
     * @param product An entity that holds the product attributes
     * @return the {@link List<Long>} of updated records
     */
    public List<Long> updatePriceTags(final Long orgId, final long userId, final Product product) {
        final List<Long> updatedRecords = new ArrayList<>();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table priceTagTable = variationManager.getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(priceTagTable);
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Variation defaultVariation = product.getDefaultVariation();
        final Collection<PriceTag> priceTags = Objects.nonNull(defaultVariation) ? defaultVariation.getPriceTags() : null;
        final Long productId = product.id();
        final Long id = defaultVariation.id();
        final Long variationId;

        if (Objects.isNull(id)) {
            final Row variationRow = new Row(variationManager.getTable());

            variationRow.set(OMPOVARIATION.ENTITY_ID, defaultVariation.getProductId());
            variationRow.set(OMPOVARIATION.NAME, defaultVariation.getName());
            targetContainer.updateRow(variationRow);

            variationId = variationRow.getPKValue();
        } else {
            variationId = id;
        }

        if (Objects.nonNull(priceTags)) {
            final WhereClause whereClause = new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID), productId)).
                    and(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), variationId));
            final Column priceTagPKColumn = priceTagTable.getPKColumn();

            priceTags.forEach(priceTag -> {
                final Long priceTagId = priceTag.id();
                final Row priceTagRow = Objects.isNull(priceTagId) ? new Row(priceTagTable) : entityContainer.getRow(priceTagTable,
                        whereClause.and(Criteria.eq(priceTagPKColumn, priceTagId)));

                if (Objects.isNull(priceTagRow)) {
                    throw ErrorCode.get(ProductErrorCodes.PRICE_TAG_NOT_FOUND, String.format(" ID : %s", priceTagId));
                }
                priceTagRow.set(MPOPRICETAG.PRICE, baseCurrencyService.applyPrecision(orgId, BigDecimal.valueOf(priceTag.getPrice())));
                priceTagRow.set(MPOPRICETAG.ENTITY_ID, productId);
                priceTagRow.set(MPOPRICETAG.CURRENCY_ID, priceTag.getCurrency().getId());
                priceTagRow.set(MPOPRICETAG.TAX, priceTag.getTax());
                priceTagRow.set(MPOPRICETAG.DISCOUNT, priceTag.getDiscount());
                priceTagRow.set(MPOPRICETAG.VARIATION_ID, variationId);

                if (priceTagRow.isNewRow()) {
                    priceTagRow.setOrgId(priceTag.orgId());
                    priceTag.setId(priceTagRow.getPKValue());
                    targetContainer.addNewRow(priceTagRow);
                } else {
                    targetContainer.updateRow(priceTagRow);
                    updatedRecords.add(priceTagId);
                }
            });

            if (priceTags.stream().map(PriceTag::getTax).anyMatch(tax -> tax > 0.0)) {
                updateTaxableField(orgId, productId, targetContainer);
            }
        }
        try {
            modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(productId), Source.PRODUCT, targetContainer);
            orgDataStore.commitChanges(targetContainer);
        } catch (ErrorCode e) {
            throw ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED);
        }

        return updatedRecords;
    }

    /**
     * <p>
     * Checks whether the given organization has any product
     * </p>
     *
     * @param orgId Represents an organization id
     * @return true, if products present
     */
    public boolean doesOrgHaveProducts(final Long orgId) {
        final CountQuery countQuery = CountQuery.get(getTable());

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the taxable field for the given product id
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param productId       Represents the product id
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void updateTaxableField(final long orgId, final Long productId, final DataContainer targetContainer) {
        final Table dropdownTable = Table.get(MPODROPDOWNFIELDS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(dropdownTable);
        final String taxableField = ProductFields.TAXABLE;
        final FieldInfo fieldInfo = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE, taxableField).get(taxableField);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dropdownTable.getColumn(MPODROPDOWNFIELDS.FIELD_ID), fieldInfo.id()))
                .and(Criteria.eq(dropdownTable.getColumn(MPODROPDOWNFIELDS.ENTITY_ID), productId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(dropdownTable);

        if (Objects.nonNull(row)) {
            final Collection<Option> options = optionService.get(orgId, fieldInfo);
            final Optional<Long> optionalDefaultOptionId = options.stream().filter(option -> Objects.equals(Product.Taxable.YES.getName(),
                    option.getOption())).map(Option::id).findFirst();
            final Long optionId = row.get(MPODROPDOWNFIELDS.OPTION_ID);

            optionalDefaultOptionId.ifPresent(defaultOptionId -> {

                if (!Objects.equals(defaultOptionId, optionId)) {
                    row.set(MPODROPDOWNFIELDS.OPTION_ID, defaultOptionId);
                    targetContainer.updateRow(row);
                }
            });

        }
    }

    /**
     * <p>
     * Retrieves the total product count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents a dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(MPOPRODUCT.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the name of the {@link Product}
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param productId Represents the product id
     * @return the product name
     */
    public String getProductName(final long orgId, final Long productId) {
        final Table table = getTable();
        final Optional<Row> productRow = Optional.ofNullable(orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getPKColumn(), productId))).getRow(table));

        return productRow.map(row -> (String) row.get(MPOPRODUCT.NAME)).orElseGet(() -> null);
    }

    /**
     * <p>
     * Gets the names of the products
     * </p>
     *
     * @param orgId               Represents the organization id
     * @param productIds          Represents the collection of product ids
     * @param groupedVariationIds Refers the variation IDs.
     * @return the collection of product name
     */
    public Collection<String> getProductNames(final long orgId, final Collection<Long> productIds, final Map<Long, Long> groupedVariationIds,
                                              final DataContainer targetContainer) {
        final Collection<String> mappedProductNames = new ArrayList<>();
        final Table table = getTable();
        final Column pkColumn = table.getPKColumn();
        final Table variationTable = variationManager.getTable();
        final Table productDetailTable = Table.get(MDOPRODUCTDETAIL.TABLE);
        final SelectQuery innerQuery = SelectQuery.get(variationTable);
        final Collection<Long> variationIds = groupedVariationIds.values().stream().filter(Objects::nonNull).toList();

        innerQuery.addSelectClause(variationTable);

        if (!variationIds.isEmpty()) {
            innerQuery.setWhereClause(new WhereClause(Criteria.in(variationTable.getPKColumn(), variationIds)));
        }

        final DerivedTable derivedTable = DerivedTable.get(variationTable.getName(), variationTable.getAlias(), innerQuery);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(pkColumn, table.getColumn(MPOPRODUCT.NAME),
                derivedTable.getPKColumn(), derivedTable.getColumn(OMPOVARIATION.NAME), derivedTable.getColumn(OMPOVARIATION.ENTITY_ID)));
        selectQuery.addJoinClause(new JoinClause(pkColumn, derivedTable.getColumn(OMPOVARIATION.ENTITY_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(pkColumn, productIds)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        for (final Map.Entry<Long, Long> entry : groupedVariationIds.entrySet()) {
            final Row row = targetContainer.getRow(productDetailTable, entry.getKey());
            final Long productId = row.get(MDOPRODUCTDETAIL.PRODUCT_ID);

            if (productIds.contains(productId)) {
                final Row productRow = Optional.ofNullable(dataContainer.getRow(table, productId))
                        .orElseGet(() -> targetContainer.getRow(table, productId));

                if (Objects.isNull(productRow)) {
                    LOGGER.warn("Product: Product details not found. Skipping product. Org ID: {}, Product ID: {}", orgId, productId);
                    continue;
                }

                Optional.ofNullable(row.get(MDOPRODUCTDETAIL.VARIATION_ID)).ifPresentOrElse(variationId -> {
                    final Row variationRow = dataContainer.getRow(derivedTable, (Long) variationId);

                    if (Objects.nonNull(variationRow)) {
                        mappedProductNames.add(String.format("%s (%s)", productRow.get(MPOPRODUCT.NAME), variationRow.get(OMPOVARIATION.NAME)));
                    } else {
                        mappedProductNames.add(productRow.get(MPOPRODUCT.NAME));
                    }
                }, () -> mappedProductNames.add(productRow.get(MPOPRODUCT.NAME)));
            }
        }

        return mappedProductNames;
    }

    /**
     * <p>
     * Gets the {@link Collection} products with deals mapped for the given organization id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param start    Represents the starting index of {@link ProductDealHolder} to be fetched
     * @param limit    Represents the maximum number of {@link ProductDealHolder} to be fetched
     * @param criteria Represents the {@link DropdownCriteria}
     * @return {@link Collection} of {@link ProductDealHolder}
     */
    public Collection<ProductDealHolder> getProductDealHolder(final long orgId, final int start, final int limit, final DropdownCriteria criteria) {
        final Table productTable = getTable();
        final Table priceTagTable = variationManager.getPriceTagTable();
        final Column productNameColumn = productTable.getColumn(MPOPRODUCT.NAME);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Column productPkColumn = productTable.getPKColumn();
        final Column pricetagEntityIdColumn = priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID);
        final Table productDetailTable = Table.get(MDOPRODUCTDETAIL.TABLE);
        final Table productDetailsHolderTable = Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
        final Column productIdColumn = productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID);
        final Column detailsHolderIdColumn = productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
        final Collection<ProductDealHolder> productDealHolders = new ArrayList<>();
        final SelectQuery productDerivedTableSelectQuery = SelectQuery.get(productTable);
        final Column productOrgIdColumn = productTable.getColumn(MPOPRODUCT.ORG_ID);
        final WhereClause orgIdCriteria = new WhereClause(Criteria.eq(productOrgIdColumn, orgId));

        productDerivedTableSelectQuery.addSelectClause(List.of(productPkColumn, productOrgIdColumn));
        productDerivedTableSelectQuery.setWhereClause(Objects.isNull(criteria) ? orgIdCriteria :
                orgIdCriteria.and(new WhereClause(criteria.build(productNameColumn))));
        productDerivedTableSelectQuery.addOrderByClause(new OrderByClause(productPkColumn));
        productDerivedTableSelectQuery.setLimitClause(new LimitClause(start, limit));
        final DerivedTable productDerivedTable = DerivedTable.get(productTable.getName(), ProductFields.PRODUCTS, productDerivedTableSelectQuery);
        final SelectQuery selectQuery = SelectQuery.get(productDerivedTable);

        selectQuery.addSelectClause(List.of(productNameColumn, priceTagTable.getColumn(MPOPRICETAG.PRICE),
                priceTagTable.getColumn(MPOPRICETAG.TAX), priceTagTable.getColumn(MPOPRICETAG.DISCOUNT), pricetagEntityIdColumn,
                priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), productTable.getColumn(MPOPRODUCT.ORG_ID),
                productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), detailsHolderIdColumn, productIdColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(productDerivedTable.getColumn(MPOPRODUCT.ID), productPkColumn),
                new JoinClause(productPkColumn, pricetagEntityIdColumn, leftJoin),
                new JoinClause(productPkColumn, productIdColumn, leftJoin),
                new JoinClause(detailsHolderIdColumn, productDetailsHolderTable.getPKColumn(), leftJoin)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(productTable).forEach(row -> {
            final ProductDealHolder productDealHolder = new ProductDealHolder();
            final Product product = convertRowToModel(row, dataContainer);
            final Stream<Row> productDetailRows = dataContainer.getRows(productDetailTable, new WhereClause(Criteria.eq(productIdColumn, product.id())));
            final Collection<Long> dealIds = new HashSet<>();

            productDetailRows.forEach(productDetailRow -> {
                final Row productDetailHolderRow = dataContainer.getRow(productDetailsHolderTable, (Long) productDetailRow.get(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID));

                if (Objects.nonNull(productDetailHolderRow)) {
                    dealIds.add(productDetailHolderRow.get(OMDOPRODUCTDETAILSHOLDER.DEAL_ID));
                }
            });

            productDealHolder.setProduct(product);
            productDealHolder.setDealIds(dealIds);
            productDealHolders.add(productDealHolder);
        });

        return productDealHolders;
    }

    /**
     * <p>
     * Gets the total counts of product mapped for the given organization
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the total count of product
     */
    public Long getTotalCount(final long orgId, final DropdownCriteria dropdownCriteria) {
        final Table productTable = getTable();
        final CountQuery countQuery = CountQuery.get(productTable);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(productTable.getColumn(MPOPRODUCT.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches a collection of {@link Product} for the given owner ids
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerIds Which contains the collection of owner ids
     * @return The collection of {@link Product}
     */
    public Collection<Product> getProducts(final long orgId, final Collection<Long> ownerIds) {
        final Table productTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(productTable);
        final Column ownerIdColumn = productTable.getColumn(MPOPRODUCT.OWNER_ID);

        selectQuery.addSelectClause(List.of(productTable.getColumn(MPOPRODUCT.NAME), ownerIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(ownerIdColumn, ownerIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(productTable).map(productRow -> {
            final Product product = new Product();

            product.setOrgId(orgId);
            product.setId(productRow.getPKValue());
            product.setName(productRow.get(MPOPRODUCT.NAME));
            product.setOwnerId(productRow.get(MPOPRODUCT.OWNER_ID));

            return product;
        }).toList();
    }

    /**
     * <p>
     * Gets the product profile details
     * </p>
     *
     * @param orgId     Represents the organisation Id
     * @param productId Represents the product Id
     * @return The {@link Product} profile
     */
    public Product getProfile(final Long orgId, final Long productId) {
        final Table productTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(productTable, new WhereClause(Criteria.eq(productTable.getPKColumn(), productId)),
                List.of(productTable.getColumn(MPOPRODUCT.ORG_ID), productTable.getColumn(MPOPRODUCT.NAME), productTable.getColumn(MPOPRODUCT.OWNER_ID)));
        final Row productRow = dataContainer.getRow(productTable);

        return convertProfile(productRow, dataContainer);
    }

    /**
     * <p>
     * Converts the row into {@link Product}
     * </p>
     *
     * @param productRow    The {@link Row} containing product data
     * @param dataContainer The {@link DataContainer}
     * @return The {@link Product}
     */
    private Product convertProfile(final Row productRow, final DataContainer dataContainer) {
        final Long orgId = productRow.getOrgId();
        final Long ownerId = productRow.get(MPOPRODUCT.OWNER_ID);
        final Product product = new Product();

        product.setId(productRow.getPKValue());
        product.setOrgId(orgId);
        product.setName(productRow.get(MPOPRODUCT.NAME));
        product.setSalesOwner(userService.getUser(orgId, ownerId));
        product.setUserIds(List.of(ownerId));
        final Variation defaultProductVariation = variationManager.getDefaultProductVariation(orgId, productRow.getPKValue());

        if (Objects.isNull(defaultProductVariation)) {
            final Variation variation = new Variation();
            final PriceTag priceTag = new PriceTag();

            priceTag.setCurrency(baseCurrencyService.getDefaultCurrency(orgId));
            priceTag.setPrice((double) 0);
            variation.addPriceTag(priceTag);
            product.setDefaultVariation(variation);
        } else {
            product.setDefaultVariation(defaultProductVariation);
        }
        return product;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the org id
     * @param entityId Represents the entity id
     * @return the user ids
     */
    @Override
    public Collection<Long> getRestrictedIamUserIdsForEntity(final Long orgId, final Long entityId) {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Gets the activity product mapping table
     * </p>
     *
     * @return The activity product mapping {@link Table}
     */
    @Override
    public Table getActivityEntityTable() {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.product.unsupported.action");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param entityId  Represents the entity id
     * @return The entity name
     */
    @Override
    public String getName(final Long orgId, final Long entityId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, entityId, List.of(table.getColumn(MPOPRODUCT.NAME)));

        return Objects.isNull(row) ? null : row.get(MPOPRODUCT.NAME);
    }
}
