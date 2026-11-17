package com.tlc.crm.entity.util;

import com.tlc.crm.entity.util.impl.TlcModelHandlerImpl;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * Provides services that involve comparing and merging similar activities within models
 *
 * @author sundar
 * @version 1.0
 */
public interface TlcModelHandler {

    /**
     * Gets the instance of {@link TlcModelHandler}
     *
     * @return {@link TlcModelHandler}
     */
    static TlcModelHandler getInstance() {
        return TlcModelHandlerImpl.getInstance();
    }

    /**
     * Fetches the field names/ID strings that have been updated with the new value
     *
     * @param primaryModel    Represents the primary model
     * @param secondaryModel  Represents the secondary model
     * @param isPartialUpdate A flag indicating whether the update action is performing partially or not
     * @return {@link Collection} of strings represents the deferred value field name/ID
     */
    Collection<String> fetchUpdatedValueFields(final TlcModel primaryModel, final TlcModel secondaryModel, final boolean isPartialUpdate);

    /**
     * <ul>
     *  <li>
     *      Compares the currentModel and the comparableModel. If these two models have the same values, the currentModel value is merged with the previousModel value.
     *  </li>
     *  <li>
     *      When there are differences in field values between the primary and comparable models, the original value of the primary model's field remains unchanged.
     *  </li>
     * </ul>
     *
     * @param currentModel    The model containing values fetched from the database.
     * @param comparableModel The model containing values equal to those in the currentModel or
     *                        representing the state before any updates.
     * @param previousModel   The model holding previously stored values,
     *                        to be merged with the currentModel if it shares similar values
     *                        with the comparableModel for a specific field.
     */
    void compareAndMerge(final TlcModel currentModel, final TlcModel comparableModel, final TlcModel previousModel);
}
