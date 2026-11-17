package com.tlc.email.server.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.server.internal.manager.EmailDomainManager;
import com.tlc.email.server.service.EmailDomainSenderService;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implementation of {@link EmailDomainSenderService} that provides services
 * for resolving and managing domain sender information based on email addresses.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainSenderServiceImpl implements EmailDomainSenderService {

    private static final char AT_SYMBOL = '@';

    private final EmailDomainManager emailDomainManager;

    /**
     * <p>
     * Creates a singleton instance of the domain sender service.
     * </p>
     */
    private EmailDomainSenderServiceImpl() {
        this.emailDomainManager = EmailDomainManager.getInstance();
    }

    /**
     * <p>
     * Holder class for lazy-loaded singleton instance.
     * </p>
     */
    private static class Holder {
        private static final EmailDomainSenderService INSTANCE = new EmailDomainSenderServiceImpl();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainSenderService}.
     * </p>
     *
     * @return The {@link EmailDomainSenderService} instance.
     */
    public static EmailDomainSenderService getInstance() {
        return EmailDomainSenderServiceImpl.Holder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getSenderId(final Long id, final String emailAddress) {
        final String domainName = getDomainName(emailAddress);

        if (Objects.isNull(domainName)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_DOMAIN);
        }
        final Optional<EmailDomain> optionalDomain = emailDomainManager.getDomainByName(domainName);

        if (optionalDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_AUTHORIZED);
        }

        return emailDomainManager.getOrInitDomainSenderId(optionalDomain.get().id(), id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName(final String emailAddress) {
        if (Objects.isNull(emailAddress) || emailAddress.isBlank()) {
            return null;
        }
        final int atIndex = emailAddress.lastIndexOf(AT_SYMBOL);

        if (atIndex == -1 || atIndex == emailAddress.length() - 1) {
            return null;
        }

        return emailAddress.substring(atIndex + 1).toLowerCase();
    }
}