package com.tlc.email.common.request;

import com.tlc.email.common.model.StateData;
import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.config.TenantConfiguration;
import com.tlc.email.common.model.exchange.EmailData;

import java.util.Objects;

/**
 * <p>
 * Encapsulates the details required for retrieving emails from inbox folder, And provides methods for accessing and modifying
 * these details.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class TenantSyncRequest extends EmailData implements RequestCodec, EmailModel {

    private final TenantConfiguration tenantConfiguration;

    private Long id;

    public TenantSyncRequest(final TenantConfiguration tenantConfiguration) {
        this.tenantConfiguration = tenantConfiguration;
    }

    public TenantConfiguration getTenantConfiguration() {
        return tenantConfiguration;
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

    /**
     * <p>
     * Provides a flexible and convenient way to construct instances of the enclosing class with a step-by-step approach.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    public static class Builder {

        private final TenantConfiguration tenantConfiguration;
        private StateData stateData;

        public Builder(final TenantConfiguration tenantConfiguration) {
            this.tenantConfiguration = tenantConfiguration;
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

        public TenantSyncRequest build() {
            final TenantSyncRequest request = new TenantSyncRequest(this.tenantConfiguration);

            request.setStateData(this.stateData);

            return request;
        }
    }
}
