package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.deal.model.converter.ProductDetailsHolderConverter;

import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.AbstractActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.internal.action.formatter.DealJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.iam.common.models.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles the deal actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class DealActionHandlerV2 extends AbstractActionHandler {

    private final DealJsonFormatterV2 dealJsonFormatter;
    private final DealConverter dealConverter;
    private final DealManager dealManager;
    private final ProductDetailsHolderConverter productDetailsHolderConverter;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final DealValidator dealValidator;

    private DealActionHandlerV2() {
        this.dealJsonFormatter = DealJsonFormatterV2.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productDetailsHolderConverter = ProductDetailsHolderConverter.getInstance();
        this.productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        this.dealValidator = new DealValidator();
    }

    private static class Instance {
        private static final DealActionHandlerV2 INSTANCE = new DealActionHandlerV2();
    }

    /**
     * Gets the instance of {@link DealActionHandlerV2}.
     *
     * @return {@link DealActionHandlerV2} instance.
     */
    static DealActionHandlerV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates the deal.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The deal.
     */
    Deal create(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);
        final Deal deal = dealConverter.convert(new ConverterConfig(orgId, userId, null, false, dealJson), dealJson);

        if (dealValidator.validate(deal, nodeExecutionContext)) {
            final Deal createdDeal = dealManager.create(deal);

            insertSourceEntity(nodeExecutionContext, createdDeal, actionConfiguration);
            nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), createdDeal));
            index(orgId, createdDeal, Action.CREATE);

            return createdDeal;
        } else {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Updates the deal.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The deal.
     */
    Deal update(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);

        if (dealManager.exists(orgId, dealJson.getLong(ActionAttributes.ID))) {
            final Deal deal = dealConverter.convert(new ConverterConfig(orgId, userId, null, true, dealJson), dealJson);

            insertSourceEntity(nodeExecutionContext, dealManager.get(orgId, dealJson.getLong(ActionAttributes.ID)), actionConfiguration);

            if (dealValidator.validate(deal, nodeExecutionContext)) {
                final WorkflowV2 workflow = nodeExecutionContext.workflow();

                loadFieldAuditInfo(orgId, workflow.id(), workflow.getName(), deal);
                final PipelineStage pipelineStage = deal.getPipelineStage();

                if (Objects.nonNull(pipelineStage)) {
                    final String pipelineStageName = PipelineStageManager.getInstance().get(orgId, pipelineStage.id()).getName();

                    if (Objects.nonNull(pipelineStageName)) {
                        deal.setState(Deal.State.getStateByName(pipelineStageName));
                    }
                }
                final Deal updatedDeal = dealManager.update(deal);

                nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), updatedDeal));
                index(orgId, updatedDeal, Action.UPDATE);

                return updatedDeal;
            }

        }
        actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

        return null;
    }

    /**
     * Duplicates the existing deal.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The deal.
     */
    Deal duplicateExistingDeal(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                               final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final JsonObject dealJson = dealJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, actionConfiguration.getTriggerActionSource());

        if (dealManager.exists(orgId, dealJson.getLong(ActionAttributes.ID))) {
            final Deal deal = dealManager.get(orgId, dealJson.getLong(ActionAttributes.ID));

            DefaultFieldValuesLoader.getInstance().load(deal);

            return dealManager.create(deal);
        } else {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Adds the product to the deal.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The product details holder.
     */
    ProductDetailsHolder addProductToDeal(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                                          final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final JsonObject productDetailsJson = dealJsonFormatter.formatAddProductJson(orgId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, actionConfiguration.getTriggerActionSource());
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
         * @param deal                 Refers the {@link Deal}.
         * @param nodeExecutionContext Refers the {@code NodeExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final Deal deal, final NodeExecutionContext nodeExecutionContext) {
            final CrmUser salesOwner = deal.getSalesOwner();
            final Map<Long, User> groupedUsers = nodeExecutionContext.groupedUsers();

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
