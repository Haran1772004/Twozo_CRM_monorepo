package com.tlc.crm.core.action.product;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.model.ProductDealHolder;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.page.ProductDealPageHandler;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.api.product.model.converter.VariationConverter;
import com.tlc.crm.core.field.deal.ProductDetailsHolderFields;
import com.tlc.crm.core.field.product.PriceTagFields;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.field.product.VariationFields;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;

import com.tlc.crm.entity.action.EntityConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.iam.common.models.User;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Product} related web actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 * @see EntityConfigAction
 */
@SourceAuthorizer(subCategory = SubCategory.Type.PRODUCTS, validateFieldPermission = true)
@WebAction(path = "/product/mgmt")
public class ProductManagement extends EntityConfigAction<Product> {

    public static final String CONFIG_ACTION_NAME = "product";
    private static final String PRODUCT_NOT_FOUND_I18N_KEY = "i18n.product.not.found";
    private static final String PRODUCT_VARIATION_NOT_FOUND_I18N_KEY = "i18n.product.variation.not.found";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ProductConverter productConverter;
    private final ProductManager productManager;
    private final SourceSettingsService sourceSettingsService;
    private final VariationManager variationManager;
    private final VariationConverter variationConverter;
    private final BaseCurrencyService baseCurrencyService;
    private final DealManager dealManager;

    public ProductManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        productConverter = ProductConverter.getInstance();
        productManager = ProductManager.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
        variationManager = VariationManager.getInstance();
        variationConverter = VariationConverter.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ProductFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ProductFields.DATA);
        final Long orgId = request.orgId();
        final String listKey = VariationFields.LIST.getName();
        final int start = Math.max(data.optInt(PriceTagFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(PriceTagFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);

        switch (type) {
            case "summary" ->
                    fetchAndDispatchSummary(request, get(orgId, data.getLong(ProductFields.ID)), data, response);
            case "summaryV2" ->
                    fetchAndDispatchSummaryV2(request, get(orgId, data.getLong(ProductFields.ID)), data, response);
            case "updatePriceTags" -> updatePriceTags(orgId, request.userId(), data, response);
            case "getProductPriceTags" ->
                    response.put(listKey, getProductPriceTags(data, orgId, start, limit, response, request.i18nResolver()));
            case "deletePriceTag" -> deletePriceTag(orgId, data, response);
            case "getPriceTag" -> response.put(listKey, getPriceTagDetail(orgId, data));
            case "getVariationPriceTags" ->
                    response.put(listKey, getVariationPriceTags(data, orgId, start, limit, response, request.i18nResolver()));
            case "profile" -> profile(orgId, data, request, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Handles the received request
     * @param crmResponse Generates the CRM response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = crmRequest.orgId();

        switch (type) {
            case "getProductsWithPriceTag" ->
                    crmResponse.put(ProductFields.PRODUCTS, getProductsWithPriceTag(orgId, data, crmResponse));
            case "summary" -> fetchAndDispatchSummary(crmRequest, data, get(orgId, data.getLong(ProductFields.ID)), crmResponse);
            case "summaryV2" ->
                fetchAndDispatchSummaryV2(crmRequest, get(orgId, data.getLong(ProductFields.ID)), data, crmResponse);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Gets the products with their price tag details
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private JsonArray getProductsWithPriceTag(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final short currencyId = jsonObject.getShort(ProductFields.CURRENCY_ID);
        final String dealIdKey = ProductFields.DEAL_ID;
        final Long dealId = jsonObject.containsKey(dealIdKey) ? jsonObject.getLong(dealIdKey) : null;

        if (baseCurrencyService.isActive(orgId, Currency.getCurrencyById(currencyId))) {
            if (Objects.isNull(dealId) || dealManager.exists(orgId, dealId)) {
                final int start = jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE);
                final int limit = jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT);
                final ProductDealPageHandler productDealPageHandler = new ProductDealPageHandler(start, limit);
                final JsonObject criteriaJson = jsonObject.optJsonObject(ProductFields.CRITERIA);
                final JsonArray products = Json.array();
                final DropdownCriteria dropdownCriteria = Objects.isNull(criteriaJson) ? null : new DropdownCriteria(criteriaJson);

                productDealPageHandler.setOrgId(orgId);
                productDealPageHandler.setDropdownCriteria(dropdownCriteria);

                final long totalRecords = productDealPageHandler.totalRecordsCount();

                if (totalRecords > 0) {
                    final Collection<ProductDealHolder> productDealHolders = productDealPageHandler.fetchPageData().getData();

                    for (final ProductDealHolder productDealHolder : productDealHolders) {
                        final JsonObject productJson = productConverter.convert(productDealHolder.getProduct(), currencyId);

                        if (Objects.nonNull(dealId) && productDealHolder.getDealIds().contains(dealId)) {
                            productJson.put(ProductFields.IS_ADDED_WITH_DEAL, true);
                        }
                        products.put(productJson);
                    }
                }

                crmResponse.put(PriceTagFields.TOTAL_RECORDS.getName(), totalRecords);
                crmResponse.put(PriceTagFields.HAS_MORE.getName(), productDealPageHandler.hasMore());
                return products;
            } else {
                throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.product.deal.not.found");
            }
        } else {
            throw ErrorCode.get(ProductErrorCodes.CURRENCY_NOT_FOUND, "i18n.product.currency.not.found");
        }
    }

    /**
     * Gets the {@link JsonArray} of price tags of product
     *
     * @param orgId    Represents organisation id
     * @param data     Represents the {@link JsonObject} with request data
     * @param start    Represents the start value
     * @param limit    Represents the limit value
     * @param response Represents the response to be sent
     * @return the {@link JsonArray} of price tags related to the product
     */
    private JsonArray getProductPriceTags(final JsonObject data, final Long orgId, final int start, final int limit,
            final CrmResponse response, final I18nResolver i18nResolver) {
        final JsonArray priceTagsArray = Json.array();
        final Long productId = data.getLong(PriceTagFields.PRODUCT_ID.getName());
        final Long totalRecords = variationManager.getDefaultPriceTagCount(orgId, productId);

        if (!productManager.exists(orgId, productId)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, PRODUCT_NOT_FOUND_I18N_KEY);
        }

        if (totalRecords > 0 && start <= totalRecords) {
            final Map<String, SortType> fieldSortMap = fetchSortFields(data);
            final Collection<PriceTag> priceTags = variationManager.getDefaultPriceTags(orgId, productId, fieldSortMap, start, limit);

            if (Objects.nonNull(priceTags)) {
                priceTags.forEach(priceTag -> priceTagsArray.put(variationConverter.convert(priceTag, i18nResolver)));
            }
        }
        response.put(PriceTagFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(PriceTagFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return priceTagsArray;
    }

    /**
     * Deletes the price tag
     *
     * @param orgId    Organization id
     * @param data     Contains the Json object
     * @param response the response to be sent
     */
    private void deletePriceTag(final Long orgId, final JsonObject data, final CrmResponse response) {
        final PriceTag priceTag = new PriceTag();

        priceTag.setId(data.getLong(PriceTagFields.ID.getName()));
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(List.of(priceTag), Group.Delete.class);

        if (errors.isEmpty() && variationManager.isPriceTagExists(orgId, priceTag)) {
            variationManager.deletePriceTag(orgId, priceTag);
        } else {
            throw ErrorCode.get(ProductErrorCodes.PRICE_TAG_NOT_FOUND, "i18n.product.priceTag.not.found");
        }

        response.put("errors", errors);
    }

    /**
     * Gets the product price and tax using by product id or variation id
     *
     * @param orgId Organization id
     * @param data  Holds the field values
     * @return {@link JsonObject}  Holds the field values of json
     */
    private JsonObject getPriceTagDetail(final Long orgId, final JsonObject data) {
        final short currencyValue = data.getShort(PriceTagFields.CURRENCY.getName());
        final Currency currency = Currency.getCurrencyById(currencyValue);

        if (Objects.isNull(currency)) {
            throw ErrorCode.get(ProductErrorCodes.CURRENCY_NOT_FOUND, "i18n.product.currency.not.found");
        }
        final int currencyId = currency.getId();
        final JsonObject jsonObject = Json.object();
        final PriceTag priceTag;

        final String productIdKey = PriceTagFields.PRODUCT_ID.getName();
        final String variationIdKey = PriceTagFields.VARIATION_ID.getName();

        if (data.containsKey(productIdKey)) {
            final Long productId = data.getLong(productIdKey);

            if (!productManager.exists(orgId, productId)) {
                throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, PRODUCT_NOT_FOUND_I18N_KEY);
            }
            priceTag = variationManager.getPriceTagDetail(orgId, productId, null, currencyId);
        } else if (data.containsKey(variationIdKey)) {
            final Long variationId = data.getLong(variationIdKey);

            if (!variationManager.exists(orgId, variationId)) {
                throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, PRODUCT_VARIATION_NOT_FOUND_I18N_KEY);
            }
            priceTag = variationManager.getPriceTagDetail(orgId, null, variationId, currencyId);
        } else {
            throw ErrorCode.get(ProductErrorCodes.INVALID_DATA, "i18n.product.invalid.data");
        }

        if (Objects.nonNull(priceTag)) {
            setPriceTagDetail(priceTag, jsonObject, data.optBoolean(ProductDetailsHolderFields.TAX_INCLUSIVE.getName()));
        }

        return jsonObject;
    }

    /**
     * Sets the price tag details into {@link JsonObject}
     *
     * @param priceTag     {@link PriceTag}
     * @param jsonObject   Holds the field values of json
     * @param taxInclusive Holds the field values
     */
    private void setPriceTagDetail(final PriceTag priceTag, final JsonObject jsonObject, final boolean taxInclusive) {
        if (Objects.nonNull(priceTag)) {
            jsonObject.put(PriceTagFields.PRICE.getName(), baseCurrencyService.applyPrecision(priceTag.orgId(), BigDecimal.valueOf(priceTag.getPrice())));
            jsonObject.put(PriceTagFields.DISCOUNT.getName(), priceTag.getDiscount());

            if (taxInclusive) {
                jsonObject.put(PriceTagFields.TAX.getName(), priceTag.getTax());
            }
        }
    }

    /**
     * Gets the {@link JsonArray} of price tags of variation
     *
     * @param orgId    organisation id
     * @param data     the {@link JsonObject} with request data
     * @param start    Represents the start value
     * @param limit    Represents the limit value
     * @param response the response to be sent
     * @return the {@link JsonArray} of price tags related to the variation
     */
    private JsonArray getVariationPriceTags(final JsonObject data, final Long orgId, final int start, final int limit,
            final CrmResponse response, final I18nResolver i18nResolver) {
        final JsonArray priceTagsArray = Json.array();
        final Long variationId = data.getLong(PriceTagFields.VARIATION_ID.getName());

        if (!variationManager.exists(orgId, variationId)) {
            throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, PRODUCT_VARIATION_NOT_FOUND_I18N_KEY);
        }
        final Long totalRecords = variationManager.getPriceTagCount(orgId, variationId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Map<String, SortType> fieldSortMap = fetchSortFields(data);
            final Collection<PriceTag> priceTags = variationManager.getPriceTags(orgId, variationId, fieldSortMap, start, limit);

            if (Objects.nonNull(priceTags)) {
                priceTags.forEach(priceTag -> priceTagsArray.put(variationConverter.convert(priceTag, i18nResolver)));
            }
        }
        response.put(PriceTagFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(PriceTagFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return priceTagsArray;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<Product> getConfigManager() {
        return productManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Product convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return productConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest crmRequest, final Product product) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);

        return productConverter.convertModelToJson(product, converterConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Product convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return productConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request Refers the {@link CrmRequest}.
     */
    @Override
    protected final void validateHttpRequest(final CrmRequest request) {
        if (!sourceSettingsService.isSourceEnabled(request.orgId(), Product.ENTITY_TYPE)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_IS_DISABLED, "i18n.product.disabled");
        }
    }

    /**
     * <p>
     * Updates the given price tag values
     * </p>
     *
     * @param orgId    Organization id
     * @param userId   Represents an user id
     * @param data     Holds the field values
     * @param response The response to be sent
     */
    private void updatePriceTags(final Long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final Product product = new Product();
        final Long productId = data.getLong(ProductFields.PRODUCT_ID);
        final String variationIdKey = ProductFields.VARIATION_ID;
        final Long variationId = data.containsKey(variationIdKey) ? data.getLong(variationIdKey) : null;
        final boolean variationIdExists = Objects.nonNull(variationId);
        final Variation variation = new Variation();

        if (Objects.isNull(variationId)) {
            final Long id = variationManager.getDefaultVariationId(orgId, productId);

            if (Objects.isNull(id)) {
                variation.setName(Variation.DEFAULT);
                variation.setProductId(productId);
            } else {
                variation.setId(id);
            }
        } else {
            variation.setId(variationId);
        }
        variation.setOrgId(orgId);
        product.setOrgId(orgId);
        product.setId(productId);
        product.setDefaultVariation(variation);

        if (!productManager.exists(orgId, productId)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, PRODUCT_NOT_FOUND_I18N_KEY);
        }

        if (variationIdExists && !variationManager.exists(orgId, variationId)) {
            throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, PRODUCT_VARIATION_NOT_FOUND_I18N_KEY);
        }
        final JsonArray priceTags = data.optJsonArray(ProductFields.PRICE_TAGS);
        final Collection<Long> priceTagIds = new HashSet<>();

        if (Objects.nonNull(priceTags)) {

            for (int i = 0; i < priceTags.size(); i++) {
                final PriceTag priceTag = new PriceTag();
                final JsonObject priceTagJson = priceTags.getJsonObject(i);
                final String idKey = PriceTagFields.ID.getName();
                final String priceKey = PriceTagFields.UNIT_PRICE.getName();
                final String taxKey = PriceTagFields.TAX.getName();
                final String currencyKey = PriceTagFields.CURRENCY.getName();
                final String discountKey = PriceTagFields.DISCOUNT.getName();
                final Double price = priceTag.getPrice();
                final Double tax = priceTag.getTax();
                final Double discount = priceTag.getDiscount();

                if (priceTagJson.containsKey(idKey)) {
                    final Long priceTagId = priceTagJson.getLong(idKey);

                    priceTag.setId(priceTagId);
                    priceTagIds.add(priceTagId);
                }

                if (priceTagJson.containsKey(currencyKey)) {
                    final short currencyId = priceTagJson.getShort(currencyKey);
                    final Currency currency = Currency.getCurrencyById(currencyId);

                    priceTag.setCurrency(currency);
                } else {
                    priceTag.setCurrency(baseCurrencyService.getDefaultCurrency(orgId));
                }
                priceTag.setVariationId(variationId);
                priceTag.setOrgId(orgId);
                priceTag.setPrice(priceTagJson.optDouble(priceKey, Objects.isNull(price) ? 0.0 :
                        baseCurrencyService.applyPrecision(orgId, BigDecimal.valueOf(price)).doubleValue()));
                priceTag.setTax(priceTagJson.optDouble(taxKey, Objects.isNull(tax) ? 0.0 : tax));
                priceTag.setDiscount(priceTagJson.optDouble(discountKey, Objects.isNull(discount) ? 0.0 : discount));
                priceTag.setProductId(productId);

                product.addPriceTag(priceTag);
            }
        }

        if (variationIdExists && !priceTagIds.isEmpty() &&
                !variationManager.isPriceTagsAssociatedWithVariation(orgId, priceTagIds, variationId)) {
            throw ErrorCode.get(ProductErrorCodes.PRICE_TAG_VARIATION_MISMATCH, "i18n.product.priceTag.variation.mismatch");
        }

        response.put(ProductFields.UPDATED_RECORDS, productManager.updatePriceTags(orgId, userId, product));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param product    Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final Product product, final JsonObject data, final Collection<User> users) {
        return productConverter.summary(product, converterConfigBuilder.build(crmRequest, true, data));
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link Product}
     * </p>
     *
     * @param product Represents the product details
     * @param users   Contains the collection of {@link User}
     */
    @Override
    public void setUser(final Product product, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final ValueHolder valueHolder = product.getValueHolder();
        final CrmUser salesOwner = product.getSalesOwner();
        final CrmUser createdBy = product.getCreatedBy();
        final CrmUser updatedBy = product.getUpdatedBy();

        if (Objects.nonNull(salesOwner)) {
            salesOwner.setUser(userMap.get(salesOwner.getIamId()));
            product.setSalesOwner(salesOwner);
        }

        if (Objects.nonNull(createdBy)) {
            createdBy.setUser(userMap.get(createdBy.getIamId()));
            product.setCreatedBy(createdBy);
        }

        if (Objects.nonNull(updatedBy)) {
            updatedBy.setUser(userMap.get(updatedBy.getIamId()));
            product.setUpdatedBy(updatedBy);
        }

        if (Objects.nonNull(valueHolder)) {

            for (final CrmUser crmUser : valueHolder.getUserValues()) {
                crmUser.setUser(userMap.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * <p>
     * Fetches and maps sort fields from the provided {@link JsonObject}
     * </p>
     *
     * @param data Represents the request {@link JsonObject}
     * @return The {@link Map} contains key as field name value as {@link SortType}
     */
    private Map<String, SortType> fetchSortFields(final JsonObject data) {
        final String sortKey = ProductFields.SORT;
        final Map<String, SortType> fieldSortMap = new HashMap<>();

        if (data.containsKey(sortKey)) {
            final String typeKey = ProductFields.TYPE;
            final String fieldKey = ProductFields.FIELD;
            final JsonArray sortedFields = data.getJsonArray(sortKey);

            for (int index = 0; index < sortedFields.size(); index++) {
                final JsonObject sortedFieldsJson = sortedFields.getJsonObject(index);
                final String fieldName = sortedFieldsJson.getString(fieldKey);
                final String type = sortedFieldsJson.getString(typeKey).toLowerCase();

                fieldSortMap.put(fieldName, SortType.getSortType(type));
            }
        }

        if (fieldSortMap.isEmpty()) {
            fieldSortMap.put(ProductFields.CURRENCY, SortType.ASCENDING);
        }

        return fieldSortMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param product    Represents the entity model
     * @param users      Contains the collection of users
     * @param jsonObject Holds the entity details
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final JsonObject jsonObject, final Product product, final Collection<User> users) {
        return productConverter.convertSummaryIntoJson(product, converterConfigBuilder.build(crmRequest, true, jsonObject), jsonObject.optBoolean(FieldKeys.IS_HIDDEN.getName(), false));
    }

    /**
     * {@inheritDoc}
     *
     * @return The config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param product    Represents the {@link Product}
     * @param data       Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummaryV2(final CrmRequest crmRequest, final Product product, final JsonObject data, final Collection<User> users) {
        return productConverter.summaryV2(crmRequest.orgId(), crmRequest.userId(), product, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param product    Represents the {@link Product}
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} containing profile details
     */
    @Override
    protected JsonObject convertProfile(final CrmRequest crmRequest, final Product product, final JsonObject data, final Collection<User> users) {
        return productConverter.profile(product, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * Fetches the profile details of a product.
     *
     * @param orgId    Represents an organization id.
     * @param data     Represents the request json data.
     * @param request  Represents the entity of {@link CrmRequest}.
     * @param response Represents the entity of {@link CrmResponse}.
     */
    private void profile(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long productId = data.getLong(ProductFields.ID);

        if (productManager.exists(orgId, productId)) {
            final Product product = productManager.getProfile(orgId, productId);

            fetchAndDispatchProfile(request, product, data, response);
        } else {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }

    }

    /**
     * Sets the response with product overview details
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    @Override
    public void overview(final long orgId, final JsonObject data, final CrmResponse response) { //Product overview is not present currently; if needed for summary screens, logic must be added here.
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.product.unsupported.action");
    }
}
