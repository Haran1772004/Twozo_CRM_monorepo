package com.tlc.commons.dns.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * Represents a domain configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class Domain {

    private String name;
    private Collection<DnsRecord> dnsRecords;

    /**
     * <p>
     * Constructs a domain with the specified name.
     * </p>
     *
     * @param name the domain name
     */
    public Domain(final String name) {
        this.name = name;
        this.dnsRecords = new HashSet<>();
    }

    /**
     * <p>
     * Default constructor for creating a domain instance.
     * </p>
     */
    public Domain() {
        this.dnsRecords = new HashSet<>();
    }

    /**
     * <p>
     * Returns the domain name.
     * </p>
     *
     * @return the domain name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Sets the domain name.
     * </p>
     *
     * @param name the domain name to be set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * <p>
     * Returns the collection of DNS records associated with the domain.
     * </p>
     *
     * @return the collection of DNS records
     */
    public Collection<DnsRecord> getDnsRecords() {
        return dnsRecords;
    }

    /**
     * <p>
     * Sets the collection of DNS records for the domain.
     * </p>
     *
     * @param dnsRecords the collection of DNS records to be set
     */
    public void setDnsRecords(final Collection<DnsRecord> dnsRecords) {
        this.dnsRecords = dnsRecords;
    }

    /**
     * <p>
     * Helps to add the dns record.
     * </p>
     *
     * @param record Represent the dns record.
     */
    public void addDnsRecord(final DnsRecord record) {
        this.dnsRecords.add(record);
    }
}