package com.tlc.crm.core.features.quote.manager;

import com.tlc.crm.core.features.quote.manager.impl.QuotationManagerImpl;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.model.QuotationTemplate;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Manager interface for handling database operations related to {@link Quotation} entities.
 * Covers full CRUD lifecycle and clone functionality.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface QuotationManager {

    /**
     * Returns the singleton instance of {@link QuotationManager}.
     *
     * @return singleton instance
     */
    static QuotationManager getInstance() {
        return QuotationManagerImpl.getInstance();
    }

    /**
     * Returns the primary quotation table.
     *
     * @return the {@link Table} representing the quotation entity
     */
    Table getTable();

    /**
     * Returns the join table that maps quotations to their associated contacts.
     *
     * @return the {@link Table} representing the contact-quotation relationship
     */
    Table getContactQuotationTable();

    /**
     * Returns the join table that maps quotations to their associated companies.
     *
     * @return the {@link Table} representing the company-quotation relationship
     */
    Table getCompanyQuotationTable();

    // ─────────────────────────── CREATE ───────────────────────────

    /**
     * Persists a new {@link Quotation} to the data store, including its
     * associated contacts and company.
     *
     * @param quotation the quotation to create
     */
    void create(final Quotation quotation);

    /**
     * Persists multiple {@link Quotation} entities to the data store in a single transaction.
     * v
     *
     * @param orgId      the organisation ID
     * @param quotations the collection of quotations to create
     */
    void bulkCreate(final long orgId, final Collection<Quotation> quotations);

    /**
     * Generates the next sequential quotation number for the given organisation.
     *
     * @param orgId the organisation ID
     * @return the next quotation number
     */
    int generateQuotationNumber(final long orgId);

    // ─────────────────────────── READ ─────────────────────────────

    /**
     * Checks whether a quotation with the supplied identifier exists within the
     * given organisation scope.
     *
     * <p>Implementations should perform an efficient existence check (for example
     * via a count query or a key lookup) scoped to {@code orgId} so callers can
     * safely determine presence without retrieving the full entity.</p>
     *
     * @param orgId   the organisation id to scope the lookup; may be {@code null}
     *                in contexts where organisation scoping is not required
     * @param quoteId the quotation id to check for existence; must not be {@code null}
     * @return {@code true} if the quotation exists within the given organisation,
     *         otherwise {@code false}
     */
    boolean exists(final Long orgId, final Long quoteId);

    /**
     * Fetches a single {@link Quotation} by its ID within the given organisation.
     *
     * @param orgId the organisation ID
     * @param id    the quotation ID
     * @return the matching {@link Quotation}
     * @throws com.tlc.commons.code.ErrorCode if the quotation is not found
     */
    Quotation get(final long orgId, final long id);

    /**
     * Fetches multiple {@link Quotation} records by their IDs within the given organisation.
     *
     * @param orgId the organisation ID
     * @param ids   collection of quotation IDs to fetch
     * @return collection of matching {@link Quotation} instances
     */
    Collection<Quotation> get(final long orgId, final Collection<Long> ids);

    // ─────────────────────────── UPDATE ───────────────────────────

    /**
     * Updates an existing {@link Quotation} in the data store, including
     * syncing its associated contacts and company.
     *
     * @param quotation the quotation containing updated data
     */
    void update(final Quotation quotation);

    /**
     * Updates multiple {@link Quotation} entities in the data store in a single transaction.
     *
     * @param quotations the collection of quotations to update
     */
    void bulkUpdate(final long orgId, final Collection<Quotation> quotations);

    /**
     * Updates only the status of an existing {@link Quotation}.
     *
     * @param orgId  the organisation ID
     * @param userId the user id
     * @param id     the quotation ID
     * @param status the new status to set
     */
    void updateStatus(final long orgId, final long userId, final long id, final Quotation.Status status);

    // ─────────────────────────── DELETE ───────────────────────────

    /**
     * Deletes a {@link Quotation} by its ID. Associated contact and company
     * join rows are removed automatically via cascade.
     *
     * @param orgId the organisation ID
     * @param id    the quotation ID to delete
     */
    void delete(final long orgId, final long id);

    // ─────────────────────────── CLONE ────────────────────────────

    /**
     * Creates and persists a deep copy of an existing {@link Quotation}.
     * The cloned quotation is assigned a new ID, a new UUID, and its
     * status is reset to {@link Quotation.Status#DRAFT}.
     *
     * @param orgId the organisation ID
     * @param id    the ID of the quotation to clone
     * @return the newly created cloned {@link Quotation}
     */
    Quotation clone(final long orgId, final long id);

    // ─────────────────────────── TEMPLATE MAPPING ────────────────────────────

    /**
     * Creates a mapping between an organization and the default quotation template.
     * This enables the organization to use the specified template for creating quotations.
     *
     * @param orgId       the organisation ID
     * @param userId      the CRM user ID performing the action
     * @param templateIds the template IDs to map to the organization
     */
    void createDefaultTemplateMapping(final long orgId, final long userId, final Collection<Long> templateIds);

    /**
     * Fetches the default quotation template ID from the system.
     * The default template is identified by having IS_DEFAULT = true and STATUS = 1 (active).
     * Since the default template is global across all organizations, no orgId is required.
     *
     * @return the default templates or empty list if not found
     */
    Collection<QuotationTemplate> getDefaultTemplates();

    /**
     * Retrieves the default quotation template for a specific organization.
     *
     * @param orgId the organization ID for which to fetch the default template
     * @return an Optional containing the default QuotationTemplate if found, 
     *         or Optional.empty() if no default template is configured for the organization
     */
    Optional<QuotationTemplate> getDefaultTemplate(final long orgId);

    // ─────────────────────────── STATUS UPDATE ────────────────────────────

    /**
     * Loads the status update of a {@link Quotation} into the provided container.
     * This method is useful when you want to update the status as part of a larger transaction.
     *
     * @param orgId           the organization ID
     * @param id              the quotation ID
     * @param status          the new status to set
     * @param targetContainer the container to load the status update into
     */
    void loadStatusUpdateIntoContainer(final long orgId, final long id, final Quotation.Status status,
                                        final DataContainer targetContainer);

    /**
     * Retrieves the current status of a quotation.
     *
     * @param orgId       the organization ID
     * @param quotationId the quotation ID to check
     * @return an {@link Optional} containing the current {@link Quotation.Status} of the quotation,
     *         or {@link Optional#empty()} if quotation not found
     */
    Optional<Quotation.Status> getStatus(final long orgId, final long quotationId);

    /**
     * Retrieves the quote file IDs associated with the given organization and quote ID.
     *
     * @param orgId the organization identifier
     * @param id    the quote identifier
     * @return an {@link Collection} containing the quote file IDs if available, otherwise empty
     */
    Collection<Long> getQuoteFileIds(final long orgId, final long id);

    /**
     * Retrieves the associated company and contact IDs for each of the given quotations,
     * grouped by their {@link Source}.
     *
     * <p>
     * The returned map is keyed by quotation ID. Each value is a nested map whose keys
     * are {@link Source} values (e.g., company source, contact source) and whose values
     * are the collections of entity IDs belonging to that source for the respective quotation.
     * </p>
     *
     * @param orgId        the organisation ID
     * @param quotationIds the collection of quotation IDs for which to fetch associations
     * @return a map of quotation ID → (source → collection of associated entity IDs);
     *         never {@code null}, but may be empty if no associations are found
     */
    Map<Long, Map<Source, Collection<Long>>> getCompanyAndContactIds(final long orgId, final Collection<Long> quotationIds);
}