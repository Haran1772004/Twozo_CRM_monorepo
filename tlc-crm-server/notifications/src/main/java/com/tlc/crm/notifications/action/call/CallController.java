package com.tlc.crm.notifications.action.call;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.model.call.CallRoutingDetails;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.call.Parameters;
import com.tlc.notification.model.call.Recipient;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.web.WebAction;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Controls the call related action like forward a call and record a call.
 *
 * @author Sathish
 * @version 1.1
 */
@WebAction(path = "/call/control/mgmt")
public class CallController extends AuthorizationController {

    private final VoiceCallService voiceCallService;

    public CallController() {
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The CRM request object containing request data.
     * @param crmResponse The CRM response object for sending responses.
     * @throws Exception if an error occurs during request processing.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "forward" -> {
                final String transferToKey = NotificationFields.TRANSFER_TO.getName();
                final String isRingBackEnabledKey = NotificationFields.IS_RING_BACK_ENABLED.getName();
                final String callId = data.getString(NotificationFields.CALL_SID.getName());
                final String parentCallId = voiceCallService.getParentCallId(callId);
                final JsonObject caller = data.getJsonObject(NotificationFields.CALLER.getName());
                final JsonObject transferTo = data.getJsonObject(transferToKey);
                final boolean isRingBackEnabled = data.optBoolean(isRingBackEnabledKey);
                final long userToBeTransferred = transferTo.getLong(NotificationFields.USER_ID.getName());
                final String displayName = transferTo.getString(NotificationFields.DISPLAY_NAME.getName());
                final String receiver = TokenIdentityBuilder.getIdentity(orgId, userToBeTransferred);
                final CallRoutingDetails callRoutingDetails = new CallRoutingDetails();
                final MobileNumber callerNumber = new MobileNumber(caller.getString(NotificationFields.NUMBER.getName()));
                final Parameters parameters = new Parameters();
                final Locale locale = request.locale();

                parameters.addParam(NotificationFields.CALL_TYPE.getName(), CallType.INCOMING.getName());
                parameters.addParam(NotificationFields.TRANSFERRED_BY.getName(), request.userId());
                parameters.addParam(transferToKey, userToBeTransferred);
                parameters.addParam(isRingBackEnabledKey, isRingBackEnabled);

                callerNumber.setFriendlyName(caller.optString(NotificationFields.NAME.getName(), null));

                callRoutingDetails.setCallId(Objects.isNull(parentCallId) ? callId : parentCallId);
                callRoutingDetails.setForwardedBy(request.getUser().getFullName());
                callRoutingDetails.setCallerLocale(Objects.isNull(locale) ? Locale.US : locale);
                callRoutingDetails.setCallType(CallType.INCOMING);
                callRoutingDetails.setCallbackParams(parameters);
                callRoutingDetails.setCaller(callerNumber);
                callRoutingDetails.setRecipients(List.of(new Recipient(receiver, displayName)));

                voiceCallService.transferCall(callRoutingDetails);
                crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notification.call.forward.success"));
            }
            case "record" -> {
                final String callId = data.getString(NotificationFields.CALL_SID.getName());
                final boolean callRecordEnabled = data.getBoolean(NotificationFields.CALL_RECORD_ENABLED.getName());

                voiceCallService.recordCall(callId, callRecordEnabled);
                crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString(callRecordEnabled ? "i18n.notification.call.recoding.enabled"
                        : "i18n.notification.call.recoding.disabled"));
            }
        }
    }
}