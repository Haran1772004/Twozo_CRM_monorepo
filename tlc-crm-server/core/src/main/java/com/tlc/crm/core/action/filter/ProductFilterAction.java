package com.tlc.crm.core.action.filter;

import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.list.filter.appender.CriteriaAppenderHolder;
import com.tlc.crm.entity.list.filter.appender.model.NullableFieldCriteriaAppenderRequisites;
import com.tlc.crm.entity.list.filter.models.FilterCriteriaDetails;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.interval.Interval;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MPOPRICETAG;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.core.sql.resource.OMITHISTORY;
import com.tlc.crm.core.sql.resource.OMITIMPORT;
import com.tlc.crm.core.sql.resource.OMITPRODUCTHISTORY;

import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.fields.FilterOptions;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides the action which can be perform product related filter actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Filter(source = FilterSourceType.PRODUCT, provider = Provider.PRODUCT_LIST, sourceLinkGroups = {
        SourceLinkGroup.PRODUCT_COMPANY, SourceLinkGroup.PRODUCT_CONTACT, SourceLinkGroup.PRODUCT_ACTIVITY})
public class ProductFilterAction extends FilterAction {

    private static final String DEFAULT = "default";

    private final ProductManager productManager;
    private final DealManager dealManager;
    private final ImportManager importManager;
    private final BaseCurrencyService baseCurrencyService;

    public ProductFilterAction() {
        productManager = ProductManager.getInstance();
        dealManager = DealManager.getInstance();
        importManager = ImportManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldInfo              {@link FieldInfo} that holds the field information.
     * @param filterCriteria         holds the details of criteria.
     * @param criteriaAppenderHolder Holds the post-criteria appender.
     * @return {@link WhereClause} holds the details about retrieval criteria.
     */
    @Override
    public WhereClause buildWhereClause(final FieldInfo fieldInfo, final FilterCriteria filterCriteria,
                                        final CriteriaAppenderHolder criteriaAppenderHolder) {
        final Collection<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaValue criteriaValue = Objects.isNull(criteriaValues) || criteriaValues.isEmpty()
                ? null : criteriaValues.stream().findAny().get();
        final Object value = Objects.isNull(criteriaValue) ? null : criteriaValue.getValue();
        final Long valueEntityId = Objects.isNull(criteriaValue) ? null : criteriaValue.getValueEntityId();
        final Comparator comparator = filterCriteria.getComparator();
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final FilterOptions filterOption = FilterOptions.get(criteriaField.getAliasName());
        final NullableFieldCriteriaAppenderRequisites.Builder nullableFieldCriteriaRequisitesBuilder
                = new NullableFieldCriteriaAppenderRequisites.Builder();
        final Table table = getTable();
        Column column = null;

        if (Objects.nonNull(fieldInfo) && fieldInfo.isPersistent() || Objects.isNull(filterOption)) {
            return super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        }
        final WhereClause filter = switch (filterOption) {
            case TAX -> {
                column = getPriceTagTable().getColumn(MPOPRICETAG.TAX);

                yield new WhereClause(new Criteria(column, value, operator));
            }
            case CREATED_AT -> {
                column = table.getColumn(MPOPRODUCT.CREATED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case UPDATED_AT -> {
                column = table.getColumn(MPOPRODUCT.MODIFIED_TIME);

                yield buildCriteriaForDateField(operator, criteriaValue, column);
            }
            case PRICE_TAGS -> {
                column = getPriceTagTable().getColumn(MPOPRICETAG.CURRENCY_ID);

                yield new WhereClause(new Criteria(column, valueEntityId, operator));
            }
            case SALES_OWNER -> {
                column = table.getColumn(MPOPRODUCT.OWNER_ID);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case CREATED_BY -> {
                column = table.getColumn(MPOPRODUCT.CREATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case UPDATED_BY -> {
                column = table.getColumn(MPOPRODUCT.UPDATED_BY);

                yield buildOwnerAndDropdownFieldCriteria(column, criteriaValues, comparator);
            }
            case IMPORTED_TIME -> {
                final Table importTable = importManager.getTable();
                column = importTable.getColumn(OMITIMPORT.MODIFIED_TIME);
                final WhereClause dateFieldCriteria = buildDateFieldCriteria((Interval<Long, Long>) value, operator, column);

                yield Objects.isNull(dateFieldCriteria) ? null : new WhereClause(Criteria.eq(importTable.getColumn(OMITIMPORT.STATUS),
                        ImportInfo.Status.FINISHED.getType())).and(dateFieldCriteria);
            }
            case BASE_CURRENCY_AMOUNT -> {
                column = getPriceTagTable().getColumn(MPOPRICETAG.PRICE);

                nullableFieldCriteriaRequisitesBuilder.setValidateEmptyValueCriteria(true);
                yield buildCriteriaForBaseCurrencyAmount(value, operator);
            }
//            case IS_ACTIVE -> {
//                column = table.getColumn(MCOCONTACT.IS_ACTIVE);
//
//                yield new WhereClause(new Criteria(column, value, operator));
//            }TODO soft delete needed.
            case LAST_ASSIGNED_AT -> buildCriteriaForDateField(operator, criteriaValue, table.getColumn(MPOPRODUCT.LAST_ASSIGNED_AT));

            default -> super.buildWhereClause(fieldInfo, filterCriteria, criteriaAppenderHolder);
        };

        setRequisitesIntoNullableFieldCriteriaAppender(nullableFieldCriteriaRequisitesBuilder.build(column), criteriaAppenderHolder);
        return filter;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTable   Represents the table of the column.
     * @param fieldInfo    Represents the details of the field.
     * @param filterOption Represents the option of the filter.
     * @return Field {@link Column}.
     */
    @Override
    protected Column getFieldColumn(final Table fieldTable, final FieldInfo fieldInfo, final FilterOptions filterOption) {
        final Table table = getTable();

        if (Objects.isNull(filterOption)) {
            return super.getFieldColumn(fieldTable, fieldInfo, null);
        }

        return switch (filterOption) {
            case ID -> table.getPKColumn();
            case NAME -> table.getColumn(MPOPRODUCT.NAME);
            case DESCRIPTION -> table.getColumn(MPOPRODUCT.DESCRIPTION);
            case SKU_NUMBER -> table.getColumn(MPOPRODUCT.SKU_NUMBER);
            case PRODUCT_CODE -> table.getColumn(MPOPRODUCT.PRODUCT_CODE);
            case QUANTITY_IN_STOCK -> table.getColumn(MPOPRODUCT.QUANTITY_IN_STOCK);
            case VALID_TILL -> table.getColumn(MPOPRODUCT.VALID_TILL);
            case CREATED_AT -> table.getColumn(MPOPRODUCT.CREATED_TIME);
            case UPDATED_AT -> table.getColumn(MPOPRODUCT.MODIFIED_TIME);
            case QUANTITY_ORDERED -> table.getColumn(MPOPRODUCT.QUANTITY_ORDERED);
            default -> super.getFieldColumn(fieldTable, fieldInfo, filterOption);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param filterCriteriaDetails Helps to manage the joins data information.
     */
    @Override
    public void appendJoinsBasedOnField(final FilterCriteriaDetails filterCriteriaDetails) {
        final Table productTable = getTable();
        final Column productPkColumn = productTable.getPKColumn();
        final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();
        final FilterOptions filterOption = filterCriteriaDetails.getFilterOptions();

        if (Objects.nonNull(filterOption)) {
            switch (filterOption) {
                case BASE_CURRENCY_AMOUNT -> joinsLoader.load(productPkColumn, getPriceTagTable().getColumn(MPOPRICETAG.ENTITY_ID));
                case TAX, PRICE_TAGS -> joinsLoader.load(productPkColumn, getPriceTagTable().getColumn(MPOPRICETAG.ENTITY_ID));
                case IMPORTED_TIME -> {
                    final Table productHistoryTable = Table.get(OMITPRODUCTHISTORY.TABLE);
                    final Table importHistoryTable = importManager.getHistoryTable();

                    joinsLoader.load(productPkColumn, productHistoryTable.getColumn(OMITPRODUCTHISTORY.ENTITY_ID));
                    joinsLoader.load(productHistoryTable.getColumn(OMITPRODUCTHISTORY.HISTORY_ID), importHistoryTable.getPKColumn());
                    joinsLoader.load(importHistoryTable.getColumn(OMITHISTORY.IMPORT_ID), importManager.getTable().getPKColumn());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return product {@link Table}.
     */
    @Override
    public Table getTable() {
        return productManager.getTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param baseTable             Refers to the base table of the previous {@link FilterAction}, which is used to
     *                              append the joins after the already joined tables.
     * @param filterCriteriaDetails Helps to manage the joins data information.
     * @param mapperTables          Refers to the mapper tables of those who had redirected this method. It is used to make
     *                              joins for the current and previous sources if those source tables have no direct relations.
     */
    @Override
    public void loadJoins(final Table baseTable, final FilterCriteriaDetails filterCriteriaDetails, final Table... mapperTables) {
        if (baseTable.equals(dealManager.getTable())) {
            final Table localTable = getTable();
            final Table productDetailTable = getProductDetailTable();
            final FieldInfo fieldInfo = filterCriteriaDetails.getFieldInfo();
            final Table productDetailsHolderTable = getProductDetailsHolderTable();
            final Table productDetailAliasTable = Objects.isNull(fieldInfo) ? productDetailTable : getAliasTable(productDetailTable, fieldInfo.aliasName());
            final JoinsLoader joinsLoader = filterCriteriaDetails.getJoinsLoader();

            joinsLoader.load(baseTable.getPKColumn(),
                    productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID));
            joinsLoader.load(productDetailsHolderTable.getPKColumn(),
                    productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID));
            joinsLoader.load(productDetailAliasTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID),
                    localTable.getPKColumn());
            loadJoinsBasedOnRelatedSourceType(localTable, getLocalMapperTables(), filterCriteriaDetails);
        } else {
            super.loadJoins(baseTable, filterCriteriaDetails, mapperTables);
        }
    }

    /**
     * <p>
     * Builds where clause for base currency total amount.
     * </p>
     *
     * @param value    Value used to filter.
     * @param operator {@link Operator}.
     * @return {@link WhereClause}.
     */
    private WhereClause buildCriteriaForBaseCurrencyAmount(final Object value, final Operator operator) {
        final Table priceTagTable = getPriceTagTable();
        final Column currencyColumn = priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID);
        final Table variationTable = getVariationTable();
        final Column entityIdColumn = variationTable.getColumn(OMPOVARIATION.ENTITY_ID);
        final Column variationIdColumn = variationTable.getColumn(OMPOVARIATION.ID);
        final Column productIdColumn = getTable().getColumn(MPOPRODUCT.ID);
        final SelectQuery variationQuery = SelectQuery.get(variationTable);
        final short currencyId = baseCurrencyService.getDefaultCurrency(LoggedInUserInfo.getUser().orgId()).getId();
        final WhereClause whereClause = new WhereClause(Criteria.eq(variationTable.getColumn(OMPOVARIATION.NAME), DEFAULT))
                .and(Criteria.eq(currencyColumn, currencyId))
                .and(new Criteria(priceTagTable.getColumn(MPOPRICETAG.PRICE), value, operator));

        variationQuery.addJoinClause(List.of(new JoinClause(entityIdColumn, productIdColumn),
                new JoinClause(variationIdColumn, priceTagTable.getColumn(MPOPRICETAG.VARIATION_ID))));
        variationQuery.addSelectClause(entityIdColumn.distinct());
        variationQuery.setWhereClause(
                (operator == Operator.NOT_EQUAL && Objects.nonNull(value))
                || (operator == Operator.EQUAL && Objects.isNull(value))
                ? whereClause.or(buildCurrencyExclusionCriteria(currencyId)) : whereClause);
        variationQuery.addOrderByClause(new OrderByClause(entityIdColumn));

        return new WhereClause(Criteria.in(productIdColumn, variationQuery));
    }

    /**
     * Builds the criteria for exclusion of currency.
     *
     * @param currencyId Represents the currency ID.
     * @return {@link WhereClause}.
     */
    private WhereClause buildCurrencyExclusionCriteria(final short currencyId) {
        final Table variationTable = getVariationTable();
        final Table priceTagTable = getPriceTagTable();
        final SelectQuery selectQuery = SelectQuery.get(priceTagTable);

        selectQuery.addSelectClause(priceTagTable.getColumn(MPOPRICETAG.ENTITY_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(priceTagTable.getColumn(MPOPRICETAG.CURRENCY_ID), currencyId)));
        return new WhereClause(Criteria.notIn(variationTable.getColumn(OMPOVARIATION.ENTITY_ID), selectQuery));
    }

    /**
     * <p>
     * Fetches the table of the variation.
     * </p>
     *
     * @return Product variation table.
     */
    private Table getVariationTable() {
        return Table.get(OMPOVARIATION.TABLE_NAME);
    }

    /**
     * <p>
     * Fetches the table of the product details holder.
     * </p>
     *
     * @return OMDOProductDetailHolder table.
     */
    private Table getProductDetailsHolderTable() {
        return Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the product detail.
     * </p>
     *
     * @return OMDOProductDetail table.
     */
    private Table getProductDetailTable() {
        return Table.get(MDOPRODUCTDETAIL.TABLE);
    }

    /**
     * <p>
     * Fetches the {@link Table} of price tag.
     * </p>
     *
     * @return price tag table.
     */
    private Table getPriceTagTable() {
        return Table.get(MPOPRICETAG.TABLE_NAME);
    }
}
