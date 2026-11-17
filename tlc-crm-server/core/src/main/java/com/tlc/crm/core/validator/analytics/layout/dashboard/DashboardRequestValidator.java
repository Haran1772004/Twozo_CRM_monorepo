package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout request configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface DashboardRequestValidator
{
    /**
     * <p>
     * Returns an instance of the dashboard create request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for creating dashboards
     */
    default RequestValidator<JsonObject> dashCreateValidator()
    {
        return DashboardCreateValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns the validator for dashboard update requests
     * </p>
     *
     * @return the {@link RequestValidator} instance for updating dashboards
     */
    default RequestValidator<JsonObject> dashUpdateValidator()
    {
        return DashboardUpdateValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns the validator for dashboard get requests
     * </p>
     *
     * @return the {@link RequestValidator} instance for getting dashboards
     */
    default RequestValidator<JsonObject> dashGetValidator()
    {
        return DashboardGetValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns the validator for dashboard clone requests
     * </p>
     *
     * @return the {@link RequestValidator} instance for getting dashboards
     */
    default RequestValidator<JsonObject> dashCloneValidator()
    {
        return DashboardCloneValidator.INSTANCE();
    }

    /**
     * <p>
     * Gets the validator for dashboard move operation
     * </p>
     *
     * @return The {@link RequestValidator} instance for move dashboard
     */
    default RequestValidator<JsonObject> dashMoveValidator()
    {
        return DashboardTransferValidator.getInstance();
    }

    /**
     * <p>
     * Gets the validator for dashboard favourite operation
     * </p>
     *
     * @return The {@link RequestValidator} instance for move dashboard
     */
    default RequestValidator<JsonObject> dashFavouriteValidator()
    {
        return FavouriteValidator.INSTANCE();
    }

    /**
     * <p>
     * Gets the validator for dashboard trash operation
     * </p>
     *
     * @return The {@link RequestValidator} instance for trash dashboard
     */
    default RequestValidator<JsonObject> dashboardTrashValidator()
    {
        return DashboardTrashValidator.getInstance();
    }

    /**
     * <p>
     * Gets the validator for dashboard restore operation
     * </p>
     *
     * @return The {@link RequestValidator} instance for restore dashboard
     */
    default RequestValidator<JsonObject> dashboardRestoreValidator()
    {
        return DashboardRestoreRequestValidator.getInstance();
    }

    /**
     * <p>
     * Gets the validator for dashboard delete operation
     * </p>
     *
     * @return The {@link RequestValidator} instance for delete dashboard
     */
    default RequestValidator<JsonObject> dashboardDeleteValidator()
    {
        return DashboardDeleteRequestValidator.INSTANCE();
    }
}
