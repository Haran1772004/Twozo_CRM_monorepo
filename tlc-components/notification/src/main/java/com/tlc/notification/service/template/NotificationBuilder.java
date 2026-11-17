package com.tlc.notification.service.template;

import com.tlc.commons.template.model.DefaultTemplateBuildRequest;
import com.tlc.notification.model.NotifiableModel;

import java.util.Optional;

/**
 * <p>
 * Builder responsible for building notification data
 * </p>
 *
 * @param <T> The type of the notification data, which extends {@link NotificationTemplateBuildRequest} containing a model that implements {@link NotifiableModel}
 * @param <R> The type of the resulting notification content that this builder will produce
 * @author Kishore J
 * @version 1.0
 */
public interface NotificationBuilder<T extends NotificationTemplateBuildRequest<? extends NotifiableModel>, R> {

    /**
     * <p>
     * Builds the notification details for the given template name and {@link DefaultTemplateBuildRequest}
     * </p>
     *
     * @param templateName     Represents the template name
     * @param notificationData Represents the {@link DefaultTemplateBuildRequest}
     * @return An {@link Optional} containing the constructed notification content, or empty if build fails
     */
    Optional<R> build(final String templateName, final T notificationData);
}
