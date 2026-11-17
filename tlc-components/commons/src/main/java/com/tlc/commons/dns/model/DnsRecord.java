package com.tlc.commons.dns.model;

/**
 * <p>
 * Represents a DNS record configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class DnsRecord {

    private String host;
    private String value;
    private Status status;
    private Integer priority;
    private RecordType recordType;
    private Long ttl;

    /**
     * <p>
     * Default constructor for creating a DNS record instance.
     * </p>
     */
    public DnsRecord() {
    }

    /**
     * <p>
     * Sets the host name for the DNS record.
     * </p>
     *
     * @param host the host name to be set
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * <p>
     * Sets the record type for the DNS record.
     * </p>
     *
     * @param recordType the record type to be set
     */
    public void setRecordType(final RecordType recordType) {
        this.recordType = recordType;
    }

    /**
     * <p>
     * Sets the value for the DNS record.
     * </p>
     *
     * @param value the value to be set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * <p>
     * Sets the status for the DNS record.
     * </p>
     *
     * @param status the status to be set
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * <p>
     * Returns the record type of the DNS record.
     * </p>
     *
     * @return the record type
     */
    public RecordType getRecordType() {
        return recordType;
    }

    /**
     * <p>
     * Returns the host name of the DNS record.
     * </p>
     *
     * @return the host name
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>
     * Returns the value of the DNS record.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Returns the status of the DNS record.
     * </p>
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * <p>
     * Returns the priority of the DNS record.
     * </p>
     *
     * @return the priority
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * <p>
     * Sets the priority for the DNS record.
     * </p>
     *
     * @param priority the priority to be set
     */
    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    /**
     * <p>
     * Returns the time-to-live (TTL) value of the DNS record.
     * </p>
     *
     * @return the TTL value
     */
    public Long getTtl() {
        return ttl;
    }

    /**
     * <p>
     * Sets the time-to-live (TTL) value for the DNS record.
     * </p>
     *
     * @param ttl the TTL value to be set
     */
    public void setTtl(final Long ttl) {
        this.ttl = ttl;
    }
}