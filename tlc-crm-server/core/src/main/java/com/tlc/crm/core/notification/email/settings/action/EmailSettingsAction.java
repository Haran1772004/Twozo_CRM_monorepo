package com.tlc.crm.core.notification.email.settings.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.addon.bcc.SmartBccManager;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSignature;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;
import com.tlc.crm.email.notification.email.validator.Group;

import com.tlc.crm.entity.service.FieldService;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Manages the email settings related web actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@WebAction(path = "/email/settings")
public class EmailSettingsAction extends AuthorizationController {

    private static final String CONTACT = "Contact";

    private final EmailSettingsManager emailSettingsManager;
    private final EmailConverter emailConverter;
    private final ModelValidator modelValidator;
    private final SmartBccManager smartBccManager;
    private final EmailSettingsService emailSettingsService;
    private final FieldService fieldService;

    public EmailSettingsAction() {
        emailSettingsManager = EmailSettingsManager.getInstance();
        emailConverter = EmailConverter.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        smartBccManager = SmartBccManager.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());

        switch (type) {
            case "fetchSmartBccAddress" ->
                    crmResponse.put(CoreEmailFields.EMAIL.getName(), smartBccManager.fetchSmartBccAddress(orgId));
            case "retrieveSignature" ->
                    crmResponse.put(CoreEmailFields.EMAIL_SIGNATURE.getName(), emailSettingsService.fetchEmailSignature(orgId, userId));
            case "updateEmailSettings" -> updateEmailSettings(orgId, userId, data, crmResponse);
            case "getEmailLinkingSettings" -> crmResponse.put(CoreEmailFields.SETTINGS.getName(),
                    getEmailLinkingSettings(orgId, userId));
            case "retrieveEmailSettings" -> crmResponse.put(CoreEmailFields.EMAIL_SETTINGS.getName(),
                    retrieveEmailSettings(orgId, userId));
        }
    }

    /**
     * <p>
     * Fetches the email settings.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @return entity of {@link JsonObject}
     */
    private JsonObject retrieveEmailSettings(final long orgId, final long userId) {
        final EmailSettings emailSettings = emailSettingsService.getEmailSettings(orgId, userId);
        final JsonObject jsonObject = Json.object();
        final JsonObject emailLinkerSettingsObject = Json.object();
        final JsonObject emailTrackerSettingsObject = Json.object();
        final JsonObject lifeCycleJson = Json.object();
        final EmailSignature emailSignature = emailSettings.getEmailSignature();
        final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses = emailSettings.getUserBlockedEmailAddresses();
        final EmailLinkerSettings emailLinkerSettings = emailSettings.getEmailLinkerSettings();
        final EmailTrackerSettings emailTrackerSettings = emailSettings.getEmailTrackerSettings();
        final String lifeCycleStageKey = CoreEmailFields.LIFE_CYCLE_STAGE.getName();

        emailLinkerSettingsObject.put(CoreEmailFields.IS_CONTACT_LINKING_ENABLED.getName(), emailLinkerSettings.isContactLinkingEnabled());
        emailLinkerSettingsObject.put(CoreEmailFields.IS_DEAL_LINKING_ENABLED.getName(), emailLinkerSettings.isDealLinkingEnabled());
        emailLinkerSettingsObject.put(CoreEmailFields.IS_CONTACT_CREATION_ENABLED.getName(), emailLinkerSettings.isContactCreationEnabled());

        lifeCycleJson.put(CoreEmailFields.FIELD_ID.getName(), fieldService.groupDefaultFieldsByAliasName(CONTACT, lifeCycleStageKey).get(lifeCycleStageKey).id());
        lifeCycleJson.put(CoreEmailFields.ID.getName(), emailLinkerSettings.getLifecycleStageId());

        emailLinkerSettingsObject.put(CoreEmailFields.LIFE_CYCLE_STAGE.getName(), lifeCycleJson);
        emailTrackerSettingsObject.put(CoreEmailFields.IS_LINK_TRACKER_ENABLED.getName(), emailTrackerSettings.isLinkTrackerEnabled());
        emailTrackerSettingsObject.put(CoreEmailFields.IS_EMAIL_TRACKER_ENABLED.getName(), emailTrackerSettings.isEmailTrackerEnabled());

        if (Objects.nonNull(userBlockedEmailAddresses) && !userBlockedEmailAddresses.isEmpty()) {
            final JsonArray blockedAddressArray = Json.array();

            for (final UserBlockedEmailAddress emailAddress : userBlockedEmailAddresses) {
                blockedAddressArray.put(emailAddress.getEmailId());
            }
            jsonObject.put(CoreEmailFields.BLOCKED_ADDRESS.getName(), blockedAddressArray);
        }

        if (Objects.nonNull(emailSignature)) {
            jsonObject.put(CoreEmailFields.EMAIL_SIGNATURE.getName(), emailSignature.getEmailSignature());
        }
        jsonObject.put(CoreEmailFields.EMAIL_LINKER_SETTINGS.getName(), emailLinkerSettingsObject);
        jsonObject.put(CoreEmailFields.EMAIL_TRACKER_SETTINGS.getName(), emailTrackerSettingsObject);
        jsonObject.put(CoreEmailFields.FORWARD_TO_ADDRESS.getName(), smartBccManager.fetchSmartBccAddress(orgId));

        return jsonObject;
    }

    /**
     * <p>
     * Updates the email settings that is given by the user
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents a user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateEmailSettings(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final EmailSettings emailSettings = emailConverter.convertEmailSettings(orgId, userId, data);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(emailSettings), Group.EmailSettings.class);

        if (errors.isEmpty()) {
            emailSettingsManager.updateEmailSettings(emailSettings);
            crmResponse.put("message",  crmResponse.resolveI18nString("i18n.notifications.mail.email.settings.updated"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Gets the email linking settings
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents a user id
     * @return Entity of {@link JsonObject}
     */
    private JsonObject getEmailLinkingSettings(final long orgId, final long userId) {
        final EmailLinkerSettings emailLinkerSettings = emailSettingsService.getEmailLinkerSettings(orgId, userId);
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CoreEmailFields.IS_CONTACT_LINKING_ENABLED.getName(), emailLinkerSettings.isContactLinkingEnabled());
        jsonObject.put(CoreEmailFields.IS_DEAL_LINKING_ENABLED.getName(), emailLinkerSettings.isContactLinkingEnabled());

        return jsonObject;
    }
}