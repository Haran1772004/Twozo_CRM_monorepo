package com.tlc.crm.core.notification.email.api.manager;

import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.sql.common.api.dml.Table;

import java.util.Optional;

/**
 * <p>
 * Interface managing email domain filter preferences for users.
 * Provides methods to create, retrieve, update, and delete preferences.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public interface EmailDomainFilterManager {

    /**
     * <p>
     * Gets the primary table for storing email domain filter preferences.
     * </p>
     *
     * @return The {@link Table} containing email domain filter preferences.
     */
    Table getTable();

    /**
     * <p>
     * Creates or updates the {@link EmailDomainFilterPreference} for a given user.
     * If a preference already exists, it is updated; otherwise, a new record is created.
     * </p>
     *
     * @param orgId      Represents the organization ID.
     * @param preference The {@link EmailDomainFilterPreference} to create or update.
     */
    void save(final Long orgId, final EmailDomainFilterPreference preference);

    /**
     * <p>
     * Retrieves the {@link EmailDomainFilterPreference} for a given user within an organization.
     * </p>
     *
     * @param orgId  Represents the organization ID.
     * @param userId Represents the user ID for which the preference is retrieved.
     * @return An {@link Optional} containing the user's {@link EmailDomainFilterPreference}, or empty if not found.
     */
    Optional<EmailDomainFilterPreference> get(final Long orgId, final Long userId);

    /**
     * <p>
     * Deletes the {@link EmailDomainFilterPreference} for a given user within an organization.
     * </p>
     *
     * @param orgId  Represents the organization ID.
     * @param userId Represents the user ID whose preference should be deleted.
     */
    void delete(final Long orgId, final Long userId);
}