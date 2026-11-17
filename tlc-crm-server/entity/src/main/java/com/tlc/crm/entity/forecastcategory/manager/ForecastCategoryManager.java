package com.tlc.crm.entity.forecastcategory.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.forecastcategory.manager.internal.ForecastCategoryManagerImpl;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Optional;

/**
 * Manager class for handling {@link ForecastCategory} related DB actions
 *
 * @author kishore J
 * @version 1.0
 */
public interface ForecastCategoryManager extends ConfigManager<ForecastCategory> {

    /**
     * <p>
     * Gets the {@link ForecastCategoryManager}
     * </p>
     *
     * @return The {@link ForecastCategoryManager}
     */
    static ForecastCategoryManager getInstance() {
        return ForecastCategoryManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the forecast category table
     * </p>
     *
     * @return The forecast category table
     */
    Table getTable();

    /**
     * <p>
     * Fetches the total number of forecast category records for the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The total count of forecast category records
     */
    long getTotalRecords(final long orgId);

    /**
     * <p>
     * Fetches a list of all forecast categories for the specified organization.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param limitConfig Represents the {@link LimitConfig}
     * @return A collection of {@link ForecastCategory}
     */
    Collection<ForecastCategory> get(final long orgId, final LimitConfig limitConfig);

    /**
     * <p>
     * Updates the default forecast category for the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the forecast category id
     */
    void updateDefaultCategory(final long orgId, final long id);

    /**
     * <p>
     * Fetches the default forecast category for the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The default {@link ForecastCategory}
     */
    ForecastCategory getDefaultCategory(final Long orgId);

    /**
     * <p>
     * Checks whether any of the given forecast category names already exist for the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param names The collection of names to verify for existence
     * @return true if at least one name exists in the organization, otherwise false
     */
    boolean nameAlreadyExists(final long orgId, final Collection<String> names);

    /**
     * <p>
     * Checks whether any of the given forecast category names already exist for the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Which contains the collection of ids
     * @param names The collection of names to verify for existence
     * @return true if at least one name exists in the organization, otherwise false
     */
    boolean nameAlreadyExists(final long orgId, final Collection<Long> ids, final Collection<String> names);

    /**
     * <p>
     * Fetches the commited forecast category id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The commited forecast category id
     */
    Optional<Long> getCommitedCategoryId(final long orgId);

    /**
     * <p>
     * Transfers the ownership of forecast category records from a set of source users to a target user.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param targetUserId  The ID of the user who will receive the records
     * @param sourceUserIds A collection of user IDs whose records will be transferred
     */
    void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds);

    /**
     * <p>
     * Fetches a list of all forecast categories for the specified organization.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @return A collection of {@link ForecastCategory}
     */
    Collection<ForecastCategory> getAll(final long orgId);

    /**
     * <p>
     * Fetches the name of the forecast category for the given organization and ID.
     * </p>
     *
     * @param orgId the unique identifier of the organization
     * @param id    the unique identifier of the forecast category
     * @return an {@link Optional} containing the forecast category name if found,
     * otherwise {@link Optional#empty()}
     */
    Optional<String> getName(final long orgId, final long id);
}
