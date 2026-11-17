package com.tlc.email.common.request;

import com.tlc.email.common.codec.RequestCodec;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.exchange.EmailData;

/**
 * <p>
 * Represents a request object used for performing domain-related actions
 * such as create, update, or delete within the email system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainRequest extends EmailData implements RequestCodec {

    private EmailDomain emailDomain;
    private Action action;

    /**
     * <p>
     * Default constructor for {@link EmailDomainRequest}.
     * </p>
     */
    public EmailDomainRequest() {
    }

    /**
     * <p>
     * Returns the {@link EmailDomain} associated with this request.
     * </p>
     *
     * @return The domain entity.
     */
    public EmailDomain getDomain() {
        return emailDomain;
    }

    /**
     * <p>
     * Returns the {@link Action} to be performed on the domain.
     * </p>
     *
     * @return The domain action.
     */
    public Action getAction() {
        return action;
    }

    /**
     * <p>
     * Sets the {@link Action} to be performed on the domain.
     * </p>
     *
     * @param action The domain action.
     */
    public void setAction(final Action action) {
        this.action = action;
    }

    /**
     * <p>
     * Sets the {@link EmailDomain} associated with this request.
     * </p>
     *
     * @param emailDomain The domain entity.
     */
    public void setDomain(final EmailDomain emailDomain) {
        this.emailDomain = emailDomain;
    }

    /**
     * <p>
     * Represents the supported actions that can be performed on a domain.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Action {

        CREATE,
        UPDATE,
        DELETE,
        ;
    }
}