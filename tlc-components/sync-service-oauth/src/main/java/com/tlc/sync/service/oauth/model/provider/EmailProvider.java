package com.tlc.sync.service.oauth.model.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.internal.SyncServiceProvider;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.service.EmailSyncService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Enum that represents the config of provider type
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public enum EmailProvider implements Provider {

    GOOGLE("google", (short) 1, true),
    OUTLOOK("outlook", (short) 2, true),
    ZOHO("zoho", (short) 3, false),
    ICLOUD("icloud", (short) 4, false),
    YAHOO("yahoo", (short) 5, false),
    AOL("aol", (short) 6, false),
    ;

    private final String name;
    private final short id;
    private final boolean isOAuth;

    EmailProvider(final String name, final short id, final boolean isOAuth) {
        this.name = Objects.requireNonNull(name);
        this.id = id;
        this.isOAuth = isOAuth;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link EmailSyncService}
     */
    @Override
    public EmailSyncService getSyncService() {
        return SyncServiceProvider.getInstance().getEmailSyncService(this);
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
        return SyncServiceCategory.EMAIL;
    }

    /**
     * <p>
     * Gets the {@link EmailProvider} by type
     * </p>
     *
     * @param id Represents the email provider type id
     * @return {@link EmailProvider}
     */
    public static EmailProvider valueOf(final short id) {
        return switch (id) {
            case 1 -> GOOGLE;
            case 2 -> OUTLOOK;
            case 3 -> ZOHO;
            case 4 -> ICLOUD;
            case 5 -> YAHOO;
            case 6 -> AOL;
            default -> throw ErrorCode.get(SyncErrorCodes.INVALID_EMAIL_PROVIDER);
        };
    }

    /**
     * <p>
     * Gets the matched {@link EmailProvider}
     * </p>
     *
     * @param providerName represents the provider name
     * @return {@link EmailProvider}
     */
    public static EmailProvider get(final String providerName) {
        try {
            return EmailProvider.valueOf(providerName.toUpperCase().trim());
        } catch (Exception e) {
            throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        }
    }

    public boolean isOauthProvider() {
        return isOAuth;
    }

    public static Collection<EmailProvider> getOauthProviders() {
        return List.of(GOOGLE, OUTLOOK);
    }
}