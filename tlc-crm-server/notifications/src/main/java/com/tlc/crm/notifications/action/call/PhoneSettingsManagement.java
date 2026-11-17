package com.tlc.crm.notifications.action.call;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;

import com.tlc.crm.notifications.marketplace.api.manager.GeneralTelephoneActionManager;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Manages phone settings related web actions
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/call/settings/mgmt")
public class PhoneSettingsManagement extends CrmSecureAction {

    private final PhoneSettingsManager phoneSettingsManager;
    private final ModelValidator modelValidator;
    private final GeneralTelephoneActionManager generalTelephoneActionManager;
    private final PhoneNumberManager phoneNumberManager;
    private final LifeCycleStageManager lifeCycleStageManager;

    public PhoneSettingsManagement() {
        phoneSettingsManager = PhoneSettingsManager.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        generalTelephoneActionManager = GeneralTelephoneActionManager.getInstance();
        phoneNumberManager = PhoneNumberManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the request to be handled
     * @param crmResponse Represents the response
     */
    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());

        switch (type) {
            case "get" -> loadPhoneSettings(orgId, crmResponse);
            case "update" -> update(orgId, userId, data, crmResponse);
        }
    }

    /**
     * <p>
     * Updates the phone settings
     * </p>
     *
     * @param orgId       Represents the org id
     * @param userId      Represents the user id
     * @param data        Represents the data to be updates
     * @param crmResponse Represents the response
     */
    private void update(final Long orgId, final Long userId, final JsonObject data, final CrmResponse crmResponse) {
        final PhoneSettings phoneSettings = new PhoneSettings(orgId);
        final PhoneSettings.CallingService callingService = new PhoneSettings.CallingService(data.getLong(TelephoneFields.CALLING_SERVICE), orgId);
        final JsonObject phoneLink = data.getJsonObject(TelephoneFields.PHONE_LINK);
        final boolean isAutoCreateContactEnabled = phoneLink.getBoolean(TelephoneFields.IS_AUTO_CREATE_CONTACT_ENABLED);

        if (phoneSettingsManager.exists(callingService)) {
            phoneSettings.setCallingServices(List.of(callingService));
            phoneSettings.setPreferredCallingMethod(PhoneSettings.CallingMethod.get(data.getShort(TelephoneFields.CALLING_METHOD)));
            phoneSettings.setLifeCycleStageId(phoneLink.getJsonObject(TelephoneFields.LIFE_CYCLE_STAGE).getLong(TelephoneFields.VALUE));

            phoneSettings.setAutoCreateContactEnabled(isAutoCreateContactEnabled);

            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(phoneSettings), Group.Update.class);

            if (errors.isEmpty()) {
                phoneSettingsManager.update(userId, phoneSettings);
            } else {
                final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.FAILED_TO_UPDATE_PHONE_SETTINGS, "i18n.notifications.call.settings.update.failed");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
            }
        } else {
            throw ErrorCode.get(NotificationErrorCodes.INVALID_CALLING_SERVICE, "i18n.notifications.call.invalid.calling.service");
        }
    }

    /**
     * <p>
     * Loads the phone settings into the response
     * </p>
     *
     * @param orgId       Represents the org id
     * @param crmResponse Represents the response
     */
    private void loadPhoneSettings(final Long orgId, final CrmResponse crmResponse) {
        final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
        final JsonArray callingServicesArray = Json.array();
        final boolean anyOrgBasedServiceExist = phoneSettings.isAnyOrgBasedCallingServiceExist();
        final boolean anyAppInstalled = generalTelephoneActionManager.anyAppInstalled(orgId);
        final boolean hasPurchasedNumber = phoneNumberManager.hasPurchasedNumber(orgId);
        final boolean hasMultipleProviders = hasPurchasedNumber && anyAppInstalled;
        final Collection<PhoneSettings.CallingService> visibleCallingServices = new ArrayList<>();

        for (final PhoneSettings.CallingService callingService : phoneSettings.getCallingServices()) {
            final String serviceName = callingService.name();

            if (Objects.equals(TelephoneFields.ASK_EVERY_TIME, serviceName) && (!anyOrgBasedServiceExist || !hasMultipleProviders)) {
                continue;
            }

            if (Objects.equals(TelephoneFields.TWOZO_DIALLER, serviceName) && !hasPurchasedNumber) {
                continue;
            }

            if (Objects.equals(TelephoneFields.TELEPHONY_PROVIDER, serviceName) && !anyAppInstalled) {
                continue;
            }
            visibleCallingServices.add(callingService);
        }

        final boolean hasValidVisibleSelection = visibleCallingServices.stream().anyMatch(PhoneSettings.CallingService::isSelected);

        for (final PhoneSettings.CallingService callingService : visibleCallingServices) {
            final JsonObject callingServiceJson = Json.object();
            final boolean isSelected = hasValidVisibleSelection ? callingService.isSelected() : isServiceSelected(callingService, anyAppInstalled, hasPurchasedNumber);

            callingServiceJson.put(TelephoneFields.ID, callingService.id())
                    .put(TelephoneFields.DISPLAY_NAME, crmResponse.resolveI18nPlaceholder(callingService.displayName()))
                    .put(TelephoneFields.IS_SELECTED, isSelected)
                    .put(TelephoneFields.NAME, callingService.getServiceName());

            callingServicesArray.put(callingServiceJson);
        }
        final boolean hasNoProvider = !anyAppInstalled && !hasPurchasedNumber;
        final boolean singleCallingServiceAvailable = Objects.equals(1, visibleCallingServices.size());
        final PhoneSettings.CallingMethod resolvedCallingMethod = singleCallingServiceAvailable ? resolveCallingMethod(visibleCallingServices.iterator().next())
                : phoneSettings.getPreferredCallingMethod();
        final JsonObject phoneLink = Json.object();
        final JsonObject lifeCycleStageJson = Json.object();
        final LifeCycleStage defaultLifeCycleStage = lifeCycleStageManager.getDefaultStage(orgId);
        final boolean isAutoCreateEnabled = phoneSettings.isAutoCreateContactEnabled();
        final Long lifeCycleStageId = phoneSettings.getLifeCycleStageId();
        final LifeCycleStage currentlifeCycleStage = Objects.nonNull(lifeCycleStageId)
                ? lifeCycleStageManager.get(orgId, lifeCycleStageId) : defaultLifeCycleStage;
        final LifeCycleStage lifeCycleStage = currentlifeCycleStage.isActive() ? currentlifeCycleStage : defaultLifeCycleStage;

        lifeCycleStageJson.put(TelephoneFields.VALUE, lifeCycleStage.id());
        lifeCycleStageJson.put(TelephoneFields.NAME, lifeCycleStage.getName());

        phoneLink.put(TelephoneFields.LIFE_CYCLE_STAGE, lifeCycleStageJson);
        phoneLink.put(TelephoneFields.IS_AUTO_CREATE_CONTACT_ENABLED, isAutoCreateEnabled);
        crmResponse.put(TelephoneFields.CALLING_SERVICE, callingServicesArray);
        crmResponse.put(TelephoneFields.CALLING_METHOD, resolvedCallingMethod.getId());
        crmResponse.put(TelephoneFields.SHOW_CALLING_METHOD, !singleCallingServiceAvailable);
        crmResponse.put(TelephoneFields.HAS_NO_PROVIDER, hasNoProvider);
        crmResponse.put(TelephoneFields.PHONE_LINK, phoneLink);
    }

    /**
     * <p>
     * Determines if a calling service should be marked as selected based on the priority logic:
     * </p>
     *
     * @param callingService       The calling service to check
     * @param hasTelephonyProvider Whether a telephony provider is installed
     * @param hasPurchasedNumber   Whether a Twilio phone number has been purchased
     * @return true if the service should be selected, false otherwise
     */
    private boolean isServiceSelected(final PhoneSettings.CallingService callingService,
                                       final boolean hasTelephonyProvider, final boolean hasPurchasedNumber) {
        final String serviceName = callingService.name();

        if (hasTelephonyProvider && Objects.nonNull(callingService.appId())) {
            return true;
        }

        if (!hasTelephonyProvider && hasPurchasedNumber && Objects.equals(TelephoneFields.TWOZO_DIALLER, serviceName)) {
            return true;
        }
        return !hasTelephonyProvider && !hasPurchasedNumber && Objects.equals(TelephoneFields.ASK_EVERY_TIME, serviceName);
    }

    /**
     * Resolves the calling method when only one calling service is available.
     *
     * @param callingService The only available {@link PhoneSettings.CallingService}
     * @return The applicable {@link PhoneSettings.CallingMethod}
     */
    private PhoneSettings.CallingMethod resolveCallingMethod(final PhoneSettings.CallingService callingService) {
        return Objects.equals(TelephoneFields.TWOZO_DIALLER, callingService.name())
                ? PhoneSettings.CallingMethod.WEB
                : PhoneSettings.CallingMethod.MOBILE;
    }
}