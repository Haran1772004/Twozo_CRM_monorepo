package com.tlc.email.common.request;

import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email.MessageCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Encapsulates the details required for deleting emails, And provides methods for accessing and modifying
 * these details.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class DeleteRequest extends AbstractEmailRequest {

    private MessageCategory messageCategory;

    public MessageCategory getMessageCategory() {
        return messageCategory;
    }

    public void setMessageCategory(final MessageCategory messageCategory) {
        this.messageCategory = messageCategory;
    }

    /**
     * <p>
     * Provides a flexible and convenient way to construct instances of the enclosing class with a step-by-step approach.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public static class Builder {

        private final Map<String, Collection<EmailStateTransitionInfo>> stateTransitionInfos;
        private StateData stateData;
        private SyncAddressInfo syncAddressInfo;

        public Builder() {
            this.stateTransitionInfos = new HashMap<>();
        }

        public Builder add(final String id, final EmailStateTransitionInfo info) {
            this.stateTransitionInfos.computeIfAbsent(id, key -> new ArrayList<>()).add(info);

            return this;
        }

        public Builder addAll(final Map<String, Collection<EmailStateTransitionInfo>> map) {
            map.forEach((emailAddress, value) -> this.stateTransitionInfos.computeIfAbsent(emailAddress, key -> new ArrayList<>()).addAll(value));

            return this;
        }

        public Builder stateData(final StateData stateData) {
            this.stateData = stateData;

            return this;
        }

        public Builder addStateData(final String key, final String value) {
            this.stateData = Objects.isNull(this.stateData) ? new StateData() : this.stateData;

            this.stateData.put(key, value);

            return this;
        }

        public Builder setSyncEmailAddress(final SyncAddressInfo syncAddressInfo) {
            this.syncAddressInfo = syncAddressInfo;

            return this;
        }

        public DeleteRequest build() {
            final DeleteRequest deleteRequest = new DeleteRequest();

            deleteRequest.setEmailStateTransitionInfos(this.stateTransitionInfos);
            deleteRequest.setStateData(this.stateData);
            deleteRequest.setSyncEmailAddress(this.syncAddressInfo);

            return deleteRequest;
        }
    }
}