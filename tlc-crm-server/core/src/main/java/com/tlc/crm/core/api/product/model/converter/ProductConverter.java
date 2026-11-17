package com.tlc.crm.core.api.product.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.field.product.VariationFields;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.entity.builder.ViewConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides different ways to convert the data from / into {@link Product}
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ProductConverter extends ViewConverter<Product> {

    private final VariationConverter variationConverter;
    private final ProductManager productManager;
    private final UserService userService;
    private final CrmUserConverter crmUserConverter;
    private final OptionConverter optionConverter;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final ProductConverter INSTANCE = new ProductConverter();
    }

    private ProductConverter() {
        variationConverter = VariationConverter.getInstance();
        productManager = ProductManager.getInstance();
        userService = UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        optionConverter = OptionConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    public static ProductConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Product}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the product data
     * @return the {@link Product}
     */
    @Override
    public Product convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildProductByPartialData(converterConfig, jsonObject);
        } else {
            return buildProduct(converterConfig, jsonObject);
        }
    }

    /**
     * Converts {@link JsonObject} into {@link Product}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the product data
     * @return the {@link Product}
     */
    private Product buildProduct(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Product product = new Product();
        final long orgId = converterConfig.orgId();
        final String idKey = ProductFields.ID;

        if (jsonObject.containsKey(idKey)) {
            final long id = jsonObject.getLong(idKey);
            final CrmUser salesOwner = productManager.get(orgId, id).getSalesOwner();

            product.setId(id);
            product.setSalesOwner(salesOwner);
            product.setOwnerId(salesOwner.id());
        } else {
            final Long ownerId = converterConfig.ownerId();

            product.setCreatedBy(userService.getUser(converterConfig.orgId(), ownerId));
            product.setOwnerId(ownerId);
        }

        setDataToProduct(converterConfig, product, jsonObject);
        return product;
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link Product}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the product data
     * @return the {@link Product}
     */
    private Product buildProductByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (!jsonObject.containsKey(ProductFields.ID)) {
            return buildProduct(converterConfig, jsonObject);
        }

        final long productId = jsonObject.getLong(ProductFields.ID);

        if (productManager.exists(converterConfig.orgId(), productId)) {
            final Product product = productManager.get(converterConfig.orgId(), productId, true);

            setDataToProduct(converterConfig, product, jsonObject);
            return product;
        } else {
            throw ErrorCode.get(ProductErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
    }
    
    /**
     * Sets the json data into product
     *
     * @param converterConfig converter configuration
     * @param product         which contains the product data
     * @param jsonObject      which contains the product fields data
     */
    public void setDataToProduct(final ConverterConfig converterConfig, final Product product,
                                 final JsonObject jsonObject) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE);
        final Long orgId = converterConfig.orgId();
        final Long ownerId = converterConfig.ownerId();
        final String nameKey = fields.get(ProductFields.NAME).idAsString();
        final String descriptionKey = fields.get(ProductFields.DESCRIPTION).idAsString();
        final String productCodeKey = fields.get(ProductFields.PRODUCT_CODE).idAsString();
        final String skuNumberKey = fields.get(ProductFields.SKU_NUMBER).idAsString();
        final FieldInfo typeField = fields.get(ProductFields.TYPE);
        final FieldInfo activeStatusField = fields.get(ProductFields.ACTIVE_STATUS);
        final String activeStatusKey = activeStatusField.idAsString();
        final String typeKey = typeField.idAsString();
        final String validTillKey = fields.get(ProductFields.VALID_TILL).idAsString();
        final String quantityKey = fields.get(ProductFields.QUANTITY_IN_STOCK).idAsString();
        final String quantityOrderedKey = fields.get(ProductFields.QUANTITY_ORDERED).idAsString();
        final String salesOwnerKey = fields.get(ProductFields.SALES_OWNER).idAsString();
        final CrmUser crmUser = userService.getUser(orgId, ownerId);
        final FieldInfo categoryField = fields.get(ProductFields.CATEGORY);
        final String categoryKey = categoryField.idAsString();
        final FieldInfo taxableField = fields.get(ProductFields.TAXABLE);
        final String taxableKey = taxableField.idAsString();
        final Option defaultActiveStatusOption = optionService.getOption(orgId, activeStatusField, Product.ActiveStatus.YES.getName());

        product.setOrgId(orgId);
        product.setName(jsonObject.optNullableValue(nameKey, product.getName()));
        product.setDescription(jsonObject.optNullableValue(descriptionKey, product.getDescription()));
        product.setSkuNumber(jsonObject.optNullableValue(skuNumberKey, product.getSkuNumber()));
        product.setProductCode(jsonObject.optNullableValue(productCodeKey, product.getProductCode()));
        product.setValidTill(jsonObject.optNullableValue(validTillKey, product.getValidTill()));
        setSalesOwner(orgId, salesOwnerKey, jsonObject, product, ownerId);

        product.setUpdatedBy(crmUser);

        if (jsonObject.containsKey(quantityKey)) {
            if (jsonObject.isKeyPresentWithNullValue(quantityKey)) {
                product.setQuantityInStock(null);
            } else {
                product.setQuantityInStock(jsonObject.getLong(quantityKey));
            }
        }

        if (jsonObject.containsKey(quantityOrderedKey)) {
            if (jsonObject.isKeyPresentWithNullValue(quantityOrderedKey)) {
                product.setQuantityOrdered(null);
            } else {
                product.setQuantityOrdered(jsonObject.getLong(quantityOrderedKey));
            }
        }

        if (jsonObject.containsKey(activeStatusKey)) {
            if (jsonObject.isKeyPresentWithNullValue(activeStatusKey)) {
                product.setActiveStatus(defaultActiveStatusOption);
            } else {
                product.setActiveStatus(optionConverter.convert(orgId, activeStatusField.id(), jsonObject.getLong(activeStatusKey)));
            }
        } else if (Objects.isNull(product.id())) {
            product.setActiveStatus(defaultActiveStatusOption);
        }

        if (jsonObject.containsKey(typeKey)) {
            if (jsonObject.isKeyPresentWithNullValue(typeKey)) {
                product.setType(null);
            } else {
                product.setType(optionConverter.convert(orgId, typeField.id(), jsonObject.getLong(typeKey)));
            }
        }

        if (jsonObject.containsKey(categoryKey)) {
            if (jsonObject.isKeyPresentWithNullValue(categoryKey)) {
                product.setCategory(null);
            } else {
                product.setCategory(optionConverter.convert(orgId, categoryField.id(), jsonObject.getLong(categoryKey)));
            }
        }
        final Variation defaultVariation = product.getDefaultVariation();

        buildDefaultVariation(converterConfig, product, jsonObject, defaultVariation, fields);
        final Option defaultTaxableOption = optionService.getOption(orgId, taxableField, Product.Taxable.YES.getName());

        if (jsonObject.containsKey(taxableKey)) {
            if (jsonObject.isKeyPresentWithNullValue(taxableKey)) {
                product.setTaxable(defaultTaxableOption);
            } else {
                product.setTaxable(optionConverter.convert(orgId, taxableField.id(), jsonObject.getLong(taxableKey)));
            }
        } else {
            product.setTaxable(defaultTaxableOption);
        }

        if (Objects.nonNull(defaultVariation) && defaultVariation.getPriceTags().stream().map(PriceTag::getTax).anyMatch(tax -> tax > 0.0)) {
            product.setTaxable(defaultTaxableOption);
        }

        if (converterConfig.areReadOnlyFieldsRequired()) {
            final String createdAtKey = fields.get(ProductFields.CREATED_ON).idAsString();

            if (jsonObject.containsKey(createdAtKey)) {
                product.setCreatedTime(new DateTime(jsonObject.getLong(createdAtKey)));
            }
        }
        entityBuilder.buildValueHolder(product, converterConfig, jsonObject);
        defaultFieldValuesLoader.load(product);
    }

    /**
     * <p>
     * Set default variation details of product
     * </p>
     *
     * @param converterConfig Converter configuration
     * @param fields          System entity of product
     * @param product         An entity that represent a product to which the priceTags will be set
     * @param jsonObject      Holds the product attributes
     */
    private void buildDefaultVariation(final ConverterConfig converterConfig, final Product product, final JsonObject jsonObject, final Variation defaultVariation, final Map<String, FieldInfo> fields) {
        final String variationIdKey = ProductFields.VARIATION_ID;
        final boolean hasVariationId = jsonObject.containsKey(variationIdKey);

        if (Objects.isNull(defaultVariation) || hasVariationId) {
            final String priceTagKey = fields.get(VariationFields.PRICE_TAGS.getName()).idAsString();
            final Variation currentVariation = variationConverter.buildDefaultVariation(converterConfig.orgId(), product.id(), priceTagKey, jsonObject);

            if (hasVariationId) {
                currentVariation.setId(jsonObject.getLong(variationIdKey));
            }
            product.setDefaultVariation(currentVariation);
        }
    }

    /**
     * <p>
     * Converts {@link Product} into {@link JsonObject}
     * </p>
     *
     * @param product An entity that represent a product
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Product product) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = product.getValueHolder();
        final Variation variation = product.getDefaultVariation();
        final Option category = product.getCategory();
        final Option type = product.getType();
        final Option activeStatus = product.getActiveStatus();
        final Option taxable = product.getTaxable();
        final DateTime createdTime = product.getCreatedTime();
        final CrmUser createdBy = product.getCreatedBy();
        final CrmUser updatedBy = product.getUpdatedBy();
        final CrmUser salesOwner = product.getSalesOwner();

        final String descriptionAlias = ProductFields.DESCRIPTION;
        final String productCodeAlias = ProductFields.PRODUCT_CODE;
        final String skuNumberAlias = ProductFields.SKU_NUMBER;
        final String validTillAlias = ProductFields.VALID_TILL;
        final String taxableAlias = ProductFields.TAXABLE;
        final String quantityInStockAlias = ProductFields.QUANTITY_IN_STOCK;
        final String typeAlias = ProductFields.TYPE;
        final String activeStatusAlias = ProductFields.ACTIVE_STATUS;
        final String createdByAlias = ProductFields.CREATED_BY;
        final String updatedByAlias = ProductFields.UPDATED_BY;
        final String salesOwnerAlias = ProductFields.SALES_OWNER;
        final String categoryAlias = ProductFields.CATEGORY;
        final String quantityOrderedAlias = ProductFields.QUANTITY_ORDERED;
        final String createdOnAlias = ProductFields.CREATED_ON;

        jsonObject.put(ProductFields.ID, product.id());
        jsonObject.put(ProductFields.ORG_ID, product.orgId());
        jsonObject.put(fields.get(ProductFields.NAME).idAsString(), product.getName());

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).idAsString(), product.getDescription());
        }

        if (fields.containsKey(productCodeAlias)) {
            jsonObject.put(fields.get(productCodeAlias).idAsString(), product.getProductCode());
        }

        if (fields.containsKey(skuNumberAlias)) {
            jsonObject.put(fields.get(skuNumberAlias).idAsString(), product.getSkuNumber());
        }

        if (fields.containsKey(validTillAlias)) {
            jsonObject.put(fields.get(validTillAlias).idAsString(), product.getValidTill());
        }

        if (Objects.nonNull(taxable) && fields.containsKey(taxableAlias)) {
            jsonObject.put(fields.get(taxableAlias).idAsString(), taxable.id());
        }

        if (fields.containsKey(quantityInStockAlias)) {
            jsonObject.put(fields.get(quantityInStockAlias).idAsString(), product.getQuantityInStock());
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            jsonObject.put(fields.get(typeAlias).idAsString(), type.id());
        }

        if (Objects.nonNull(activeStatus) && fields.containsKey(activeStatusAlias)) {
            jsonObject.put(fields.get(activeStatusAlias).idAsString(), activeStatus.id());
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), salesOwner.id());
        }

        if (Objects.nonNull(category) && fields.containsKey(categoryAlias)) {
            jsonObject.put(fields.get(categoryAlias).idAsString(), category.id());
        }

        if (fields.containsKey(quantityOrderedAlias)) {
            jsonObject.put(fields.get(quantityOrderedAlias).idAsString(), product.getQuantityOrdered());
        }

        if (Objects.nonNull(variation)) {
            final JsonArray variationArray = Json.array();
            final JsonArray priceTagArray = Json.array();
            final Collection<PriceTag> priceTags = variation.getPriceTags();
            final String priceTagsAlias = ProductFields.PRICE_TAGS;

            if (Objects.equals(variation.getName(), Variation.DEFAULT) && Objects.nonNull(priceTags)) {

                for (final PriceTag priceTag : priceTags) {
                    priceTagArray.put(variationConverter.convertPriceTagToJson(priceTag, true));
                }
            } else {
                variationArray.put(variationConverter.convert(variation));
            }
            jsonObject.put(ProductFields.VARIATION_ID, variation.id());

            if (fields.containsKey(priceTagsAlias)) {
                jsonObject.put(fields.get(priceTagsAlias).idAsString(), priceTagArray);
            }
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject);
        }

        if (Objects.nonNull(createdTime) && fields.containsKey(createdOnAlias)) {
            jsonObject.put(fields.get(createdOnAlias).idAsString(), createdTime.getTimeStamp());
        }
        return jsonObject;
    }

    /**
     * Converts {@link Product} into {@link JsonObject}
     *
     * @param product         An entity that holds the attributes of product details
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Product product) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(product);
        } else {
            final ValueHolder valueHolder = product.getValueHolder();
            final Variation variation = product.getDefaultVariation();
            final Option type = product.getType();
            final Option activeStatus = product.getActiveStatus();
            final Option taxable = product.getTaxable();

            jsonObject.put(ProductFields.ID, product.id());
            jsonObject.put(ProductFields.ORG_ID, product.orgId());
            jsonObject.put(ProductFields.NAME, product.getName());
            jsonObject.put(ProductFields.DESCRIPTION, product.getDescription());
            jsonObject.put(ProductFields.PRODUCT_CODE, product.getProductCode());
            jsonObject.put(ProductFields.SKU_NUMBER, product.getSkuNumber());
            jsonObject.put(ProductFields.VALID_TILL, product.getValidTill());
            jsonObject.put(ProductFields.TAXABLE, Objects.isNull(taxable) ? null : taxable.id());
            jsonObject.put(ProductFields.QUANTITY_IN_STOCK, product.getQuantityInStock());
            jsonObject.put(ProductFields.TYPE, Objects.isNull(type) ? null : type.id());
            jsonObject.put(ProductFields.ACTIVE_STATUS, Objects.isNull(activeStatus) ? null : activeStatus.id());
            jsonObject.put(ProductFields.QUANTITY_ORDERED, product.getQuantityOrdered());

            if (Objects.nonNull(variation)) {
                jsonObject.put(ProductFields.VARIATION_ID, variation.id());
            }
            final Option category = product.getCategory();

            if (Objects.nonNull(category)) {
                jsonObject.put(ProductFields.CATEGORY, category.id());
            }

            if (Objects.nonNull(variation)) {
                final JsonArray variationArray = Json.array();
                final JsonArray priceTagArray = Json.array();
                final Collection<PriceTag> priceTags = variation.getPriceTags();

                if (Objects.equals(variation.getName(), Variation.DEFAULT) && Objects.nonNull(priceTags)) {

                    for (final PriceTag priceTag : priceTags) {
                        priceTagArray.put(variationConverter.convertPriceTagToJson(priceTag, false));
                    }
                } else {
                    variationArray.put(variationConverter.convert(variation));
                }

                jsonObject.put(ProductFields.PRICE_TAGS, priceTagArray);

            }

            if (Objects.nonNull(valueHolder)) {
                valueHolder.mapToJson(jsonObject);
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Product} into {@link JsonObject}
     *
     * @param product Represents an entity that represent a product
     * @return the {@link JsonObject} Holds the field values of product data
     */
    @Override
    public JsonObject convertPartialData(final Product product) {
        final JsonObject productJson = Json.object();

        productJson.put(ProductFields.ID, product.id());
        productJson.put(ProductFields.NAME, product.getName());

        return productJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param product         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject summary(final Product product, final ConverterConfig converterConfig) {
        final JsonObject json = Json.object();

        json.put(ProductFields.ID, product.id());
        json.put(ProductFields.PROFILE, convertProfileToJsonObject(product, converterConfig));
        json.put(ProductFields.SUMMARY, convertSummaryToJsonArray(product, converterConfig));
        convertSubFieldToJson(product, converterConfig, json);

        return json;
    }

    /**
     * <p>
     * Converts the summary details
     * </p>
     *
     * @param product          Represents the {@link Product}
     * @param converterConfig  Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} contains product summary details
     */
    @Override
    public JsonObject summaryV2(final Long orgId, final Long userId, final Product product, final ConverterConfig converterConfig) {
        final JsonObject productJson = Json.object();

        productJson.put(ProductFields.ID, product.id());
        loadSummaryFieldPreference(orgId, userId, Source.PRODUCT.getId(), productJson);
        loadSummaryFields(product, productJson, converterConfig);
        convertSubFieldToJson(product, converterConfig, productJson);

        return productJson;
    }

    /**
     * <p>
     * Converts the profile details
     * </p>
     *
     * @param product          Represents the {@link Product}
     * @param converterConfig  Represents the {@link ConverterConfig}
     * @return The {@link JsonObject} contains product profile details
     */
    @Override
    public JsonObject profile(final Product product, final ConverterConfig converterConfig) {
        final JsonObject productJson = convertProfileToJsonObject(product, converterConfig);

        productJson.put(ProductFields.ID, product.id());

        return productJson;
    }

    /**
     * <p>
     * Converts the fields into json object
     * </p>
     *
     * @param fieldInfo Represents a data carrier that holds field details
     * @param product   A model that represents product
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertFieldInfoToJsonObject(final FieldInfo fieldInfo, final Product product, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        if (fieldInfo.isPersistent()) {
            setValuesToJson(product.getValueHolder().get(fieldInfo.id()), jsonObject, fieldInfo);
        } else {
            switch (fieldInfo.aliasName()) {
                case "category" -> {
                    final Option category = product.getCategory();

                    if (Objects.nonNull(category)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(category, converterConfig));
                    }
                }
                case "skuNumber" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getSkuNumber());
                case "productCode" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getProductCode());
                case "validTill" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getValidTill());
                case "name" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getName());
                case "description" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getDescription());
                case "quantityInStock" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getQuantityInStock());
                case "type" -> {
                    final Option type = product.getType();

                    if (Objects.nonNull(type)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(type, converterConfig));
                    }
                }
                case "activeStatus" -> {
                    final Option activeStatus = product.getActiveStatus();

                    if (Objects.nonNull(activeStatus)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(activeStatus, converterConfig));
                    }
                }
                case "taxable" -> {
                    final Option taxable = product.getTaxable();

                    if (Objects.nonNull(taxable)) {
                        jsonObject.put(FieldKeys.VALUE.getName(), convertOptionToJson(taxable, converterConfig));
                    }
                }
                case "createdOn" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(product.getCreatedTime()));
                case "salesOwner" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(product.getSalesOwner(), true, true));
                case "createdBy" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(product.getCreatedBy(), true, false));
                case "updatedBy" ->
                        jsonObject.put(FieldKeys.VALUE.getName(), crmUserConverter.convert(product.getUpdatedBy(), true, false));
                case "id" -> jsonObject.put(FieldKeys.VALUE.getName(), product.id());
                case "baseCurrencyAmount" -> {
                    final Variation variation = product.getDefaultVariation();
                    final Long orgId = product.orgId();

                    if (Objects.nonNull(variation)) {
                        final Collection<PriceTag> priceTags = variation.getPriceTags();
                        final Currency orgBaseCurrency = baseCurrencyService.getDefaultCurrency(orgId);

                        for (final PriceTag priceTag : priceTags) {
                            final Currency currency = priceTag.getCurrency();

                            if (Objects.equals(orgBaseCurrency, currency)) {
                                final Monetary monetary = new Monetary();

                                monetary.setOrgId(orgId);
                                monetary.setValue(BigDecimal.valueOf(priceTag.getPrice()));
                                monetary.setCurrency(orgBaseCurrency);
                                jsonObject.put(FieldKeys.VALUE.getName(), convertSubTypesToJson(StoreType.MONETARY, List.of(monetary)));
                                break;
                            }
                        }
                    }
                }
                case "quantityOrdered" -> jsonObject.put(FieldKeys.VALUE.getName(), product.getQuantityOrdered());
                case "updatedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(product.getModifiedTime()));
                case "lastAssignedAt" -> jsonObject.put(FieldKeys.VALUE.getName(), dateTimeUtil.getTimeStamp(product.getLastAssignedAt()));
            }
        }
        loadFieldsIntoJson(jsonObject, fieldInfo, converterConfig);

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param product Represents the {@link Product}
     * @return the summary fields
     */
    @Override
    protected final Map<Integer, List<FieldInfo>> getSummaryFields(final Product product) {
        final Collection<FieldInfo> fields = secureFieldService.getFieldsBySource(product.orgId(), product.getEntityType());

        return fields.stream()
                .filter(field -> {
                    final PresetConfig presetConfig = field.presetConfig();
                    final String fieldAliasName = field.aliasName();

                    return (!presetConfig.showOnProfile() || Objects.equals(ProductFields.NAME, fieldAliasName))
                            && !presetConfig.hidden() && !Objects.equals(ProductFields.PRICE_TAGS, fieldAliasName);
                })
                .sorted(Comparator.comparing(field -> field.presetConfig().orderOnView()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().rowIndexOnView()));
    }

    /**
     * <p>
     * Converts a {@link Product} into a {@link JsonObject}.
     * </p>
     *
     * @param product         An entity of type {@link Product} that holds the attributes of product details.
     * @param converterConfig The configuration settings used for conversion, including locale information.
     * @return A {@link JsonObject} containing the product's attributes structured as JSON.
     */
    @Override
    public JsonObject convertModelToJson(final Product product, final ConverterConfig converterConfig) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = product.getValueHolder();
        final Variation variation = product.getDefaultVariation();
        final Option category = product.getCategory();
        final Option type = product.getType();
        final Option activeStatus = product.getActiveStatus();
        final Option taxable = product.getTaxable();
        final CrmUser salesOwner = product.getSalesOwner();
        final CrmUser updatedBy = product.getUpdatedBy();
        final CrmUser createdBy = product.getCreatedBy();

        final String descriptionAlias = ProductFields.DESCRIPTION;
        final String productCodeAlias = ProductFields.PRODUCT_CODE;
        final String skuNumberAlias = ProductFields.SKU_NUMBER;
        final String validTillAlias = ProductFields.VALID_TILL;
        final String taxableAlias = ProductFields.TAXABLE;
        final String quantityInStockAlias = ProductFields.QUANTITY_IN_STOCK;
        final String typeAlias = ProductFields.TYPE;
        final String activeStatusAlias = ProductFields.ACTIVE_STATUS;
        final String createdByAlias = ProductFields.CREATED_BY;
        final String updatedByAlias = ProductFields.UPDATED_BY;
        final String salesOwnerAlias = ProductFields.SALES_OWNER;
        final String categoryAlias = ProductFields.CATEGORY;
        final String quantityOrderedAlias = ProductFields.QUANTITY_ORDERED;

        jsonObject.put(ProductFields.ID, product.id());
        jsonObject.put(ProductFields.ORG_ID, product.orgId());
        jsonObject.put(fields.get(ProductFields.NAME).idAsString(), product.getName());

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).idAsString(), product.getDescription());
        }

        if (fields.containsKey(productCodeAlias)) {
            jsonObject.put(fields.get(productCodeAlias).idAsString(), product.getProductCode());
        }

        if (fields.containsKey(skuNumberAlias)) {
            jsonObject.put(fields.get(skuNumberAlias).idAsString(), product.getSkuNumber());
        }

        if (fields.containsKey(validTillAlias)) {
            jsonObject.put(fields.get(validTillAlias).idAsString(), product.getValidTill());
        }

        if (Objects.nonNull(taxable) && fields.containsKey(taxableAlias)) {
            jsonObject.put(fields.get(taxableAlias).idAsString(), optionConverter.convertModelToOptionJson(taxable, converterConfig));
        }

        if (fields.containsKey(quantityInStockAlias)) {
            jsonObject.put(fields.get(quantityInStockAlias).idAsString(), product.getQuantityInStock());
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            jsonObject.put(fields.get(typeAlias).idAsString(), optionConverter.convertModelToOptionJson(type, converterConfig));
        }

        if (Objects.nonNull(activeStatus) && fields.containsKey(activeStatusAlias)) {
            jsonObject.put(fields.get(activeStatusAlias).idAsString(), optionConverter.convertModelToOptionJson(activeStatus, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).idAsString(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).idAsString(), updatedBy.id());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).idAsString(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (Objects.nonNull(category) && fields.containsKey(categoryAlias)) {
            jsonObject.put(fields.get(categoryAlias).idAsString(), optionConverter.convertModelToOptionJson(category, converterConfig));
        }

        if (fields.containsKey(quantityOrderedAlias)) {
            jsonObject.put(fields.get(quantityOrderedAlias).idAsString(), product.getQuantityOrdered());
        }

        if (Objects.nonNull(variation)) {
            final JsonArray priceTagArray = Json.array();
            final Collection<PriceTag> priceTags = variation.getPriceTags();
            final String priceTagsAlias = ProductFields.PRICE_TAGS;

            if (Objects.nonNull(priceTags)) {

                for (final PriceTag priceTag : priceTags) {
                    priceTagArray.put(variationConverter.convertPriceTagToJson(true, priceTag));
                }
            }

            jsonObject.put(ProductFields.VARIATION_ID, variation.id());

            if (fields.containsKey(priceTagsAlias)) {
                jsonObject.put(fields.get(priceTagsAlias).idAsString(), priceTagArray);
            }
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig);
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param product         An entity of type {@link Product} that holds the attributes of product details.
     * @param converterConfig The configuration settings used for conversion, including locale information.
     * @return A {@link JsonObject} containing the product's attributes structured as JSON.
     */
    @Override
    public JsonObject convertToRestJson(final Product product, final ConverterConfig converterConfig) {
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE);
        final JsonObject jsonObject = Json.object();
        final ValueHolder valueHolder = product.getValueHolder();
        final Variation variation = product.getDefaultVariation();
        final Option category = product.getCategory();
        final Option type = product.getType();
        final Option activeStatus = product.getActiveStatus();
        final Option taxable = product.getTaxable();
        final CrmUser salesOwner = product.getSalesOwner();
        final CrmUser updatedBy = product.getUpdatedBy();
        final CrmUser createdBy = product.getCreatedBy();

        final String descriptionAlias = ProductFields.DESCRIPTION;
        final String productCodeAlias = ProductFields.PRODUCT_CODE;
        final String skuNumberAlias = ProductFields.SKU_NUMBER;
        final String validTillAlias = ProductFields.VALID_TILL;
        final String taxableAlias = ProductFields.TAXABLE;
        final String quantityInStockAlias = ProductFields.QUANTITY_IN_STOCK;
        final String typeAlias = ProductFields.TYPE;
        final String activeStatusAlias = ProductFields.ACTIVE_STATUS;
        final String createdByAlias = ProductFields.CREATED_BY;
        final String updatedByAlias = ProductFields.UPDATED_BY;
        final String salesOwnerAlias = ProductFields.SALES_OWNER;
        final String categoryAlias = ProductFields.CATEGORY;
        final String quantityOrderedAlias = ProductFields.QUANTITY_ORDERED;

        jsonObject.put(ProductFields.ID, product.id());
        jsonObject.put(fields.get(ProductFields.NAME).getInternalName(), product.getName());

        if (fields.containsKey(descriptionAlias)) {
            jsonObject.put(fields.get(descriptionAlias).getInternalName(), product.getDescription());
        }

        if (fields.containsKey(productCodeAlias)) {
            jsonObject.put(fields.get(productCodeAlias).getInternalName(), product.getProductCode());
        }

        if (fields.containsKey(skuNumberAlias)) {
            jsonObject.put(fields.get(skuNumberAlias).getInternalName(), product.getSkuNumber());
        }

        if (fields.containsKey(validTillAlias)) {
            jsonObject.put(fields.get(validTillAlias).getInternalName(), product.getValidTill());
        }

        if (Objects.nonNull(taxable) && fields.containsKey(taxableAlias)) {
            jsonObject.put(fields.get(taxableAlias).getInternalName(), optionConverter.convertModelToOptionJson(taxable, converterConfig));
        }

        if (fields.containsKey(quantityInStockAlias)) {
            jsonObject.put(fields.get(quantityInStockAlias).getInternalName(), product.getQuantityInStock());
        }

        if (Objects.nonNull(type) && fields.containsKey(typeAlias)) {
            jsonObject.put(fields.get(typeAlias).getInternalName(), optionConverter.convertModelToOptionJson(type, converterConfig));
        }

        if (Objects.nonNull(activeStatus) && fields.containsKey(activeStatusAlias)) {
            jsonObject.put(fields.get(activeStatusAlias).getInternalName(), optionConverter.convertModelToOptionJson(activeStatus, converterConfig));
        }

        if (Objects.nonNull(createdBy) && fields.containsKey(createdByAlias)) {
            jsonObject.put(fields.get(createdByAlias).getInternalName(), createdBy.id());
        }

        if (Objects.nonNull(updatedBy) && fields.containsKey(updatedByAlias)) {
            jsonObject.put(fields.get(updatedByAlias).getInternalName(), updatedBy.id());
        }

        if (Objects.nonNull(salesOwner) && fields.containsKey(salesOwnerAlias)) {
            jsonObject.put(fields.get(salesOwnerAlias).getInternalName(), crmUserConverter.convertModelToOptionJson(salesOwner));
        }

        if (Objects.nonNull(category) && fields.containsKey(categoryAlias)) {
            jsonObject.put(fields.get(categoryAlias).getInternalName(), optionConverter.convertModelToOptionJson(category, converterConfig));
        }

        if (fields.containsKey(quantityOrderedAlias)) {
            jsonObject.put(fields.get(quantityOrderedAlias).getInternalName(), product.getQuantityOrdered());
        }

        if (Objects.nonNull(variation)) {
            final JsonArray priceTagArray = Json.array();
            final Collection<PriceTag> priceTags = variation.getPriceTags();
            final String priceTagsAlias = ProductFields.PRICE_TAGS;

            if (Objects.nonNull(priceTags)) {

                for (final PriceTag priceTag : priceTags) {
                    priceTagArray.put(variationConverter.convertPriceTagToJson(false, priceTag));
                }
            }

            if (fields.containsKey(priceTagsAlias)) {
                jsonObject.put(fields.get(priceTagsAlias).getInternalName(), priceTagArray);
            }
        }

        if (Objects.nonNull(valueHolder)) {
            valueHolder.mapToJson(jsonObject, converterConfig, true);
        }

        return jsonObject;
    }

    /**
     * Converts {@link Product} into {@link JsonObject}
     *
     * @param product The {@link Product} entity containing basic company information.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Product product) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CompanyFields.VALUE, product.id());
        jsonObject.put(CompanyFields.NAME, product.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Product} to {@link JsonObject}
     * </p>
     *
     * @param product    Represents the {@link Product}
     * @param currencyId Represents the currency id
     * @return {@link JsonObject}
     */
    public JsonObject convert(final Product product, final short currencyId) {
        final JsonObject jsonObject = Json.object();
        final JsonArray priceTagArray = Json.array();
        final Variation variation = product.getDefaultVariation();
        final String hasPriceTagsKey = ProductFields.HAS_PRICE_TAGS;

        if (Objects.nonNull(variation)) {
            final Collection<PriceTag> priceTags = variation.getPriceTags();

            if (Objects.nonNull(priceTags)) {

                for (final PriceTag priceTag : priceTags) {
                    final Currency currency = priceTag.getCurrency();
                    final JsonObject priceTagJson = Json.object();

                    if (Objects.equals(currencyId, currency.getId())) {
                        final JsonObject currencyJson = Json.object();

                        priceTagJson.put(ProductFields.PRICE, priceTag.getPrice());
                        priceTagJson.put(ProductFields.DISCOUNT, priceTag.getDiscount());
                        priceTagJson.put(ProductFields.TAX, priceTag.getTax());

                        currencyJson.put(ProductFields.SYMBOL, currency.getSymbol());
                        currencyJson.put(ProductFields.ID, currency.getId());
                        currencyJson.put(ProductFields.NAME, currency.getName());
                        currencyJson.put(ProductFields.CODE, currency.getCode());

                        priceTagJson.put(ProductFields.CURRENCY, currencyJson);

                        priceTagArray.put(priceTagJson);
                    }
                }
            }
        }
        jsonObject.put(ProductFields.ID, product.id());
        jsonObject.put(ProductFields.NAME, product.getName());

        if (priceTagArray.size() > 0) {
            jsonObject.put(ProductFields.PRICE_TAGS, priceTagArray);
            jsonObject.put(hasPriceTagsKey, true);
        } else {
            jsonObject.put(hasPriceTagsKey, false);
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param product         Holds the data of specific entity
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param isHidden        A boolean flag indicating whether to display empty fields or not
     * @return {@link JsonObject}
     */
    public JsonObject convertSummaryIntoJson(final Product product, final ConverterConfig converterConfig, final boolean isHidden) {
        final JsonObject json = Json.object();
        final JsonObject profileJson = convertProfileToJson(product, converterConfig);

        json.put(ProductFields.ID, product.id());
        json.put(ProductFields.PROFILE, profileJson);
        json.put(ProductFields.SUMMARY, convertSummaryToJsonArray(product, isHidden, converterConfig));
        convertSubFieldToJson(product, converterConfig, json);

        return json;
    }
}
