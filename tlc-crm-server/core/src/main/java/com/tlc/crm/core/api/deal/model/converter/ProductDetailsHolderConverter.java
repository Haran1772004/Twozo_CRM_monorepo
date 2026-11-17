package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.field.deal.ProductDetailFields;
import com.tlc.crm.core.field.deal.ProductDetailsHolderFields;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.api.model.converter.CurrencyUnitConverter;
import com.tlc.crm.entity.currency.action.config.CurrencyConverterConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from Json into {@link ProductDetailsHolder}
 *
 * @author karthick.v
 * @version 1.0
 */
public class ProductDetailsHolderConverter extends EntityConverter<ProductDetailsHolder> {

    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final ProductConverter productConverter;
    private final CurrencyUnitConverter currencyUnitConverter;
    private final BaseCurrencyService baseCurrencyService;

    private static final long DEFAULT_QUANTITY = 1;
    private static final double DEFAULT_PRICE = 0.0;
    private static final double DEFAULT_PERCENTAGE = 0.0;

    private static class Instance {
        private static final ProductDetailsHolderConverter INSTANCE = new ProductDetailsHolderConverter();
    }

    private ProductDetailsHolderConverter() {
        this.productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        this.productConverter = ProductConverter.getInstance();
        this.currencyUnitConverter = CurrencyUnitConverter.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * Gets the {@link ProductDetailsHolderConverter} Instance
     *
     * @return the {@link ProductDetailsHolderConverter}
     */
    public static ProductDetailsHolderConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Converter Configuration
     * @param jsonObject      Holds the field values of product details holder
     * @return the {@link ProductDetailsHolder} filled with data
     */
    @Override
    public ProductDetailsHolder convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return constructProductDetailsHolderByPartialData(converterConfig, jsonObject);
        } else {
            return constructProductDetailsHolder(converterConfig, jsonObject);
        }
    }

    /**
     * Converts partial data into {@link ProductDetailsHolder}
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      which contains the json data
     * @return {@link ProductDetailsHolder} An entity that holds the attributes of product details holder
     */
    private ProductDetailsHolder constructProductDetailsHolderByPartialData(final ConverterConfig converterConfig,
                                                                            final JsonObject jsonObject) {
        final long orgId = converterConfig.orgId();
        final long dealId = jsonObject.getLong(ProductDetailsHolderFields.DEAL_ID.getName());
        final ProductDetailsHolder productDetailsHolder = productDetailsHolderManager.isProductLinkedToDeal(orgId, dealId) ? productDetailsHolderManager.get(orgId,
                dealId) : new ProductDetailsHolder();

        setJsonDataToModel(converterConfig, productDetailsHolder, jsonObject);

        return productDetailsHolder;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link ProductDetailsHolder}
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param jsonObject      Holds the field values of product details holder
     * @return {@link ProductDetailsHolder} An entity that holds the attributes of product details holder
     */
    private ProductDetailsHolder constructProductDetailsHolder(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ProductDetailsHolder productDetailsHolder = new ProductDetailsHolder();
        final String idKey = ProductDetailsHolderFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            productDetailsHolder.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, productDetailsHolder, jsonObject);

        return productDetailsHolder;
    }

    /**
     * Sets the JSON data into {@link ProductDetailsHolder}
     *
     * @param converterConfig      Converter configuration
     * @param productDetailsHolder A model to which the product details holder attributes will be set
     * @param jsonObject           Holds the field values of product details holder
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final ProductDetailsHolder productDetailsHolder,
                                    final JsonObject jsonObject) {
        final String dealIdKey = ProductDetailsHolderFields.DEAL_ID.getName();
        final String currencyKey = ProductDetailsHolderFields.CURRENCY.getName();

        if (jsonObject.containsKey(currencyKey)) {
            final short currencyValue = jsonObject.getShort(currencyKey);
            final Currency currency = Currency.getCurrencyById(currencyValue);

            productDetailsHolder.setCurrency(currency);
        }

        if (jsonObject.containsKey(dealIdKey)) {
            productDetailsHolder.setDealId(jsonObject.getLong(dealIdKey));
        }
        productDetailsHolder.setOrgId(converterConfig.orgId());
        productDetailsHolder.setTaxInclusive(jsonObject.optBoolean(ProductDetailsHolderFields.TAX_INCLUSIVE.getName(),
                productDetailsHolder.isTaxInclusive()));
        productDetailsHolder.setManuallyAdded(false);

        buildProductDetails(converterConfig, productDetailsHolder, jsonObject);
    }

    /**
     * <p>
     * Builds the Json data into {@link ProductDetail}
     * </p>
     *
     * @param converterConfig      Converter configuration
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     * @param jsonObject           Holds the field values of product details
     */
    protected void buildProductDetails(final ConverterConfig converterConfig, final ProductDetailsHolder productDetailsHolder,
                                       final JsonObject jsonObject) {
        final JsonArray products = jsonObject.optJsonArray(ProductDetailsHolderFields.ITEMS.getName());
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();
        final Collection<ProductDetail> oldProductDetails = Objects.isNull(productDetails) ? Collections.emptyList() : productDetails;
        final String idKey = ProductDetailFields.ID.getName();
        final boolean isPartialData = converterConfig.isPartialData();

        productDetailsHolder.setProductDetails(null);

        if (Objects.nonNull(products)) {
            final int size = products.size();
            final Collection<Long> productIds = new HashSet<>(size);

            for (int index = 0; index < products.size(); index++) {
                final JsonObject productJson = products.optJsonObject(index);
                final ProductDetail productDetail = new ProductDetail();
                ProductDetail oldProductDetail = null;

                if (productJson.containsKey(idKey)) {
                    final long id = productJson.getLong(idKey);

                    productDetail.setId(id);
                    productIds.add(id);

                    if (isPartialData) {
                        oldProductDetail = oldProductDetails.stream().filter(oldProduct -> Objects.nonNull(oldProduct.id())
                                && Objects.equals(productDetail.id(), oldProduct.id())).findAny().orElse(null);
                    }
                }

                setProductDetails(productDetailsHolder, converterConfig, productJson, productDetail, oldProductDetail);
            }

            oldProductDetails.removeIf(productDetail -> {
                        final Long id = productDetail.id();

                        return Objects.nonNull(id) && !productIds.contains(id);
                    }
            );

            if (isPartialData) {
                final Collection<ProductDetail> details = productDetailsHolder.getProductDetails();
                final Set<Long> currentProductDetailIds = Objects.isNull(details) ? Collections.emptySet() : details.stream().map(ProductDetail::id)
                        .filter(Objects::nonNull).collect(Collectors.toSet());

                oldProductDetails.stream().filter(productDetail -> !currentProductDetailIds.contains(productDetail.id()))
                        .forEach(productDetailsHolder::addProductDetail);
            }
        } else if (isPartialData) {
            productDetailsHolder.setProductDetails(oldProductDetails);
        }
    }

    /**
     * <p>
     * Sets the Json data into {@link ProductDetail}
     * </p>
     *
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     * @param converterConfig      Converter configuration
     * @param productJson          Holds the field values of product json
     * @param productDetail        An entity that holds the attributes of product details
     * @param oldProductDetail     which contains old productDetail
     */
    private void setProductDetails(final ProductDetailsHolder productDetailsHolder, final ConverterConfig converterConfig,
                                   final JsonObject productJson, final ProductDetail productDetail,
                                   final ProductDetail oldProductDetail) {
        final String variationIdKey = ProductDetailFields.VARIATION_ID.getName();
        final boolean productDetailExist = Objects.nonNull(oldProductDetail);
        final double discount = productJson.optDouble(ProductDetailFields.DISCOUNT.getName(), productDetailExist ?
                oldProductDetail.getDiscount() : DEFAULT_PERCENTAGE);
        final long quantity = productJson.optLong(ProductDetailFields.QUANTITY.getName(), productDetailExist ?
                oldProductDetail.getQuantity() : DEFAULT_QUANTITY);
        final double price = productJson.optDouble(ProductDetailFields.PRICE.getName(), productDetailExist ?
                oldProductDetail.getPrice() : DEFAULT_PRICE);

        if (productDetailsHolder.isTaxInclusive()) {
            productDetail.setTax(productJson.optDouble(ProductDetailFields.TAX.getName(), productDetailExist ?
                    oldProductDetail.getTax() : DEFAULT_PERCENTAGE));
        }

        buildProduct(ProductDetailFields.PRODUCT.getName(), converterConfig, productDetail, productJson);
        productDetail.setVariationId(productJson.containsKey(variationIdKey) ? Long.valueOf(productJson.getLong(variationIdKey))
                : productDetailExist ? oldProductDetail.getVariationId() : null);
        productDetail.setComment(productJson.optString(ProductDetailFields.COMMENT.getName(), productDetailExist ?
                oldProductDetail.getComment() : null));
        productDetail.setDiscount(discount);
        productDetail.setQuantity(quantity);
        productDetail.setPrice(price);
        productDetail.setAmount((price - (price * discount / 100)) * quantity);
        productDetail.setProductDetailsHolderId(productDetailsHolder.id());
        productDetail.setOrgId(converterConfig.orgId());

        productDetailsHolder.addProductDetail(productDetail);
    }

    /**
     * Builds the {@link ProductDetail} entity from the given json objects and map it to product
     *
     * @param productIdKey  Represents the Product fields
     * @param config        Converter configuration
     * @param productDetail Represents the entity that holds the attributes of productDetail
     * @param jsonObject    Holds the field values of product detail
     */
    private void buildProduct(final String productIdKey, final ConverterConfig config, final ProductDetail productDetail,
                              final JsonObject jsonObject) {
        final Object value = jsonObject.opt(productIdKey);
        final Product product;

        if (Objects.nonNull(value) && TypeChecker.isNonDecimalNumber(value)) {
            product = new Product();

            product.setId(NumberUtils.parseNumber(value).longValue());
        } else {
            product = value instanceof final Map<?, ?> productMap
                    ? productConverter.convert(config, Json.object((Map<String, ?>) productMap)) : config.isPartialData() ?
                    productDetail.getProduct() : null;
        }

        productDetail.setProduct(product);
    }

    /**
     * {@inheritDoc}
     *
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     * @return {@link JsonObject} Holds the field values of product details holder json
     */
    @Override
    public JsonObject convert(final ProductDetailsHolder productDetailsHolder) {
        final JsonObject productDetailsHolderJson = Json.object();

        productDetailsHolderJson.put(ProductDetailsHolderFields.ID.getName(), productDetailsHolder.id());
        productDetailsHolderJson.put(ProductDetailsHolderFields.ORG_ID.getName(), productDetailsHolder.orgId());
        productDetailsHolderJson.put(ProductDetailsHolderFields.TAX_INCLUSIVE.getName(), productDetailsHolder.isTaxInclusive());
        productDetailsHolderJson.put(ProductDetailsHolderFields.DEAL_ID.getName(), productDetailsHolder.getDealId());

        convertProductDetailToJson(productDetailsHolder, productDetailsHolderJson);

        return productDetailsHolderJson;
    }

    /**
     * <p>
     * Converts {@link ProductDetail} into {@link JsonObject}
     * </p>
     *
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     * @param productDetailsJson   Holds the field values of product details json
     */
    private void convertProductDetailToJson(final ProductDetailsHolder productDetailsHolder,
                                            final JsonObject productDetailsJson) {
        final JsonArray productDetailArray = Json.array();
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails) && !productDetails.isEmpty()) {
            final Currency currency = productDetailsHolder.getCurrency();
            final Currency baseCurrency = baseCurrencyService.getDefaultCurrency(productDetailsHolder.orgId());
            final boolean isBaseCurrency = baseCurrency.getCode().contains(currency.getCode());

            for (final ProductDetail productDetail : productDetails) {
                final JsonObject productDetailJson = Json.object();

                productDetailJson.put(ProductDetailFields.ID.getName(), productDetail.id());
                productDetailJson.put(ProductDetailFields.PRICE.getName(), productDetail.getPrice());
                productDetailJson.put(ProductDetailFields.QUANTITY.getName(), productDetail.getQuantity());
                productDetailJson.put(ProductDetailFields.DISCOUNT.getName(), productDetail.getDiscount());
                productDetailJson.put(ProductDetailFields.AMOUNT.getName(), productDetail.getAmount());
                productDetailJson.put(ProductDetailFields.VARIATION_ID.getName(), productDetail.getVariationId());
                productDetailJson.put(ProductDetailFields.COMMENT.getName(), productDetail.getComment());
                productDetailJson.put(ProductDetailFields.PRODUCT.getName(), productDetail.getProduct().id());

                if (productDetailsHolder.isTaxInclusive()) {
                    productDetailJson.put(ProductDetailFields.TAX.getName(), productDetail.getTax());
                }
                productDetailArray.put(productDetailJson);
            }

            productDetailsJson.put(ProductDetailFields.CURRENCY.getName(), currencyUnitConverter.convert(new CurrencyConverterConfig(isBaseCurrency), currency));
            productDetailsJson.put(ProductDetailFields.TAX_TOTAL.getName(), Math.round((productDetailsHolder.getTaxTotal(productDetailsHolder)) * 100.0) / 100.0);
            productDetailsJson.put(ProductDetailFields.SUB_TOTAL.getName(), Math.round((productDetailsHolder.getSubTotal(productDetailsHolder)) * 100.0) / 100.0);
            productDetailsJson.put(ProductDetailFields.TOTAL.getName(), Math.round(productDetailsHolder.getTotalAmount(productDetailsHolder) * 100.0) / 100.0);
        }

        productDetailsJson.put(ProductDetailFields.ITEMS.getName(), productDetailArray);
    }

    /**
     * Converts {@link ProductDetail} into {@link JsonObject}
     *
     * @param productDetailsHolder Represents the productDetailsHolder
     * @return {@link JsonObject} Holds the field values of product detail data
     */
    public JsonObject convertPartialDataToJson(final ProductDetailsHolder productDetailsHolder) {
        final JsonObject jsonObject = Json.object();
        final JsonArray productDetailArray = Json.array();
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails) && !productDetails.isEmpty()) {
            final String symbol = productDetailsHolder.getCurrency().getSymbol();

            for (final ProductDetail productDetail : productDetails) {
                final JsonObject productDetailJson = Json.object();

                productDetailJson.put(ProductFields.ID, productDetail.id());
                productDetailJson.put(ProductFields.PRICE, productDetail.getPrice());
                productDetailJson.put(ProductFields.PRODUCT, productConverter.convertPartialData(productDetail.getProduct()));
                productDetailJson.put(ProductFields.SYMBOL, symbol);

                productDetailArray.put(productDetailJson);
            }
        }
        jsonObject.put(ProductDetailFields.COUNT.getName(), productDetailArray.size());
        jsonObject.put(ProductDetailFields.ITEMS.getName(), productDetailArray);

        return jsonObject;
    }

    /**
     * <p>
     * Converts a {@link ProductDetailsHolder} entity into a {@link JsonObject}.
     * </p>
     *
     * @param productDetailsHolder An entity that holds the attributes of the product details holder.
     * @param converterConfig      Refers to the converter config.
     * @return {@link JsonObject} A JSON object that holds the field values of the product details holder.
     */
    @Override
    public JsonObject convertModelToJson(final ProductDetailsHolder productDetailsHolder, final ConverterConfig converterConfig) {
        final JsonObject productDetailsHolderJson = Json.object();

        productDetailsHolderJson.put(ProductDetailsHolderFields.ID.getName(), productDetailsHolder.id());
        productDetailsHolderJson.put(ProductDetailsHolderFields.ORG_ID.getName(), productDetailsHolder.orgId());
        productDetailsHolderJson.put(ProductDetailsHolderFields.TAX_INCLUSIVE.getName(), productDetailsHolder.isTaxInclusive());
        productDetailsHolderJson.put(ProductDetailsHolderFields.DEAL_ID.getName(), productDetailsHolder.getDealId());
        convertProductDetailHolderToJson(productDetailsHolder, productDetailsHolderJson, converterConfig);

        return productDetailsHolderJson;
    }

    /**
     * <p>
     * Converts {@link ProductDetail} into {@link JsonObject}
     * </p>
     *
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     * @param productDetailsJson   Holds the field values of product details json
     * @param converterConfig      Refers to the converter config
     */
    private void convertProductDetailHolderToJson(final ProductDetailsHolder productDetailsHolder, final JsonObject productDetailsJson,
                                                  final ConverterConfig converterConfig) {
        final JsonArray productDetailArray = Json.array();
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails) && !productDetails.isEmpty()) {
            final Currency currency = productDetailsHolder.getCurrency();
            final Currency baseCurrency = baseCurrencyService.getDefaultCurrency(productDetailsHolder.orgId());
            final boolean isBaseCurrency = baseCurrency.getCode().contains(currency.getCode());

            for (final ProductDetail productDetail : productDetails) {
                final JsonObject productDetailJson = Json.object();
                final Product product = productDetail.getProduct();
                final Variation variation = productDetail.getVariation();

                productDetailJson.put(ProductDetailFields.ID.getName(), productDetail.id());
                productDetailJson.put(ProductDetailFields.PRICE.getName(), productDetail.getPrice());
                productDetailJson.put(ProductDetailFields.QUANTITY.getName(), productDetail.getQuantity());
                productDetailJson.put(ProductDetailFields.DISCOUNT.getName(), productDetail.getDiscount());
                productDetailJson.put(ProductDetailFields.AMOUNT.getName(), productDetail.getAmount());
                productDetailJson.put(ProductDetailFields.VARIATION_ID.getName(), productDetail.getVariationId());
                productDetailJson.put(ProductDetailFields.COMMENT.getName(), productDetail.getComment());
                productDetailJson.put(ProductDetailFields.PRODUCT.getName(), productConverter.convertModelToOptionJson(product));

                if (Objects.nonNull(variation)) {
                    productDetailJson.put(ProductDetailFields.VARIATION.getName(), Json.object().put(ProductDetailFields.ID.getName(), variation.id())
                            .put(ProductFields.NAME, variation.getName()));
                }

                if (productDetailsHolder.isTaxInclusive()) {
                    productDetailJson.put(ProductDetailFields.TAX.getName(), productDetail.getTax());
                }
                productDetailArray.put(productDetailJson);
            }

            productDetailsJson.put(ProductDetailFields.CURRENCY.getName(), currencyUnitConverter.convertModelToJson(new CurrencyConverterConfig(converterConfig.locale(), isBaseCurrency), currency));
            productDetailsJson.put(ProductDetailFields.TAX_TOTAL.getName(), Math.round((productDetailsHolder.getTaxTotal(productDetailsHolder)) * 100.0) / 100.0);
            productDetailsJson.put(ProductDetailFields.SUB_TOTAL.getName(), Math.round((productDetailsHolder.getSubTotal(productDetailsHolder)) * 100.0) / 100.0);
            productDetailsJson.put(ProductDetailFields.TOTAL.getName(), Math.round(productDetailsHolder.getTotalAmount(productDetailsHolder) * 100.0) / 100.0);
        }

        productDetailsJson.put(ProductDetailFields.ITEMS.getName(), productDetailArray);
    }
}