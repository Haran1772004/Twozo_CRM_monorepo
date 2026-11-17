package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.SourceActivityConverter;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;
import com.tlc.crm.zapier.internal.action.formatter.ActivityZapActionJsonFormatter;

import com.tlc.validator.type.Group;

import java.util.Collection;

/**
 * Implementation of {@link AbstractSourceZapActionDispatcher} for handling
 * Zapier actions related to activity entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActivityZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final ActivityZapActionJsonFormatter activityZapActionJsonFormatter;
    private final SourceActivityManager sourceActivityManager;
    private final SourceActivityConverter sourceActivityConverter;

    private ActivityZapActionDispatcher() {
        this.activityZapActionJsonFormatter = ActivityZapActionJsonFormatter.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.sourceActivityConverter = SourceActivityConverter.getInstance();
    }

    private static final class Instance {
        private static final ActivityZapActionDispatcher ZAP_ACTION_DISPATCHER = new ActivityZapActionDispatcher();
    }

    public static ActivityZapActionDispatcher getInstance() {
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
            case CREATE_ACTIVITY -> create(zapActionRequestContext);
            case UPDATE_ACTIVITY -> update(zapActionRequestContext);
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
        final JsonObject activityJson = activityZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());
        final ActivityHolder activityHolder = sourceActivityConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, false, activityJson), activityJson);
        final Collection<String> errors = validate(activityHolder, Group.Create.class);

        if (errors.isEmpty()) {
            try {
                sourceActivityManager.create(activityHolder);
                index(orgId, activityHolder, Action.CREATE);

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.activity.create.success"));
                response.send();
            } catch (Throwable throwable) {
                LOGGER.error("Zapier Action Execution: Failed to execute the activity create action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.activity.create.failed"));
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
        final JsonObject activityJson = activityZapActionJsonFormatter.format(orgId, zapActionRequestContext.getZapAction(), zapActionRequestContext.getJsonObject());

        if (sourceActivityManager.exists(orgId, activityJson.getLong(ZapAttributes.ID))) {
            final ActivityHolder activityHolder = sourceActivityConverter.convert(new ConverterConfig(orgId, zapActionRequestContext.getUserId(), null, true, activityJson), activityJson);
            final Collection<String> errors = validate(activityHolder, Group.Update.class);

            if (errors.isEmpty()) {
                try {
                    sourceActivityManager.update(activityHolder);
                    index(orgId, activityHolder, Action.UPDATE);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.activity.update.success"));
                    response.send();
                } catch (Throwable throwable) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the activity update action. OrgId: {}, Reason: {}", orgId, throwable.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.activity.update.failed"));
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
}
