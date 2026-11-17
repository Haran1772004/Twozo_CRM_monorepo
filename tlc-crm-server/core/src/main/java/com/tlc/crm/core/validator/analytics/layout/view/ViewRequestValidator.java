package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;

/**
 * <p>
 * Provides an abstract way to validate the view configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface ViewRequestValidator
{
    /**
     * <p>
     * Returns an instance of the view data request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for view data
     */
    default RequestValidator<JsonObject> viewDataValidator()
    {
        return ViewDataValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the view get request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for view data
     */
    default RequestValidator<JsonObject> viewGetValidator()
    {
        return ViewGetValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the view apply request validator
     * </p>
     *
     * @return the {@link ViewApplyValidator} instance for view apply
     */
    default RequestValidator<JsonObject> viewApplyValidator()
    {
        return ViewApplyValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the view underlying request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for view data
     */
    default RequestValidator<JsonObject> viewUnderlyingValidator()
    {
        return ViewUnderlyingValidator.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link ViewDataExportValidator}
     * </p>
     *
     * @return the {@link RequestValidator} instance for view data
     */
    default RequestValidator<JsonObject> exportDataValidator()
    {
        return ViewDataExportValidator.INSTANCE();
    }

    default RequestValidator<JsonObject> underlyingExportDataValidator()
    {
        return ViewUnderlyingDataExportValidator.INSTANCE();
    }
}
