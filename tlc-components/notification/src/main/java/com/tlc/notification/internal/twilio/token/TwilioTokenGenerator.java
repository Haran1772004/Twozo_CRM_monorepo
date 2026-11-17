package com.tlc.notification.internal.twilio.token;

import com.tlc.commons.util.Configurations;

import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.call.CallerToken;

import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;

/**
 * Generates Twilio WebRTC Access Tokens for making and receiving calls.
 *
 * <p>
 *     This class uses Twilio's AccessToken API to generate JWT tokens
 * that allow users to authenticate and initiate WebRTC-based calls.
 * </p>
 *
 * <p>
 *     The generated token contains a {@link VoiceGrant} that permits
 * outgoing and incoming calls through Twilio.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class TwilioTokenGenerator {

    private final String accountSid;
    private final String twimlAppSid;
    private final String apiKeySid;
    private final String apiSecretKeySid;
    private final int ttl;
    private final boolean allowIncomingCalls;

    public TwilioTokenGenerator(final String accountServiceId, final Configurations configurations) {
        accountSid = accountServiceId;
        twimlAppSid = configurations.getConfig(NotificationKeys.TWIML_APP_SERVICE_ID.getKey());
        apiKeySid = configurations.getConfig(NotificationKeys.API_KEY_SERVICE_ID.getKey());
        apiSecretKeySid = configurations.getConfig(NotificationKeys.API_SECRET_KEY_SERVICE_ID.getKey());
        ttl = configurations.getInt(NotificationKeys.ACCESS_TOKEN_TTL.getKey());
        allowIncomingCalls = Boolean.parseBoolean(configurations.getConfig(NotificationKeys.ALLOW_INCOMING_CALLS.getKey()));
    }

    /**
     * Generates a Twilio Access Token for WebRTC calling.
     *
     * <p>
     *     This method creates a new {@link AccessToken} with a {@link VoiceGrant}, allowing
     * both outgoing and incoming calls for the given user identity.
     * </p>
     *
     * @param identity The unique identity of the user (e.g., "agent123").
     * @return A {@link CallerToken} containing the generated JWT token.
     */
    public CallerToken generateCallerToken(final String identity) {
        final VoiceGrant voiceGrant = new VoiceGrant();

        voiceGrant.setOutgoingApplicationSid(twimlAppSid);
        voiceGrant.setIncomingAllow(allowIncomingCalls);
        final AccessToken token = new AccessToken.Builder(accountSid, apiKeySid, apiSecretKeySid)
                .identity(identity).grant(voiceGrant).ttl(ttl).build();

        return new CallerToken(token.toJwt());
    }
}
