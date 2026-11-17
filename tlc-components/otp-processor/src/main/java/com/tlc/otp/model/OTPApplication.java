package com.tlc.otp.model;

import com.tlc.commons.model.Application;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Represents the OTP applications
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Setter
@Getter
@AllArgsConstructor
public class OTPApplication implements Application {

    private Long id;
    private final String name;

    public OTPApplication(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return The application name
     */
    @Override
    public String getName() {
        return name;
    }
}
