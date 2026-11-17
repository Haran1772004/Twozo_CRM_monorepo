package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.exchange.EmailData;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;

import java.util.Objects;

/**
 * <p>
 * Encapsulates the details required for retrieving emails, And provides methods for accessing and modifying
 * these details.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SyncRequest extends EmailData implements RequestCodec, EmailModel {

    private EmailReceiverConfiguration receiverConfiguration;
    private SyncJobType syncJobType;
    private Long id;

    public SyncRequest(final EmailReceiverConfiguration receiverConfiguration) {
        this.receiverConfiguration = receiverConfiguration;
    }

    public EmailReceiverConfiguration getReceiverConfiguration() {
        return receiverConfiguration;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the sync id
     */
    @Override
    public Long id() {
        return id;
    }

    public SyncJobType getSyncJobType() {
        return syncJobType;
    }

    public void setSyncJobType(final SyncJobType syncJobType) {
        this.syncJobType = syncJobType;
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

        private final Long id;
        private final EmailReceiverConfiguration receiverConfiguration;
        private StateData stateData;

        public Builder(final Long id, final EmailReceiverConfiguration receiverConfiguration) {
            this.receiverConfiguration = receiverConfiguration;
            this.id = id;
        }

        public Builder stateData(final StateData stateData) {
            this.stateData = stateData;

            return this;
        }

        public SyncRequest build() {
            final SyncRequest request = new SyncRequest(this.receiverConfiguration);

            request.setStateData(this.stateData);
            request.setId(id);

            return request;
        }
    }
}