package com.tlc.crm.core.api.deal.model;

/**
 * An interface holds the forecast fields data
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ForecastFieldHolder {

    /**
     * <p>
     * Fetches the id of the forecast field holder
     * </p>
     *
     * @return The forecast field id
     */
    short id();

    /**
     * <p>
     * Fetches the name of the forecast field holder
     * </p>
     *
     * @return The forecast field name
     */
    String getName();
}
