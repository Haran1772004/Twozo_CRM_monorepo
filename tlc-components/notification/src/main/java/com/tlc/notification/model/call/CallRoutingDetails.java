package com.tlc.notification.model.call;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents the details of a call, including caller information, recipients, and call type.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallRoutingDetails {

    private String callId;
    private String forwardedBy;
    private MobileNumber caller;
    private Collection<Recipient> recipients;
    private Locale callerLocale;
    private CallType callType;
    private Parameters callbackParams;
    private boolean isCallRecordingEnabled;

    public String getCallId() {
        return callId;
    }

    public void setCallId(final String callId) {
        this.callId = callId;
    }

    public String getForwardedBy() {
        return forwardedBy;
    }

    public void setForwardedBy(final String forwardedBy) {
        this.forwardedBy = forwardedBy;
    }

    public MobileNumber getCaller() {
        return caller;
    }

    public void setCaller(final MobileNumber caller) {
        this.caller = caller;
    }

    public Collection<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(final Collection<Recipient> recipients) {
        this.recipients = recipients;
    }

    public Locale getCallerLocale() {
        return callerLocale;
    }

    public void setCallerLocale(final Locale callerLocale) {
        this.callerLocale = callerLocale;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(final CallType callType) {
        this.callType = callType;
    }

    public String getCallbackParamsAsString() {
        return Objects.isNull(callbackParams) ? "" : callbackParams.getParamsAsString();
    }

    public Parameters getCallbackParams() {
        return callbackParams;
    }

    public void setCallbackParams(final Parameters callbackParams) {
        this.callbackParams = callbackParams;
    }

    public boolean isCallRecordingEnabled() {
        return isCallRecordingEnabled;
    }

    public void setCallRecordingEnabled(final boolean callRecordingEnabled) {
        this.isCallRecordingEnabled = callRecordingEnabled;
    }
}
