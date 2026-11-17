package com.tlc.email.client.internal.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.client.service.EmailDomainService;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.publish.EmailDomainRequestPublisher;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.email.server.events.EmailEvents;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Default implementation of {@link EmailDomainService}.
 * Handles domain addition and retrieval via events and converters.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainServiceImpl implements EmailDomainService {

    private final EmailDomainConverter emailDomainConverter;
    private final EmailDomainRequestPublisher emailDomainRequestPublisher;

    /**
     * <p>
     * Private constructor to enforce singleton usage.
     * </p>
     */
    private EmailDomainServiceImpl() {
        this.emailDomainConverter = EmailDomainConverter.getInstance();
        this.emailDomainRequestPublisher = EmailDomainRequestPublisher.getInstance();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link EmailDomainServiceImpl}.
     * </p>
     */
    private static class Instance {
        private static final EmailDomainService INSTANCE = new EmailDomainServiceImpl();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainService}.
     * </p>
     *
     * @return The {@link EmailDomainService} singleton instance.
     */
    public static EmailDomainService getInstance() {
        return EmailDomainServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Publishes a request to add a new domain via the {@link EmailDomainRequestPublisher}.
     * </p>
     *
     * @param emailDomainRequest The {@link EmailDomainRequest} representing the domain to add.
     */
    @Override
    public void addDomain(final EmailDomainRequest emailDomainRequest) {
        emailDomainRequestPublisher.publish(emailDomainRequest);
    }

    /**
     * <p>
     * Fetches {@link EmailDomain} objects for the given collection of domain IDs.
     * Sends an event to retrieve domains and uses the provided {@link EventCallback} to handle the response.
     * </p>
     *
     * @param domainIds           A collection of domain IDs to fetch.
     * @param domainEventCallback The {@link EventCallback} to handle the fetched domains or failure.
     */
    @Override
    public void fetchDomainsByIds(final Collection<Long> domainIds, final EventCallback<Collection<EmailDomain>> domainEventCallback) {
        final Event<byte[]> domainFetchEvent = new Event<>(
                EmailEvents.FETCH_DOMAINS,
                Json.object()
                        .put(EmailFields.IDS, domainIds)
                        .getBytes()
        );

        final EventCallback<byte[]> domainFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] response) {
                final Collection<EmailDomain> emailDomains = new HashSet<>();

                if (Objects.nonNull(response)) {
                    final JsonArray jsonArray = Json.array(new String(response));
                    final int size = jsonArray.size();

                    for (int index = 0; index < size; index++) {
                        emailDomains.add(emailDomainConverter.convert(jsonArray.getJsonObject(index)));
                    }
                }
                domainEventCallback.onSuccess(emailDomains);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                domainEventCallback.onFailure(ErrorCode.get(EmailErrorCodes.DOMAIN_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };

        EventNotifier.get().send(domainFetchEvent, domainFetchCallBack);
    }

    /**
     * <p>
     * Fetches a single {@link EmailDomain} object for the specified domain ID.
     * Sends an event and uses the provided {@link EventCallback} to handle the response.
     * </p>
     *
     * @param domainId            The ID of the domain to fetch.
     * @param domainEventCallback The {@link EventCallback} to handle the fetched domain or failure.
     */
    public void fetchDomainById(final Long domainId, final EventCallback<EmailDomain> domainEventCallback) {
        final Event<byte[]> domainFetchEvent = new Event<>(
                EmailEvents.FETCH_DOMAIN,
                Json.object()
                        .put(EmailFields.ID, domainId)
                        .getBytes()
        );

        final EventCallback<byte[]> domainFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] response) {
                domainEventCallback.onSuccess(emailDomainConverter.convert(Json.object(response)));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                domainEventCallback.onFailure(ErrorCode.get(EmailErrorCodes.DOMAIN_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };

        EventNotifier.get().send(domainFetchEvent, domainFetchCallBack);
    }

    /**
     * <p>
     * Validates the specified DNS records for the given domain and fetches their updated status.
     * </p>
     *
     * @param domainId          The ID of the domain whose DNS records are to be validated.
     * @param dnsRecordIds      Collection of DNS record IDs to be verified.
     * @param dnsRecordCallback The {@link EventCallback} to handle the validated DNS records or failure.
     */
    public void verifyAndFetchDnsRecords(final Long domainId, final Collection<Long> dnsRecordIds,
                                         final EventCallback<Collection<EmailDnsRecord>> dnsRecordCallback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailFields.DOMAIN_ID, domainId);
        jsonObject.put(EmailFields.IDS, emailDomainConverter.getJsonArray(dnsRecordIds));
        final Event<byte[]> dnsVerifyAndFetchEvent = new Event<>(EmailEvents.VALIDATE_AND_FETCH_RECORDS, jsonObject.getBytes());
        final EventCallback<byte[]> dnsVerifyAndFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] response) {
                final JsonObject json = Json.object(response);
                final JsonArray jsonArray = json.getJsonArray(EmailFields.DNS_RECORDS);
                final Collection<EmailDnsRecord> emailDnsRecords = new HashSet<>();
                final int size = jsonArray.size();

                for (int index = 0; index < size; index++) {
                    emailDnsRecords.add(emailDomainConverter.convertDnsRecord(jsonArray.getJsonObject(index)));
                }
                dnsRecordCallback.onSuccess(emailDnsRecords);
            }

            @Override
            public void onFailure(Throwable throwable) {
                dnsRecordCallback.onFailure(ErrorCode.get(EmailErrorCodes.DNS_RECORDS_RETRIEVE_ACTION_FAILED, throwable.getMessage()));
            }
        };

        EventNotifier.get().send(dnsVerifyAndFetchEvent, dnsVerifyAndFetchCallBack);
    }
}