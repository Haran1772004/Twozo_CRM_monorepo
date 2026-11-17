package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

/**
 * <p>
 * Loads company note comment mention alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class CompanyNoteCommentMentionAlertLoaderAbstract extends AbstractEntityNoteCommentMentionAlertLoader {

    private CompanyNoteCommentMentionAlertLoaderAbstract() {
    }

    private static class Instance {
        private static final CompanyNoteCommentMentionAlertLoaderAbstract INSTANCE = new CompanyNoteCommentMentionAlertLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link CompanyNoteCommentMentionAlertLoaderAbstract}.
     * </p>
     *
     * @return {@link CompanyNoteCommentMentionAlertLoaderAbstract}.
     */
    public static CompanyNoteCommentMentionAlertLoaderAbstract getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.COMPANY_NOTE_COMMENT_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @return Represents the {@link NotificationAlert.Category }
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.COMPANY_NOTE_COMMENT_MENTIONED;
    }
}
