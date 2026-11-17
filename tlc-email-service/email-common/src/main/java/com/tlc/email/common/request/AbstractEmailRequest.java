package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.exchange.SyncEmailData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Serves as a base class for email-related requests,
 * providing common properties and methods for handling email state transitions and synchronization.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public abstract class AbstractEmailRequest extends SyncEmailData implements RequestCodec {

    private Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos;

    protected AbstractEmailRequest() {
        this.emailStateTransitionInfos = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @return the synchronization email address information
     */
    @Override
    public SyncAddressInfo getSyncEmailAddress() {

        if (Objects.isNull(syncAddressInfo)) {
            syncAddressInfo = new SyncAddressInfo(emailStateTransitionInfos.keySet().stream().findFirst().orElseGet(() -> null));
        }

        return syncAddressInfo;
    }

    /**
     * <p>
     * Gets the map of email state transition information,
     * where the key is the email address and the value is a collection of state transition info.
     * </p>
     *
     * @return a map containing email addresses and their corresponding state transition information
     */
    public Map<String, Collection<EmailStateTransitionInfo>> getEmailStateTransitionInfos() {
        return emailStateTransitionInfos;
    }

    /**
     * <p>
     * Sets the map of email state transition information,
     * where the key is the email address and the value is a collection of state transition info.
     * </p>
     *
     * @param emailStateTransitionInfos a map containing email addresses and their corresponding state transition information to be set
     */
    public void setEmailStateTransitionInfos(final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfos) {
        this.emailStateTransitionInfos = emailStateTransitionInfos;
    }
}
