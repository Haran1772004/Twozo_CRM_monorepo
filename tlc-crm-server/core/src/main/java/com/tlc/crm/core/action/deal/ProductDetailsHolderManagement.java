package com.tlc.crm.core.action.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.ProductDetailsHolderConverter;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.ProductDetailsHolderFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the {@link ProductDetailsHolder} related web actions
 *
 * @author karthickV
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS)
@WebAction(path = "/deal/product/mgmt")
public class ProductDetailsHolderManagement extends AbstractProductAction {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final DealManager dealManager;
    private final ProductManager productManager;
    private final VariationManager variationManager;

    public ProductDetailsHolderManagement() {
        productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        dealManager = DealManager.getInstance();
        productManager = ProductManager.getInstance();
        variationManager = VariationManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(ProductDetailsHolderFields.DATA.getName());
        final String type = requestJson.getString(ProductDetailsHolderFields.TYPE.getName());

        switch (type) {
            case "update", "pUpdate" -> {
                final ProductDetailsHolder productDetailsHolder = convert(request, data);

                update(crmResponse, orgId, request.userId(), productDetailsHolder);
            }
            case "getLinkedProducts" -> crmResponse.put(DealFields.LIST, getProductList(orgId, data));
        }
    }

    /**
     * Gets the {@link JsonArray} of product list with deal
     *
     * @param orgId Represents the organization id
     * @param data  Represents the {@link JsonObject} with request data
     * @return {@link JsonArray} which contains the product data
     */
    private JsonObject getProductList(final Long orgId, final JsonObject data) {
        final Long dealId = data.getLong(DealFields.DEAL_ID);
        final boolean isPartial = data.getBoolean(DealFields.IS_PARTIAL);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
        final ProductDetailsHolder productDetailsHolder = productDetailsHolderManager.getLinkedProductList(orgId, dealId, isPartial);

        return isPartial ? productDetailsHolderConverter.convertPartialDataToJson(productDetailsHolder) :
                productDetailsHolderConverter.convertModelToJson(productDetailsHolder, new ConverterConfig(false, false));
    }

    /**
     * {@inheritDoc}
     */
    public ProductDetailsHolder convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return productDetailsHolderConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Checks the details are valid or not
     * </p>
     *
     * @param orgId                It represents organization id
     * @param productDetailsHolder Collection of {@link ProductDetailsHolder}
     * @return boolean Return true means is valid details
     */
    private boolean isValidDetails(final Long orgId, final ProductDetailsHolder productDetailsHolder) {
        if (Objects.nonNull(productDetailsHolder)) {
            final Long dealId = productDetailsHolder.getDealId();

            if (Objects.isNull(dealId)) {
                throw ErrorCode.get(DealErrorCodes.DEAL_ID_REQUIRED);
            }
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            if (Objects.nonNull(productDetails)) {
                final Set<Long> productIds = productDetails.stream().map(ProductDetail::getProduct)
                        .filter(Objects::nonNull).map(Product::id).filter(Objects::nonNull).collect(Collectors.toSet());
                final Set<Long> variationIds = productDetails.stream().map(ProductDetail::getVariationId)
                        .filter(Objects::nonNull).collect(Collectors.toSet());

                if (!productIds.isEmpty() && !productManager.exists(orgId, productIds)) {
                    throw ErrorCode.get(ProductErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
                }

                if (!variationIds.isEmpty() && !variationManager.exists(orgId, variationIds)) {
                    throw ErrorCode.get(ProductErrorCodes.VARIATION_NOT_FOUND, "i18n.product.variation.not.found");
                }
            }

            if (!dealManager.exists(orgId, dealId)) {
                throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
            }

            if (Objects.isNull(productDetailsHolder.getCurrency())) {
                throw ErrorCode.get(ProductErrorCodes.CURRENCY_NOT_FOUND, "i18n.product.currency.not.found");
            }
        }


        return true;
    }

    /**
     * <p>
     * Updates the given entities
     * </p>
     *
     * @param crmResponse the response to be sent
     * @param orgId represents the organization id
     * @param productDetailsHolder An entity that holds the attributes of product details holder
     */
    private void update(final CrmResponse crmResponse, final Long orgId, final long userId, final ProductDetailsHolder productDetailsHolder) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(List.of(productDetailsHolder), Group.Update.class);
        final String updatedRecordsKey = DealFields.UPDATED_RECORDS;

        if (errors.isEmpty() && isValidDetails(orgId, productDetailsHolder)) {
            final ProcessOutcome processOutcome = productDetailsHolderManager.update(orgId, userId, productDetailsHolder);

            crmResponse.put(updatedRecordsKey, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }
}