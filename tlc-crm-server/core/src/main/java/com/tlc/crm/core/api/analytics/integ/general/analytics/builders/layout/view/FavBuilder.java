package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.view;

import com.tlc.analytics.config.model.view.fav.Favourite;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Responsible for building instances of {@link Favourite} objects.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FavBuilder
{
    private static class Instance
    {
        private static final FavBuilder INSTANCE = new FavBuilder();
    }

    public static FavBuilder INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Creates and returns a new Favourite object with the specified view ID, organization ID, and user ID.
     *
     * @param viewId the ID of the view that is marked as a favorite
     * @param orgId  the ID of the organization to which the favorite belongs
     * @param userId the ID of the user who has marked the view as a favorite
     * @return a new instance of Favourite with the provided details
     */
    @NonNull
    protected static Favourite getFav( final Long viewId, final Long orgId, final Long userId )
    {
        return Favourite.builder()
                        .viewId( viewId )
                        .orgId( orgId )
                        .userId( userId )
                        .build();
    }
}
