package com.tlc.email.server.internal.provider;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.service.Service;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.service.EmailNotificationSender;

import java.util.Collection;

/**
 * <p>
 * Represents an email service that handles domain-authenticated email sending.
 * This interface is responsible for managing email delivery using verified domains
 * (i.e., domains authenticated via SPF, DKIM, or DMARC records).
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface DomainAuthenticatedEmailService extends EmailNotificationSender<Email>, Service {

    /**
     * <p>
     * Adds and authenticates the given {@link EmailDomain} with the underlying
     * email service provider.
     * </p>
     *
     * @param emailDomain The domain entity to be authenticated.
     */
    void addDomain(final EmailDomain emailDomain);

    /**
     * <p>
     * Retrieves the DNS records required for domain authentication
     * using the provider-specific internal domain identifier.
     * </p>
     *
     * @param id The internal identifier of the domain.
     * @return A collection of {@link EmailDnsRecord} objects required for verification.
     */
    Collection<DnsRecord> getDnsRecords(final long id);
}