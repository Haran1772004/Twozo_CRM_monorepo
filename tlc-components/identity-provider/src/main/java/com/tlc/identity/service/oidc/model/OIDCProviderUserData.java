package com.tlc.identity.service.oidc.model;

/**
 * <p>
 * Represents the user profile from OIDC Identity provider
 * </p>
 *
 * @param firstName    Represents the first name
 * @param emailAddress Represents the email address
 * @param userId       Represents the unique user ID
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public record OIDCProviderUserData(String firstName, String emailAddress, String userId) {
}