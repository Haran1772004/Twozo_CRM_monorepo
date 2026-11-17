package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.crypto.EncryptionHelper;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.validator.SocialEntityValidator;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.server.whatsapp.internal.service.WAService;
import com.tlc.social.server.whatsapp.internal.service.WATemplateSyncPublisher;

import com.tlc.social.server.whatsapp.internal.service.WASyncTokenService;
import com.tlc.social.server.whatsapp.internal.service.WAWebhookService;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenWriteRequest;
import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * A handler which listens to the WhatsApp Business account retrieve and process event
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.FETCH_WA_BUSINESS_ACCOUNT)
public class WABusinessAccountDataHandler implements EventConsumer<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WABusinessAccountDataHandler.class);

    private final WABusinessManager waBusinessManager;
    private final WAService waService;
    private final WAWebhookService waWebhookService;
    private final HashGenerator hashGenerator;
    private final EncryptionHelper encryptionHelper;
    private final WATemplateSyncPublisher waTemplateSyncPublisher;
    private final WASyncTokenService waSyncTokenService;

    public WABusinessAccountDataHandler() {
        waBusinessManager = WABusinessManager.getInstance();
        waService = WAService.getInstance();
        waWebhookService = WAWebhookService.getInstance();
        hashGenerator = HashGenerator.getInstance();
        encryptionHelper = EncryptionHelper.getInstance();
        waTemplateSyncPublisher = WATemplateSyncPublisher.getInstance();
        waSyncTokenService = WASyncTokenService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     * @return the response in byte array
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        LOGGER.info("Whatsapp: Event received to retrieve and process WhatsApp business account");
        final JsonObject data = Json.object(bytes);
        final JsonObject response = Json.object();
        final String accessToken = data.getString(WAFields.ACCESS_TOKEN);
        final Long externalBusinessId = data.getLong(WAFields.BUSINESS_ID);
        final Long externalPhoneNumberId = data.getLong(WAFields.PHONE_NUMBER_ID);
        final Long externalWABusinessAccountId = data.getLong(WAFields.WABA_ID);
        final WABusinessAccount waBusinessAccount = waService.fetchWABusinessAccount(externalWABusinessAccountId, accessToken, externalPhoneNumberId);

        waBusinessAccount.setExternalBusinessId(externalBusinessId);
        waBusinessAccount.setExternalWABAId(externalWABusinessAccountId);

        final Collection<String> errors = SocialEntityValidator.validate(List.of(waBusinessAccount), Group.Create.class);

        if (errors.isEmpty()) {
            waBusinessManager.storeWABusinessAccount(waBusinessAccount, accessToken);
            final Long waBusinessAccountId = waBusinessAccount.id();
            final WATokenWriteRequest writeRequest = SyncTokenRequestInstanceProvider.getWATokenWriteRequest(waBusinessAccountId, accessToken, TokenRequest.TokenType.ACCESS);

            waSyncTokenService.putAccessToken(writeRequest);
            waWebhookService.subscribe(waBusinessAccount);
            registerBusinessPhoneNumber(waBusinessAccount.getBusinessPhoneNumbers(), accessToken);

            response.put(WAFields.BUSINESS_ACCOUNT_ID, waBusinessAccountId);
            response.put(WAFields.PHONE_NUMBER_IDS, waBusinessAccount.getBusinessPhoneNumbers().stream().map(WABusinessPhoneNumber::id).collect(Collectors.toUnmodifiableSet()));
            LOGGER.info("Whatsapp: WhatsApp business account is retrieved and processed successfully");
            waTemplateSyncPublisher.publish(waBusinessAccount);
        } else {
            response.put(WAFields.ERRORS, errors);
        }

        return response.getBytes();
    }

    /**
     * <p>
     * Registers the business phone number
     * </p>
     *
     * @param waBusinessPhoneNumbers Represents the collection of {@link WABusinessPhoneNumber}
     * @param businessAccessToken    Represents the business access token
     */
    private void registerBusinessPhoneNumber(final Collection<WABusinessPhoneNumber> waBusinessPhoneNumbers, final String businessAccessToken) {

        for (final WABusinessPhoneNumber waBusinessPhoneNumber : waBusinessPhoneNumbers) {
            final String externalPhoneIdInString = String.valueOf(waBusinessPhoneNumber.getExternalPhoneNumberId());
            final String encodedPIN = waBusinessPhoneNumber.getEncodedPIN();
            final String uniquePIN;

            if (Objects.isNull(encodedPIN)) {
                uniquePIN = hashGenerator.getRandomUniqueId(externalPhoneIdInString, NumericalConstant.SIX, true);

                waBusinessPhoneNumber.setEncodedPIN(encryptionHelper.encrypt(externalPhoneIdInString, uniquePIN));
            } else {
                uniquePIN = encryptionHelper.decrypt(externalPhoneIdInString, encodedPIN);
            }

            waService.registerBusinessPhoneNumber(waBusinessPhoneNumber, businessAccessToken, uniquePIN);

            if (waBusinessPhoneNumber.isRegistered()) {
                waBusinessManager.updatePhoneRegistrationDetails(waBusinessPhoneNumber);
            }
        }
    }
}
