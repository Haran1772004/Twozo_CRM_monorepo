package com.tlc.crm.notifications.action.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.notifications.api.call.manager.CallManager;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.MCubeConfig;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.api.call.model.TeleCMIConfig;
import com.tlc.crm.notifications.api.call.model.TelephoneConfig;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The {@link CallManagement} class handles all operations related to call configuration and telephony integration
 *
 * @author kishore J
 * @version 1.0
 */
@WebAction(path = "/call/mgmt")
public class CallManagement extends CrmSecureAction {

    private final CallManager callManager;
    private final VoiceCallService voiceCallService;
    private final PhoneNumberManager phoneNumberManager;
    private final PhoneSettingsManager phoneSettingsManager;

    public CallManagement() {
        callManager = CallManager.getInstance();
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
        phoneNumberManager = PhoneNumberManager.getInstance();
        phoneSettingsManager = PhoneSettingsManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());

        switch (type) {
            case TelephoneFields.GET_CALL_INITIALIZATION_CONFIG ->
                    getCallInitializationConfig(crmRequest.orgId(), crmRequest.userId(), crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Fetches the installed app configs for a given organization and user
     * </p>
     *
     * @param orgId       Represents an org id
     * @param userId      Represents an user id
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getCallInitializationConfig(final long orgId, final long userId, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final TelephoneProvider defaultProvider = TelephoneProvider.getDefaultProvider();
        final JsonObject defaultProviderConfig = Json.object();
        final String defaultProviderName = defaultProvider.name().toLowerCase();

        defaultProviderConfig.put(TelephoneFields.NAME, defaultProviderName);
        defaultProviderConfig.put(TelephoneFields.DISPLAY_NAME, crmResponse.resolveI18nString(defaultProvider.getDisplayName()));
        defaultProviderConfig.put(TelephoneFields.CAN_MAKE_CALL, phoneNumberManager.hasPurchasedNumber(orgId));
        defaultProviderConfig.put(TelephoneFields.CONFIG, Json.object().put(TelephoneFields.TOKEN,
                voiceCallService.generateToken(TokenIdentityBuilder.getIdentity(orgId, userId)).token()).put(TelephoneFields.HAS_PURCHASED_NUMBER,
                phoneNumberManager.hasPurchasedNumber(orgId)));

        crmResponse.put(defaultProviderName, defaultProviderConfig);
        crmResponse.put(TelephoneFields.NATIVE, true);

        callManager.getTelephonyConfigs(orgId, userId).stream().findFirst().ifPresent(telephonyConfig -> {
            final String telephoneProviderName = telephonyConfig.getName();

            crmResponse.put(TelephoneFields.NATIVE, false);
            crmResponse.put(telephoneProviderName, telephonyConfig.getConfigJson(crmRequest.locale()));
        });

        loadPhoneSettingsConfig(orgId, userId, crmResponse);
    }

    /**
     * <p>
     * Loads the phone settings into the call initialization config
     * </p>
     *
     * @param orgId       Represents the org id
     * @param userId      Represents the user id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void loadPhoneSettingsConfig(final Long orgId, final long userId, final CrmResponse crmResponse) {
        final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
        final PhoneSettings.CallingMethod preferredCallingMethod = phoneSettings.getPreferredCallingMethod();
        final Collection<PhoneSettings.CallingService> callingServices = phoneSettings.getCallingServices();

        crmResponse.put(TelephoneFields.CALLING_METHOD, preferredCallingMethod.getId());
        crmResponse.put(TelephoneFields.SHOW_CALLING_METHOD, Objects.equals(PhoneSettings.CallingMethod.ASK_EVERY_TIME, preferredCallingMethod));

        if (phoneSettings.isAnyOrgBasedCallingServiceExist()) {
            loadOrgBasedCallingServices(orgId, userId, callingServices, crmResponse);
        } else {
            loadDefaultCallingService(orgId, userId, callingServices, crmResponse);
        }
    }

    /**
     * <p>
     * Loads the default calling service when no org-based service exists
     * </p>
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param callingServices Represents the calling services
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    private void loadDefaultCallingService(final long orgId, final long userId, final Collection<PhoneSettings.CallingService> callingServices, final CrmResponse crmResponse) {
        for (final PhoneSettings.CallingService callingService : callingServices) {
            if (Objects.equals(TelephoneFields.ASK_EVERY_TIME, callingService.name())) {
                continue;
            }

            if (isCallingServiceAvailable(orgId, userId, callingService)) {
                crmResponse.put(TelephoneFields.PREFERRED_CALLING_SERVICE, callingService.getServiceName());
                crmResponse.put(TelephoneFields.SHOW_CALLING_SERVICE, false);
                return;
            }
        }

        crmResponse.put(TelephoneFields.PREFERRED_CALLING_SERVICE, resolveFallbackCallingService(orgId, userId));
        crmResponse.put(TelephoneFields.SHOW_CALLING_SERVICE, false);
    }

    /**
     * Returns the fallback calling service based on the installed telephony
     * provider, or the default provider if none is available.
     *
     * @param orgId Represents the org id
     * @param userId Represents the user id
     * @return the fallback calling service name
     */
    private String resolveFallbackCallingService(final long orgId, final long userId) {
        return callManager.getTelephonyConfigs(orgId, userId).stream()
                .findFirst()
                .map(TelephoneConfig::getName)
                .orElseGet(() -> TelephoneProvider.getDefaultProvider().name().toLowerCase());
    }

    /**
     * <p>
     * Loads org-based calling services configuration
     * </p>
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param callingServices Represents the calling services
     * @param crmResponse     Represents the {@link CrmResponse}
     */
    private void loadOrgBasedCallingServices(final long orgId, final long userId, final Collection<PhoneSettings.CallingService> callingServices, final CrmResponse crmResponse) {
        final Optional<PhoneSettings.CallingService> optionalCallingService = callingServices.stream()
                .filter(PhoneSettings.CallingService::isSelected).findFirst();

        if (optionalCallingService.isEmpty()) {
            loadDefaultCallingService(orgId, userId, callingServices, crmResponse);
            return;
        }
        final PhoneSettings.CallingService selectedCallingService = optionalCallingService.get();

        if (Objects.equals(TelephoneFields.ASK_EVERY_TIME, selectedCallingService.name())) {
            final JsonArray callingServiceArray = Json.array();
            PhoneSettings.CallingService availableService = null;

            for (final PhoneSettings.CallingService callingService : callingServices) {
                if (Objects.equals(TelephoneFields.ASK_EVERY_TIME, callingService.name())) {
                    continue;
                }

                if (!isCallingServiceAvailable(orgId, userId, callingService)) {
                    continue;
                }
                final JsonObject callingServiceJson = Json.object()
                        .put(TelephoneFields.ID, callingService.id())
                        .put(TelephoneFields.NAME, callingService.getServiceName())
                        .put(TelephoneFields.DISPLAY_NAME, crmResponse.resolveI18nPlaceholder(callingService.displayName()));

                callingServiceArray.put(callingServiceJson);
                availableService = callingService;
            }

            if (callingServiceArray.size() <= 1) {
                crmResponse.put(TelephoneFields.PREFERRED_CALLING_SERVICE,Objects.isNull(availableService) ? resolveFallbackCallingService(orgId, userId) : availableService.getServiceName());
                crmResponse.put(TelephoneFields.SHOW_CALLING_SERVICE, false);

                if (Objects.nonNull(availableService)) {
                    final PhoneSettings.CallingMethod resolvedCallingMethod = Objects.equals(TelephoneFields.TWOZO_DIALLER, availableService.name())
                            ? PhoneSettings.CallingMethod.WEB : PhoneSettings.CallingMethod.MOBILE;

                    crmResponse.put(TelephoneFields.CALLING_METHOD, resolvedCallingMethod.getId());
                    crmResponse.put(TelephoneFields.SHOW_CALLING_METHOD, false);
                }
            } else {
                crmResponse.put(TelephoneFields.SHOW_CALLING_SERVICE, true);
                crmResponse.put(TelephoneFields.CALLING_SERVICE, callingServiceArray);
            }
        } else {
            if (isCallingServiceAvailable(orgId, userId, selectedCallingService)) {
                crmResponse.put(TelephoneFields.PREFERRED_CALLING_SERVICE, selectedCallingService.getServiceName());
            } else {
                crmResponse.put(TelephoneFields.PREFERRED_CALLING_SERVICE, resolveFallbackCallingService(orgId, userId));
            }
            crmResponse.put(TelephoneFields.SHOW_CALLING_SERVICE, false);
        }
    }

    /**
     * <p>
     * Checks if a calling service is available for the given organization and user.
     * </p>
     *
     * @param orgId          Represents the org id
     * @param userId         Represents the user id
     * @param callingService Represents the calling service
     * @return true if the calling service is available, false otherwise
     */
    private boolean isCallingServiceAvailable(final long orgId, final long userId, final PhoneSettings.CallingService callingService) {
        final String serviceName = callingService.name();

        if (Objects.equals(TelephoneFields.TWOZO_DIALLER, serviceName)) {
            return phoneNumberManager.hasPurchasedNumber(orgId);
        }

        return callManager.getTelephonyConfigs(orgId, userId).stream()
                .filter(config -> Objects.equals(config.getName(), callingService.appName()))
                .findFirst()
                .map(config -> {
                    if (config instanceof TeleCMIConfig teleCMIConfig) {
                        return teleCMIConfig.canMakeCall();
                    } else if (config instanceof MCubeConfig mCubeConfig) {
                        return mCubeConfig.canMakeCall();
                    }
                    return false;
                })
                .orElse(false);
    }
}