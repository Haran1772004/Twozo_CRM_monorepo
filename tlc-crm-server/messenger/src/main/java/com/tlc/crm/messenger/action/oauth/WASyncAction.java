package com.tlc.crm.messenger.action.oauth;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.service.EventRequestService;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmWABusinessAccount;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;

import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.social.WASyncService;

import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Handles the WhatsApp sync related actions
 *
 * @author Maharaja S
 * @version 1.0
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_INTEGRATIONS_FOR_CHAT)
@WebAction(path = "/oauth/whatsapp", method = HttpMethod.POST)
public class WASyncAction extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WASyncAction.class);
    private static final String ACCOUNT_RETRIEVAL_FAILED_I18N_KEY = "i18n.whatsapp.business.account.retrieval.failed";

    private final WABusinessAccountService waBusinessAccountService;
    private final SyncServiceProvider syncServiceProvider;
    private final EventRequestHandler socialEventRequestHandler;
    private final WABusinessAccountManager waBusinessAccountManager;

    public WASyncAction() {
        waBusinessAccountService = WABusinessAccountService.getInstance();
        syncServiceProvider = SyncServiceProvider.getInstance();
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the request to be handled
     * @param crmResponse Represents the response
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Long orgId = crmRequest.orgId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject messageEvent = requestJson.getJsonObject(MessengerFields.MESSAGE_EVENT);
        final String type = messageEvent.getString(MessengerFields.TYPE);

        if (Objects.equals(MessengerFields.WA_EMBEDDED_SIGNUP, type)) {
            final String event = messageEvent.getString(MessengerFields.EVENT);

            if (Objects.equals(MessengerFields.CANCEL, event)) {
                final JsonObject errorData = messageEvent.getJsonObject(MessengerFields.DATA);

                if (errorData.containsKey(MessengerFields.CURRENT_STEP)) {
                    final String currentStep = errorData.getString(MessengerFields.CURRENT_STEP);

                    LOGGER.error("Whatsapp: The embedded sign up flow was abandoned at :{}", currentStep);
                    throw ErrorCode.get(WAErrorCodes.USER_ABANDONED_THE_EMBEDDED_SIGN_UP_FLOW, "i18n.whatsapp.user.abandoned.embedded.signup.flow");
                } else {
                    final String errorMessage = errorData.getString(MessengerFields.ERROR_MESSAGE);

                    LOGGER.error("Whatsapp: Embedded sign up failed. Reason:{}", errorMessage);
                    throw ErrorCode.get(WAErrorCodes.EMBEDDED_SIGN_UP_FAILED, "i18n.whatsapp.embedded.signup.failed");
                }
            } else if (Objects.equals(MessengerFields.FINISH, event)) {
                LOGGER.info("Whatsapp: Embedded sign up flow successfully completed. Org Id:{}, User Id:{}", crmRequest.orgId(), crmRequest.userId());
                final JsonObject syncData = Json.object();
                final String accessCode = requestJson.getString(MessengerFields.ACCESS_CODE);
                final WASyncService waSyncService = syncServiceProvider.getWaSyncService();
                final String accessToken = waSyncService.fetchAccessToken(SyncTokenRequestInstanceProvider.getWATokenReadRequest(accessCode, TokenRequest.TokenType.ACCESS));
                final JsonObject data = messageEvent.getJsonObject(MessengerFields.DATA);

                if (Objects.nonNull(data)) {
                    syncData.put(MessengerFields.BUSINESS_ID, data.getLong(MessengerFields.BUSINESS_ID));
                    syncData.put(MessengerFields.WABA_ID, data.getLong(MessengerFields.WABA_ID));
                    syncData.put(MessengerFields.PHONE_NUMBER_ID, data.getLong(MessengerFields.PHONE_NUMBER_ID));
                    syncData.put(MessengerFields.ACCESS_TOKEN, accessToken);

                    crmResponse.convertIntoAsync();
                    final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final JsonObject jsonObject) {

                            socialEventRequestHandler.handle(() -> {
                                try {
                                    if (jsonObject.containsKey(MessengerFields.ERRORS)) {
                                        final List<String> errors = (List<String>) jsonObject.get(MessengerFields.ERRORS);
                                        final ErrorCode errorCode = ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT, ACCOUNT_RETRIEVAL_FAILED_I18N_KEY);
                                        final String errorDetail = errors.get(NumericalConstant.ZERO);

                                        LoggerUtil.logError(LOGGER, errorCode, "Whatsapp: Failed to retrieve WhatsApp business account due to validation error. OrgId: {}, Message: {}", orgId, errorDetail);
                                        setErrorDescriptor(crmResponse, errorCode, errorDetail, null);
                                    } else {
                                        final Long waBusinessAccountId = jsonObject.getLong(MessengerFields.BUSINESS_ACCOUNT_ID);
                                        final Collection<Long> phoneNumbersIds = (Collection<Long>) jsonObject.get(MessengerFields.PHONE_NUMBER_IDS);
                                        final CrmWABusinessAccount crmWABusinessAccount = new CrmWABusinessAccount();
                                        final Collection<CrmWAPhoneNumber> crmWAPhoneNumbers = new ArrayList<>();

                                        crmWABusinessAccount.setOrgId(orgId);
                                        crmWABusinessAccount.setId(waBusinessAccountId);

                                        for (final Long phoneNumberId : phoneNumbersIds) {
                                            final CrmWAPhoneNumber crmWAPhoneNumber = new CrmWAPhoneNumber();

                                            crmWAPhoneNumber.setId(phoneNumberId);
                                            crmWAPhoneNumber.setState(ChannelState.ENABLED);
                                            crmWAPhoneNumber.setOrgId(orgId);
                                            crmWAPhoneNumbers.add(crmWAPhoneNumber);
                                        }

                                        if (crmWAPhoneNumbers.isEmpty()) {
                                            LOGGER.error("Whatsapp: Business phone number is empty. Org Id:{}", orgId);
                                            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_IS_EMPTY, "i18n.whatsapp.business.phone.numbers.empty");
                                        } else {
                                            crmWABusinessAccount.setCrmWAPhoneNumbers(crmWAPhoneNumbers);
                                            waBusinessAccountManager.storeBusinessAccount(orgId, crmWABusinessAccount);
                                        }
                                    }
                                } catch (final Throwable cause) {
                                    final String errorDetail = cause.getMessage();

                                    LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to retrieve WhatsApp business account, OrgId: {}, Message: {}", orgId, errorDetail);
                                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT, ACCOUNT_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
                                } finally {
                                    crmResponse.send();
                                }
                            });
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            final String errorDetail = cause.getMessage();

                            LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to integrate WhatsApp. OrgId: {}, Message {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT, ACCOUNT_RETRIEVAL_FAILED_I18N_KEY), errorDetail, null);
                            crmResponse.send();
                        }
                    };

                    waBusinessAccountService.fetchAndStoreWABusinessAccount(syncData, eventCallback);
                }
            }
        } else {
            throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_EMBEDDED_SIGNUP, "i18n.whatsapp.unsupported.embedded.signup");
        }
    }
}