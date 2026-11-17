package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Implementation of {@link AbstractAppNotificationAlertLoader} to load note alerts based on source.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
public abstract class AbstractEntityNoteMentionAlertLoader extends AbstractAppNotificationAlertLoader<NoteHolder> {

    private static final Map<Source, AppNotificationEventType> APP_NOTIFICATION_EVENT_TYPE_MAP = Map.of(Source.CONTACT, AppNotificationEventType.CONTACT_NOTE_MENTIONED,
            Source.COMPANY, AppNotificationEventType.COMPANY_NOTE_MENTIONED, Source.DEAL, AppNotificationEventType.DEAL_NOTE_MENTIONED);

    private final ConfigManagerHolder configManagerHolder;

    protected AbstractEntityNoteMentionAlertLoader() {
        configManagerHolder = ConfigManagerHolder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param noteHolders Represents the {@link NoteHolder}
     */
    @Override
    public void load(final long orgId, final Collection<NoteHolder> noteHolders) {
        Source source = null;
        final Collection<Long> entityIds = new HashSet<>();
        final Collection<Long> mentionUserIds = new HashSet<>();

        for (final NoteHolder noteHolder : noteHolders) {
            final Collection<CrmUser> mentionedUsers = noteHolder.getNote().getMentionedUsers();

            if (Objects.nonNull(mentionedUsers)) {
                mentionedUsers.forEach(user -> mentionUserIds.add(user.id()));
            }

            if (Objects.isNull(source)) {
                source = Source.get(noteHolder.getSource().name());
            }
            entityIds.add(noteHolder.getEntityId());
        }

        if (Objects.nonNull(source) && Objects.equals(APP_NOTIFICATION_EVENT_TYPE_MAP.get(source), getType())) {

            if (mentionUserIds.isEmpty()) {
                return;
            }
            final Map<Long, TlcModel> models = configManagerHolder.getManager(source).partialGet(orgId, entityIds)
                    .stream().collect(Collectors.toMap(TlcModel::id, Function.identity()));

            final Map<Long, MentionAlertNotificationSettings> mentionsAlertNotificationSettingsMap = notificationSettingsManager.getMentionNotificationSettings(orgId, mentionUserIds)
                    .stream().collect(Collectors.toMap(MentionAlertNotificationSettings::getUserId, Function.identity()));

            for (final NoteHolder noteHolder : noteHolders) {
                final Note note = noteHolder.getNote();
                final Collection<CrmUser> mentionedUsers = note.getMentionedUsers();

                if (Objects.isNull(mentionedUsers) || mentionedUsers.isEmpty()) {
                    continue;
                }
                final TlcModel tlcModel = models.get(noteHolder.getEntityId());

                for (final CrmUser mentionedUser : mentionedUsers) {
                    final MentionAlertNotificationSettings notificationSettings = Objects.requireNonNullElse(mentionsAlertNotificationSettingsMap.get(mentionedUser.id()), new MentionAlertNotificationSettings());

                    if (notificationSettings.isEntityEnabled()) {
                        notificationAlertManager.storeSourceMentionAlert(notificationAlertConverter.convertSourceMentionAlert(orgId, source, tlcModel, mentionedUser, getCategory()));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Gets the {@link NotificationAlert.Category}
     * </p>
     *
     * @return Represents the {@link NotificationAlert.Category }
     */
    protected abstract NotificationAlert.Category getCategory();
}
