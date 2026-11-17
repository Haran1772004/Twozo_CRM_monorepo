package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.request.SendRequest;

import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding send request data related to email threads or individual emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractEmailCodec
 */
public final class SendRequestCodec extends AbstractEmailCodec<SendRequest> {

    private SendRequestCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link SendRequestCodec}.
     * </p>
     */
    private static class Instance {
        private static final SendRequestCodec INSTANCE = new SendRequestCodec();
    }

    public static SendRequestCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sendRequest the {@link SendRequest} to be encoded
     * @return encoded bytes of send request codec data
     */
    @Override
    public byte[] encode(final SendRequest sendRequest) {
        final JsonObject sendRequestJson = Json.object();
        final OutboundEmail email = sendRequest.getEmail();
        final Email.MessageCategory messageCategory = email.getMessageCategory();
        final JsonObject emailJson = Email.MessageCategory.DRAFT == messageCategory ?
                getDraftEmailAsJson((DraftEmail) email) : getOutboundEmailAsJson(email);

        emailJson.put(EmailFields.SUBJECT, email.getSubject());
        emailJson.put(EmailFields.CONTENT, email.getContent());

        sendRequestJson.put(EmailFields.EMAIL, emailJson);
        sendRequestJson.put(EmailFields.STATE_DATA, sendRequest.getStateData());
        sendRequestJson.put(EmailFields.PROVIDER_TYPE, sendRequest.getProviderType().name());

        loadContent(email, sendRequestJson);

        return sendRequestJson.getBytes();
    }

    /**
     * <p>
     * Loads the subject and content from the given {@link Email}.
     * </p>
     *
     * @param email      The {@link Email} object containing subject and content.
     * @param jsonObject The JSON structure to update.
     */
    private void loadContent(final Email email, final JsonObject jsonObject) {
        if (jsonObject.containsKey(EmailFields.EMAIL)) {
            jsonObject.getJsonObject(EmailFields.EMAIL)
                    .put(EmailFields.SUBJECT, email.getSubject())
                    .put(EmailFields.CONTENT, email.getContent());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link SendRequest}
     */
    @Override
    public SendRequest decode(final byte[] data) {

        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }
        final JsonObject sendRequestJson = Json.object(data);
        final SendRequest sendRequest = new SendRequest();
        final JsonObject emailJson = sendRequestJson.getJsonObject(EmailFields.EMAIL);
        final Email.MessageCategory messageCategory = Email.MessageCategory.get(emailJson.getShort(EmailFields.MESSAGE_CATEGORY_ID));
        final OutboundEmail outboundEmail = messageCategory.isDraft() ? getDraftEmail(emailJson) : getOutboundEmail(emailJson);

        sendRequest.setEmail(outboundEmail);
        sendRequest.setProviderType(SendRequest.ProviderType.valueOf(sendRequestJson.getString(EmailFields.PROVIDER_TYPE)));
        sendRequest.setStateData(getStateDataAsJson(sendRequestJson));
        loadConversationType(sendRequest);

        return sendRequest;
    }

    /**
     * <p>
     * Helps to load the conversation type into email.
     * </p>
     *
     * @param sendRequest Represents the send request.
     */
    private void loadConversationType(final SendRequest sendRequest) {
        if (sendRequest.getProviderType().isServiceProvider()) {
            sendRequest.getEmail().setConversationType(Email.Type.INTERNAL);
        }
    }
}
