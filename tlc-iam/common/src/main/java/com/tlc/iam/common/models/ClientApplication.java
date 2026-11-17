package com.tlc.iam.common.models;

import com.tlc.commons.model.Application;

/**
 * Holds an application's information
 *
 * @author Selvakumar
 * @version 1.0
 */
public record ClientApplication(String name, String authSuccessRedirectUrl, String authFailureRedirectUrl,
                                boolean isSecureCookieEnabled, String cookieDomain, String cookiePath,
                                int cookieValidityInSeconds, String sameSiteMode) implements Application {

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
