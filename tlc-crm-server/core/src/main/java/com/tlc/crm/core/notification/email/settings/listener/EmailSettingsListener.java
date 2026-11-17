package com.tlc.crm.core.notification.email.settings.listener;

import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;
import com.tlc.crm.core.notification.email.settings.internal.cache.impl.EmailSettingsCacheImpl;

import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.Objects;

/**
 * A listener that will listen the creation/update/delete actions of {@link EmailSettings}
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailSettingsListener implements CompleteRowListener {

    private final EmailSettingsCacheImpl emailSettingsCacheImpl;
    private final EmailSettingsManager emailSettingsManager;

    private EmailSettingsListener() {
        emailSettingsCacheImpl = EmailSettingsCacheImpl.getInstance();
        emailSettingsManager = EmailSettingsManager.getInstance();
    }

    private static class Instance {
        private static final EmailSettingsListener INSTANCE = new EmailSettingsListener();
    }

    /**
     * <p>
     * Gets {@link EmailSettingsListener} instance
     * </p>
     *
     * @return The {@link EmailSettingsListener}
     */
    public static EmailSettingsListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email settings rows
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        addEmailSettingsToCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email settings rows
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        addEmailSettingsToCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email settings rows
     */
    @Override
    public void deleteRows(Collection<Row> rows) {

    }

    /**
     * <p>
     * Adds a collection of email settings into cache
     * </p>
     *
     * @param rows Which contains the collection of email settings rows
     */
    private void addEmailSettingsToCache(final Collection<Row> rows) {
        if (Objects.nonNull(rows)) {

            for (final Row row : rows) {
                emailSettingsCacheImpl.addEmailSettings(emailSettingsManager.convertRowToModel(row));
            }
        }
    }
}
