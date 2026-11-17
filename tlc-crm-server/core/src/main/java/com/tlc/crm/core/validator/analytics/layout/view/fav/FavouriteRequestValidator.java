package com.tlc.crm.core.validator.analytics.layout.view.fav;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.validator.analytics.RequestValidator;
import org.apache.poi.ss.formula.functions.T;

/**
 * <p>
 * Provides an abstract way to validate the favourite configuration
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface FavouriteRequestValidator
{
    /**
     * <p>
     * Returns an instance of the favourite request validator
     * </p>
     *
     * @return the {@link RequestValidator} instance for view data
     */
    default RequestValidator<JsonObject> favouriteValidator()
    {
        return FavouriteValidator.INSTANCE();
    }
}
