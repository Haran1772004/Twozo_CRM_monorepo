package com.tlc.crm.core.api.deal.model.converter.mobile;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.deal.model.Deal;

/**
 * <p>
 * Converts {@link Deal} into Json and vice versa.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface DealConverterMobileClient {

    /**
     * <p>
     * Gets the {@link DealConverterMobileClientImpl} instance.
     * </p>
     *
     * @return {@link DealConverterMobileClientImpl}.
     */
    static DealConverterMobileClient getInstance() {
        return DealConverterMobileClientImpl.getInstance();
    }

    /**
     * <p>
     * Converts the {@link Deal} into {@link JsonObject} for summary response.
     * </p>
     *
     * @param deal            {@link Deal}.
     * @param converterConfig {@link ConverterConfig}.
     * @return {@link JsonObject}.
     */
    JsonObject convertSummary(final Deal deal, final ConverterConfig converterConfig);
}
