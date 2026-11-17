package com.tlc.sync.service.oauth.model;

import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.facebook.FBTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.facebook.FBTokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenWriteRequest;

/**
 * <p>
 * Provides the token request instances for the token read or write operation
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SyncTokenRequestInstanceProvider {

    /**
     * <p>
     * Gets the instance of {@link EmailTokenReadRequest}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param tokenType    Represents the token type
     * @return the instance of {@link EmailTokenReadRequest}
     */
    public static EmailTokenReadRequest getEmailTokenReadRequest(final EmailAddress emailAddress, final TokenRequest.TokenType tokenType) {
        return new EmailTokenReadRequest(emailAddress, SyncServiceCategory.EMAIL, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param accessCode    Represents the access code
     * @param syncUrlConfig Represents the {@link SyncUrlConfig}
     * @param tokenType     Represents the token type
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getEmailTokenWriteRequest(final String accessCode, final SyncUrlConfig syncUrlConfig,
                                                                   final TokenRequest.TokenType tokenType) {
        return new EmailTokenWriteRequest(accessCode, syncUrlConfig, SyncServiceCategory.EMAIL, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param tokenType    Represents the token type
     * @param emailAddress Represents the email address
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getEmailTokenWriteRequest(final TokenRequest.TokenType tokenType, final EmailAddress emailAddress) {
        return new EmailTokenWriteRequest(SyncServiceCategory.EMAIL, tokenType, emailAddress);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenReadRequest}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param tokenType    Represents the token type
     * @return the instance of {@link EmailTokenReadRequest}
     */
    public static EmailTokenReadRequest getCalendarTokenReadRequest(final EmailAddress emailAddress,
                                                                    final TokenRequest.TokenType tokenType) {
        return new EmailTokenReadRequest(emailAddress, SyncServiceCategory.CALENDAR, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param accessCode    Represents the access code
     * @param syncUrlConfig Represents the {@link SyncUrlConfig}
     * @param tokenType     Represents the token type
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getCalendarTokenWriteRequest(final String accessCode, final SyncUrlConfig syncUrlConfig,
                                                                      final TokenRequest.TokenType tokenType) {
        return new EmailTokenWriteRequest(accessCode, syncUrlConfig, SyncServiceCategory.CALENDAR, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param tokenType    Represents the token type
     * @param emailAddress Represents the email address
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getCalendarTokenWriteRequest(final TokenRequest.TokenType tokenType, final EmailAddress emailAddress) {
        return new EmailTokenWriteRequest(SyncServiceCategory.CALENDAR, tokenType, emailAddress);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenReadRequest}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param tokenType    Represents the token type
     * @return the instance of {@link EmailTokenReadRequest}
     */
    public static EmailTokenReadRequest getContactTokenReadRequest(final EmailAddress emailAddress,
                                                                   final TokenRequest.TokenType tokenType) {
        return new EmailTokenReadRequest(emailAddress, SyncServiceCategory.CONTACT, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param accessCode    Represents the access code
     * @param syncUrlConfig Represents the {@link SyncUrlConfig}
     * @param tokenType     Represents the token type
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getContactTokenWriteRequest(final String accessCode, final SyncUrlConfig syncUrlConfig,
                                                                     final TokenRequest.TokenType tokenType) {
        return new EmailTokenWriteRequest(accessCode, syncUrlConfig, SyncServiceCategory.CONTACT, tokenType);
    }

    /**
     * <p>
     * Gets the instance of {@link EmailTokenWriteRequest}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param tokenType    Represents the token type
     * @return the instance of {@link EmailTokenWriteRequest}
     */
    public static EmailTokenWriteRequest getContactTokenWriteRequest(final TokenRequest.TokenType tokenType, final EmailAddress emailAddress) {
        return new EmailTokenWriteRequest(SyncServiceCategory.CONTACT, tokenType, emailAddress);
    }

    /**
     * Gets the instance of {@link FBTokenReadRequest}
     *
     * @param orgId         Represents the org id
     * @param accessCode    Represents the access code
     * @param syncUrlConfig Represents the {@link SyncUrlConfig}
     * @param tokenType     Represents the token type
     * @return the {@link FBTokenWriteRequest}
     */
    public static FBTokenReadRequest getFBTokenReadRequest(final Long orgId, final String accessCode, final SyncUrlConfig syncUrlConfig,
                                                           final TokenRequest.TokenType tokenType) {
        final FBTokenReadRequest fbTokenReadRequest = new FBTokenReadRequest(orgId, tokenType);

        fbTokenReadRequest.setAccessCode(accessCode);
        fbTokenReadRequest.setSyncUrlConfig(syncUrlConfig);

        return fbTokenReadRequest;
    }

    /**
     * Gets the instance of {@link FBTokenReadRequest}
     *
     * @param tokenType Represents the token type
     * @return the {@link FBTokenWriteRequest}
     */
    public static FBTokenReadRequest getFBTokenReadRequest(final TokenRequest.TokenType tokenType) {
        return new FBTokenReadRequest(tokenType);
    }

    /**
     * Gets the instance of {@link FBTokenWriteRequest}
     *
     * @param orgId       Represents the org id
     * @param tokenType   Represents the token type
     * @param accessToken Represents the access token
     * @return the {@link FBTokenWriteRequest}
     */
    public static FBTokenWriteRequest getFBTokenWriteRequest(final Long orgId, final TokenRequest.TokenType tokenType, final String accessToken) {
        return new FBTokenWriteRequest(orgId, tokenType, accessToken);
    }

    /**
     * Gets the instance of {@link WATokenWriteRequest}
     *
     * @param waBusinessAccountId Represents the whatsapp business account id
     * @param accessToken         Represents the access token
     * @param tokenType           Represents the token type
     * @return the {@link WATokenWriteRequest}
     */
    public static WATokenWriteRequest getWATokenWriteRequest(final Long waBusinessAccountId,  final String accessToken, final TokenRequest.TokenType tokenType) {
        return new WATokenWriteRequest(waBusinessAccountId, accessToken, tokenType);
    }

    /**
     * Gets the instance of {@link WATokenReadRequest}
     *
     * @param accessCode Represents the access code
     * @param tokenType  Represents the token type
     * @return the {@link WATokenReadRequest}
     */
    public static WATokenReadRequest getWATokenReadRequest(final String accessCode, final TokenRequest.TokenType tokenType) {
        final WATokenReadRequest waTokenReadRequest = new WATokenReadRequest(tokenType);

        waTokenReadRequest.setAccessCode(accessCode);

        return waTokenReadRequest;
    }

    /**
     * Gets the instance of {@link WATokenReadRequest}
     *
     * @param waBusinessAccountId Represents the WhatsApp business account id
     * @param tokenType           Represents the token type
     * @return the {@link WATokenReadRequest}
     */
    public static WATokenReadRequest getWATokenReadRequest(final Long waBusinessAccountId, final TokenRequest.TokenType tokenType) {
        final WATokenReadRequest waTokenReadRequest = new WATokenReadRequest(tokenType);

        waTokenReadRequest.setWaBusinessAccountId(waBusinessAccountId);

        return waTokenReadRequest;
    }

    /**
     * Gets the instance of {@link WATokenReadRequest}
     *
     * @param tokenType Represents the token type
     * @return the {@link WATokenReadRequest}
     */
    public static WATokenReadRequest getWATokenReadRequest(final TokenRequest.TokenType tokenType) {
        return new WATokenReadRequest(tokenType);
    }
}