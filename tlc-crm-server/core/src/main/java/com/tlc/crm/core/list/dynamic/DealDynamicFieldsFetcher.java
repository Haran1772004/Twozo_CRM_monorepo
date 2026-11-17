package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.action.deal.handler.DynamicDealFieldEvaluatorHandler;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;
import com.tlc.crm.entity.models.type.Monetary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to deal list
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DealDynamicFieldsFetcher extends DynamicFieldsFetcher {

    private static final String PRODUCT_VALUE_FORMAT = "%s %s";

    private final ActivityDynamicFieldsFetcher activityDynamicFieldsFetcher;
    private final WebFormFieldsFetcher webFormFieldsFetcher;
    private final DealManager dealManager;
    private final DynamicDealFieldEvaluatorHandler dynamicDealFieldEvaluatorHandler;
    private final EmailFieldsFetcher emailFieldsFetcher;

    private DealDynamicFieldsFetcher() {
        activityDynamicFieldsFetcher = ActivityDynamicFieldsFetcher.getInstance();
        webFormFieldsFetcher = WebFormFieldsFetcher.getInstance();
        dealManager = DealManager.getInstance();
        dynamicDealFieldEvaluatorHandler = DynamicDealFieldEvaluatorHandler.getInstance();
        emailFieldsFetcher = EmailFieldsFetcher.getInstance();
    }

    private static class InstanceHolder {
        private static final DealDynamicFieldsFetcher INSTANCE = new DealDynamicFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link DealDynamicFieldsFetcher} instance
     * </p>
     *
     * @return The {@link DealDynamicFieldsFetcher}
     */
    public static DealDynamicFieldsFetcher getInstance() {
        return DealDynamicFieldsFetcher.InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();
        final DynamicFieldsData webFormDynamicFieldsData = webFormFieldsFetcher.get(dynamicFieldRequest);
        final DynamicFieldsData activityDynamicFieldsData = activityDynamicFieldsFetcher.get(dynamicFieldRequest);
        final DynamicFieldsData dealDynamicFieldsData = fetchDealFields(dynamicFieldRequest);
        final DynamicFieldsData emailDynamicFieldsData = emailFieldsFetcher.get(dynamicFieldRequest);

        dynamicFieldsData.append(webFormDynamicFieldsData);
        dynamicFieldsData.append(activityDynamicFieldsData);
        dynamicFieldsData.append(dealDynamicFieldsData);
        dynamicFieldsData.append(emailDynamicFieldsData);

        return dynamicFieldsData;
    }

    /**
     * <p>
     * Loads deal fields for the given organization id and entities
     * </p>
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    private DynamicFieldsData fetchDealFields(final DynamicFieldRequest dynamicFieldRequest) {
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();
        final long orgId = dynamicFieldRequest.orgId();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final Collection<String> dealFields = getDealFields();
        final boolean hasRequiredDealField = dynamicFields.stream().anyMatch(dealFields::contains);

        if (hasRequiredDealField) {
            final Map<Long, Deal> dealMap = dealManager.fetchDeals(orgId, entityIds);
            final String idKey = TransformerFields.ID.getName();
            final Map<String, Map<String, Object>> dealRows = new HashMap<>();

            entityIds.forEach(id -> {

                if (dealMap.containsKey(id)) {
                    final Map<String, Object> dealRow = new HashMap<>();
                    final Deal deal = dealMap.get(id);

                    for (final String fieldName : dynamicFields) {
                        dynamicDealFieldEvaluatorHandler.computeFieldValue(fieldName, deal);

                        switch (fieldName) {
                            case ListDynamicFields.AGE -> dealRow.put(fieldName, deal.getAge());
                            case ListDynamicFields.ROTTING_DAYS -> dealRow.put(fieldName, deal.getRottenDays());
                            case ListDynamicFields.WEIGHTED_VALUE ->
                                    addMonetaryToDealRow(fieldName, dealRow, deal.getWeightedRevenue());
                            case ListDynamicFields.EXPECTED_DEAL_VALUE ->
                                    addMonetaryToDealRow(fieldName, dealRow, deal.getExpectedDealValue());
                            case ListDynamicFields.DEAL_VALUE_IN_BASE_CURRENCY ->
                                    addMonetaryToDealRow(fieldName, dealRow, deal.getDealValueInBaseCurrency());
                            case ListDynamicFields.PRODUCT_AMOUNT ->
                                    addMonetaryToDealRow(fieldName, dealRow, deal.getProductAmount());
                            case ListDynamicFields.STATUS -> dealRow.put(fieldName, deal.getState().getName());
                            case ListDynamicFields.STAGE_PROBABILITY ->
                                    dealRow.put(fieldName, deal.getPipelineStage().getProbability());
                            case ListDynamicFields.WON_TIME -> {
                                if (Deal.State.WON == deal.getState()) {
                                    dealRow.put(fieldName, deal.getClosedTime().getUtcTimeStamp());
                                }
                            }
                            case ListDynamicFields.LOST_TIME -> {
                                if (Deal.State.LOST == deal.getState()) {
                                    dealRow.put(fieldName, deal.getClosedTime().getUtcTimeStamp());
                                }
                            }
                            case ListDynamicFields.PRODUCTS -> {
                                final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                                if (Objects.nonNull(productDetailsHolder)) {
                                    final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

                                    if (Objects.nonNull(productDetails) && !productDetails.isEmpty()) {
                                        final Map<String, Object> productsMap = new HashMap<>();
                                        final Collection<Object> productDetailList = new ArrayList<>();
                                        final String symbol = productDetailsHolder.getCurrency().getSymbol();
                                        final String nameKey = TransformerFields.NAME.getName();

                                        productDetails.forEach(productDetail -> {
                                            final Map<String, Object> productDetailMap = new HashMap<>();
                                            final Product product = productDetail.getProduct();

                                            productDetailMap.put(idKey, product.id());
                                            productDetailMap.put(nameKey, product.getName());
                                            productDetailMap.put(ListDynamicFields.PRICE,
                                                    String.format(PRODUCT_VALUE_FORMAT, symbol, productDetail.getPrice()));

                                            productDetailList.add(productDetailMap);
                                        });
                                        productsMap.put(ListDynamicFields.ITEMS, productDetailList);
                                        dealRow.put(fieldName, productsMap);
                                    }
                                }
                            }
                        }
                    }
                    dealRows.put(id.toString(), dealRow);
                }
            });
            dynamicFieldsData.addAll(dealRows);
        }
        return dynamicFieldsData;
    }

    /**
     * <p>
     * Adds monetary information to a specified field in the deal row
     * </p>
     *
     * @param fieldName Represents the field name
     * @param dealRow   The {@link Map} representing the deal row where the monetary information will be added
     * @param monetary  The {@link Monetary} containing the currency and value to be added
     */
    private void addMonetaryToDealRow(final String fieldName, final Map<String, Object> dealRow, final Monetary monetary) {

        if (Objects.nonNull(monetary)) {
            dealRow.put(fieldName, Map.of(TransformerFields.SYMBOL.getName(), monetary.getCurrency().getSymbol(),
                    TransformerFields.VALUE.getName(), monetary.getValue()));
        }
    }

    /**
     * <p>
     * Fetches the collection of deal fields
     * </p>
     *
     * @return The collection of deal fields
     */
    private Collection<String> getDealFields() {
        return Set.of(ListDynamicFields.PRODUCT_AMOUNT, ListDynamicFields.PRODUCTS, ListDynamicFields.AGE,
                ListDynamicFields.ROTTING_DAYS, ListDynamicFields.WEIGHTED_VALUE, ListDynamicFields.EXPECTED_DEAL_VALUE,
                ListDynamicFields.DEAL_VALUE_IN_BASE_CURRENCY, ListDynamicFields.STATUS, ListDynamicFields.WON_TIME,
                ListDynamicFields.LOST_TIME, ListDynamicFields.STAGE_PROBABILITY);
    }
}