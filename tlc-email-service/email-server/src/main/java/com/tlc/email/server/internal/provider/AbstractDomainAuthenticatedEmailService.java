package com.tlc.email.server.internal.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;

import java.util.Collection;

/**
 * <p>
 * Abstract base implementation of {@link DomainAuthenticatedEmailService}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractDomainAuthenticatedEmailService implements DomainAuthenticatedEmailService {

    /**
     * <p>
     * Adds and authenticates a domain for email sending.
     * </p>
     *
     * @param emailDomain Represents the domain to be added.
     */
    @Override
    public void addDomain(final EmailDomain emailDomain) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * <p>
     * Retrieves DNS records for a domain using its internal identifier.
     * </p>
     *
     * @param id Represents the internal domain identifier.
     * @return Collection of {@link EmailDnsRecord} objects.
     */
    @Override
    public Collection<DnsRecord> getDnsRecords(final long id) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
    }
}