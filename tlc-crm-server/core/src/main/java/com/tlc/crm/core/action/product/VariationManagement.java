package com.tlc.crm.core.action.product;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.api.product.model.converter.VariationConverter;
import com.tlc.crm.core.field.product.VariationFields;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.core.page.VariationPageHandler;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages {@link Variation} related web actions
 *
 * @author EswariNivethaVU
 * @version 1.0
 * @see CrmConfigAction
 */
@WebAction(path = "/product/variation/mgmt")
public class VariationManagement extends CrmConfigAction<Variation> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;

    private final VariationConverter variationConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final VariationManager variationManager;
    private final ProductManager productManager;
    private final ModelValidator validator;
    private final SourceSettingsService sourceSettingsService;
    private final BaseCurrencyService baseCurrencyService;

    public VariationManagement() {
        variationConverter = VariationConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        variationManager = VariationManager.getInstance();
        productManager = ProductManager.getInstance();
        validator = ValidatorServiceProvider.get().modelValidator();
        sourceSettingsService = SourceSettingsService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final String listKey = VariationFields.LIST.getName();

        switch (type) {
            case "list" -> response.put(listKey, getProductVariations(data, orgId, response));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AbstractConfigManager of variation manager}
     */
    @Override
    public ConfigManager<Variation> getConfigManager() {
        return variationManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    the received request
     * @param jsonObject which contains variation json
     */
    @Override
    public Variation convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return variationConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param variation An entity that holds the variation attributes
     */
    @Override
    public JsonObject convert(final Variation variation) {
        return variationConverter.convert(variation);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    the received request
     * @param jsonObject which contains variation json data
     */
    @Override
    public Variation convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return variationConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Gets the {@link JsonArray} of variations by providing product id
     *
     * @param orgId    organisation id
     * @param data     the {@link JsonObject} with request data
     * @param response the response to be sent
     * @return the {@link JsonArray} of variations related to the product
     */
    private JsonArray getProductVariations(final JsonObject data, final Long orgId, final CrmResponse response) {
        final JsonArray variationsArray = Json.array();
        final Long productId = data.getLong(VariationFields.PRODUCT_ID.getName());

        if (!productManager.exists(orgId, productId)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }

        final int start = Math.max(data.optInt(VariationFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(VariationFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final boolean includeProduct = data.optBoolean(VariationFields.INCLUDE_PRODUCT.getName(), false);
        final JsonObject criteriaJson = data.optJsonObject(FieldKeys.CRITERIA.getName());
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        if (Objects.nonNull(criteriaJson)) {
            final Object criteriaValue = criteriaJson.get(FieldKeys.VALUE.getName());
            final String criteriaType = criteriaJson.getString(FieldKeys.TYPE.getName());

            dropdownCriteria.setCriteriaValue(criteriaValue);
            dropdownCriteria.setCriteriaType(criteriaType);
        }
        final VariationPageHandler variationPageHandler = new VariationPageHandler(productId, start, limit, dropdownCriteria);

        variationPageHandler.setOrgId(orgId);
        final long totalRecords = variationPageHandler.totalRecordsCount();

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<Variation> variations = variationPageHandler.fetchPageData().getData();

            if (Objects.nonNull(variations)) {
                variations.forEach(variation -> variationsArray.put(variationConverter.partialConvert(variation)));
            }

            if (includeProduct) {
                final String productName = productManager.getProductName(orgId, productId);

                if (Objects.nonNull(productName)) {
                    response.put(VariationFields.PRODUCT.getName(), Json.object()
                            .put(VariationFields.ID.getName(), productId)
                            .put(VariationFields.NAME.getName(), productName));
                }
            }
        }
        response.put(VariationFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(VariationFields.HAS_MORE.getName(), variationPageHandler.hasMore());

        return variationsArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id
     * @param userId     Represents the user id.
     * @param action     Represents the create or update actions
     * @param variations Which contains the {@link Collection} of variations
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Variation> variations) {

        if (Objects.nonNull(variations) && !variations.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final Set<Long> productIds = variations.stream().map(Variation::getProductId)
                            .collect(Collectors.toSet());

                    if (!productManager.exists(orgId, productIds)) {
                        throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
                    }

                    if (action.equals(Action.CREATE)) {

                        for (final Variation variation : variations) {
                            final String variationName = variation.getName();
                            String uniqueName = variationName;
                            final Set<String> existingNames = variationManager.getVariationNameByPartialMatch(orgId,
                                    uniqueName, variation.getProductId());
                            int count = 1;

                            while (existingNames.contains(uniqueName)) {
                                uniqueName = variationName + count;
                                count++;
                            }
                            variation.setName(uniqueName);
                            final Collection<Currency> currencies = variation.getPriceTags().stream().map(PriceTag::getCurrency).toList();

                            if (!baseCurrencyService.getActiveCurrencies(orgId).containsAll(currencies)) {
                                throw ErrorCode.get(ProductErrorCodes.UNSUPPORTED_CURRENCY, "i18n.currency.unsupported");
                            }
                        }
                    }

                    for (final Variation variation : variations) {
                        if (variationManager.isVariationAlreadyExists(orgId, variation.getProductId(), variation.id(), variation.getName())) {
                            throw ErrorCode.get(ProductErrorCodes.VARIATION_ALREADY_EXISTS, "i18n.product.variation.name.already.exists");
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param request Refers the {@link CrmRequest}.
     */
    @Override
    protected final void validateHttpRequest(final CrmRequest request) {
        final SourceSettings sourceSettings = sourceSettingsService.getSourceSettingsBySource(request.orgId(), Product.ENTITY_TYPE);

        if (sourceSettings.isEnabled()) {
            final String config = sourceSettings.getConfig();
            final JsonObject configJson = Objects.isNull(config) ? Json.object() : Json.object(config);

            if (!configJson.optBoolean(VariationFields.IS_PRICE_VARIATIONS_ENABLED.getName())) {
                throw ErrorCode.get(ProductErrorCodes.PRODUCT_VARIATION_IS_DISABLED, "i18n.product.variation.disabled");
            }
        } else {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_IS_DISABLED, "i18n.product.disabled");
        }
    }
}