package com.tlc.notification.internal.template.mail.model;

import com.tlc.commons.internal.template.model.VelocityTemplateWrapper;
import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.template.model.TemplateBuildRequest;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.notification.service.template.EmailTemplateBuildRequest;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.DateTool;

import java.util.Objects;
import java.util.TimeZone;

/**
 * <p>
 * A wrapper for Apache Velocity Template to facilitate email template formatting with provided models
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class VelocityEmailTemplateWrapper extends VelocityTemplateWrapper {

    public VelocityEmailTemplateWrapper(final Template template) {
        super(template);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateBuildRequest Represents the {@link DefaultTemplateBuildRequest}
     * @return The {@link VelocityContext}
     */
    @Override
    protected <T extends TemplateModel> VelocityContext buildVelocityContext(final TemplateBuildRequest<T> templateBuildRequest) {
        final VelocityContext context = new VelocityContext();

        final EmailTemplateBuildRequest emailNotificationData = (EmailTemplateBuildRequest) templateBuildRequest;
        final EmailNotifiableModel emailNotifiableModel = emailNotificationData.get(NotificationKeys.MODEL.getKey());

        context.put(NotificationKeys.TEMPLATE.getKey(), emailNotificationData);

        if (Objects.nonNull(emailNotifiableModel) && emailNotifiableModel.hasDateTimeField()) {
            context.put(NotificationKeys.DATE.getKey(), new DateTool());
            context.put(NotificationKeys.TIME_ZONE.getKey(), TimeZone.class);
        }

        return context;
    }
}
