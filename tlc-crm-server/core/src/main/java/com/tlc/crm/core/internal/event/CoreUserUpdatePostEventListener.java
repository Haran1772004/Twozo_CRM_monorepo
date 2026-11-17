package com.tlc.crm.core.internal.event;

import com.tlc.crm.common.event.listener.PostEventListener;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CommonEvent;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles post process actions for user update operations
 *
 * @author Kishore J
 * @version 1.0
 */
public class CoreUserUpdatePostEventListener implements PostEventListener {

    private final SourceFollowersManager sourceFollowersManager;

    private CoreUserUpdatePostEventListener() {
        sourceFollowersManager = SourceFollowersManager.getInstance();
    }

    private static class Instance {
        private static final CoreUserUpdatePostEventListener INSTANCE = new CoreUserUpdatePostEventListener();
    }

    /**
     * <p>
     * Provides the {@link CoreUserUpdatePostEventListener}.
     * </p>
     *
     * @return {@link CoreUserUpdatePostEventListener}
     */
    public static CoreUserUpdatePostEventListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents {@link EventData}.
     */
    @Override
    public void processPostEvent(final EventData eventData) {
        final Collection<? extends TlcModel> models = eventData.getValues();

        if (models.stream().anyMatch(CrmUser.class::isInstance)) {
            final Set<Long> deactivatedUserIds = models.stream().map(CrmUser.class::cast)
                    .filter(user -> Objects.equals(user.getState(), User.State.DEACTIVATED)).map(CrmUser::id)
                    .collect(Collectors.toSet());

            if (!deactivatedUserIds.isEmpty()) {
                sourceFollowersManager.removeSourceFollower(eventData.getOrgId(), deactivatedUserIds);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Event}.
     */
    @Override
    public Event event() {
        return CommonEvent.USER_UPDATE;
    }
}