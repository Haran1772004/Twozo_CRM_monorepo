package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.exchange.SyncEmailData;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to update an email notification.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class UpdateRequest extends AbstractEmailRequest {

    private Email.State state;

    public Email.State getState() {
        return state;
    }

    public void setState(final Email.State state) {
        this.state = state;
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

        private final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionInfoMap;
        private Email.State state;
        private StateData stateData;
        private SyncAddressInfo syncAddressInfo;

        public Builder() {
            this.emailStateTransitionInfoMap = new java.util.HashMap<>();
        }

        public Builder setState(final Email.State state) {
            this.state = state;

            return this;
        }

        public Builder add(final String email, final EmailStateTransitionInfo emailStateTransitionInfo) {
            this.emailStateTransitionInfoMap.computeIfAbsent(email, key -> new ArrayList<>()).add(emailStateTransitionInfo);

            return this;
        }

        public Builder addAll(final Map<String, Collection<EmailStateTransitionInfo>> stateTransitionInfos) {
            stateTransitionInfos.forEach((email, emailStateTransitionInfos) ->
                    this.emailStateTransitionInfoMap.computeIfAbsent(email, key -> new ArrayList<>()).addAll(emailStateTransitionInfos));

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

        public UpdateRequest build() {
            final UpdateRequest updateRequest = new UpdateRequest();

            updateRequest.setEmailStateTransitionInfos(this.emailStateTransitionInfoMap);
            updateRequest.setStateData(this.stateData);
            updateRequest.setSyncEmailAddress(this.syncAddressInfo);
            updateRequest.setState(this.state);

            return updateRequest;
        }
    }
}
