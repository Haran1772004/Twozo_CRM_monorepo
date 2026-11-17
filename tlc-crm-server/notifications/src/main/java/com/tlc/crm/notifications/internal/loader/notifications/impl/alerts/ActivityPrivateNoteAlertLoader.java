package com.tlc.crm.notifications.internal.loader.notifications.impl.alerts;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.internal.loader.registry.NotificationAlertLoaderRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Loads activity private note alerts into DB.
 * </p>
 *
 * @author Kishore J
 * @version 1.0.
 */
@AutoServiceRegistrar(registryClazz = NotificationAlertLoaderRegistry.class)
public class ActivityPrivateNoteAlertLoader extends AbstractActivityAlertLoader {

    private ActivityPrivateNoteAlertLoader() {
    }

    private static class Instance {
        private static final ActivityPrivateNoteAlertLoader INSTANCE = new ActivityPrivateNoteAlertLoader();
    }

    /**
     * <p>
     * Gets {@link ActivityPrivateNoteAlertLoader}.
     * </p>
     *
     * @return {@link ActivityPrivateNoteAlertLoader}.
     */
    public static ActivityPrivateNoteAlertLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AppNotificationEventType}.
     */
    @Override
    public AppNotificationEventType getType() {
        return AppNotificationEventType.ACTIVITY_PRIVATE_NOTE_MENTIONED;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      An organization id.
     * @param modelsData Converted model data.
     */
    @Override
    protected void loadAlerts(final long orgId, final JsonArray modelsData) {

    }

    /**
     * <p>
     * Converts models
     * </p>
     *
     * @param orgId           Represents an organization id.
     * @param activityHolders Represents the collection {@link ActivityHolder}.
     */
    @Override
    public void load(final long orgId, final Collection<ActivityHolder> activityHolders) {

        for (final ActivityHolder activityHolder : activityHolders) {
            final Note note = activityHolder.getActivity().getNote();

            if (Objects.isNull(note)) {
                continue;
            }
            final Collection<CrmUser> mentionedUsers = note.getMentionedUsers();

            if (Objects.isNull(mentionedUsers) || mentionedUsers.isEmpty()) {
                continue;
            }
            final Collection<Long> userIds = mentionedUsers.stream().map(CrmUser::id).collect(Collectors.toSet());
            final Map<Long, MentionAlertNotificationSettings> mentionsAlertNotificationSettingsMap = notificationSettingsManager.getMentionNotificationSettings(orgId, userIds)
                    .stream().collect(Collectors.toMap(MentionAlertNotificationSettings::getUserId, Function.identity()));

            for (final CrmUser mentionedUser : mentionedUsers) {
                final MentionAlertNotificationSettings notificationSettings = Objects.requireNonNullElse(mentionsAlertNotificationSettingsMap.get(mentionedUser.id()), new MentionAlertNotificationSettings());

                if (notificationSettings.isActivityPrivateNoteEnabled()) {
                    notificationAlertManager.storeSourceMentionAlert(notificationAlertConverter.convertSourceMentionAlert(orgId, Source.ACTIVITY, activityHolder, mentionedUser,
                            NotificationAlert.Category.ACTIVITY_PRIVATE_NOTE_MENTIONED));
                }
            }
        }
    }
}