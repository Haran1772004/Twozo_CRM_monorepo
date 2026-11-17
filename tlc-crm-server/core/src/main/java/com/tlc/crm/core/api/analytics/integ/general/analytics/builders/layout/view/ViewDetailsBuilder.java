package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.view;

import com.tlc.analytics.config.model.view.ViewDetails;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Contract for building {@link ViewDetails} within the CRM reporting framework.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ViewDetailsBuilder
{
    private static class Instance
    {
        private static final ViewDetailsBuilder INSTANCE = new ViewDetailsBuilder();
    }

    public static ViewDetailsBuilder INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Creates and returns a {@link ViewDetails} object with the specified attributes.
     *
     * @param orgId        the ID of the organization the view belongs to
     * @param createdBy    the ID of the user who created the view
     * @param modifiedBy   the ID of the user who last modified the view
     * @param createdTime  the timestamp when the view was created
     * @param modifiedTime the timestamp when the view was last modified
     * @param viewTypeId   the type ID of the view (indicating the type of view)
     * @param displayName  the display name of the view
     * @param description  the description of the view
     * @return a ViewDetails object containing all the provided information
     */
    protected static ViewDetails getViewDetails( final Long orgId,
                                                 final Long createdBy,
                                                 final Long modifiedBy,
                                                 final Long createdTime,
                                                 final Long modifiedTime,
                                                 final Integer viewTypeId,
                                                 final String displayName,
                                                 final String description,
                                                 final boolean moveToTrash )
    {
        return ViewDetails.builder()
                          .orgId( orgId )
                          .createdBy( createdBy )
                          .modifiedBy( modifiedBy )
                          .createdTime( createdTime )
                          .modifiedTime( modifiedTime )
                          .isDeleted( false )
                          .isHidden( false )
                          .isCustomView( true )
                          .viewTypeId( viewTypeId )
                          .displayName( displayName )
                          .description( description )
                          .moveToTrash( moveToTrash )
                          .build();
    }
}
