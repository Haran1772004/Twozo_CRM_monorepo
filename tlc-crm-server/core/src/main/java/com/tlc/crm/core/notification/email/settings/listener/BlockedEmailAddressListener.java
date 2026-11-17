package com.tlc.crm.core.notification.email.settings.listener;

import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;
import com.tlc.crm.core.notification.email.settings.internal.cache.impl.EmailSettingsCacheImpl;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import com.tlc.email.common.model.address.EmailAddress;

import java.util.Collection;
import java.util.Objects;

/**
 * A listener that will listen the creation/update/delete actions of {@link EmailAddress}
 *
 * @author Kishore J
 * @version 1.0
 */
public class BlockedEmailAddressListener implements CompleteRowListener {

    private final EmailSettingsCacheImpl emailSettingsCache;
    private final EmailSettingsManager emailSettingsManager;

    private BlockedEmailAddressListener() {
        emailSettingsCache = EmailSettingsCacheImpl.getInstance();
        emailSettingsManager = EmailSettingsManager.getInstance();
    }

    private static class Instance {
        private static final BlockedEmailAddressListener INSTANCE = new BlockedEmailAddressListener();
    }

    /**
     * <p>
     * Gets the {@link BlockedEmailAddressListener} instance
     * </p>
     *
     * @return The {@link BlockedEmailAddressListener}
     */
    public static BlockedEmailAddressListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email address rows
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        if (Objects.nonNull(rows)) {

            for (final Row row : rows) {
                emailSettingsCache.addBlockedEmailAddress(emailSettingsManager.convertRowToBlockedEmailAddress(row));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email address rows
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        removeBlockedEmailAddressToCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of email address rows
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        removeBlockedEmailAddressToCache(rows);
    }

    /**
     * <p>
     * Removes a collection of blocked email address into cache
     * </p>
     *
     * @param rows Which contains the collection of email address rows
     */
    private void removeBlockedEmailAddressToCache(final Collection<Row> rows) {
        if (Objects.nonNull(rows)) {

            for (final Row row : rows) {
                emailSettingsCache.removeBlockedAddress(emailSettingsManager.convertRowToBlockedEmailAddress(row));
            }
        }
    }
}
