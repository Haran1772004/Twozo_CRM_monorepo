package com.tlc.email.client.template.mail;

import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlElements;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.builder.TemplateBuilder;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;

import com.tlc.notification.service.template.EmailTemplateBuildRequest;
import com.tlc.notification.service.template.NotificationBuilder;

import java.util.Optional;

/**
 * <p>
 * Implements the {@link NotificationBuilder} for building {@link Email} from provided {@link EmailTemplateBuildRequest}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailNotificationBuilder implements NotificationBuilder<EmailTemplateBuildRequest, Email> {

    private static final String DIV_WITH_DATA_INFO_GROUP = "div[data-info-group]";
    private static final String DATA_INFO_GROUP = "data-info-group";
    private static final String SUBJECT = "subject";

    private final HtmlParser htmParser;
    private final TemplateBuilder<EmailTemplateBuildRequest> templateBuilder = TemplateBuilder.getInstance();

    private EmailNotificationBuilder() {
        htmParser = HtmlParser.getInstance();
    }

    private static class InstanceHolder {
        private static final EmailNotificationBuilder EMAIL_NOTIFICATION_BUILDER = new EmailNotificationBuilder();
    }

    /**
     * <p>
     * Gets the {@link EmailNotificationBuilder} instance
     * </p>
     *
     * @return The {@link EmailNotificationBuilder}
     */
    public static EmailNotificationBuilder getInstance() {
        return InstanceHolder.EMAIL_NOTIFICATION_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param templateName            Represents the template name
     * @param emailTemplateDataHolder Represents the {@link EmailTemplateBuildRequest}
     * @return An Optional containing {@link Email} if it exists, or an empty Optional if the template is not found
     */
    @Override
    public Optional<Email> build(final String templateName, final EmailTemplateBuildRequest emailTemplateDataHolder) {
        return templateBuilder.build(templateName, emailTemplateDataHolder).flatMap(this::build);
    }

    /**
     * <p>
     * Builds the {@link Email} from the template content
     * </p>
     *
     * @param templateContent Represents the template content
     * @return An Optional containing {@link Email} if it exists, or an empty Optional if the template is not found
     */
    private Optional<Email> build(final String templateContent) {
        final HtmlElements htmlElements = htmParser.parse(templateContent).select(DIV_WITH_DATA_INFO_GROUP);

        if (htmlElements.isEmpty()) {
            return Optional.empty();
        }
        final Email email = new OutboundEmail();

        for (final HtmlElement htmlElement : htmlElements.getElements()) {

            if (SUBJECT.equals(htmlElement.attribute(DATA_INFO_GROUP))) {
                email.setSubject(htmlElement.htmlString());
                break;
            }
        }
        email.setContent(templateContent);

        return Optional.of(email);
    }
}
