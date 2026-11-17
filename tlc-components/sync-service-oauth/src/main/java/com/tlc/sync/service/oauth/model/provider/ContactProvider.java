package com.tlc.sync.service.oauth.model.provider;

import com.tlc.commons.code.ErrorCode;

import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.internal.contact.ContactSyncService;

import java.util.Objects;

/**
 * <p>
 * Enum that represents the config of contact provider type
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum ContactProvider implements Provider {

    GOOGLE("google", 1),

    OUTLOOK("outlook", 2);

    private final String name;
    private final int id;

    ContactProvider(final String name, final int id) {
        this.name = Objects.requireNonNull(name);
        this.id = id;
    }

    /**
     * <p>
     * Gets the name of the provider.
     * </p>
     *
     * @return name of the provider.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the id of the provider.
     * </p>
     *
     * @return id of the provider.
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link SyncServiceCategory}
     */
    @Override
    public SyncServiceCategory serviceCategory() {
        return SyncServiceCategory.CONTACT;
    }

    /**
     * <p>
     * Gets the {@link ContactProvider} by type
     * </p>
     *
     * @param id Represents the contact provider type id
     * @return {@link ContactProvider}
     */
    public static ContactProvider valueOf(final int id) {
        return switch (id) {
            case 1 -> GOOGLE;
            case 2 -> OUTLOOK;
            default -> throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        };
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link ContactSyncService}
     */
    @Override
    public ContactSyncService getSyncService() {
        return SyncServiceProvider.getInstance().getContactSyncService(this);
    }

    /**
     * <p>
     * Gets the matched {@link ContactProvider}
     * </p>
     *
     * @param providerName represents the {@link String} of provider name
     * @return {@link ContactProvider}
     */
    public static ContactProvider get(final String providerName) {
        try {
            return ContactProvider.valueOf(providerName.toUpperCase().trim());
        } catch (Exception e) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        }
    }
}