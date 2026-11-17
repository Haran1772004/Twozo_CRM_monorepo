package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.request.SendRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Codec for encoding and decoding the {@link Email}
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class TransactionalMailCodec extends AbstractEmailCodec<SendRequest> {

    private static final String EMPTY_STRING = "";

    private TransactionalMailCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link TransactionalMailCodec}.
     * </p>
     */
    private static final class InstanceHolder {
        private static final TransactionalMailCodec INSTANCE = new TransactionalMailCodec();
    }

    /**
     * <p>
     * Gets the instance of {@link TransactionalMailCodec}
     * </p>
     *
     * @return the instance
     */
    public static TransactionalMailCodec getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sendRequest Represents the email to be encoded
     * @return the encoded email in byte array
     */
    @Override
    public byte[] encode(final SendRequest sendRequest) {
        final JsonObject emailJson = Json.object();
        final Email email = sendRequest.getEmail();
        final String content = email.getContent();
        final String subject = email.getSubject();
        final SendRequest.ProviderType providerType = sendRequest.getProviderType();
        final EmailRecipients emailRecipients = email.getEmailRecipients();

        emailJson.put(EmailFields.CONTENT, Objects.isNull(content) ? EMPTY_STRING : content);
        emailJson.put(EmailFields.SUBJECT, Objects.isNull(subject) ? EMPTY_STRING : subject);
        emailJson.put(EmailFields.SENDER_ADDRESS, email.getSender().getEmailAddress());

        if (Objects.nonNull(emailRecipients)) {
            final JsonObject toAddressJson = Json.object();
            final Collection<String> toAddresses = emailRecipients.toRecipients().stream().map(EmailAddress::getEmailAddress).filter(Objects::nonNull).toList();

            toAddressJson.put(EmailFields.TO_ADDRESSES, toAddresses);
            emailJson.put(EmailFields.RECIPIENTS, toAddressJson);
        }
        emailJson.put(EmailFields.PROVIDER_TYPE, providerType.name());

        return emailJson.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to decode
     * @return the {@link SendRequest}
     */
    @Override
    public SendRequest decode(final byte[] data) {
        final JsonObject emailJson = Json.object(data);
        final SendRequest sendRequest = new SendRequest();
        final OutboundEmail email = new OutboundEmail();

        if (emailJson.containsKey(EmailFields.CONTENT)) {
            final String content = emailJson.getString(EmailFields.CONTENT);

            email.setContent(Objects.nonNull(content) ? content : EMPTY_STRING);
        } else {
            email.setContent(EMPTY_STRING);
        }

        if (emailJson.containsKey(EmailFields.SUBJECT)) {
            final String subject = emailJson.getString(EmailFields.SUBJECT);

            email.setSubject(Objects.nonNull(subject) ? subject : EMPTY_STRING);
        } else {
            email.setSubject(EMPTY_STRING);
        }
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(emailJson.getString(EmailFields.SENDER_ADDRESS));

        email.setSender(syncAddressInfo);

        if (emailJson.containsKey(EmailFields.RECIPIENTS)) {
            final JsonObject recipientJson = emailJson.getJsonObject(EmailFields.RECIPIENTS);
            final EmailRecipients emailRecipients = new EmailRecipients();
            final Collection<EmailAddress> addresses = new ArrayList<>();

            if (recipientJson.containsKey(EmailFields.TO_ADDRESSES)) {
                final JsonArray addressArray = recipientJson.getJsonArray(EmailFields.TO_ADDRESSES);
                final int addressSize = addressArray.size();

                for (int addressIndex = 0; addressIndex < addressSize; addressIndex++) {
                    addresses.add(new EmailAddress(addressArray.getString(addressIndex)));
                }
            }
            emailRecipients.addRecipients(AddressType.TO, addresses);
            email.setRecipients(List.of(emailRecipients));
        }
        sendRequest.setEmail(email);
        sendRequest.setProviderType(SendRequest.ProviderType.valueOf(emailJson.getString(EmailFields.PROVIDER_TYPE)));

        return sendRequest;
    }
}