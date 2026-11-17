package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

/**
 * <p>
 * Loads contact note comment mention alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class ContactNoteCommentMentionAlertLoaderAbstract extends AbstractEntityNoteCommentMentionAlertLoader {

    private ContactNoteCommentMentionAlertLoaderAbstract() {
    }

    private static class Instance {
        private static final ContactNoteCommentMentionAlertLoaderAbstract INSTANCE = new ContactNoteCommentMentionAlertLoaderAbstract();
    }

    /**
     * <p>
     * Gets {@link ContactNoteCommentMentionAlertLoaderAbstract}.
     * </p>
     *
     * @return {@link ContactNoteCommentMentionAlertLoaderAbstract}.
     */
    public static ContactNoteCommentMentionAlertLoaderAbstract getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.CONTACT_NOTE_COMMENT_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link NotificationAlert.Category}
     */
    @Override
    protected NotificationAlert.Category getCategory() {
        return NotificationAlert.Category.CONTACT_NOTE_COMMENT_MENTIONED;
    }
}
