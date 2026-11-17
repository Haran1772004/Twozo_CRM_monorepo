package com.tlc.notification.service.template;

import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.commons.template.model.TemplateModel;
import com.tlc.notification.model.NotifiableModel;

import java.util.Map;

/**
 * Represents the notification template data holder
 *
 * @param <T> The type of the {@link TemplateModel}
 * @author Kishore J
 * @version 1.0
 */
public abstract class NotificationTemplateBuildRequest<T extends NotifiableModel> extends DefaultTemplateBuildRequest<T> {

    protected NotificationTemplateBuildRequest(final Map<String, T> models) {
        super(models);
    }
}
