package com.tlc.crm.email.notification.email;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.api.impl.EmailDispatcherImpl;

import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.Email.MessageCategory;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the email dispatcher services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface EmailDispatcher {

    /**
     * <p>
     * Gets the instance of {@link EmailDispatcher}.
     * </p>
     *
     * @return {@link EmailDispatcher}
     */
    static EmailDispatcher get() {
        return EmailDispatcherImpl.getInstance();
    }

    /**
     * <p>
     * Sends the email.
     * </p>
     *
     * @param email       Represents the {@link Email}
     * @param requestData Represents the json which contains request data
     */
    void send(final Email email, final JsonObject requestData);

    /**
     * <p>
     * Deletes the email services.
     * </p>
     *
     * @param emailStateTransitionInfos Represents the state transition infos
     * @param messageCategory           Represents the {@link MessageCategory}
     */
    void delete(final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos, final MessageCategory messageCategory);

    /**
     * <p>
     * Updates the email based on its given {@link EmailConfiguration}
     * </p>
     *
     * @param emailStateTransitions Represents the {@link Collection} of {@link EmailStateTransitionInfo}
     * @param state                 Represents the {@link Email.State}
     */
    void update(final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitions, final Email.State state);
}
