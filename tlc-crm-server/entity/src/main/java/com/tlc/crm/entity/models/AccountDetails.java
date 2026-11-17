package com.tlc.crm.entity.models;

/**
 * Represents a data transfer object (DTO) used for storing account details.
 *
 * @author Maharaja S
 * @version 1.0
 */
public record AccountDetails(long orgId, Long userId, String name, String organizationName, String mobile, String email) {

    public AccountDetails(final long orgId, final Long userId, final String name, final String organizationName, final String mobile) {
        this(orgId, userId, name, organizationName, mobile, null);
    }
}