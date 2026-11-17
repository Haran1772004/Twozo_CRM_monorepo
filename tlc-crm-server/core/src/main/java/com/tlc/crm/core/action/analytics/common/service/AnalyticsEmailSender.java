package com.tlc.crm.core.action.analytics.common.service;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.json.JsonObject;

import com.tlc.email.common.model.type.Email;

import lombok.NonNull;

/**
 * Interface for handling the sending of analytics-related emails.
 */
public interface AnalyticsEmailSender
{

    /**
     * Sends an analytics email based on the provided request and request data.
     *
     * @param request     Represents the {@link AnalyticsRequest} details
     * @param requestData The JsonObject containing the request data, must not be null
     * @param email       the {@link Email} object representing the email to be sent
     */
    void sendEmail( @NonNull final AnalyticsRequest request,
                    @NonNull final JsonObject requestData,
                    @NonNull final Email email );
}