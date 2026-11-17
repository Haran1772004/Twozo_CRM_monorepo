package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;

import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.formatter.ActivityJsonFormatter;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.validator.Validator;
import com.tlc.iam.common.models.User;

import java.util.Map;
import java.util.Calendar;
import java.util.Objects;

/**
 * The handler which handles the {@link com.tlc.crm.entity.features.api.activity.model.Activity} related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActivityActionHandler extends AbstractActionHandler implements SourceActionHandler {

    private static final int MAXIMUM_MONTH = 1;

    private final ActivityJsonFormatter activityJsonFormatter;
    private final SourceActivityConverter activityConverter;
    private final SourceActivityManager activityManager;
    private final SourceActivityValidator sourceActivityValidator;

    private ActivityActionHandler() {
        this.activityJsonFormatter = ActivityJsonFormatter.getInstance();
        this.activityConverter = SourceActivityConverter.getInstance();
        this.activityManager = SourceActivityManager.getInstance();
        this.sourceActivityValidator = new SourceActivityValidator();
    }

    private static class Instance {
        private static final ActivityActionHandler INSTANCE = new ActivityActionHandler();
    }

    /**
     * Gets the instance of {@link ActivityActionHandler}.
     *
     * @return {@link ActivityActionHandler} instance.
     */
    public static ActivityActionHandler getInstance() {
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
     * @return
     */
    @Override
    public ActivityHolder create(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject activityHolderJson = activityJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());
        final ActivityHolder activityHolder = activityConverter.convert(new ConverterConfig(orgId, userId, null, false, activityHolderJson), activityHolderJson);

        if (sourceActivityValidator.validate(activityHolder, actionExecutionContext)) {
            final ActivityHolder createdActivityHolder = activityManager.create(activityHolder);

            index(orgId, createdActivityHolder, Action.CREATE);
            return createdActivityHolder;
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
     * @return
     */
    @Override
    public ActivityHolder update(final Long orgId, final Long userId, final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final Long workflowActionId = workflowAction.getId();
        final JsonObject activityHolderJson = activityJsonFormatter.format(orgId, workflowActionId, workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        if (activityManager.exists(orgId, activityHolderJson.getLong(ActionAttributes.ID))) {
            final ActivityHolder activityHolder = activityConverter.convert(new ConverterConfig(orgId, userId, null, true, activityHolderJson), activityHolderJson);

            if (sourceActivityValidator.validate(activityHolder, actionExecutionContext)) {
                final ActivityHolder updatedActivityHolder = activityManager.update(activityHolder);

                index(orgId, updatedActivityHolder, Action.UPDATE);
                return updatedActivityHolder;
            }
        }
        actionExecutionContext.tracker().track(workflowActionId, WorkflowActionExecutionFailureCause.INVALID_DATA);

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
         * @param activityHolder         Refers the {@link ActivityHolder} to be evaluated.
         * @param actionExecutionContext Refers the {@code ActionExecutionContext}.
         * @return true if the entity is valid, otherwise false.
         */
        @Override
        public boolean validate(final ActivityHolder activityHolder, final ActionExecutionContext actionExecutionContext) {
            final Activity activity = activityHolder.getActivity();
            final Long startTime = activity.getStartTime().getTimeStamp();
            final Long endTime = activity.getEndTime().getTimeStamp();
            final Calendar calendar = Calendar.getInstance();
            final CrmUser assignee = activity.getAssignee();
            final Map<Long, User> groupedUsers = actionExecutionContext.groupedUsers();

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
