package com.tlc.crm.notifications.internal.loader.notifications.impl.notifications;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notifications.api.model.AppNotification;
import com.tlc.crm.notifications.api.model.NotificationUser;
import com.tlc.crm.notifications.api.model.converter.AppNotificationConverter;
import com.tlc.crm.notifications.internal.loader.notifications.AppNotificationLoader;
import com.tlc.crm.notifications.service.AppNotificationManager;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * <p>
 * Provides the services for the notification
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public abstract class AbstractNotificationLoader<T extends TlcModel> implements AppNotificationLoader<T> {

    protected final AppNotificationConverter appNotificationConverter;
    protected final UserNotificationSettingsManager notificationSettingsManager;

    private final AppNotificationManager appNotificationManager;
    private final ModelValidator modelValidator;

    protected AbstractNotificationLoader() {
        appNotificationManager = AppNotificationManager.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        appNotificationConverter = AppNotificationConverter.getInstance();
        notificationSettingsManager = UserNotificationSettingsManager.getInstance();
    }

    /**
     * <p>
     * Fetches the receivers required for the notification
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerIds Represents the owner ids
     * @return the  collection of {@link NotificationUser}
     */
    protected List<NotificationUser> getReceivers(final long orgId, final Collection<Long> ownerIds) {
        final List<NotificationUser> receivers = new ArrayList<>();

        for (final Long ownerId : ownerIds) {
            final NotificationUser user = new NotificationUser();

            user.setOrgId(orgId);
            user.setId(ownerId);
            receivers.add(user);
        }

        return receivers;
    }

    /**
     * <p>
     * Fetches the sender for the notification user
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param ownerId Represents an owner id
     * @return the {@link NotificationUser}
     */
    protected NotificationUser getSender(final long orgId, final long ownerId) {
        final NotificationUser sender = new NotificationUser();

        sender.setId(ownerId);
        sender.setOrgId(orgId);

        return sender;
    }

    /**
     * <p>
     * Saves the notifications based on the sender and receivers
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param sender       Represents the sender
     * @param receivers    Represents the receivers
     * @param notification Represents the notification to be stored
     */
    protected void saveNotification(final long orgId, final NotificationUser sender, final List<NotificationUser> receivers,
                                    final JsonObject notification) {
        final AppNotification appNotification = appNotificationConverter.constructNotification(
                orgId, sender, receivers, notification, getType());
        final Collection<ErrorDescriptor> errors = modelValidator.validate(
                List.of(appNotification), Group.Create.class);

        if (errors.isEmpty()) {
            appNotificationManager.storeNotification(orgId, List.of(appNotification));
        }
    }

    /**
     * <p>
     * Processes the notifications based on the given {@link TlcModel}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param models     Represents the collection of {@link TlcModel}
     * @param converter  Represents the converter
     * @param salesOwner Represents the sales owner
     * @return the {@link JsonObject}
     */
    protected JsonObject processNotification(final long orgId, final Collection<T> models,
                                             final Function<Collection<T>, JsonObject> converter,
                                             final Function<T, TlcModel> salesOwner) {
        final Optional<Long> salesOwnerId = models.stream().findFirst().map(salesOwner).map(TlcModel::id);

        if (salesOwnerId.isPresent()) {
            final Optional<AssignmentNotificationSettings> settings = notificationSettingsManager.retrieveAssignmentNotificationSettings(orgId, salesOwnerId.get());

            if (settings.isPresent() && settings.get().isSourceAssigned()) {
                return converter.apply(models);
            }
        }
        return Json.object();
    }
}
