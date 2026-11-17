package com.tlc.crm.email.notification.email.api.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;

import com.tlc.email.client.service.EmailService;

import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.request.DeleteRequest;
import com.tlc.email.common.request.SendRequest;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.common.model.type.Email.MessageCategory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the services for email dispatching services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailDispatcher
 */
public final class EmailDispatcherImpl implements EmailDispatcher {

    private final EmailService emailService;

    private EmailDispatcherImpl() {
        this.emailService = EmailService.get();
    }

    private static class Instance {
        private static final EmailDispatcherImpl INSTANCE = new EmailDispatcherImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link EmailDispatcher}.
     * </p>
     *
     * @return {@link EmailDispatcher}
     */
    public static EmailDispatcher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param email     Represents the {@link Email}
     * @param stateData Represents the state data in json
     */
    @Override
    public void send(final Email email, final JsonObject stateData) {
        final SendRequest sendRequest = new SendRequest();

        sendRequest.setEmail((OutboundEmail) email);
        sendRequest.setStateData(new StateData(stateData));
        sendRequest.setProviderType(getProviderType(email));

        emailService.send(sendRequest);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailStateTransitionInfos Represents the email state transition infos
     * @param messageCategory           Represents the message category
     */
    public void delete(final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos, final MessageCategory messageCategory) {
        final DeleteRequest deleteRequest = new DeleteRequest();

        deleteRequest.setEmailStateTransitionInfos(emailStateTransitionInfos);
        deleteRequest.setMessageCategory(messageCategory);

        emailService.delete(deleteRequest);

    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailStateTransitions Represents the {@link Collection} of {@link EmailStateTransitionInfo}
     * @param state                 Represents the {@link Email.State}
     */
    public void update(final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitions, final Email.State state) {
        final UpdateRequest updateRequest = new UpdateRequest();

        updateRequest.setEmailStateTransitionInfos(emailStateTransitions);
        updateRequest.setState(state);

        emailService.update(updateRequest);
    }

    /**
     * <p>
     * Provide the provider type based on the from address type
     * </p>
     *
     * @param email Represents the email.
     */
    private SendRequest.ProviderType getProviderType(final Email email) {
        if (email.getSender() instanceof FromAddressInfo fromAddressInfo &&
                Objects.equals(fromAddressInfo.getType(), FromAddressInfo.Type.SYNC_EMAIL)) {
            return SendRequest.ProviderType.OAUTH_CLIENT;
        }

        return SendRequest.ProviderType.SERVICE_PROVIDER;
    }
}