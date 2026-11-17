package com.tlc.email.common.model;

import com.tlc.commons.dns.model.Domain;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Represents an email domain configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomain extends Domain implements EmailModel {

    private Long id;
    private EmailDomainMetaData emailDomainMetaData;

    public EmailDomain(final Long id, final String name) {
        super(name);
        this.id = id;
    }

    public EmailDomain() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets only email DNS records from the collection.
     *
     * @return collection of email DNS records
     */
    public Collection<EmailDnsRecord> getEmailDnsRecords() {
        return getDnsRecords().stream()
                .filter(record -> record instanceof EmailDnsRecord)
                .map(record -> (EmailDnsRecord) record)
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long id() {
        return id;
    }

    public EmailDomainMetaData getEmailDomainMetaData() {
        return emailDomainMetaData;
    }

    public void setEmailDomainMetaData(final EmailDomainMetaData emailDomainMetaData) {
        this.emailDomainMetaData = emailDomainMetaData;
    }
}