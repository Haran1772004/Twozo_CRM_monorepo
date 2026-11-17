package com.tlc.email.common.service;

import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.address.EmailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * Base class for implementing add-on email services. Provides common
 * functionalities such as generating service accounts and shared
 * mailbox addresses.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractAddOnService implements EmailAddOnService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAddOnService.class);

    protected final RandomIdGenerator randomIdGenerator;

    protected String domain;
    protected String prefix;
    protected String envDomain;
    protected boolean includeEnvInSharedAddress;

    protected AbstractAddOnService() {
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    /**
     * <p>
     * Retrieves the configured domain for the service.
     * </p>
     *
     * @return domain as {@link String}
     */
    @Override
    public String getDomain() {
        return domain;
    }

    /**
     * <p>
     * Builds the service account email address using the service name
     * and the configured domain.
     * Format: {@code serviceName + domain}
     * </p>
     *
     * @return {@link EmailAddress} representing the service account
     */
    @Override
    public EmailAddress getAccount() {
        return new EmailAddress(String.format("%s%s", this.getServiceType().getName(), domain));
    }

    /**
     * <p>
     * Generates a unique shared mailbox email address using the prefix,
     * a random ID, and the configured domain. When includeEnvInSharedAddress
     * is true the format will be: {@code prefix-env-random@domain} (no extra hyphens
     * inside prefix or env).
     * </p>
     *
     * @return generated shared mailbox email address as {@link String}
     */
    @Override
    public String generateSharedMailBoxAddress() {
        final String random = randomIdGenerator.generateUniqueId().replace(StringUtil.Delimiter.HYPHEN.getDelimiter(),
                StringUtil.Delimiter.UNDER_SCORE.getDelimiter());

        LOGGER.info("SharedMailBox: generating address. serviceType=[{}], prefix=[{}], envDomain=[{}], "
                        + "includeEnvInSharedAddress=[{}], domain=[{}], random=[{}]",
                getServiceType(), prefix, envDomain, includeEnvInSharedAddress, domain, random);

        if (!includeEnvInSharedAddress || envDomain == null || envDomain.isBlank()) {
            final String address = String.format("%s+%s%s", prefix, random, domain);

            LOGGER.info("SharedMailBox: env not included. includeEnvInSharedAddress=[{}], envDomainBlank=[{}], address=[{}]",
                    includeEnvInSharedAddress, envDomain == null || envDomain.isBlank(), address);

            return address;
        }
        final String sanitizedPrefix = Objects.isNull(prefix) ? StringUtil.Delimiter.EMPTY_STRING.getDelimiter()
                : prefix.replace(StringUtil.Delimiter.HYPHEN.getDelimiter(), StringUtil.Delimiter.EMPTY_STRING.getDelimiter());
        final String sanitizedEnvDomain = envDomain.replace(StringUtil.Delimiter.HYPHEN.getDelimiter(), StringUtil.Delimiter.DOT.getDelimiter());
        final String address = String.format("%s-%s-%s%s", sanitizedPrefix, sanitizedEnvDomain, random, domain);

        LOGGER.info("SharedMailBox: env included. sanitizedPrefix=[{}], sanitizedEnvDomain=[{}], address=[{}]",
                sanitizedPrefix, sanitizedEnvDomain, address);

        return address;
    }

    /**
     * <p>
     * Initializes the service by loading domain and addon configurations.
     * Supports optional keys: alias. Prefix, env, includeEnvInSharedAddress.
     * </p>
     */
    public void start(final Configurations configurations) {
        this.domain = configurations.getConfig(EmailFields.DOMAIN);

        if (this.prefix == null || this.prefix.isBlank()) {
            this.prefix = configurations.optConfig(EmailFields.ALIAS_PREFIX, StringUtil.Delimiter.EMPTY_STRING.getDelimiter());
        }
        this.envDomain = configurations.optConfig(EmailFields.ENV, "").trim();
        this.includeEnvInSharedAddress = Boolean.parseBoolean(configurations.optConfig(EmailFields.INCLUDE_ENV_IN_SHARED_ADDRESS, "false"));

        LOGGER.info("AddOnService started. domain=[{}], prefix=[{}], envDomain=[{}], includeEnvInSharedAddress=[{}]",
                domain, prefix, envDomain, includeEnvInSharedAddress);
    }

    /**
     * <p>
     * Stops the service. Override to perform cleanup if required.
     * </p>
     */
    @Override
    public void stop() {
        // Need to clean when needed
    }
}