package com.tlc.crm.email.notification.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.email.notification.email.sync.listener.EmailSyncSettingListener;
import com.tlc.crm.email.notification.email.tracking.internal.EmailTrackingFile;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class EmailNotificationServiceInitializer implements ServiceInitializer {

    private final EmailTrackingFile emailTrackingFile;
    private final WritableDataStore writableDataStore;
    private final EmailSyncSettingListener emailSyncListener;

    private EmailNotificationServiceInitializer() {
        emailTrackingFile = EmailTrackingFile.getInstance();
        writableDataStore = SqlServiceProvider.get().getAdminDataStore();
        emailSyncListener = EmailSyncSettingListener.getInstance();
    }

    private static final class Instance {
        private static final EmailNotificationServiceInitializer NOTIFICATION_SERVICE_INITIALIZER = new EmailNotificationServiceInitializer();
    }

    public static EmailNotificationServiceInitializer getInstance() {
        return Instance.NOTIFICATION_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        emailTrackingFile.load();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        writableDataStore.registerUpdatedEntryListener(Table.get(OMEMSYNCEDEMAIL.TABLE), emailSyncListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
    }
}
