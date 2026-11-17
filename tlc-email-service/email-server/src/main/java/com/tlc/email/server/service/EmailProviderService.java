package com.tlc.email.server.service;

import com.tlc.email.common.request.DeleteRequest;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.server.internal.provider.jakarta.JakartaMailService;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;

import com.tlc.email.server.internal.provider.gmail.service.GmailService;
import com.tlc.email.server.internal.provider.outlook.OutlookService;

import com.tlc.notification.service.conversationmode.NotificationReceiver;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;

import java.util.Collection;

/**
 * <p>
 * Provides the email services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface EmailProviderService extends EmailNotificationSender<Email>, NotificationReceiver<EmailConfiguration, EmailProviderDataStore> {

    /**
     * <p>
     * Gets the appropriate service to be done
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link EmailProviderService} relevant to the provided email address
     */
    static EmailProviderService getService(final EmailAddress emailAddress) {
        final EmailProvider emailProvider = ProviderService.getEmailProviderService()
                .findProvider(new com.tlc.sync.service.oauth.model.EmailAddress(emailAddress.getEmailAddress()));

        return getService(emailProvider);
    }

    /**
     * <p>
     * Gets the appropriate service to be done
     * </p>
     *
     * @param emailAddress Represents the emailAddress
     * @return {@link EmailProviderService} relevant to the provided email address
     */
    static EmailProviderService getService(final String emailAddress) {
        final EmailProvider emailProvider = ProviderService.getEmailProviderService()
                .findProvider(new com.tlc.sync.service.oauth.model.EmailAddress(emailAddress));

        return getService(emailProvider);
    }

    /**
     * <p>
     * Gets the appropriate service to be done
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link EmailProviderService} relevant to the provided email address
     */
    static EmailProvider getProvider(final EmailAddress emailAddress) {
        return ProviderService.getEmailProviderService()
                .findProvider(new com.tlc.sync.service.oauth.model.EmailAddress(emailAddress.getEmailAddress()));
    }

    /**
     * <p>
     * Provides the {@link EmailProviderService} instance.
     * </p>
     *
     * @return {@link EmailProviderService} instance.
     */
    static EmailProviderService getService(final EmailProvider emailProvider) {
        return switch (emailProvider) {
            case GOOGLE -> GmailService.getInstance();
            case OUTLOOK -> OutlookService.getInstance();
            case ZOHO, ICLOUD, AOL, YAHOO -> JakartaMailService.getInstance();
        };
    }

    /**
     * <p>
     * Performs the delete action for the provided email and message ids.
     * </p>
     *
     * @param emailStateTransitionInfo Represent the email sate transition info.
     * @param emailAddress             Refers the email address.
     */
    void delete(final EmailStateTransitionInfo emailStateTransitionInfo, final String emailAddress);

    /**
     * <p>
     * Performs update action for the given email and message ids.
     * </p>
     *
     * @param updateRequest            Refers to the {@link UpdateRequest}
     * @param emailStateTransitionInfo An entity that holds {@link EmailStateTransitionInfo} attributes
     */
    void update(final UpdateRequest updateRequest, final Collection<EmailStateTransitionInfo> emailStateTransitionInfo);

    /**
     * <p>
     * Loads and processes the given email provider data.
     * </p>
     *
     * @param emailProviderDataStore The email provider data to be loaded.
     */
    void loadEmailProviderData(final EmailProviderDataStore emailProviderDataStore);
}