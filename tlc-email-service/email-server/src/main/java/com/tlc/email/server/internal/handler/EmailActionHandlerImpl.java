package com.tlc.email.server.internal.handler;

import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.SendRequest;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.email.common.request.UpdateRequest;

import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.service.EmailActionHandler;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.sync.AbstractSyncActionHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides the process of email related services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailActionHandler
 */
public final class EmailActionHandlerImpl extends AbstractSyncActionHandler implements EmailActionHandler {

    private final ThreadManager threadManager;
    private final EmailEventPublisher emailEventPublisher;
    private final DomainAuthenticatedEmailService domainAuthenticatedEmailService;

    private EmailActionHandlerImpl() {
        this.threadManager = ThreadManager.getInstance();
        this.emailEventPublisher = EmailEventPublisher.get();
        this.domainAuthenticatedEmailService = DomainAuthenticatedEmailServiceProvider.getInstance().getService();
    }

    /**
     * <p>
     * Holds the single instance of {@link EmailActionHandler}.
     * </p>
     */
    private static class Instance {
        private static final EmailActionHandler INSTANCE = new EmailActionHandlerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link EmailActionHandler}.
     * </p>
     *
     * @return {@link EmailActionHandler}
     */
    public static EmailActionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncRequest Represents the {@link SyncRequest}
     */
    @Override
    public void sync(final SyncRequest syncRequest) {
        handleSync(syncRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param sendRequest Represents the {@link SendRequest}
     */
    @Override
    public void send(final SendRequest sendRequest) {
        final Email email = sendRequest.getEmail();
        final SendRequest.ProviderType providerType = sendRequest.getProviderType();

        if (providerType.isServiceProvider()) {
            domainAuthenticatedEmailService.send(email);
        } else {
            final EmailProviderService emailProviderService = EmailProviderService.getService(email.getSender());

            emailProviderService.send(email);
        }
        final Thread thread = new Thread();
        final Long emailId = email.id();

        thread.setEmails(List.of(email));

        if (Objects.nonNull(emailId)) {
            final Optional<Long> optionalThreadId = threadManager.getThreadId(emailId);

            optionalThreadId.ifPresent(thread::setId);
        }
        threadManager.create(thread);

        emailEventPublisher.publishThread(sendRequest, thread);
    }

    /**
     * {@inheritDoc}
     *
     * @param updateRequest Represents the {@link UpdateRequest}
     */
    @Override
    public void update(final UpdateRequest updateRequest) {
        final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitions = updateRequest.getEmailStateTransitionInfos();

        for (final Map.Entry<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfoEntry : emailStateTransitions.entrySet()) {
            final String email = emailStateTransitionInfoEntry.getKey();
            final EmailProviderService emailProviderService = EmailProviderService.getService(email);
            final Collection<EmailStateTransitionInfo> emailStateTransitionInfos = emailStateTransitionInfoEntry.getValue();

            updateRequest.setSyncEmailAddress(new SyncAddressInfo(email));
            emailProviderService.update(updateRequest, emailStateTransitionInfos);
            threadManager.updateConversation(updateRequest.getState(), emailStateTransitionInfos);
        }
    }
}