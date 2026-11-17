package com.tlc.crm.notifications.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.notification.app.model.UserNotificationSettings;
import com.tlc.crm.notifications.api.model.converter.UserNotificationSettingsConverter;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link UserNotificationSettings} related web actions
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@WebAction(path = "/notification/settings")
public class NotificationSettingsAction extends AuthorizationController {

    private final ModelValidator modelValidator;
    private final UserNotificationSettingsManager userNotificationSettingsManager;
    private final UserNotificationSettingsConverter userNotificationSettingsConverter;

    public NotificationSettingsAction() {
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
        this.userNotificationSettingsManager = UserNotificationSettingsManager.getInstance();
        this.userNotificationSettingsConverter = UserNotificationSettingsConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the received request
     * @param crmResponse Represents the response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final String dataKey = NotificationFields.DATA.getName();
        final JsonObject data = requestJson.getJsonObject(dataKey);
        final String type = requestJson.getString(NotificationFields.TYPE.getName());

        switch (type) {
            case "get" -> crmResponse.put(dataKey, userNotificationSettingsConverter
                    .convertNotificationSettingsToJson(userNotificationSettingsManager.retrieveUserNotificationSettings(orgId, userId)));
            case "reset" -> resetUserNotificationSettings(orgId, userId, crmResponse);
            case "update" -> updateNotificationSettings(orgId, userId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Updates the user notification settings.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateNotificationSettings(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final UserNotificationSettings userNotificationSettings = userNotificationSettingsConverter.convert(orgId, userId, data);
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
        final Set<ErrorDescriptor> errors = modelValidator.validate(userNotificationSettings, Group.Create.class).stream()
                .map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                        violation.getPropertyPath().toString())).collect(Collectors.toSet());

        if (errors.isEmpty()) {
            userNotificationSettingsManager.updateUserNotificationSettings(userNotificationSettings);
            crmResponse.put("message", crmResponse.resolveI18nString("i18n.notifications.app.settings.update.success"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.NOTIFICATION_SETTINGS_UPDATE_FAILED, "i18n.notifications.app.settings.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Resets the user notification settings to its default value.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void resetUserNotificationSettings(final long orgId, final long userId, final CrmResponse crmResponse) {

        try {
            userNotificationSettingsManager.resetUserNotificationSettings(orgId, userId, new UserNotificationSettings());
            crmResponse.put("message", crmResponse.resolveI18nString("i18n.notifications.app.settings.reset.success"));
        } catch (Exception exception) {
            crmResponse.setErrorCode(ErrorCode.get(NotificationErrorCodes.NOTIFICATION_SETTINGS_RESET_FAILED, "i18n.notifications.app.settings.reset.failed", exception));
        }
    }
}