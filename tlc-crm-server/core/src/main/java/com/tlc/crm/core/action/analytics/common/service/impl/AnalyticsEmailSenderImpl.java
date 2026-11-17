package com.tlc.crm.core.action.analytics.common.service.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.core.action.analytics.common.service.AnalyticsEmailSender;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;

import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.keys.view.BaseFieldKeys;

import lombok.NonNull;

import java.util.List;

/**
 * <p>
 * The {@code AnalyticsEmailSender} is a singleton utility class responsible for building and sending
 * analytics-related email notifications via Amazon SES.
 * </p>
 *
 * @author Kathir
 * @version 1.0
 */
public final class AnalyticsEmailSenderImpl implements AnalyticsEmailSender
{
    private final AnalyticsExportViewServiceImpl viewService;
    private final NoReplyMailService noReplyMailService;

    private AnalyticsEmailSenderImpl()
    {
        this.viewService = AnalyticsExportViewServiceImpl.getInstance();
        this.noReplyMailService = NoReplyMailService.get();
    }

    private static class Instance
    {
        private static final AnalyticsEmailSenderImpl INSTANCE = new AnalyticsEmailSenderImpl();
    }

    public static AnalyticsEmailSenderImpl getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Builds and sends an email with the specified content and attachment for the given analytics request.
     *
     * <p> Sends an email message using Amazon SES via Kafka.
     * This method encodes the email message and publishes it to the appropriate Kafka topic
     * for processing and transmission through Amazon Simple Email Service (SES).</p>
     *
     * @param request     the {@link AnalyticsRequest} containing request context and org/user info.
     * @param requestData the JSON object containing additional request-specific data (e.g., view ID).
     * @param email     the content/body of the email message.
     */
    public void sendEmail( final @NonNull AnalyticsRequest request,
                           final @NonNull JsonObject requestData,
                           final @NonNull Email email )
    {
        final EmailRecipients emailRecipients = new EmailRecipients();
        final EmailAddress emailAddress = new EmailAddress( requestData.getString( CRMReportKeys.RECIPIENT_EMAIL ) );

        emailRecipients.addRecipient( AddressType.TO, emailAddress );
        email.setSubject( viewService.getViewName( request, requestData.getLong( BaseFieldKeys.VIEW_ID ) ) );
        email.setRecipients( List.of( emailRecipients ) );
        email.setContent( email.getContent() );

        noReplyMailService.send(email);
    }
}
