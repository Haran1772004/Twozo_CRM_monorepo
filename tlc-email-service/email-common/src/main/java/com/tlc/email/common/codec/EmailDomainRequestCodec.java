package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.request.EmailDomainRequest;

import java.util.Objects;

/**
 * <p>
 * Responsible for encoding and decoding domain-related request data used
 * for email domain operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainRequestCodec extends AbstractEmailCodec<EmailDomainRequest> {

    private final EmailDomainConverter emailDomainConverter;

    private EmailDomainRequestCodec() {
        this.emailDomainConverter = EmailDomainConverter.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of {@link EmailDomainRequestCodec}.
     * </p>
     */
    private static class Instance {
        private static final EmailDomainRequestCodec INSTANCE = new EmailDomainRequestCodec();
    }

    public static EmailDomainRequestCodec getInstance() {
        return EmailDomainRequestCodec.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailDomainRequest the {@link EmailDomainRequest} to be encoded
     * @return encoded bytes of domain request codec data
     */
    @Override
    public byte[] encode(final EmailDomainRequest emailDomainRequest) {
        final JsonObject domainRequestJson = Json.object();

        domainRequestJson.put(EmailFields.DOMAIN, emailDomainConverter.convert(emailDomainRequest.getDomain()));
        domainRequestJson.put(EmailFields.ACTION, emailDomainRequest.getAction().name());
        domainRequestJson.put(EmailFields.STATE_DATA, emailDomainRequest.getStateData());

        return domainRequestJson.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return {@link EmailDomainRequest}
     */
    @Override
    public EmailDomainRequest decode(final byte[] data) {
        if (Objects.isNull(data)) {
            return null;
        }
        final JsonObject jsonObject = Json.object(data);
        final EmailDomainRequest emailDomainRequest = new EmailDomainRequest();

        if (jsonObject.containsKey(EmailFields.DOMAIN)) {
            emailDomainRequest.setDomain(
                    emailDomainConverter.convert(jsonObject.getJsonObject(EmailFields.DOMAIN))
            );
        }

        if (jsonObject.containsKey(EmailFields.ACTION)) {
            emailDomainRequest.setAction(
                    EmailDomainRequest.Action.valueOf(jsonObject.getString(EmailFields.ACTION))
            );
        }
        emailDomainRequest.setStateData(getStateDataAsJson(jsonObject));

        return emailDomainRequest;
    }
}