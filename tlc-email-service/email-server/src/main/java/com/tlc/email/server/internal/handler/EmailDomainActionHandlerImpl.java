package com.tlc.email.server.internal.handler;

import com.tlc.commons.dns.model.RecordType;
import com.tlc.commons.dns.model.Status;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.model.Category;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.EmailDomainMetaData;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.email.server.internal.field.EmailKeys;
import com.tlc.email.server.internal.manager.EmailDomainManager;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailService;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;
import com.tlc.email.server.service.EmailDomainActionHandler;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Default implementation of {@link EmailDomainActionHandler}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainActionHandlerImpl implements EmailDomainActionHandler {

    private final String trackingDomain;
    private final String trackingNode;

    private final EmailDomainManager emailDomainManager;
    private final DomainAuthenticatedEmailService authenticatedEmailService;
    private final RandomIdGenerator randomIdGenerator;

    /**
     * <p>
     * Private constructor to enforce singleton usage.
     * </p>
     */
    private EmailDomainActionHandlerImpl() {
        this.emailDomainManager = EmailDomainManager.getInstance();
        this.authenticatedEmailService = DomainAuthenticatedEmailServiceProvider.getInstance().getService();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance()
                .getProperties(EmailKeys.EMAIL.getKey())
                .getNestedConfigs(EmailKeys.DOMAIN.getKey());

        this.trackingDomain = configurations.getConfig(EmailKeys.NAME.getKey());
        this.trackingNode = configurations.getConfig(EmailKeys.NODE.getKey());
    }

    /**
     * <p>
     * Holds the singleton instance of {@link EmailDomainActionHandlerImpl}.
     * </p>
     */
    private static class Instance {
        private static final EmailDomainActionHandlerImpl INSTANCE =
                new EmailDomainActionHandlerImpl();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainActionHandlerImpl}.
     * </p>
     *
     * @return The {@link EmailDomainActionHandlerImpl} instance.
     */
    public static EmailDomainActionHandlerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Handles domain creation and update requests.
     * </p>
     *
     * @param emailDomainRequest Represents the domain request to be processed.
     */
    @Override
    public void add(final EmailDomainRequest emailDomainRequest) {
        final EmailDomain emailDomain = emailDomainRequest.getDomain();
        final Optional<EmailDomain> optionalDomain = emailDomainManager.getDomainByName(emailDomain.getName());

        if (optionalDomain.isEmpty()) {
            authenticatedEmailService.addDomain(emailDomain);
            loadInternalDnsRecord(emailDomain);
            emailDomainManager.create(emailDomain);
        } else {
            final EmailDomain existingEmailDomain = optionalDomain.get();

            emailDomain.setId(existingEmailDomain.id());
            final EmailDomainMetaData emailDomainMetaData = getDomainMetaData(existingEmailDomain.getEmailDomainMetaData());

            emailDomain.setEmailDomainMetaData(emailDomainMetaData);
            emailDomain.setDnsRecords(
                    existingEmailDomain.getDnsRecords()
                            .stream()
                            .filter(dnsRecord -> dnsRecord instanceof EmailDnsRecord emailDnsRecord &&
                                    emailDnsRecord.getRecordProvider() == EmailDnsRecord.RecordProvider.SENDGRID)
                            .collect(Collectors.toSet())
            );

            loadInternalDnsRecord(emailDomain);
            emailDomainManager.update(emailDomain);
        }
    }

    /**
     * <p>
     * Converts an internal {@link EmailDomainMetaData} object into a new {@link EmailDomainMetaData} instance.
     * </p>
     *
     * @param emailDomainMetaData the internal {@link EmailDomainMetaData} object to convert.
     * @return a new {@link EmailDomainMetaData} instance mapped from the input.
     */
    private EmailDomainMetaData getDomainMetaData(final EmailDomainMetaData emailDomainMetaData) {
        final EmailDomainMetaData metaData = new EmailDomainMetaData();

        if (Objects.nonNull(emailDomainMetaData)) {
            metaData.setExternalId(emailDomainMetaData.getExternalId());
            metaData.setServiceProvider(emailDomainMetaData.getServiceProvider());
        }

        return metaData;
    }

    /**
     * <p>
     * Loads and appends the internally generated DNS tracking record
     * to the domain if DNS records are present.
     * </p>
     *
     * @param emailDomain Represents the domain to which the DNS record is added.
     */
    private void loadInternalDnsRecord(final EmailDomain emailDomain) {
        final Collection<EmailDnsRecord> emailDnsRecords = emailDomain.getEmailDnsRecords();

        if (Objects.nonNull(emailDnsRecords)) {
            emailDomain.addDnsRecord(createInternalDnsRecord(emailDomain.getName()));
        }
    }

    /**
     * <p>
     * Creates an internal tracking DNS record for the given domain.
     * If an existing internal record is found, its value is reused.
     * </p>
     *
     * @param domainName Represents the domain name.
     * @return The internally generated {@link EmailDnsRecord}.
     */
    private EmailDnsRecord createInternalDnsRecord(final String domainName) {
        final EmailDnsRecord emailDnsRecord = new EmailDnsRecord();

        emailDnsRecord.setRecordProvider(EmailDnsRecord.RecordProvider.INTERNAL);
        emailDnsRecord.setCategory(Category.TRACKING);
        emailDnsRecord.setStatus(Status.PENDING);
        emailDnsRecord.setRecordType(RecordType.CNAME);
        emailDnsRecord.setHost(build(domainName, randomIdGenerator.generateUniqueId()));
        emailDnsRecord.setValue(build(domainName));

        return emailDnsRecord;
    }

    /**
     * <p>
     * Builds the DNS host name for tracking using a generated random identifier.
     * </p>
     *
     * @param domainName Represents the domain name.
     * @return The constructed DNS host.
     */
    private String build(final String domainName) {
        return build(domainName, null);
    }

    /**
     * <p>
     * Constructs a DNS name using the tracking node, optional random identifier,
     * and the given domain name.
     * </p>
     *
     * @param domainName Represents the domain name.
     * @param randomId   Represents an optional random identifier.
     * @return The constructed DNS name.
     */
    private String build(final String domainName, final String randomId) {
        final boolean isRandomIdNotExits = Objects.isNull(randomId);
        final String suffix = StringUtil.Delimiter.DOT.getDelimiter() + (isRandomIdNotExits ? trackingDomain : domainName);

        return isRandomIdNotExits ? trackingNode + suffix : trackingNode + randomId + suffix;
    }
}