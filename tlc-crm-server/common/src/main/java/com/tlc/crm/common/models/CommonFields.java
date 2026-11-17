package com.tlc.crm.common.models;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Holds the common fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonFields {

    public static final String MODEL = "model";
    public static final String OTP = "otp";
    public static final String DIGIT = "digit";
    public static final String TYPE = "type";
    public static final String CHANNEL = "channel";
    public static final String CATEGORY = "category";
    public static final String SIGN_UP = "signup";
    public static final String LOGIN = "login";
    public static final String LINK_EXPIRED = "linkExpired";
    public static final String RESEND_DURATION = "resendDuration";
    public static final String RECEIVER = "receiver";
    public static final String ACCOUNT_VERIFICATION = "accountVerification";
    public static final String CRM = "crm";
    public static final String CSRF = "csrf";
    public static final String CSRF_TOKEN = "X-CSRF-Token";
    public static final String FILE_PREVIEW_REDIRECT_URL = "file.preview.redirect.url";
    public static final String CRM_COMMON_CONFIG_KEY = "crm-common";
    public static final String FIELD_NAME = "fieldName";
}
