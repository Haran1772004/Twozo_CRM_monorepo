package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.entity.features.api.note.manager.CommentManager;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Implementation of {@link AbstractAppNotificationAlertLoader} to load comment alerts based on source.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
public abstract class AbstractEntityNoteCommentMentionAlertLoader extends AbstractAppNotificationAlertLoader<Comment> {

    private static final Map<Source, AppNotificationEventType> APP_NOTIFICATION_EVENT_TYPE_MAP = Map.of(Source.CONTACT, AppNotificationEventType.CONTACT_NOTE_COMMENT_MENTIONED,
            Source.COMPANY, AppNotificationEventType.COMPANY_NOTE_COMMENT_MENTIONED, Source.DEAL, AppNotificationEventType.DEAL_NOTE_COMMENT_MENTIONED);

    private final SourceNoteManager sourceNoteManager;
    private final CommentManager commentManager;

    protected AbstractEntityNoteCommentMentionAlertLoader() {
        sourceNoteManager = SourceNoteManager.getInstance();
        commentManager = CommentManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param comments Represents the {@link Comment}
     */
    @Override
    public void load(final long orgId, final Collection<Comment> comments) {
        final Collection<Long> noteIds = new HashSet<>(comments.size());
        final Collection<Long> commentIds = new HashSet<>(comments.size());
        final Collection<Long> mentionedUserIds = new HashSet<>();

        for (final Comment comment : comments) {
            final Collection<CrmUser> mentionedUsers = comment.getMentionedUsers();

            if (Objects.isNull(mentionedUsers) || mentionedUsers.isEmpty()) {
                continue;
            }

            mentionedUsers.forEach(user -> mentionedUserIds.add(user.id()));
            final Long noteId = comment.getNoteId();

            if (Objects.nonNull(noteId)) {
                noteIds.add(noteId);
            } else {
                commentIds.add(comment.id());
            }
        }
        final boolean isCommentIdsEmpty = commentIds.isEmpty();

        if (noteIds.isEmpty() && isCommentIdsEmpty) {
            return;
        }
        final Map<Long, Long> noteIdMap = isCommentIdsEmpty ? Collections.emptyMap() : commentManager.getNoteIds(orgId, commentIds);

        noteIds.addAll(noteIdMap.values());

        final Map<Long, Map<NoteHolder.Source, Entity>> entities = sourceNoteManager.getEntities(orgId, noteIds);

        if (entities.isEmpty()) {
            return;
        }
        final Map<Long, MentionAlertNotificationSettings> mentionsAlertNotificationSettingsMap = mentionedUserIds.isEmpty() ? Collections.emptyMap() :
                notificationSettingsManager.getMentionNotificationSettings(orgId, mentionedUserIds).stream()
                        .collect(Collectors.toMap(MentionAlertNotificationSettings::getUserId, Function.identity()));

        for (final Comment comment : comments) {
            final Long noteId = Objects.requireNonNullElse(comment.getNoteId(), noteIdMap.get(comment.id()));
            final Map<NoteHolder.Source, Entity> entityMap = entities.get(noteId);

            if (Objects.isNull(entityMap)) {
                continue;
            }
            final Collection<CrmUser> mentionedUsers = comment.getMentionedUsers();

            if (Objects.isNull(mentionedUsers) || mentionedUsers.isEmpty()) {
                continue;
            }

            for (final Map.Entry<NoteHolder.Source, Entity> entry : entityMap.entrySet()) {
                final Source source = Source.get(entry.getKey().name());

                if (Objects.equals(APP_NOTIFICATION_EVENT_TYPE_MAP.get(source), getType())) {

                    for (final CrmUser mentionedUser : mentionedUsers) {
                        final MentionAlertNotificationSettings notificationSettings = Objects.requireNonNullElse(mentionsAlertNotificationSettingsMap.get(mentionedUser.id()),
                                new MentionAlertNotificationSettings());

                        if (notificationSettings.isEntityEnabled()) {
                            final SourceMentionAlert sourceMentionAlert = notificationAlertConverter.convertSourceMentionAlert(orgId,
                                    source, entry.getValue(), mentionedUser, getCategory());

                            notificationAlertManager.storeSourceMentionAlert(sourceMentionAlert);
                        }
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
     * @return {@link NotificationAlert.Category}
     */
    protected abstract NotificationAlert.Category getCategory();
}
