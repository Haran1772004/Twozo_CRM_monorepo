package com.tlc.crm.notifications.registrar;

import com.tlc.commons.service.registrar.ResourceRegistrar;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

/**
 * <p>
 * The registrar that registers the given actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class NotificationResourceRegistrar implements ResourceRegistrar<CrmServerRegistrarData> {

    private NotificationResourceRegistrar() {
    }

    private static final class Instance {
        private static final NotificationResourceRegistrar NOTIFICATION_REGISTRAR = new NotificationResourceRegistrar();
    }

    public static NotificationResourceRegistrar getInstance() {
        return Instance.NOTIFICATION_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CrmServerRegistrarData registrarData) {

    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CrmServerRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CrmServerRegistrarData registrarData) {

    }
}
