package com.tlc.crm.email.core.consumer;

import com.tlc.commons.dns.model.Status;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.manager.CrmEmailDomainManager;
import com.tlc.crm.core.notification.email.api.manager.DomainConfigManager;
import com.tlc.crm.core.notification.email.api.manager.impl.CrmEmailDomainManagerImpl;
import com.tlc.crm.core.notification.email.api.manager.impl.DomainConfigManagerImpl;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.core.notification.email.inbox.fields.EmailDomainFields;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.service.CrmEmailDomainService;
import com.tlc.crm.core.notification.email.api.service.impl.CrmEmailDomainServiceImpl;
import com.tlc.crm.core.notification.email.inbox.api.model.converter.CrmEmailDomainConverter;
import com.tlc.crm.email.core.fields.EmailLinkerFields;
import com.tlc.email.common.codec.EmailDomainRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Handles incoming domain data load events from the queue.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainRequestActionHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDomainRequestActionHandler.class);

    private final CrmEmailDomainConverter crmEmailDomainConverter;
    private final CrmEmailDomainService crmEmailDomainService;
    private final EmailDomainRequestCodec emailDomainRequestCodec;
    private final CrmEmailDomainManager crmDomainManager;
    private final DomainConfigManager domainConfigManager;

    /**
     * <p>
     * Constructs a new {@link EmailDomainRequestActionHandler} with required converters, services, and codecs.
     * </p>
     */
    public EmailDomainRequestActionHandler() {
        this.crmEmailDomainConverter = CrmEmailDomainConverter.getInstance();
        this.crmEmailDomainService = CrmEmailDomainServiceImpl.getInstance();
        this.emailDomainRequestCodec = (EmailDomainRequestCodec) EmailCodec.get(EmailCodec.CodecType.DOMAIN);
        this.crmDomainManager = CrmEmailDomainManagerImpl.getInstance();
        this.domainConfigManager = DomainConfigManagerImpl.getInstance();
    }

    /**
     * <p>
     * Processes incoming domain load messages from the queue.
     * </p>
     *
     * @param streamMessage the incoming stream message containing domain request data
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final EmailDomainRequest emailDomainRequest = emailDomainRequestCodec.decode(Json.object(streamMessage.data()).getBytes());
        final JsonObject stateJson = emailDomainRequest.getStateData().toJson();
        final long orgId = stateJson.getLong(EmailLinkerFields.ORG_ID);
        final long userId = stateJson.getLong(EmailLinkerFields.USER_ID);

        LOGGER.debug("Email domain: Domain request processing initiated for orgId: {}, userId: {}", orgId, userId);

        secureProcess(orgId, userId, () -> processDomainRequest(orgId, userId, emailDomainRequest, stateJson));

        LOGGER.debug("Email domain: Domain request processing completed for orgId: {}, userId: {}", orgId, userId);
    }

    /**
     * <p>
     * Processes the domain request by converting, validating, and persisting domain data.
     * </p>
     *
     * @param orgId              the organization identifier
     * @param userId             the user identifier
     * @param emailDomainRequest the decoded domain request
     * @param stateJson          the state data JSON object
     */
    private void processDomainRequest(final long orgId, final long userId,
                                      final EmailDomainRequest emailDomainRequest, final JsonObject stateJson) {
        final CrmEmailDomain crmEmailDomain = buildCrmDomain(orgId, userId, emailDomainRequest, stateJson);
        final DomainConfig domainConfig = buildDomainConfig(orgId, userId, crmEmailDomain, stateJson);

        crmEmailDomainService.validateDomainConfig(orgId, crmEmailDomain, domainConfig);
        resolveExistingDomain(orgId, crmEmailDomain);
        loadDomainData(orgId, crmEmailDomain, domainConfig);
    }

    /**
     * <p>
     * Builds and returns a fully populated {@link CrmEmailDomain} from the request and state data.
     * </p>
     *
     * @param orgId              the organization identifier
     * @param userId             the user identifier
     * @param emailDomainRequest the decoded domain request
     * @param stateJson          the state data JSON object
     * @return the populated {@link CrmEmailDomain}
     */
    private CrmEmailDomain buildCrmDomain(final long orgId, final long userId,
                                          final EmailDomainRequest emailDomainRequest, final JsonObject stateJson) {
        final CrmEmailDomain crmEmailDomain = crmEmailDomainConverter.convert(orgId, userId, emailDomainRequest.getDomain());

        crmEmailDomain.setActive(true);
        crmEmailDomain.setOrgId(orgId);
        crmEmailDomain.setUserId(userId);
        crmEmailDomain.setCreatedAt(Instant.now().toEpochMilli());
        crmEmailDomain.setDomainStatus(resolveDomainStatus(stateJson));

        return crmEmailDomain;
    }

    /**
     * <p>
     * Builds and returns a fully populated {@link DomainConfig} from the state data.
     * </p>
     *
     * @param orgId          the organization identifier
     * @param userId         the user identifier
     * @param crmEmailDomain the resolved {@link CrmEmailDomain}
     * @param stateJson      the state data JSON object
     * @return the populated {@link DomainConfig}
     */
    private DomainConfig buildDomainConfig(final long orgId, final long userId,
                                           final CrmEmailDomain crmEmailDomain, final JsonObject stateJson) {
        final DomainConfig domainConfig = crmEmailDomainConverter.convertDomainConfig(stateJson);

        domainConfig.setOrgId(orgId);
        domainConfig.setUserId(userId);
        domainConfig.setDomainId(crmEmailDomain.id());

        return domainConfig;
    }

    /**
     * <p>
     * Resolves the domain status based on DNS record verification.
     * </p>
     *
     * @param stateJson the state data JSON object
     * @return {@link DomainStatus#VERIFIED} if all DNS records are verified, {@link DomainStatus#PENDING} otherwise
     */
    private DomainStatus resolveDomainStatus(final JsonObject stateJson) {
        return allDnsRecordsVerified(stateJson) ? DomainStatus.VERIFIED : DomainStatus.PENDING;
    }

    /**
     * <p>
     * Checks if an existing domain is present and assigns its ID to the given {@link CrmEmailDomain}.
     * </p>
     *
     * @param orgId          the organization identifier
     * @param crmEmailDomain the domain to resolve
     */
    private void resolveExistingDomain(final long orgId, final CrmEmailDomain crmEmailDomain) {
        final Optional<Long> existingDomainId = crmEmailDomainService.getId(orgId, crmEmailDomain.getName());

        existingDomainId.ifPresent(crmEmailDomain::setId);
    }

    /**
     * <p>
     * Persists domain and domain config data into the data store.
     * </p>
     *
     * @param orgId          the organization identifier
     * @param crmEmailDomain the domain to persist
     * @param domainConfig   the domain config to persist
     */
    private void loadDomainData(final long orgId, final CrmEmailDomain crmEmailDomain, final DomainConfig domainConfig) {
        final boolean isCreateEvent = Objects.isNull(crmEmailDomain.id());
        final DataContainer targetContainer = DataContainer.create();

        crmDomainManager.loadDomainDataIntoContainer(orgId, isCreateEvent, targetContainer, crmEmailDomain);
        domainConfig.setDomainId(crmEmailDomain.id());
        domainConfigManager.loadDomainConfigIntoContainer(orgId, isCreateEvent, targetContainer, domainConfig);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Checks whether all DNS records in the given state data are verified.
     * </p>
     *
     * @param jsonObject the state data containing DNS record statuses
     * @return {@code true} if all DNS records are verified, {@code false} otherwise
     */
    private boolean allDnsRecordsVerified(final JsonObject jsonObject) {
        if (!jsonObject.containsKey(EmailDomainFields.DNS_RECORDS)) {
            return false;
        }
        final JsonArray dnsRecords = jsonObject.getJsonArray(EmailDomainFields.DNS_RECORDS);
        final int size = dnsRecords.size();

        for (int index = 0; index < size; index++) {
            final JsonObject dnsRecord = dnsRecords.getJsonObject(index);

            if (dnsRecord.containsKey(EmailDomainFields.STATUS)) {
                final Status status = Status.get(dnsRecord.getShort(EmailDomainFields.STATUS));

                if (!status.isVerified()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Returns the {@link OrgDataStore} for the given org.
     * </p>
     *
     * @param orgId the organization identifier
     * @return the {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }
}