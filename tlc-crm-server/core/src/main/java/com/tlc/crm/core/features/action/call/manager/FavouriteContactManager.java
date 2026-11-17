package com.tlc.crm.core.features.action.call.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.action.call.manager.impl.FavouriteContactManagerImpl;
import com.tlc.crm.core.features.action.call.model.FavouriteContact;

import java.util.Collection;

/**
 * Manager contract for {@link FavouriteContact} entries that link a user to the
 * contacts they have marked as favourites
 *
 * @author kishore J
 * @version 1.0
 */
public interface FavouriteContactManager extends ConfigManager<FavouriteContact> {

    /**
     * Gets the {@link FavouriteContactManager}
     *
     * @return The shared {@link FavouriteContactManager} instance.
     */
    static FavouriteContactManager getInstance() {
        return FavouriteContactManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the total number of favourite contacts marked by the given user
     * within the specified organization.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id whose favourite count is requested.
     * @return The count of favourite contacts for the user.
     */
    long getTotalRecords(final long orgId, final long userId);

    /**
     * <p>
     * Fetches the collection of {@link FavouriteContact} by the given user within the specified
     * organization, optionally paginated using the supplied {@link LimitConfig}.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param userId      Represents the user id whose favourites are to be loaded.
     * @param limitConfig Optional pagination configuration; when {@code null} no limit is applied.
     * @return Collection of {@link FavouriteContact} for the user.
     */
    Collection<FavouriteContact> get(final long orgId, final long userId, final LimitConfig limitConfig);
}
