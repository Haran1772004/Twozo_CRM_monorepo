package com.tlc.crm.core.features.api.quota.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.core.features.api.quota.model.Quota.RelatedEntityType;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaPeriod;
import com.tlc.crm.core.features.api.quota.model.QuotaType;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Defines the contract for {@link QuotaHolder} management operations, including
 * retrieval, creation, attainment recalculation, ownership checks, and duplicate detection.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see QuotaHolderManagerImpl
 */
public interface QuotaHolderManager extends ConfigManager<QuotaHolder> {

    static QuotaHolderManager getInstance() {
        return QuotaHolderManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link QuotaMetric} for the given quota id.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param quotaId Represents the quota id
     * @return The {@link QuotaMetric}
     */
    QuotaMetric getQuotaMetric(final long orgId, final long quotaId);

    /**
     * <p>
     * Gets the count of assigned quotas for the given user ids. It considers both direct user
     * quotas and team quotas assigned to the users.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Which contains the collection of user ids
     * @return A map where the key is the user ID and the value is the total count of assigned quotas
     */
    Map<Long, Long> getUserAssignedQuotaCount(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Recalculates the quota attainment for the given quota id and user id.
     * If the user id is null, recalculates for all users under that quota.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents the quota id
     * @param userId  Represents the user id; if null, recalculates for all users under the quota
     */
    void recalculateQuotaAttainment(final long orgId, final long quotaId, final Long userId);

    /**
     * <p>
     * Checks whether the specified quota has been recalculated within the given time threshold.
     * </p>
     *
     * @param orgId   the organization ID
     * @param quotaId the quota ID
     * @param userId  the user ID (applicable for team quota checks)
     * @param time    the threshold time in milliseconds
     * @return true if the quota was recalculated after the given time; otherwise false
     */
    boolean isQuotaRecentlyRecalculated(final long orgId, final long quotaId, final Long userId, final Long time);

    /**
     * <p>
     * Gets the {@link QuotaType} for the given quota id.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the quota id
     * @return The {@link QuotaType}
     */
    QuotaType getQuotaType(final long orgId, final Long id);

    /**
     * Gets the active period of the given quota used to calculate its attainment.
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents the quota id
     * @return The {@link QuotaPeriod}, or null if the quota does not exist
     */
    QuotaPeriod getQuotaPeriod(final long orgId, final long quotaId);

    /**
     * Fetches the ids of the given related entity type configured on the quota.
     * An empty collection means the quota is not restricted on that entity type.
     *
     * @param orgId   Represents an organization id
     * @param quotaId Represents the quota id
     * @param relatedEntityType Represents the {@link RelatedEntityType}
     * @return The collection of related entity ids, empty when there is no restriction
     */
    Collection<Long> fetchRelatedEntityIds(final long orgId, final long quotaId, final RelatedEntityType relatedEntityType);

    /**
     * <p>
     * Checks if the current user is the owner (creator) of the quota.
     * </p>
     *
     * @param orgId   Represents the organization Id
     * @param userId  Represents the user Id
     * @param quotaId Represents the quota Id
     * @return true if the current user created the quota
     */
    boolean isQuotaOwner(final long orgId, final long userId, final Long quotaId);

    /**
     * <p>
     * Checks if the quota belongs to the specified user.
     * </p>
     *
     * @param orgId   Represents the organization Id
     * @param userId  Represents the user Id
     * @param quotaId Represents the quota Id
     * @return true if the quota belongs to the user
     */
    boolean isUserQuota(final long orgId, final long userId, final Long quotaId);

    /**
     * <p>
     * Checks if the quota belongs to any of the specified teams.
     * </p>
     *
     * @param orgId       Represents the organization Id
     * @param userTeamIds Represents the collection of user's team Ids
     * @param quotaId     Represents the quota Id
     * @return true if the quota belongs to any of the user's teams
     */
    boolean isTeamQuota(final long orgId, final Collection<Long> userTeamIds, final Long quotaId);

    /**
     * <p>
     * Checks whether a duplicate {@link QuotaHolder} exists, applying directional
     * containment and configuration-aware duplicate rules.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param quotaHolders Which contains the collection of {@link QuotaHolder}
     * @param excludeCurrentHolder  True to exclude each quota holder's own id from the duplicate search
     * @return true if a matching duplicate quota exists, otherwise false
     */
    boolean isQuotaHolderExists(final long orgId, final Collection<QuotaHolder> quotaHolders, final boolean excludeCurrentHolder);
}