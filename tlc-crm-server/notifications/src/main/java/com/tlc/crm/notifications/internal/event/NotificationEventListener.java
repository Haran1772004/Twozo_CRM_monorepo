package com.tlc.crm.notifications.internal.event;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.event.NotificationEvent;
import com.tlc.crm.notifications.internal.handler.registry.NotifiableModelHandlerRegistry;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;
import com.tlc.crm.notifications.internal.loader.registry.NotificationLoaderRegistry;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles post process actions for notification operations
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class NotificationEventListener implements PostEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotifiableModelHandlerRegistry notifiableModelHandlerRegistry;
    private final NotificationAlertLoaderRegistry notificationAlertLoaderRegistry;
    private final NotificationLoaderRegistry notificationLoaderRegistry;

    private NotificationEventListener() {
        notifiableModelHandlerRegistry = NotifiableModelHandlerRegistry.getInstance();
        notificationAlertLoaderRegistry = NotificationAlertLoaderRegistry.getInstance();
        notificationLoaderRegistry = NotificationLoaderRegistry.getInstance();
    }

    @Override
    public Event event() {
        return NotificationEvent.APP_NOTIFICATION_EVENT;
    }

    private static class Instance {
        private static final NotificationEventListener INSTANCE = new NotificationEventListener();
    }

    /**
     * <p>
     * Gets the {@link NotificationEventListener}.
     * </p>
     *
     * @return {@link NotificationEventListener}
     */
    public static NotificationEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Initiates the post process action which is publishing.
     * </p>
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        LOGGER.info("NotificationEventListener listened to post event");
        final Collection<? extends TlcModel> models = eventData.getValues();
        final Optional<? extends TlcModel> model = models.stream().findFirst();

        if (model.isPresent()) {
            final Class<? extends TlcModel> tlcModelClass = model.get().getClass();
            final Class<Notifiable> notificationTypesClass = Notifiable.class;

            if (tlcModelClass.isAnnotationPresent(notificationTypesClass)) {
                final long orgId = eventData.getOrgId();
                final Notifiable notifiable = tlcModelClass.getAnnotation(notificationTypesClass);

                preProcess(eventData, notifiable);

                if (notifiable.needHandling()) {
                    handle(orgId, notifiable.modelName(), models);
                } else {
                    final AppNotificationEventType[] appNotificationEventTypes = notifiable.types();

                    for (final AppNotificationEventType appNotificationEventType : appNotificationEventTypes) {
                        load(orgId, appNotificationEventType, models);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Handles notifiable models.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param crmModelType {@link CrmModelType}.
     * @param models       Collection {@link TlcModel}.
     */
    private <R extends TlcModel> void handle(final Long orgId, final CrmModelType crmModelType, final Collection<R> models) {

        try {
            notifiableModelHandlerRegistry.opt(crmModelType).ifPresentOrElse(
                    handler -> handler.handle(orgId, (Collection) models),
                    () -> LOGGER.debug("App Notification: Notifiable model handler not found for model type: {}", crmModelType));
        } catch (Throwable throwable) {
            LOGGER.error("App Notification: Error while handling notifiable model. ModelType: {}, Org ID : {}, ID: {}",
                    crmModelType, orgId, models.stream().findFirst().map(TlcModel::id).orElseGet(() -> null), throwable);
            throwable.printStackTrace();
        }
    }

    /**
     * <p>
     * Loads notifiable models.
     * </p>
     *
     * @param orgId     Represents an organization id.
     * @param eventType {@link AppNotificationEventType}.
     * @param models    Collection {@link TlcModel}.
     */
    private <R extends TlcModel> void load(final Long orgId, final AppNotificationEventType eventType, final Collection<R> models) {

        if (eventType.isAlertRequired()) {
            notificationAlertLoaderRegistry.opt(eventType).ifPresent(alertLoader -> alertLoader.load(orgId, (Collection) models));
        } else {
            notificationLoaderRegistry.opt(eventType).ifPresent(alertLoader -> alertLoader.load(orgId, (Collection) models));
        }
    }

    /**
     * <p>
     * Pre-processes the models to filter out already notified mentions in case of an update.
     * </p>
     *
     * @param eventData  Represents {@link EventData}.
     * @param notifiable Represents {@link Notifiable}.
     */
    private void preProcess(final EventData eventData, final Notifiable notifiable) {
        if (Objects.equals(eventData.getAction(), Action.UPDATE)) {
            final Collection<? extends TlcModel> models = eventData.getValues();
            final CrmModelType crmModelType = notifiable.modelName();
            final Map<Long, TlcModel> previousValues = eventData.getPreviousValues();

            for (final TlcModel tlcModel : models) {
                final Collection<CrmUser> mentionedUsers = getMentionedUsers(tlcModel, crmModelType);

                if (Objects.nonNull(mentionedUsers) && !mentionedUsers.isEmpty()) {
                    final TlcModel previousValue = previousValues.get(tlcModel.id());
                    final Collection<CrmUser> previousMentionedUsers = getMentionedUsers(previousValue, crmModelType);

                    if (Objects.nonNull(previousMentionedUsers)) {
                        mentionedUsers.removeAll(previousMentionedUsers);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets mentioned users from the model based on type.
     * </p>
     *
     * @param model {@link TlcModel}.
     * @param type  {@link CrmModelType}.
     * @return collection of {@link CrmUser}.
     */
    private Collection<CrmUser> getMentionedUsers(final TlcModel model, final CrmModelType type) {
        if (Objects.isNull(model)) {
            return null;
        }
        return switch (type) {
            case NOTE -> ((NoteHolder) model).getNote().getMentionedUsers();
            case ACTIVITY -> {
                final Note note = ((ActivityHolder) model).getActivity().getNote();

                yield Objects.isNull(note) ? null : note.getMentionedUsers();
            }
            case COMMENT -> ((Comment) model).getMentionedUsers();
            default -> null;
        };
    }
}
