package com.tlc.otp.model;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Holds the OTP {@link Category} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Setter
@Getter
public class Category {

    private Long id;
    private CategoryConfig categoryConfig;

    private final OTPApplication otpApplication;
    private final String name;

    public Category(final OTPApplication otpApplication, final String name) {
        this.otpApplication = otpApplication;
        this.name = name;
    }
}
