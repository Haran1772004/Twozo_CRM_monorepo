package com.tlc.crm.core.notification.email.api.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.core.notification.email.api.service.impl.CrmEmailDomainServiceImpl;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Service interface for managing email domains within an organization.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public interface CrmEmailDomainService {

    static CrmEmailDomainService get() {
        return CrmEmailDomainServiceImpl.getInstance();
    }

    /**
     * <p>
     * Saves or updates the given {@link CrmEmailDomain} for the specified organization.
     * </p>
     *
     * @param orgId          organization ID
     * @param crmEmailDomain the domain entity to save
     */
    void save(final Long orgId, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Adds a new domain for email handling using the provided state data.
     * </p>
     *
     * @param crmEmailDomain the domain entity to add
     * @param stateData      additional state information required for domain creation
     */
    void buildAndPublishDomainRequest(final CrmEmailDomain crmEmailDomain, final JsonObject stateData);

    /**
     * <p>
     * Updates an existing domain for the specified organization.
     * </p>
     *
     * @param orgId          organization ID
     * @param crmEmailDomain the domain entity to update
     */
    void updateDomain(final Long orgId, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Deletes a domain by ID for the specified organization.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID to delete
     */
    void deleteDomain(final Long orgId, final Long id);

    /**
     * <p>
     * Update the state for the given domain id.
     * </p>
     *
     * @param orgId    organization ID
     * @param id       domain ID to delete
     * @param isActive represent whether the state is active or not.
     */
    void updateState(final Long orgId, final Long id, final boolean isActive);

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by organization ID and domain ID.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID
     * @return optional {@link CrmEmailDomain} if found
     */
    Optional<CrmEmailDomain> get(final Long orgId, final Long id);

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by organization ID and domain name.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName domain name to look up
     * @return optional {@link CrmEmailDomain} if found
     */
    Optional<CrmEmailDomain> get(final Long orgId, final String domainName);

    /**
     * <p>
     * Sends DNS records of the given domain via email to the specified recipients.
     * </p>
     *
     * @param recipients     collection of recipient email addresses
     * @param crmEmailDomain the domain whose DNS records should be sent
     */
    void sendDnsRecordsViaMail(final Collection<String> recipients, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Checks if a domain exists for the specified organization by domain ID.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID
     * @return {@code true} if domain exists, {@code false} otherwise
     */
    boolean exists(final Long orgId, final Long id);

    /**
     * <p>
     * Checks if a domain exists for the specified organization by domain name.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName domain name
     * @return {@code true} if domain exists, {@code false} otherwise
     */
    boolean exists(final Long orgId, final String domainName);

    /**
     * <p>
     * Retrieves the internal ID of a domain for a given organization.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName Represent the domain name
     * @return optional internal ID if found
     */
    Optional<Long> getId(final Long orgId, final String domainName);

    /**
     * <p>
     * Retrieves an existing domain for the given organization based on the user's email address,
     * or initializes and persists a new one if it does not exist.
     * </p>
     *
     * @param orgId        Represents the organization ID.
     * @param userId       Represents the user ID.
     * @param emailAddress Represents the user's email address.
     * @return existing or newly created {@link CrmEmailDomain}.
     */
    CrmEmailDomain getOrInitDomainByEmail(final Long orgId, final Long userId, final String emailAddress);

    /**
     * <p>
     * Validates an EntityDomain by checking that its default email addresses match the domain,
     * and appends the domain to any missing addresses.
     * </p>
     *
     * @param orgId          Represents the organization ID.
     * @param crmEmailDomain Represents the crm domain.
     * @param domainConfig   Represents the domain config.
     */
    void validateDomainConfig(final Long orgId, final CrmEmailDomain crmEmailDomain, final DomainConfig domainConfig);

    /**
     * <p>
     * Updates the {@link DomainConfig} for the given domain.
     * </p>
     *
     * @param domainConfig Represents the domain config to update.
     */
    void updateDomainConfig(final DomainConfig domainConfig);

    /**
     * <p>
     * Retrieves the {@link DomainConfig} for the given organization and domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID.
     * @return An {@link Optional} containing the {@link DomainConfig} if found, or empty otherwise.
     */
    Optional<DomainConfig> getDomainConfig(final Long orgId, final Long domainId);

    /**
     * <p>
     * Retrieves the total number of active, non-new entity domains for a given organization.
     * </p>
     *
     * @param orgId Represents the organization ID.
     * @return the total count of domains.
     */
    long getDomainCount(final long orgId);

    /**
     * <p>
     * Updates the status for the domain.
     * </p>
     *
     * @param orgId        organization ID
     * @param id           domain ID
     * @param domainStatus Represents the domain status.
     */
    void updateDomainStatus(final Long orgId, final Long id, final DomainStatus domainStatus);
}