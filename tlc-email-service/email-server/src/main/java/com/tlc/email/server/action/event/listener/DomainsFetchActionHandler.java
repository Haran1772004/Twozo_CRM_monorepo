package com.tlc.email.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.server.events.EmailEvents;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.internal.manager.EmailDomainManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * Responsible for fetching {@link EmailDomain} objects from the {@link EmailDomainManager}
 * and converting them to {@link JsonObject} for event response.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@EventListener(name = EmailEvents.FETCH_DOMAINS)
public class DomainsFetchActionHandler implements EventConsumer<byte[]> {

    private final EmailDomainManager emailDomainManager;
    private final EmailDomainConverter emailDomainConverter;

    /**
     * <p>
     * Default constructor initializes the {@link EmailDomainManager} and
     * {@link EmailDomainConverter} instances.
     * </p>
     */
    public DomainsFetchActionHandler() {
        this.emailDomainManager = EmailDomainManager.getInstance();
        this.emailDomainConverter = EmailDomainConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param data represents the incoming event data in byte array format
     * @return byte array representing the JSON array of converted {@link EmailDomain} objects
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final JsonArray response = Json.array();
        final Collection<EmailDomain> emailDomains;

        if (jsonObject.containsKey(EmailFields.IDS)) {
            emailDomains = getDomainsByIds(jsonObject.getJsonArray(EmailFields.IDS));
        } else {
            emailDomains = getDomainsByNames(jsonObject.getJsonArray(EmailFields.DOMAIN_NAMES));
        }
        emailDomains.forEach(domain -> response.put(emailDomainConverter.convert(domain)));

        return response.getBytes();
    }

    /**
     * <p>
     * Fetches {@link EmailDomain} objects from {@link EmailDomainManager} by their IDs.
     * </p>
     *
     * @param jsonArray contains the list of domain IDs
     * @return collection of {@link EmailDomain} objects
     */
    private Collection<EmailDomain> getDomainsByIds(final JsonArray jsonArray) {
        final Collection<Long> ids = new HashSet<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            ids.add(jsonArray.getLong(index));
        }

        return emailDomainManager.get(ids);
    }

    /**
     * <p>
     * Fetches {@link EmailDomain} objects from {@link EmailDomainManager} by their names.
     * </p>
     *
     * @param jsonArray contains the list of domain names
     * @return collection of {@link EmailDomain} objects
     */
    private Collection<EmailDomain> getDomainsByNames(final JsonArray jsonArray) {
        final Collection<String> names = new HashSet<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            names.add(jsonArray.getString(index));
        }

        return emailDomainManager.getDomainByNames(names);
    }
}