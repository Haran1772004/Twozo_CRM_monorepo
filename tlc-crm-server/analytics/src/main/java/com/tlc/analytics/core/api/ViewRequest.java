package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.crm.common.action.CrmRequest;

import lombok.NonNull;

/**
 * Interface defining a view request, representing a request made by a user to access a specific view.
 *
 * @author Aswath
 * @version 1.0
 */
public interface ViewRequest<V extends ViewModel>
{
    /**
     * Retrieves the view associated with this request.
     *
     * @return The view associated with this request. Must not be null.
     */
    @NonNull
    V getView();

    /**
     * Retrieves the actual request associated with this view request.
     *
     * @return The actual request. Must not be null.
     */
    @NonNull
    CrmRequest getRequest();

    /**
     * Checks whether the current view is new or not.
     *
     * @return true if the view is new, false otherwise
     */
    boolean isNewView();

    /**
     * Retrieves the organization ID associated with this view request.
     *
     * @return The organization ID associated with this view request.
     */
    @NonNull
    default Long getOrgId()
    {
        return getRequest().orgId();
    }

    /**
     * Retrieves the user ID associated with this view request.
     *
     * @return The user ID associated with this view request.
     */
    @NonNull
    default Long getUserId()
    {
        return getRequest().userId();
    }
}
