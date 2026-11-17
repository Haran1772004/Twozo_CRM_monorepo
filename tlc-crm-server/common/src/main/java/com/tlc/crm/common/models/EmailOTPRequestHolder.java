package com.tlc.crm.common.models;

import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.otp.model.OTPRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Represents the email otp holder
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Setter
@Getter
public class EmailOTPRequestHolder implements EmailNotifiableModel {

    private OTPRequest otpRequest;
    private String otpVerificationUrl;
}
