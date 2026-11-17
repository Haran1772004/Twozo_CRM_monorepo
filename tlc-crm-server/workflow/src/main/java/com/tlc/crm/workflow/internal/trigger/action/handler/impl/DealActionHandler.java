package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.deal.model.converter.ProductDetailsHolderConverter;

import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.formatter.DealJsonFormatter;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Collection;
import java.util.Optional;

/**
 * The handler which handles the {@link Deal} related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class DealActionHandler extends AbstractActionHandler implements SourceActionHandler {

    private final DealJsonFormatter dealJsonFormatter;
    private final DealConverter dealConverter;
    private final DealManager dealManager;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final DealValidator dealValidator;

    private DealActionHandler() {
        this.dealJsonFormatter = DealJsonFormatter.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        this.productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        this.dealValidator = new DealValidator();
    }

    private static class Instance {
        private static final DealActionHandler INSTANCE = new DealActionHandler();
    }

    /**
     * Gets the instance of {@link DealActionHandler}.
     *
     * @return {@link DealActionHandler} instance.
     */
    public static DealActionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The created {@link Deal}.
     */
    @Override
    public Deal create(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final Deal deal = dealConverter.convert(new ConverterConfig(orgId, userId, null, false, dealJson), dealJson);

        if (dealValidator.validate(deal, actionExecutionContext)) {
            final Deal createdDeal = dealManager.create(deal);

            index(orgId, createdDeal, Action.CREATE);
            return createdDeal;
        } else {
            actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The updated {@link Deal}.
     */
    @Override
    public Deal update(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        if (dealManager.exists(orgId, dealJson.getLong(ActionAttributes.ID))) {
            final Deal deal = dealConverter.convert(new ConverterConfig(orgId, userId, null, true, dealJson), dealJson);

            if (dealValidator.validate(deal, actionExecutionContext)) {
                loadFieldAuditInfo(orgId, workflowActionId, deal);
                final PipelineStage pipelineStage = deal.getPipelineStage();

                if (Objects.nonNull(pipelineStage)) {
                    final String pipelineStageName = PipelineStageManager.getInstance().get(orgId, pipelineStage.id()).getName();

                    if (Objects.nonNull(pipelineStageName)) {
                        deal.setState(Deal.State.getStateByName(pipelineStageName));
                    }
                }
                final Deal updatedDeal = dealManager.update(deal);

                index(orgId, updatedDeal, Action.UPDATE);
                return updatedDeal;
            }

        }
        actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

        return null;
    }

    /**
     * Manages the action of duplicating the existing {@link Deal}.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The duplicating {@link Deal}.
     */
    public Deal duplicateExistingDeal(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        if (dealManager.exists(orgId, dealJson.getLong(ActionAttributes.ID))) {
            final Deal deal = dealManager.get(orgId, dealJson.getLong(ActionAttributes.ID));

            DefaultFieldValuesLoader.getInstance().load(deal);

            return dealManager.create(deal);
        } else {
            actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Manages the action of add product to {@link Deal}.
     *
     * @param orgId                  Refers the organization id.
     * @param userId                 Refers the user id.
     * @param workflowAction         Refers the {@link WorkflowAction} that encapsulates specific details about the action
     *                               utilized in processing the associated entity.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The {@link ProductDetailsHolder}.
     */
    public ProductDetailsHolder addProductToDeal(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final JsonObject productDetailsJson = dealJsonFormatter.formatAddProductJson(orgId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final ProductDetailsHolder existingProductDetailsHolder = productDetailsHolderManager.getLinkedProductList(orgId, productDetailsJson.getLong(ActionAttributes.DEAL_ID), false);
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
        }

        return productDetailsHolder;
    }

    /**
     * Validates the {@link Deal} entity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class DealValidator implements Validator<Deal> {

        private final PipelineStageManager pipelineStageManager;

        private DealValidator() {
            this.pipelineStageManager = PipelineStageManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param deal                   Refers the {@link Deal}.
         * @param actionExecutionContext Refers the {@code ActionExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Deal deal, final ActionExecutionContext actionExecutionContext) {
            final CrmUser salesOwner = deal.getSalesOwner();
            final Map<Long, User> groupedUsers = actionExecutionContext.groupedUsers();

            return Objects.nonNull(deal.getTitle())
                    && validatePipelineAndStage(deal)
                    && Objects.nonNull(deal.getPrimaryContact())
                    && isActiveUser(groupedUsers.get(salesOwner.getIamId()));
        }

        /**
         * <p>
         * Validates pipeline and pipeline stage of the provided {@link Deal}
         * </p>
         *
         * @param deal Represents the deal to be validated
         * @return true if the deal contains valid pipeline, false otherwise
         */
        private boolean validatePipelineAndStage(final Deal deal) {
            final Pipeline pipeline = deal.getPipeline();
            final PipelineStage pipelineStage = deal.getPipelineStage();

            if (Objects.nonNull(pipeline) && Objects.nonNull(pipelineStage)) {
                final Long pipelineId = pipeline.id();
                final Collection<Long> pipelineStageIds = pipelineStageManager.getPipelineStagesIds(deal.orgId(),
                        List.of(pipelineId)).get(pipelineId);

                return pipelineStageIds.contains(pipelineStage.id());
            }

            return false;
        }
    }
}
