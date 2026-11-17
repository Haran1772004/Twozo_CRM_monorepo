package com.tlc.crm.entity.list.summary.model;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * Represents the summary for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ListSummary {

    /**
     * <p>
     * Provides the type of the summary.
     * </p>
     *
     * @return {@link SummaryType}.
     */
    SummaryType getType();

    /**
     * <p>
     * Fetches the summary of the list API.
     * </p>
     *
     * @return Summary details as {@link JsonObject}.
     */
    <T> T getDetails();
}