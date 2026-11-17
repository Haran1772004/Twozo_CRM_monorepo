package com.tlc.crm.core.api.product.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.page.Page;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.core.sql.resource.MPOPRICETAG;
import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Variation} related DB actions
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class VariationManager extends AbstractConfigManager<Variation> {

    private static final String DEFAULT = "default";

    private final BaseCurrencyService baseCurrencyService;

    private static class Instance {
        private static final VariationManager INSTANCE = new VariationManager();
    }

    private VariationManager() {
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * Gets the {@link VariationManager} Instance
     *
     * @return {@link Instance} of variation manager
     */
    public static VariationManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return variation table
     */
    @Override
    public Table getTable() {
        return Table.get(OMPOVARIATION.TABLE_NAME);
    }

    /**
     * <p>
     * Fetches the {@link Variation} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Variation ids
     * @return Collection of Variation
     */
    @Override
    public Collection<Variation> get(final Long orgId, final Collection<Long> ids) {
        final Table variationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addJoinClause(new JoinClause(variationTable.getPKColumn(), getPriceTagTable()
                .getColumn(MPOPRICETAG.VARIATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(variationTable.getColumn(OMPOVARIATION.ID), ids)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = dataContainer.getRows(variationTable);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, "i18n.product.variation.not.found");
        } else {
            return rows.map(row -> convertRowToModel(row, dataContainer)).toList();
        }
    }

    /**
     * <p>
     * Saves the attributes of {@link Variation} into the database
     * </p>
     *
     * @param variation An entity that holds the variation attributes which have to be saved into database
     */
    @Override
    public Variation create(final Variation variation) {
        final DataContainer targetContainer = DataContainer.create();

        loadVariationIntoContainer(true, variation, targetContainer);
        orgDataStore(variation.orgId()).commitChanges(targetContainer);

        return variation;
    }

    /**
     * <p>
     * Updates the given variation
     * </p>
     *
     * @param orgId      Organization id
     * @param variations Collection of variation that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Variation> variations) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = variations.stream().map(Variation::id).filter(Objects::nonNull).toList();
        final Table variationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addJoinClause(new JoinClause(variationTable.getPKColumn(), getPriceTagTable().getColumn(MPOPRICETAG.VARIATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(variationTable.getPKColumn(), ids)));
        final DataContainer entityContainer = orgDataStore.get(selectQuery);

        variations.forEach(variation -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadVariationIntoContainer(false, variation, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(variation.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Variation update failed Org ID : {} ID : {} reason : {}", orgId, variation.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, variation.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * Gets the PriceTag Table
     *
     * @return {@link Table}
     */
    public Table getPriceTagTable() {
        return Table.get(MPOPRICETAG.TABLE_NAME);
    }

    /**
     * <p>
     * Loads the variation details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param variation       An entity that holds the variation attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadVariationIntoContainer(final boolean isCreateEvent, final Variation variation,
                                           final DataContainer targetContainer) {
        final DataContainer entityContainer = isCreateEvent ? null
                : orgDataStore(variation.orgId()).get(getTable(), List.of(variation.id()));

        loadVariationIntoContainer(isCreateEvent, variation, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the variation details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param variation       An entity that holds the variation attributes
     * @param entityContainer Holds the user defined and system field values of existing variation
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadVariationIntoContainer(final boolean isCreateEvent, final Variation variation,
                                           final DataContainer entityContainer, final DataContainer targetContainer) {
        final Table variationTable = getTable();
        final Long variationId = variation.id();
        final Long productId = variation.getProductId();
        final String variationName = variation.getName();

        final Row variationRow;

        if (isCreateEvent) {
            variationRow = new Row(variationTable);
        } else {

            if (Objects.isNull(variationId)) {
                final Row existingVariationRow = entityContainer.getRow(variationTable,
                        new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                        .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), variationName)));

                if (Objects.isNull(existingVariationRow)) {
                    variationRow = new Row(variationTable);
                } else {
                    variationRow = existingVariationRow;
                    variation.setId(existingVariationRow.getPKValue());
                }
            } else {
                variationRow = new Row(variationTable, variationId);
            }
        }
        variationRow.set(OMPOVARIATION.NAME, variationName);
        variationRow.set(OMPOVARIATION.ENTITY_ID, productId);

        if (variationRow.isNewRow()) {
            variationRow.setOrgId(variation.orgId());
            variation.setId(variationRow.getPKValue());
        }
        loadPriceTagIntoContainer(isCreateEvent, variation, targetContainer, entityContainer);
        targetContainer.updateRow(variationRow);
    }

    /**
     * <p>
     * Loads the priceTag details into the target container
     * </p>
     *
     * @param variation       An entity that holds the variation attributes
     * @param entityContainer Holds the user defined and system field values of existing priceTag
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadPriceTagIntoContainer(final boolean isCreateEvent, final Variation variation, final DataContainer targetContainer, final DataContainer entityContainer) {
        final Table priceTagTable = getPriceTagTable();
        final Collection<PriceTag> priceTags = variation.getPriceTags();
        final Long orgId = variation.orgId();

        if (Objects.nonNull(priceTags)) {
            priceTags.forEach(priceTag -> {
                final Long priceTagId = priceTag.id();
                final Row priceTagRow = isCreateEvent || Objects.isNull(priceTagId) ? new Row(priceTagTable)
                        : entityContainer.getRow(priceTagTable, priceTagId);

                if (Objects.isNull(priceTagRow)) {
                    LOGGER.error("Price tag not found for the ORG ID: {}, ID: {}", orgId, priceTagId);
                    throw ErrorCode.get(ProductErrorCodes.PRICE_TAG_NOT_FOUND, "Price tag not found");
                }
                final Long variationId = variation.id();

                if (Objects.nonNull(variationId)) {
                    priceTagRow.set(MPOPRICETAG.VARIATION_ID, variationId);
                }
                priceTagRow.set(MPOPRICETAG.PRICE, baseCurrencyService.applyPrecision(variation.orgId(), BigDecimal.valueOf(priceTag.getPrice())));
                priceTagRow.set(MPOPRICETAG.ENTITY_ID, variation.getProductId());
                priceTagRow.set(MPOPRICETAG.CURRENCY_ID, priceTag.getCurrency().getId());
                priceTagRow.set(MPOPRICETAG.TAX, priceTag.getTax());
                priceTagRow.set(MPOPRICETAG.DISCOUNT, priceTag.getDiscount());

                if (priceTagRow.isNewRow()) {
                    priceTagRow.setOrgId(priceTag.orgId());
                    priceTag.setId(priceTagRow.getPKValue());
                    targetContainer.addNewRow(priceTagRow);
                } else {
                    targetContainer.updateRow(priceTagRow);
                }
            });
        }
    }

    /**
     * <p>
     * Converts row into {@link Variation}
     * </p>
     *
     * @param row           Variation row to be converted
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Variation}
     */
    private Variation convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Variation variation = new Variation();
        final Table priceTagTable = getPriceTagTable();
        final Stream<Row> priceTagRows = dataContainer.getRows(priceTagTable,
                new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), row.getPKValue())));
        final Collection<PriceTag> priceTags = new ArrayList<>();

        priceTagRows.forEach(priceTagRow -> priceTags.add(convertRowToPriceTag(priceTagRow)));
        variation.setId(row.getPKValue());
        variation.setOrgId(row.getOrgId());
        variation.setName(row.get(OMPOVARIATION.NAME));
        variation.setProductId(row.get(OMPOVARIATION.ENTITY_ID));
        variation.setPriceTags(priceTags);

        return variation;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return {@link Collection} of {@link Variation}
     */
    @Override
    public Collection<Variation> partialGet(Long orgId, Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * Fetches the variations available for the given product
     * </p>
     *
     * @param orgId            Organization id
     * @param productId        Product id
     * @param start            Start value represents the lower bound of variations to be fetched
     * @param limit            Limit value represents the upper bound of variations to be fetched
     * @param dropdownCriteria Refers the {@code DropdownCriteria}.
     * @return the {@link Collection} of variations related to product
     */
    public Page<Variation> getProductVariations(final Long orgId, final Long productId, final int start, final int limit, final DropdownCriteria dropdownCriteria) {
        final Table variationTable = getTable();
        final Collection<Variation> variations = new ArrayList<>();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);
        final WhereClause whereClause = getVariationCondition(productId).and(dropdownCriteria.build(variationTable.getColumn(OMPOVARIATION.NAME)));

        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final Page<Variation> variationPage = new Page<>();

        final Stream<Row> variationRows = orgDataStore(orgId).get(selectQuery).getRows(variationTable);

        variationPage.setData(variationRows.map(variationRow -> {
            final Variation variation = new Variation();

            variation.setId(variationRow.getPKValue());
            variation.setOrgId(variationRow.getOrgId());
            variation.setName(variationRow.get(OMPOVARIATION.NAME));
            variation.setProductId(variationRow.get(OMPOVARIATION.ENTITY_ID));

            return variation;
        }).toList());

        return variationPage;
    }

    /**
     * <p>
     * Fetches the default variation available for the given product
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param productId Represents the product id
     * @return {@link Variation}
     */
    public Variation getDefaultProductVariation(final Long orgId, final Long productId) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addJoinClause(new JoinClause(variationTable.getPKColumn(),
                priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(getDefaultPriceTagCondition(productId));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row variationRow = dataContainer.getRow(variationTable);

        return Objects.nonNull(variationRow) ? convertRowToModel(variationRow, dataContainer) : null;
    }

    /**
     * <p>
     * Retrieves the number of variation rows for given product id
     * </p>
     *
     * @param orgId     Organization id
     * @param productId ProductId id
     * @param dropdownCriteria Refers the {@code DropdownCriteria}.
     * @return the {@link Long} of variation count
     */
    public Long getVariationCount(final Long orgId, final Long productId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = getVariationCondition(productId).and(dropdownCriteria.build(table.getColumn(OMPOVARIATION.NAME)));

        countQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Retrieves the where clause for the given product id
     * </p>
     *
     * @param productId Product id
     * @return the {@link WhereClause} for variations
     */
    private WhereClause getVariationCondition(final Long productId) {
        final Table variationTable = getTable();

        return new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                .and(Criteria.notEq(variationTable.getColumn(OMPOVARIATION.NAME), Variation.DEFAULT));
    }

    /**
     * <p>
     * Fetches the default price tags available for the given product
     * </p>
     *
     * @param orgId        Organization id
     * @param productId    Product id
     * @param fieldSortMap Which contains key as field name value as {@link SortType}
     * @param start        Start value represents the lower bound of price tags to be fetched
     * @param limit        Limit value represents the upper bound of price tags to be fetched
     * @return the {@link Collection} of price tags
     */
    public Collection<PriceTag> getDefaultPriceTags(final Long orgId, final Long productId, final Map<String, SortType> fieldSortMap,
                                                    final int start, final int limit) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(priceTagTable, variationTable));
        selectQuery.setWhereClause(getDefaultPriceTagCondition(productId));
        applyPriceTagOrderByClause(priceTagTable, selectQuery, fieldSortMap);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final Stream<Row> priceTagRows = orgDataStore(orgId).get(selectQuery).getRows(priceTagTable);
        final Collection<PriceTag> priceTagList = new ArrayList<>();

        priceTagRows.forEach(priceTagRow -> {
            final PriceTag priceTag = convertRowToPriceTag(priceTagRow);

            priceTagList.add(priceTag);
        });

        return priceTagList;
    }

    /**
     * <p>
     * Retrieves the where clause for the given product id
     * </p>
     *
     * @param productId Product id
     * @return the {@link WhereClause} for price tags
     */
    private WhereClause getDefaultPriceTagCondition(final Long productId) {
        final Table variationTable = getTable();

        return new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), Variation.DEFAULT));
    }

    /**
     * <p>
     * Retrieves the number of priceTags rows for given product id
     * </p>
     *
     * @param orgId     Organization id
     * @param productId Product id
     * @return the {@link Long} of default priceTag count
     */
    public Long getDefaultPriceTagCount(final Long orgId, final Long productId) {
        final Table priceTagTable = getPriceTagTable();
        final CountQuery countQuery = CountQuery.get(priceTagTable);

        countQuery.addJoinClause(new JoinClause(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), getTable().getPKColumn()));
        countQuery.setWhereClause(getDefaultPriceTagCondition(productId));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the price tags available for the given variation id
     * </p>
     *
     * @param orgId        Organization id
     * @param variationId  Variation id
     * @param fieldSortMap Which contains key as field name value as {@link SortType}
     * @param start        Start value represents the lower bound of price tags to be fetched
     * @param limit        Limit value represents the upper bound of price tags to be fetched
     * @return the {@link Collection} of price tags
     */
    public Collection<PriceTag> getPriceTags(final Long orgId, final Long variationId, final Map<String, SortType> fieldSortMap,
                                             final int start, final int limit) {
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(priceTagTable);

        selectQuery.setWhereClause(getPriceTagCondition(variationId));
        applyPriceTagOrderByClause(priceTagTable, selectQuery, fieldSortMap);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final Stream<Row> priceTagRows = orgDataStore(orgId).get(selectQuery).getRows(priceTagTable);
        final Collection<PriceTag> priceTagList = new ArrayList<>();

        priceTagRows.forEach(priceTagRow -> {
            final PriceTag priceTag = convertRowToPriceTag(priceTagRow);

            priceTagList.add(priceTag);
        });

        return priceTagList;
    }

    /**
     * <p>
     * Retrieves the where clause for the given variation id
     * </p>
     *
     * @param variationId Variation id
     * @return the {@link WhereClause} for price tags
     */
    private WhereClause getPriceTagCondition(final Long variationId) {
        return new WhereClause(Criteria.eq(getPriceTagTable().getColumn(MPOPRICETAG.VARIATION_ID), variationId));
    }

    /**
     * <p>
     * Retrieves the number of priceTags rows for given variation id
     * </p>
     *
     * @param orgId       Organization id
     * @param variationId Variation id
     * @return the {@link Long} of priceTag count
     */
    public Long getPriceTagCount(final Long orgId, final Long variationId) {
        final CountQuery countQuery = CountQuery.get(getPriceTagTable());

        countQuery.setWhereClause(getPriceTagCondition(variationId));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Converts row into {@link PriceTag}
     * </p>
     *
     * @param row price tag row to be converted
     * @return the {@link PriceTag}
     */
    public PriceTag convertRowToPriceTag(final Row row) {
        final PriceTag priceTag = new PriceTag();
        final Short currencyId = row.get(MPOPRICETAG.CURRENCY_ID);

        if (Objects.nonNull(currencyId)) {
            priceTag.setCurrency(Currency.getCurrencyById(currencyId));
        }

        priceTag.setOrgId(row.getOrgId());
        priceTag.setId(row.getPKValue());
        priceTag.setProductId(row.get(MPOPRICETAG.ENTITY_ID));
        priceTag.setVariationId(row.get(MPOPRICETAG.VARIATION_ID));
        priceTag.setTax(row.get(MPOPRICETAG.TAX));
        priceTag.setPrice(row.get(MPOPRICETAG.PRICE));
        priceTag.setDiscount(row.get(MPOPRICETAG.DISCOUNT));

        return priceTag;
    }

    /**
     * Deletes the price tag
     *
     * @param orgId    Organization id
     * @param priceTag An entity that holds the price tags attributes
     */
    public void deletePriceTag(final Long orgId, final PriceTag priceTag) {
        final Table priceTagTable = getPriceTagTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(getPriceTagTable());

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getPKColumn(), priceTag.id())));
        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Checks whether the price tag is exists or not
     * </p>
     *
     * @param orgId    Organization Id
     * @param priceTag An entity that holds the tag attributes
     * @return true if the price tag exists
     */
    public boolean isPriceTagExists(final Long orgId, final PriceTag priceTag) {
        final Table priceTagTable = getPriceTagTable();
        final CountQuery countQuery = CountQuery.get(priceTagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getPKColumn(), priceTag.id())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Gets price tag details using by product or variation id.
     *
     * @param orgId      Organization id
     * @param productId  product id
     * @param currencyId Currency id
     * @return {@link PriceTag}
     */
    public PriceTag getPriceTagDetail(final Long orgId, final Long productId, final Long variationId, final int currencyId) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final Column currencyIdColumn = priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID);
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(variationTable, priceTagTable));
        selectQuery.addSelectClause(List.of(priceTagTable.getColumn(MPOPRICETAG.PRICE), priceTagTable.getColumn(MPOPRICETAG.DISCOUNT),
                priceTagTable.getColumn(MPOPRICETAG.TAX), priceTagTable.getColumn(MPOPRICETAG.ORG_ID)));

        if (Objects.nonNull(productId)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                    .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), Variation.DEFAULT))
                    .and(Criteria.eq(currencyIdColumn, currencyId)));
        } else if (Objects.nonNull(variationId)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(variationTable.getPKColumn(), variationId))
                    .and(Criteria.eq(currencyIdColumn, currencyId)));
        }

        final Row priceTagRow = orgDataStore(orgId).get(selectQuery).getRow(priceTagTable);

        if (Objects.nonNull(priceTagRow)) {
            final PriceTag priceTag = new PriceTag();

            priceTag.setPrice(priceTagRow.get(MPOPRICETAG.PRICE));
            priceTag.setTax(priceTagRow.get(MPOPRICETAG.TAX));
            priceTag.setOrgId(priceTagRow.get(MPOPRICETAG.ORG_ID));
            priceTag.setDiscount(priceTagRow.get(MPOPRICETAG.DISCOUNT));

            return priceTag;
        }

        return null;
    }

    /**
     * Checks whether all the given variations are exists or not
     *
     * @param orgId        represents the Organisation ID
     * @param variationIds which contains the collection of variation IDs
     * @return true if all the variations are exists
     */
    public boolean exists(final Long orgId, final Set<Long> variationIds) {
        if (Objects.nonNull(variationIds) && !variationIds.isEmpty()) {
            final Table variationTable = getTable();
            final CountQuery variationsCountQuery = CountQuery.get(variationTable);

            variationsCountQuery.setWhereClause(new WhereClause(Criteria.in(variationTable.getPKColumn(), variationIds)));

            return variationIds.size() == orgDataStore(orgId).get(variationsCountQuery);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Long orgId, final Collection<Variation> variations) {
        final Set<Long> variationIds = variations.stream().map(Variation::id).filter(Objects::nonNull).collect(Collectors.toSet());

        if (variationIds.isEmpty()) {
            return;
        }

        final DataContainer dataContainer = DataContainer.create();
        final Table table = getTable();

        variationIds.forEach(variationId -> dataContainer.deleteRow(new Row(table, variationId)));
        DealManager.getInstance().updateDealValuesByVariationIds(orgId, variationIds, dataContainer);
    }

    /**
     * Gets the {@link Set<String>} of variation names that partially match the given name and for the given product ID.
     *
     * @param orgId organisation ID
     * @return {@link Set<String>} of variation names
     */
    public Set<String> getVariationNameByPartialMatch(final Long orgId, final String name, final Long productId) {
        final Table variationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);
        final Column nameColumn = variationTable.getColumn(OMPOVARIATION.NAME);

        selectQuery.addSelectClause(nameColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.contains(nameColumn, name))
                .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId)));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(variationTable);

        return rows.map(row -> row.get(OMPOVARIATION.NAME).toString()).collect(Collectors.toSet());
    }

    /**
     * Checks whether a variation with the given name already exists for the product,
     * excluding the current variation id during update.
     *
     * @param orgId       organisation ID
     * @param productId   product ID
     * @param variationId variation ID to exclude from the lookup
     * @param name        variation name
     * @return true if a duplicate variation name exists
     */
    public boolean isVariationAlreadyExists(final Long orgId, final Long productId, final Long variationId, final String name) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                .and(Criteria.eq(table.getColumn(OMPOVARIATION.NAME), name));

        countQuery.setWhereClause(Objects.nonNull(variationId) ? whereClause.and(Criteria.notEq(table.getPKColumn(), variationId)) : whereClause);

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Checks whether the provided list of price tag ids is associated with the given variation id.
     *
     * @param priceTagIds Represents the list of {@link PriceTag} ids.
     * @param variationId Represents the id of the {@link Variation}.
     * @return True if all the price tag ids are associated with the given variation id, else return false.
     */
    public boolean isPriceTagsAssociatedWithVariation(final Long orgId, final Collection<Long> priceTagIds, final Long variationId) {
        final Table priceTagTable = getPriceTagTable();
        final CountQuery countQuery = CountQuery.get(priceTagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), variationId))
                .and(Criteria.in(priceTagTable.getPKColumn(), priceTagIds)));

        return orgDataStore(orgId).get(countQuery) == priceTagIds.size();
    }

    /**
     * <p>
     * Returns the default variation id of the product.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param productId Represents the product id.
     * @return Id of the default variation.
     */
    public Long getDefaultVariationId(final Long orgId, final Long productId) {
        final Table variationTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addSelectClause(variationTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), Variation.DEFAULT)));

        final Row variationRow = orgDataStore(orgId).get(selectQuery).getRow(variationTable);

        if (Objects.nonNull(variationRow)) {
            return variationRow.getPKValue();
        }

        return null;
    }

    /**
     * <p>
     * Gets the {@link Collection} of base currency {@link PriceTag}.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param productId Represents the product id
     * @return {@link Collection} of {@link PriceTag}
     */
    Collection<PriceTag> getBaseCurrencyPriceTags(final long orgId, final Long productId) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addSelectClause(priceTagTable);
        selectQuery.addJoinClause(new JoinClause(variationTable.getPKColumn(), priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productId))
                .and(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), baseCurrencyService.getDefaultCurrency(orgId).getId())));

        final Collection<Row> priceTagRows = orgDataStore(orgId).get(selectQuery).getRows(priceTagTable).toList();

        return priceTagRows.stream().map(this::convertRowToPriceTag).toList();
    }

    /**
     * <p>
     * Applies the appropriate {@link OrderByClause} to the given {@link SelectQuery} based on the type of field type
     * </p>
     *
     * @param priceTagTable Represents a price tag table
     * @param selectQuery   Represents the {@link SelectQuery} price tag rows
     * @param fieldSortMap  Which contains key as field name value as {@link SortType}
     */
    private void applyPriceTagOrderByClause(final Table priceTagTable, final SelectQuery selectQuery,
                                            final Map<String, SortType> fieldSortMap) {
        final BiConsumer<Column, SortType> orderByAppender = (column, sortType) -> {
            final OrderByClause.OrderType orderType = SortType.ASCENDING == sortType
                    ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING;

            selectQuery.addOrderByClause(new OrderByClause(column, orderType));
        };

        for (final Map.Entry<String, SortType> fieldSortEntry : fieldSortMap.entrySet()) {

            switch (fieldSortEntry.getKey()) {
                case "price" -> orderByAppender.accept(priceTagTable.getColumn(MPOPRICETAG.PRICE), fieldSortEntry.getValue());
                case "discount" -> orderByAppender.accept(priceTagTable.getColumn(MPOPRICETAG.DISCOUNT), fieldSortEntry.getValue());
                case "tax" -> orderByAppender.accept(priceTagTable.getColumn(MPOPRICETAG.TAX), fieldSortEntry.getValue());
                case "currency" -> orderByAppender.accept(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), fieldSortEntry.getValue());
            }
        }
    }

    /**
     * <p>
     * Fetches the collection of {@link PriceTag} based on product ids
     * </p>
     *
     * @param orgId      Represents the org id
     * @param productIds Which contains the collection of product ids
     * @return The {@link Map} containing key is the product id and values is collection of  {@link PriceTag}
     */
    public Map<Long, PriceTag> fetchDefaultCurrencyPriceTag(final long orgId, final Collection<Long> productIds) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);
        final Column variationPkColumn = variationTable.getPKColumn();

        selectQuery.addSelectClause(priceTagTable, variationTable);
        selectQuery.addJoinClause(new JoinClause(variationPkColumn, priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), productIds))
                .and(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), DEFAULT))
                .and(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), baseCurrencyService.getDefaultCurrency(orgId).getId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, PriceTag> productPriceTags = new HashMap<>();

        dataContainer.getRows(variationTable).forEach(row -> {
            final Row priceTagRow = dataContainer.getRow(priceTagTable,
                    new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID), row.getPKValue())));

            productPriceTags.put(row.get(OMPOVARIATION.ENTITY_ID), convertRowToPriceTag(priceTagRow));
        });

        return productPriceTags;
    }

    /**
     * <p>
     * Fetches the collection of {@link Variation} for the given product id
     * </p>
     *
     * @param orgId     Represents the org id
     * @param productId Represents the product id
     * @return The collection of {@link Variation}
     */
    public Collection<Variation> getVariations(final long orgId, final long productId) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);
        final Column variationPkColumn = variationTable.getPKColumn();

        selectQuery.addSelectClause(variationTable.getColumn(OMPOVARIATION.NAME));
        selectQuery.addSelectClause(priceTagTable);
        selectQuery.addJoinClause(new JoinClause(variationPkColumn, priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID)));
        selectQuery.setWhereClause(getVariationCondition(productId));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> variationRows = dataContainer.getRows(variationTable);

        return variationRows.map(variationRow -> convertRowToModel(variationRow, dataContainer)).toList();
    }

    /**
     * <p>
     * Fetches the default variation price tags for the given product id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param productId Represents the product id
     * @return The collection of {@link PriceTag}
     */
    public Collection<PriceTag> getDefaultVariationPriceTags(final Long orgId, final long productId) {
        final Table variationTable = getTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(variationTable);

        selectQuery.addSelectClause(priceTagTable);
        selectQuery.addJoinClause(DmlUtil.getJoins(priceTagTable, variationTable));
        selectQuery.setWhereClause(getDefaultPriceTagCondition(productId));
        final Stream<Row> priceTagRows = orgDataStore(orgId).get(selectQuery).getRows(priceTagTable);

        return priceTagRows.map(this::convertRowToPriceTag).toList();
    }
}