package com.tlc.notification.webhook.model;

import com.tlc.commons.data.model.ConsumableData;
import com.tlc.commons.json.JsonObject;

import com.tlc.l10n.model.country.Country;

import com.tlc.notification.model.call.CallType;

/**
 * Model representing call handling (routing, transfer, etc.) webhook data.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallHandleWebhookData implements ConsumableData {

    private String callId;
    private String callerName;
    private String caller;
    private String receiver;

    private JsonObject customParams;
    private Country callerCountry;
    private CallType callType;

    public String getCallId() {
        return callId;
    }

    public void setCallId(final String callId) {
        this.callId = callId;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(final String callerName) {
        this.callerName = callerName;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(final String caller) {
        this.caller = caller;
    }

    public Country getCallerCountry() {
        return callerCountry;
    }

    public void setCallerCountry(final Country callerCountry) {
        this.callerCountry = callerCountry;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(final String receiver) {
        this.receiver = receiver;
    }

    public JsonObject getCustomParams() {
        return customParams;
    }

    public void setCustomParams(final JsonObject customParams) {
        this.customParams = customParams;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(final CallType callType) {
        this.callType = callType;
    }
}
