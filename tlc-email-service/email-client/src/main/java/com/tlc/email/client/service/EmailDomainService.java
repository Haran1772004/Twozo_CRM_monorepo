package com.tlc.email.client.service;

import com.tlc.email.client.internal.impl.EmailDomainServiceImpl;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;

/**
 * <p>
 * Service interface for performing client-side operations related to
 * {@link EmailDomain} entities.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailDomainService {

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainService}.
     * </p>
     *
     * @return The {@link EmailDomainService} implementation instance.
     */
    static EmailDomainService get() {
        return EmailDomainServiceImpl.getInstance();
    }

    /**
     * <p>
     * Publishes a request to add a new domain using {@link EmailDomainRequest}.
     * </p>
     *
     * @param emailDomainRequest The {@link EmailDomainRequest} representing the domain to add.
     */
    void addDomain(final EmailDomainRequest emailDomainRequest);

    /**
     * <p>
     * Fetches {@link EmailDomain} objects for the given collection of domain IDs.
     * Uses {@link EventCallback} to handle asynchronous response.
     * </p>
     *
     * @param domainIds           The collection of domain IDs to fetch.
     * @param domainEventCallback The {@link EventCallback} to handle the fetched domains or errors.
     */
    void fetchDomainsByIds(final Collection<Long> domainIds, final EventCallback<Collection<EmailDomain>> domainEventCallback);

    /**
     * <p>
     * Fetches a single {@link EmailDomain} object for the given domain ID.
     * Uses {@link EventCallback} to handle asynchronous response.
     * </p>
     *
     * @param domainId            The ID of the domain to fetch.
     * @param domainEventCallback The {@link EventCallback} to handle the fetched domain or errors.
     */
    void fetchDomainById(final Long domainId, final EventCallback<EmailDomain> domainEventCallback);

    /**
     * <p>
     * Validates the specified DNS records for the given domain and fetches their updated status.
     * </p>
     *
     * @param domainId          The ID of the domain whose DNS records are to be validated.
     * @param dnsRecordIds      Collection of DNS record IDs to be verified.
     * @param dnsRecordCallback The {@link EventCallback} to handle the validated DNS records or failure.
     */
    void verifyAndFetchDnsRecords(final Long domainId, final Collection<Long> dnsRecordIds,
                                  final EventCallback<Collection<EmailDnsRecord>> dnsRecordCallback);
}