package com.tlc.crm.core.notification.email.api.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.core.notification.email.api.manager.CrmEmailDomainManager;
import com.tlc.crm.core.notification.email.api.manager.DomainConfigManager;
import com.tlc.crm.core.notification.email.api.manager.impl.CrmEmailDomainManagerImpl;
import com.tlc.crm.core.notification.email.api.manager.impl.DomainConfigManagerImpl;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.service.CrmEmailDomainService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.email.client.service.EmailDomainService;
import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.notification.service.template.EmailTemplateBuildRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * Implementation of {@link CrmEmailDomainService} that handles operations related
 * to email domains, including persistence, retrieval, addition, deletion, and sending
 * DNS records via email.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class CrmEmailDomainServiceImpl implements CrmEmailDomainService {

    private static final String AT_STRING = "@";
    private static final char AT_CHAR = '@';

    private final CrmEmailDomainManager crmDomainManager;
    private final EmailNotificationBuilder emailNotificationBuilder;
    private final NoReplyMailService noReplyMailService;
    private final EmailDomainService emailDomainService;
    private final DomainConfigManager domainConfigManager;

    private static class Instance {
        private static final CrmEmailDomainService INSTANCE = new CrmEmailDomainServiceImpl();
    }

    /**
     * <p>
     * Private constructor initializes required manager and service instances.
     * </p>
     */
    private CrmEmailDomainServiceImpl() {
        this.crmDomainManager = CrmEmailDomainManagerImpl.getInstance();
        this.emailNotificationBuilder = EmailNotificationBuilder.getInstance();
        this.noReplyMailService = NoReplyMailService.get();
        this.emailDomainService = EmailDomainService.get();
        this.domainConfigManager = DomainConfigManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link CrmEmailDomainServiceImpl}.
     * </p>
     *
     * @return {@link CrmEmailDomainService} instance
     */
    public static CrmEmailDomainService getInstance() {
        return CrmEmailDomainServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Saves or updates the given {@link CrmEmailDomain} for the specified organization ID.
     * </p>
     *
     * @param orgId          organization ID
     * @param crmEmailDomain the domain entity to save
     */
    @Override
    public void save(final Long orgId, CrmEmailDomain crmEmailDomain) {
        crmDomainManager.create(orgId, crmEmailDomain);
    }

    /**
     * <p>
     * Adds a new domain via {@link EmailDomainService} for email handling.
     * </p>
     *
     * @param crmEmailDomain the domain entity to add
     * @param stateData      additional state information required for domain creation
     */
    @Override
    public void buildAndPublishDomainRequest(final CrmEmailDomain crmEmailDomain, final JsonObject stateData) {
        final EmailDomainRequest emailDomainRequest = new EmailDomainRequest();
        final EmailDomain emailDomain = new EmailDomain();

        emailDomain.setName(crmEmailDomain.getName());
        emailDomainRequest.setAction(EmailDomainRequest.Action.CREATE);
        emailDomainRequest.setDomain(emailDomain);
        emailDomainRequest.setStateData(
                new StateData(
                        stateData.put(CoreEmailFields.ORG_ID.getName(), crmEmailDomain.getOrgId())
                                .put(CoreEmailFields.USER_ID.getName(), crmEmailDomain.getUserId())
                )
        );

        emailDomainService.addDomain(emailDomainRequest);
    }

    /**
     * <p>
     * Updates an existing domain for the specified organization ID.
     * </p>
     *
     * @param orgId          organization ID
     * @param crmEmailDomain the domain entity to update
     */
    @Override
    public void updateDomain(final Long orgId, final CrmEmailDomain crmEmailDomain) {
        crmDomainManager.update(orgId, crmEmailDomain);
    }

    /**
     * <p>
     * Deletes a domain by ID for the specified organization.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID
     */
    @Override
    public void deleteDomain(final Long orgId, final Long id) {
        crmDomainManager.delete(orgId, id);
    }

    /**
     * <p>
     * Update the state for the given domain id.
     * </p>
     *
     * @param orgId    organization ID
     * @param id       domain ID to delete
     * @param isActive Represents whether the state is active or not.
     */
    @Override
    public void updateState(final Long orgId, final Long id, final boolean isActive) {
        crmDomainManager.updateState(orgId, id, isActive);
    }

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by organization ID and domain name.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName domain name to look up
     * @return optional {@link CrmEmailDomain} if found
     */
    public Optional<CrmEmailDomain> get(final Long orgId, final String domainName) {
        return crmDomainManager.get(orgId, domainName);
    }

    /**
     * <p>
     * Retrieves an {@link CrmEmailDomain} by organization ID and domain ID.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID
     * @return optional {@link CrmEmailDomain} if found
     */
    @Override
    public Optional<CrmEmailDomain> get(final Long orgId, final Long id) {
        return crmDomainManager.get(orgId, id);
    }

    /**
     * <p>
     * Retrieves the internal ID of a domain for a given organization.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName Represent the domain name
     * @return optional internal ID if found
     */
    @Override
    public Optional<Long> getId(final Long orgId, final String domainName) {
        return crmDomainManager.getId(orgId, domainName);
    }

    /**
     * <p>
     * Sends DNS records of the domain via email to the specified recipients.
     * </p>
     *
     * @param recipients     collection of recipient email addresses
     * @param crmEmailDomain the domain whose DNS records should be sent
     */
    @Override
    public void sendDnsRecordsViaMail(final Collection<String> recipients, final CrmEmailDomain crmEmailDomain) {
        final Map<String, EmailNotifiableModel> emailNotifiableModelMap = new HashMap<>();

        emailNotifiableModelMap.put(CommonFields.MODEL, crmEmailDomain);
        final Optional<Email> optionalSetUpMail = emailNotificationBuilder
                .build(EmailNotificationTemplate.DNS_RECORD_SET_UP_EMAIL.getName(),
                        new EmailTemplateBuildRequest(emailNotifiableModelMap));

        optionalSetUpMail.ifPresent(email -> {

            for (final String recipient : recipients) {
                final EmailRecipients emailRecipients = new EmailRecipients();

                emailRecipients.addToRecipient(recipient);
                email.setRecipients(List.of(emailRecipients));

                noReplyMailService.send(email);
            }
        });
    }

    /**
     * <p>
     * Checks if a domain exists for the specified organization by domain ID.
     * </p>
     *
     * @param orgId organization ID
     * @param id    domain ID
     * @return {@code true} if domain exists, {@code false} otherwise
     */
    @Override
    public boolean exists(final Long orgId, final Long id) {
        return crmDomainManager.exists(orgId, id);
    }

    /**
     * <p>
     * Checks if a domain exists for the specified organization by domain name.
     * </p>
     *
     * @param orgId      organization ID
     * @param domainName domain name
     * @return {@code true} if domain exists, {@code false} otherwise
     */
    @Override
    public boolean exists(final Long orgId, final String domainName) {
        return crmDomainManager.exists(orgId, domainName);
    }

    /**
     * <p>
     * Retrieves an existing domain for the given organization based on the user's email address,
     * or initializes and persists a new one if it does not exist.
     * </p>
     *
     * @param orgId        Represents the organization ID.
     * @param userId       Represents the user ID.
     * @param emailAddress Represents the user's email address.
     * @return existing or newly created {@link CrmEmailDomain}.
     */
    @Override
    public CrmEmailDomain getOrInitDomainByEmail(final Long orgId, final Long userId, final String emailAddress) {
        if (Objects.isNull(emailAddress) || !emailAddress.contains(AT_STRING)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_ADDRESS);
        }
        final String domainName = emailAddress.substring(emailAddress.indexOf(AT_CHAR) + 1).toLowerCase();
        final Optional<CrmEmailDomain> optionalEntityDomain = crmDomainManager.get(orgId, domainName);

        if (optionalEntityDomain.isPresent()) {
            return optionalEntityDomain.get();
        }
        final CrmEmailDomain crmEmailDomain = new CrmEmailDomain();

        crmEmailDomain.setOrgId(orgId);
        crmEmailDomain.setUserId(userId);
        crmEmailDomain.setName(domainName);
        crmEmailDomain.setActive(true);
        crmEmailDomain.setDomainStatus(DomainStatus.NEW);

        crmDomainManager.create(orgId, crmEmailDomain);

        return crmEmailDomain;
    }

    /**
     * <p>
     * Validates an EntityDomain by checking that its default email addresses match the domain,
     * and appends the domain to any missing addresses.
     * </p>
     *
     * @param orgId          Represents the organization ID.
     * @param crmEmailDomain Represent the crm domain.
     * @param domainConfig   Represent the domain config.
     */
    @Override
    public void validateDomainConfig(final Long orgId, final CrmEmailDomain crmEmailDomain, final DomainConfig domainConfig) {
        String domainName = crmEmailDomain.getName();

        if (Objects.isNull(domainName)) {
            final Long id = crmEmailDomain.getEmailDomainId();
            final Optional<CrmEmailDomain> existing = this.get(orgId, id);

            if (existing.isEmpty()) {
                throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found");
            }
            domainName = existing.get().getName();
        }
        domainConfig.setUserId(crmEmailDomain.getUserId());
        domainConfig.setOrgId(orgId);
        final FromAddressInfo defaultFromAddress = domainConfig.getDefaultFromAddress();
        EmailAddress defaultReplyToAddress = domainConfig.getDefaultReplyToAddress();

        if (Objects.nonNull(defaultFromAddress)) {
            validateAndNormalizeEmail(
                    defaultFromAddress::getEmailAddress, defaultFromAddress::setEmailAddress,
                    domainName, com.tlc.crm.email.notification.email.status.EmailErrorCodes.DEFAULT_FROM_ADDRESS_DOMAIN_MISMATCH
            );
        }

        if (Objects.isNull(defaultReplyToAddress)) {

            if (Objects.nonNull(defaultFromAddress)) {
                domainConfig.setDefaultReplyToAddress(new EmailAddress(defaultFromAddress.getEmailAddress()));

            }
        } else {
            validateAndNormalizeEmail(
                    defaultReplyToAddress::getEmailAddress, defaultReplyToAddress::setEmailAddress,
                    domainName, com.tlc.crm.email.notification.email.status.EmailErrorCodes.DEFAULT_REPLY_TO_ADDRESS_DOMAIN_MISMATCH
            );
        }
    }

    /**
     * <p>
     * Updates the {@link DomainConfig} for the given domain.
     * </p>
     *
     * @param domainConfig Represents the domain config to update.
     */
    @Override
    public void updateDomainConfig(final DomainConfig domainConfig) {
        domainConfigManager.update(domainConfig);
    }

    /**
     * <p>
     * Retrieves the {@link DomainConfig} for the given organization and domain ID.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param domainId Represents the domain ID.
     * @return An {@link Optional} containing the {@link DomainConfig} if found, or empty otherwise.
     */
    @Override
    public Optional<DomainConfig> getDomainConfig(final Long orgId, final Long domainId) {
        return domainConfigManager.get(orgId, domainId);
    }

    /**
     * <p>
     * Retrieves the total number of active, non-new entity domains for a given organization.
     * </p>
     *
     * @param orgId Represents the organization ID.
     * @return the total count of domains.
     */
    @Override
    public long getDomainCount(final long orgId) {
        return crmDomainManager.getDomainCount(orgId, null);
    }

    /**
     * <p>
     * Updates the status for the domain.
     * </p>
     *
     * @param orgId        organization ID
     * @param id           domain ID
     * @param domainStatus Represents the domain status.
     */
    public void updateDomainStatus(final Long orgId, final Long id, final DomainStatus domainStatus) {
        crmDomainManager.updateStatus(orgId, id, domainStatus);
    }

    /**
     * <p>
     * Validates a single email address by checking if its domain matches the given entity domain.
     * If the email is missing a domain, the entity domain is appended automatically.
     * </p>
     *
     * @param getter     Supplier to fetch the current email address.
     * @param setter     Consumer to set the potentially normalized email address.
     * @param domainName The expected domain name.
     * @param errorCode  The error code to throw if the domain does not match.
     */
    private void validateAndNormalizeEmail(final Supplier<String> getter, final Consumer<String> setter,
                                           final String domainName, final com.tlc.crm.email.notification.email.status.EmailErrorCodes errorCode) {
        final String email = getter.get();
        final String emailDomain = getDomainFromEmail(email);

        if (Objects.isNull(email) || email.isEmpty()) {
            return;
        }

        if (Objects.isNull(emailDomain) || emailDomain.isBlank()) {
            setter.accept(email + AT_STRING + domainName);
            return;
        }

        if (!emailDomain.equals(domainName)) {
            throw ErrorCode.get(errorCode);
        }
    }

    /**
     * <p>
     * Extracts the domain portion of an email address.
     * </p>
     *
     * @param email The email address from which to extract the domain.
     * @return The domain portion of the email, or null if invalid.
     */
    private String getDomainFromEmail(final String email) {
        if (Objects.isNull(email) || email.isBlank()) {
            return null;
        }
        final int atIndex = email.indexOf(AT_CHAR);

        if (atIndex < 0 || atIndex == email.length() - 1) {
            return null;
        }

        return email.substring(atIndex + 1).trim();
    }
}