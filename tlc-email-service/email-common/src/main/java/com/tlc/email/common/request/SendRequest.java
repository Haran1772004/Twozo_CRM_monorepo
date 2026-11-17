package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.exchange.EmailData;
import com.tlc.email.common.model.type.OutboundEmail;

import java.util.Objects;

/**
 * Represents a request to send an email notification.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SendRequest extends EmailData implements RequestCodec {

    private OutboundEmail email;
    private ProviderType providerType = ProviderType.OAUTH_CLIENT;

    private boolean isTransactionalMail;

    public OutboundEmail getEmail() {
        return email;
    }

    public void setEmail(final OutboundEmail email) {
        this.email = email;
    }

    public boolean isTransactionalMail() {
        return isTransactionalMail;
    }

    public void setTransactionalMail(final boolean transactionalMail) {
        isTransactionalMail = transactionalMail;
    }


    public void setProviderType(final ProviderType providerType) {
        this.providerType = providerType;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public enum ProviderType {
        OAUTH_CLIENT,
        SERVICE_PROVIDER,
        ;

        public boolean isServiceProvider() {
            return Objects.equals(this, SERVICE_PROVIDER);
        }
    }
}
