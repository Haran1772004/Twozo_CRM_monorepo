package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.deal.model.converter.ProductDetailsHolderConverter;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.formatter.DealZapActionJsonFormatter;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.validator.type.Group;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link DealZapActionDispatcher} for handling
 * Zapier actions related to deal entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class DealZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final DealConverter dealConverter;
    private final DealManager dealManager;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final DealZapActionJsonFormatter dealZapActionJsonFormatter;

    private DealZapActionDispatcher() {
        this.dealConverter = DealConverter.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        this.productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        this.dealZapActionJsonFormatter = DealZapActionJsonFormatter.getInstance();
    }

    private static final class Instance {
        private static final DealZapActionDispatcher ZAP_ACTION_DISPATCHER = new DealZapActionDispatcher();
    }

    public static DealZapActionDispatcher getInstance() {
        return Instance.ZAP_ACTION_DISPATCHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapActionRequestContext The context containing the request, response, and action details.
     */
    @Override
    public void dispatch(final ZapActionRequestContext zapActionRequestContext) {
        switch (zapActionRequestContext.getZapAction()) {
            case CREATE_DEAL -> create(zapActionRequestContext);
            case UPDATE_DEAL -> update(zapActionRequestContext);
            case ADD_PRODUCT_TO_DEAL -> addProductToDeal(zapActionRequestContext);
        }
    }

    /**
     * Handles the creation logic for a Zapier action using the provided request context.
     *
     * @param zapActionRequestContext The context containing request data, metadata, and utilities needed to perform the action.
     */
    private void create(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final CrmResponse response = zapActionRequestContext.getResponse();
        final JsonObject dealJson = dealZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
        final Deal deal = dealConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, false, dealJson), dealJson);
        final Collection<String> errors = validate(deal, Group.Create.class);

        if (errors.isEmpty()) {
            try {
                dealManager.create(deal);
                index(orgId, deal, Action.CREATE);

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.deal.create.success"));
                response.send();
            } catch (Throwable throwable) {
                LOGGER.error("Zapier Action Execution: Failed to execute the deal create action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.deal.create.failed"));
                response.setErrorCode(ErrorCode.get(ZapierErrorCodes.CREATE_ACTION_FAILED));
                response.send();
            }
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.DATA_VALIDATION_FAILED));
            response.put(ZapAttributes.ERRORS, errors);
            response.send();
        }
    }

    /**
     * Performs an update operation for a Zapier action using the provided request context.
     *
     * @param zapActionRequestContext The context containing data and parameters required to update the target entity.
     */
    private void update(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final CrmResponse response = zapActionRequestContext.getResponse();
        final JsonObject dealJson = dealZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());

        if (dealManager.exists(orgId, dealJson.getLong(ZapAttributes.ID))) {
            final Deal deal = dealConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, true, dealJson), dealJson);
            final Collection<String> errors = validate(deal, Group.Update.class);

            if (errors.isEmpty()) {
                try {
                    setDealState(orgId, deal);
                    dealManager.update(deal);
                    index(orgId, deal, Action.UPDATE);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.deal.update.success"));
                    response.send();
                } catch (Throwable throwable) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the deal update action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.deal.update.failed"));
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
                    response.send();
                }
            } else {
                response.setErrorCode(ErrorCode.get(ZapierErrorCodes.DATA_VALIDATION_FAILED));
                response.put(ZapAttributes.ERRORS, errors);
                response.send();
            }
        } else {
            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.ENTITY_NOT_FOUND));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.invalid"));
            response.send();
        }
    }

    /**
     * Sets the state of the given deal for the specified organization.
     *
     * @param orgId The ID of the organization to which the deal belongs.
     * @param deal  The {@link Deal} object whose state is to be updated.
     */
    private void setDealState(final Long orgId, final Deal deal) {
        final PipelineStage pipelineStage = deal.getPipelineStage();

        if (Objects.nonNull(pipelineStage)) {
            final String pipelineStageName = PipelineStageManager.getInstance().get(orgId, pipelineStage.id()).getName();

            if (Objects.nonNull(pipelineStageName)) {
                deal.setState(Deal.State.getStateByName(pipelineStageName));
            }
            final State state = deal.getState();

            switch (state) {
                case WON -> {
                    if (Objects.isNull(deal.getWonReason())) {
                        fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.WON_REASON)
                                .ifPresent(fieldInfo -> optionService.get(orgId, fieldInfo).stream().findFirst().ifPresent(deal::setWonReason));
                    }
                }
                case LOST -> {
                    if (Objects.isNull(deal.getLostReason())) {
                        fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.LOST_REASON)
                                .ifPresent(fieldInfo -> optionService.get(orgId, fieldInfo).stream().findFirst().ifPresent(deal::setLostReason));
                    }
                }
            }
        }
    }

    /**
     * Adds a product to an existing deal using the provided Zapier action context.
     *
     * @param zapActionRequestContext The context containing deal and product data required to perform the operation.
     */
    private void addProductToDeal(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final Long userId = zapActionRequestContext.getUserId();
        final CrmResponse response = zapActionRequestContext.getResponse();

        try {
            final JsonObject productDetailsJson = dealZapActionJsonFormatter.formatAddProductJson(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
            final ProductDetailsHolder existingProductDetailsHolder = productDetailsHolderManager.getLinkedProductList(orgId, productDetailsJson.getLong(ZapAttributes.DEAL_ID), false);
            final ProductDetailsHolder productDetailsHolder = productDetailsHolderConverter.convert(new ConverterConfig(orgId, userId, null, false, productDetailsJson), productDetailsJson);
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();
            final Optional<ProductDetail> optionalProductDetail = Objects.isNull(productDetails) ? Optional.empty() : productDetails.stream().findFirst();

            if (optionalProductDetail.isPresent()) {
                final ProductDetail productDetail = optionalProductDetail.get();
                final Long productId = productDetail.getProduct().id();
                final Collection<ProductDetail> existingProductDetails = Objects.nonNull(existingProductDetailsHolder)
                        ? existingProductDetailsHolder.getProductDetails() : null;

                if (Objects.nonNull(existingProductDetails) && !existingProductDetails.isEmpty()) {
                    existingProductDetailsHolder.getProductDetails().removeIf(linkedProductDetail -> Objects.equals(productId,
                            linkedProductDetail.getProduct().id()));
                    existingProductDetailsHolder.addProductDetail(productDetail);

                    productDetailsHolderManager.update(orgId, userId, existingProductDetailsHolder);
                } else {
                    productDetailsHolderManager.update(orgId, userId, productDetailsHolder);
                }

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.add.product.deal.success"));
            }
        } catch (Throwable throwable) {
            LOGGER.error("Zapier Action Execution: Failed to execute the add product to deal action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.add.product.deal.failed"));
        } finally {
            response.send();
        }
    }
}
