package com.tlc.notification.service.template;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import java.util.Map;

/**
 * Represents the email template details
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailTemplateBuildRequest extends NotificationTemplateBuildRequest<EmailNotifiableModel> {

    public EmailTemplateBuildRequest(final Map<String, EmailNotifiableModel> models) {
        super(models);
    }
}
