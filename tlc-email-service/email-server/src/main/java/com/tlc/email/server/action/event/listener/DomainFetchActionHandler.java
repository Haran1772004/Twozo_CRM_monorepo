package com.tlc.email.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.server.events.EmailEvents;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.internal.manager.EmailDomainManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Optional;

/**
 * <p>
 * Responsible for fetching a single {@link EmailDomain} object from the {@link EmailDomainManager}
 * based on its ID or name and converting it to {@link JsonObject} for event response.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@EventListener(name = EmailEvents.FETCH_DOMAIN)
public class DomainFetchActionHandler implements EventConsumer<byte[]> {

    private final EmailDomainManager emailDomainManager;
    private final EmailDomainConverter emailDomainConverter;

    /**
     * <p>
     * Default constructor initializes the {@link EmailDomainManager} and
     * {@link EmailDomainConverter} instances.
     * </p>
     */
    public DomainFetchActionHandler() {
        this.emailDomainManager = EmailDomainManager.getInstance();
        this.emailDomainConverter = EmailDomainConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param data represents the incoming event data in byte array format
     * @return byte array representing the JSON object of the converted {@link EmailDomain}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject request = Json.object(data);
        final JsonObject response = Json.object();
        Optional<EmailDomain> optionalDomain = Optional.empty();

        if (request.containsKey(EmailFields.ID)) {
            optionalDomain = emailDomainManager.get(request.getLong(EmailFields.ID));
        } else if (request.containsKey(EmailFields.NAME)) {
            optionalDomain = emailDomainManager.getDomainByName(request.getString(EmailFields.NAME));
        }

        optionalDomain.ifPresent(domain -> response.append(emailDomainConverter.convert(domain)));

        return response.getBytes();
    }
}