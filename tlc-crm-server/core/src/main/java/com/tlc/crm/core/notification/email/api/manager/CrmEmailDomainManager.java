package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Interface managing domain handling actions.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public interface CrmEmailDomainManager {

    /**
     * <p>
     * Gets the primary table.
     * </p>
     *
     * @return The {@link Table} containing Domain.
     */
    Table getTable();

    /**
     * <p>
     * Persists a new {@link CrmEmailDomain} into the database.
     * </p>
     *
     * @param orgId          Represent the orgId.
     * @param crmEmailDomain The {@link CrmEmailDomain} to be created.
     */
    void create(final Long orgId, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Updates an existing {@link CrmEmailDomain} in the database.
     * </p>
     *
     * @param orgId          Represent the orgId.
     * @param crmEmailDomain The {@link CrmEmailDomain} to be updated.
     */
    void update(final Long orgId, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Deletes an {@link CrmEmailDomain} from the database by its CRM ID for the specified organization.
     * </p>
     *
     * @param orgId Represents the organization ID to which the domain belongs.
     * @param id    The CRM ID of the {@link CrmEmailDomain} to delete.
     */
    void delete(final Long orgId, final Long id);

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by its unique identifier.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param id    The CRM ID of the entity domain to retrieve.
     * @return An {@link Optional} containing the entity domain if found, or empty otherwise.
     */
    Optional<CrmEmailDomain> get(final Long orgId, final Long id);

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by its domain name.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param name  Represents the domain name.
     * @return An {@link Optional} containing the entity domain if found, or empty otherwise.
     */
    Optional<CrmEmailDomain> get(final Long orgId, final String name);

    /**
     * <p>
     * Retrieves a paged collection of {@link CrmEmailDomain} objects.
     * </p>
     *
     * @param orgId              Represents the orgId.
     * @param start              Represents the start offset.
     * @param limit              Represents the max number of results.
     * @param additionalCriteria Represents the additional criteria.
     * @return A collection of {@link CrmEmailDomain} objects.
     */
    Collection<CrmEmailDomain> get(final Long orgId, final int start, final int limit, final WhereClause additionalCriteria);

    /**
     * <p>
     * Retrieves multiple {@link CrmEmailDomain} objects for a collection of IDs.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param ids   A collection of entity domain CRM IDs.
     * @return A collection of {@link CrmEmailDomain} objects found for the given IDs.
     */
    Collection<CrmEmailDomain> get(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Checks whether a domain exists by its CRM ID.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param id    Represent the domain id.
     * @return Whether the domain exists.
     */
    boolean exists(final Long orgId, final Long id);

    /**
     * <p>
     * Checks whether a domain exists by its name.
     * </p>
     *
     * @param orgId Represents the orgId.
     * @param name  Represent the domain name.
     * @return Whether the domain exists.
     */
    boolean exists(final Long orgId, final String name);

    /**
     * <p>
     * Retrieves the total number of active, non-new entity domains for a given organization.
     * </p>
     *
     * @param orgId              Represents the organization ID.
     * @param additionalCriteria Represents the additional criteria.
     * @return the total count of domains.
     */
    long getDomainCount(final Long orgId, final WhereClause additionalCriteria);

    /**
     * <p>
     * Retrieves the CRM primary key of a domain given its domain name.
     * </p>
     *
     * @param orgId      Represents the organization ID.
     * @param domainName Represents the domainName.
     * @return An {@link Optional} containing the CRM primary key if found, or empty otherwise.
     */
    Optional<Long> getId(final Long orgId, final String domainName);

    /**
     * <p>
     * Builds the domain for the given id from the container.
     * </p>
     *
     * @param id            Represents the id.
     * @param dataContainer Represents the data container.
     * @return The entity domain for the id and the container.
     */
    Optional<CrmEmailDomain> getDomain(final Long id, final DataContainer dataContainer);

    /**
     * <p>
     * Updates the status for the domain.
     * </p>
     *
     * @param orgId        organization ID
     * @param id           domain ID
     * @param domainStatus Represents the domain status.
     */
    void updateStatus(final Long orgId, final Long id, final DomainStatus domainStatus);

    /**
     * <p>
     * Loads domain data into the target container for the given org and event type.
     * </p>
     *
     * @param orgId           Represents the orgId.
     * @param isCreateEvent   Represents whether this is a create event.
     * @param targetContainer Represents the target container.
     * @param crmEmailDomain  The {@link CrmEmailDomain} to load.
     */
    void loadDomainDataIntoContainer(final Long orgId, final boolean isCreateEvent,
                                     final DataContainer targetContainer, final CrmEmailDomain crmEmailDomain);

    /**
     * <p>
     * Updates the active state for the given domain id.
     * </p>
     *
     * @param orgId    organization ID
     * @param id       domain ID
     * @param isActive Represents whether the state is active or not.
     */
    void updateState(final Long orgId, final Long id, final boolean isActive);
}