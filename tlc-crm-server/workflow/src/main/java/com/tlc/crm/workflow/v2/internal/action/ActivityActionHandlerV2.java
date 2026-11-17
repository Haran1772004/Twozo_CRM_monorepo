package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;

import com.tlc.crm.entity.features.api.activity.model.Activity;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.AbstractActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.internal.action.formatter.ActivityJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import com.tlc.iam.common.models.User;

import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the activity actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class ActivityActionHandlerV2 extends AbstractActionHandler {

    private static final int MAXIMUM_MONTH = 1;

    private final ActivityJsonFormatterV2 activityJsonFormatter;
    private final SourceActivityConverter activityConverter;
    private final SourceActivityManager activityManager;
    private final SourceActivityValidator sourceActivityValidator;

    private ActivityActionHandlerV2() {
        this.activityJsonFormatter = ActivityJsonFormatterV2.getInstance();
        this.activityConverter = SourceActivityConverter.getInstance();
        this.activityManager = SourceActivityManager.getInstance();
        this.sourceActivityValidator = new SourceActivityValidator();
    }

    private static class Instance {
        private static final ActivityActionHandlerV2 INSTANCE = new ActivityActionHandlerV2();
    }

    /**
     * Gets the instance of {@link ActivityActionHandlerV2}.
     *
     * @return {@link ActivityActionHandlerV2} instance.
     */
    static ActivityActionHandlerV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Creates the activity holder.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The activity holder.
     */
    ActivityHolder create(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                          final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject activityHolderJson = activityJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);
        final ActivityHolder activityHolder = activityConverter.convert(new ConverterConfig(orgId, userId, null, false, activityHolderJson), activityHolderJson);

        if (sourceActivityValidator.validate(activityHolder, nodeExecutionContext)) {
            final ActivityHolder createdActivityHolder = activityManager.create(activityHolder);

            insertSourceEntity(nodeExecutionContext, createdActivityHolder, actionConfiguration);
            nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), createdActivityHolder));
            index(orgId, createdActivityHolder, Action.CREATE);

            return createdActivityHolder;
        } else {
            actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

            return null;
        }
    }

    /**
     * Updates the activity holder.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The activity holder.
     */
    ActivityHolder update(final Long orgId, final Long userId, final ActionConfiguration actionConfiguration,
                          final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final Long workflowActionId = actionConfiguration.id();
        final TriggerActionSource triggerActionSource = actionConfiguration.getTriggerActionSource();
        final JsonObject activityHolderJson = activityJsonFormatter.format(orgId, workflowActionId, actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, triggerActionSource);

        if (activityManager.exists(orgId, activityHolderJson.getLong(ActionAttributes.ID))) {
            final ActivityHolder activityHolder = activityConverter.convert(new ConverterConfig(orgId, userId, null, true, activityHolderJson), activityHolderJson);

            insertSourceEntity(nodeExecutionContext, activityManager.get(orgId, activityHolderJson.getLong(ActionAttributes.ID)), actionConfiguration);

            if (sourceActivityValidator.validate(activityHolder, nodeExecutionContext)) {
                final ActivityHolder updatedActivityHolder = activityManager.update(activityHolder);

                nodeExecutionContext.addActionModel(nodeExecutionContext.nodeId(), Pair.of(triggerActionSource.getAliasName(), updatedActivityHolder));
                index(orgId, updatedActivityHolder, Action.UPDATE);

                return updatedActivityHolder;
            }
        }
        actionFailureCauseTracker.track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

        return null;
    }

    /**
     * Validates the {@link ActivityHolder} entity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class SourceActivityValidator implements Validator<ActivityHolder> {

        private SourceActivityValidator() {
        }

        /**
         * {@inheritDoc}
         *
         * @param activityHolder       Refers the {@link ActivityHolder} to be evaluated.
         * @param nodeExecutionContext Refers the {@code NodeExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final ActivityHolder activityHolder, final NodeExecutionContext nodeExecutionContext) {
            final Activity activity = activityHolder.getActivity();
            final Long startTime = activity.getStartTime().getTimeStamp();
            final Long endTime = activity.getEndTime().getTimeStamp();
            final Calendar calendar = Calendar.getInstance();
            final CrmUser assignee = activity.getAssignee();
            final Map<Long, User> groupedUsers = nodeExecutionContext.groupedUsers();

            calendar.setTimeInMillis(startTime);
            calendar.add(Calendar.MONTH, MAXIMUM_MONTH);

            if (Objects.isNull(activity.getType()) && (endTime > calendar.getTimeInMillis() || endTime < startTime)
                    && !isActiveUser(groupedUsers.get(assignee.getIamId()))) {
                return false;
            }

            return true;
        }
    }
}
