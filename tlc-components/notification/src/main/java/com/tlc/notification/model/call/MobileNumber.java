package com.tlc.notification.model.call;

import com.tlc.notification.model.identity.ReceiverIdentity;
import com.tlc.notification.model.identity.SenderIdentity;

/**
 * <p>
 * A model that represents the mobile number.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class MobileNumber implements SenderIdentity, ReceiverIdentity {

    private Long mobileNumber;
    private String serviceId;
    private String countryCode;
    private String friendlyName;
    private final String mobileNumberAsString;

    public MobileNumber(final Long mobileNumber, final String countryCode) {
        this.mobileNumber = mobileNumber;
        this.countryCode = countryCode;
        this.mobileNumberAsString = new StringBuilder(countryCode).append(mobileNumber).toString();
    }

    public MobileNumber(final String mobileNumberAsString) {
        this.mobileNumberAsString = mobileNumberAsString;
    }

    public Long getMobileNumber() {
        return mobileNumber;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getMobileNumberAsString() {
        return mobileNumberAsString;
    }
}
