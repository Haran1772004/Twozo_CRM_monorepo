package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

/**
 * <p>
 * Loads deal note comment mention alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class DealNoteCommentMentionAlertLoaderAbstract extends AbstractEntityNoteCommentMentionAlertLoader {

    private DealNoteCommentMentionAlertLoaderAbstract() {
    }

    private static class Instance {
        private static final DealNoteCommentMentionAlertLoaderAbstract INSTANCE = new DealNoteCommentMentionAlertLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link DealNoteCommentMentionAlertLoaderAbstract}.
     * </p>
     *
     * @return {@link DealNoteCommentMentionAlertLoaderAbstract}.
     */
    public static DealNoteCommentMentionAlertLoaderAbstract getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.DEAL_NOTE_COMMENT_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link NotificationAlert.Category}
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.DEAL_NOTE_COMMENT_MENTIONED;
    }
}
