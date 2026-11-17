package com.tlc.notification.webhook.model;

import com.tlc.commons.data.model.ConsumableData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.l10n.model.country.Country;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Model representing status event data for a call webhook.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallStatusWebhookData implements ConsumableData {

    private String userId;
    private String callId;
    private String caller;
    private String receiver;
    private String routingNumber;
    private String transferTo;
    private String transferredBy;
    private String parentCallId;
    private Long startTime;
    private String duration;

    private JsonObject extraParams;
    private CallStatus callStatus;
    private CallType callType;
    private boolean isClickToCall;
    private boolean isRingBackEnabled;
    private String recordFileName;

    private Country callerCountry;

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
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

    public String getCallId() {
        return callId;
    }

    public void setCallId(final String callId) {
        this.callId = callId;
    }

    public String getParentCallId() {
        return parentCallId;
    }

    public void setParentCallId(final String parentCallId) {
        this.parentCallId = parentCallId;
    }

    public JsonObject getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(final LinkedHashMap<String, ?> extraParams) {
       if (Objects.nonNull(extraParams)) {
           this.extraParams = Json.object(extraParams);
       }
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(final CallStatus callStatus) {
        this.callStatus = callStatus;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(final CallType callType) {
        this.callType = callType;
    }

    public boolean isClickToCall() {
        return isClickToCall;
    }

    public void setClickToCall(final boolean clickToCall) {
        this.isClickToCall = clickToCall;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(final Long startTime) {
        this.startTime = startTime;
    }

    public String getRecordFileName() {
        return recordFileName;
    }

    public void setRecordFileName(final String recordFileName) {
        this.recordFileName = recordFileName;
    }

    public void setTransferTo(final String transferTo) {
        this.transferTo = transferTo;
    }

    public String getTransferTo() {
        return transferTo;
    }

    public String getTransferredBy() {
        return transferredBy;
    }

    public void setTransferredBy(final String transferredBy) {
        this.transferredBy = transferredBy;
    }

    public boolean isRingBackEnabled() {
        return isRingBackEnabled;
    }

    public void setRingBackEnabled(final boolean ringBackEnabled) {
        isRingBackEnabled = ringBackEnabled;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(final String routingNumber) {
        this.routingNumber = routingNumber;
    }

    @Override
    public String toString() {
        return "CallStatusWebhookData{" +
                "userId='" + userId + '\'' +
                ", callId='" + callId + '\'' +
                ", caller='" + caller + '\'' +
                ", receiver='" + receiver + '\'' +
                ", routingNumber='" + routingNumber + '\'' +
                ", transferTo='" + transferTo + '\'' +
                ", transferredBy='" + transferredBy + '\'' +
                ", parentCallId='" + parentCallId + '\'' +
                ", startTime=" + startTime +
                ", duration='" + duration + '\'' +
                ", extraParams=" + extraParams +
                ", callStatus=" + callStatus +
                ", callType=" + callType +
                ", isClickToCall=" + isClickToCall +
                ", isRingBackEnabled=" + isRingBackEnabled +
                ", recordFileName='" + recordFileName + '\'' +
                ", callerCountry=" + callerCountry +
                '}';
    }
}
