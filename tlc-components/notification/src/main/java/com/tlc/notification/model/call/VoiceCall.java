package com.tlc.notification.model.call;

import com.tlc.notification.model.Notification;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the call.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class VoiceCall extends Notification<MobileNumber, MobileNumber> {

    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.voiceCall.co.dialer.invalid")
    private MobileNumber coDialer;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.voiceCall.service.id.invalid")
    private String serviceId;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.voiceCall.duration.invalid")
    private String duration;
    private Long completedTime;
    private BigDecimal price;
    private String priceUnit;
    private URI redirectUri;
    private URI callBackUri;
    private boolean recordEnable;
    private String status;
    private Collection<CallRecord> callRecords;

    public MobileNumber getCoDialer() {
        return coDialer;
    }

    public void setCoDialer(final MobileNumber coDialer) {
        this.coDialer = coDialer;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(final URI url) {
        this.redirectUri = url;
    }

    public URI getCallBackUri() {
        return callBackUri;
    }

    public void setCallBackUri(final URI callBackUri) {
        this.callBackUri = callBackUri;
    }

    public boolean recordEnable() {
        return recordEnable;
    }

    public void setRecordEnable(final boolean recordEnable) {
        this.recordEnable = recordEnable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Collection<CallRecord> getCallRecords() {
        return callRecords;
    }

    public void setCallRecords(final Collection<CallRecord> callRecords) {
        this.callRecords = callRecords;
    }

    public MobileNumber getRecipient() {
        return super.recipients.stream().findFirst().get();
    }

    public void addRecipient(final MobileNumber mobileNumber) {
        if (Objects.isNull(super.recipients)) {
            super.recipients = new ArrayList<>();
        }
        super.recipients.add(mobileNumber);
    }

    public void addCallRecord(final CallRecord callRecord) {
        if (Objects.isNull(this.callRecords)) {
            this.callRecords = new ArrayList<>();

            this.callRecords.add(callRecord);
        } else {
            this.callRecords.add(callRecord);
        }
    }

    public Long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(final Long completedTime) {
        this.completedTime = completedTime;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(final String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price.abs();
    }
}
