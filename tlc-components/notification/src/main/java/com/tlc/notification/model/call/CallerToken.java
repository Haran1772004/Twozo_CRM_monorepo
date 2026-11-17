package com.tlc.notification.model.call;

/**
 * Represents a Twilio WebRTC Access Token used for authentication.
 *
 * <p>
 *     This record is a simple wrapper around the JWT token. It is used to securely
 * pass authentication credentials to WebRTC clients.
 * </p>
 *
 * @param token The JWT access token for Twilio WebRTC authentication.
 *
 * @author Sathish
 * @version 1.0
 */
public record CallerToken(String token) {
}
