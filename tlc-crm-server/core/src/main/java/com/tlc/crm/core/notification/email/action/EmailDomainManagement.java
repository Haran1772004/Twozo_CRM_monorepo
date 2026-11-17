package com.tlc.crm.core.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.Status;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.internal.UserServiceImpl;
import com.tlc.crm.core.notification.email.api.model.email.Criteria;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDnsRecord;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.core.notification.email.api.service.CrmEmailDomainService;
import com.tlc.crm.core.notification.email.api.service.EmailDomainFilterService;
import com.tlc.crm.core.notification.email.api.service.impl.CrmEmailDomainServiceImpl;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.inbox.api.model.converter.CrmEmailDomainConverter;
import com.tlc.crm.core.notification.email.inbox.fields.EmailDomainFields;
import com.tlc.crm.core.page.CrmEmailDomainPageHandler;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.email.client.service.EmailDomainService;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles all CRM-side email domain operations including
 * creation, update, deletion, retrieval, DNS resolution,
 * and notification workflows.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/email/domain")
public class EmailDomainManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDomainManagement.class);

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final String SUCCESS = "SUCCESS";
    private static final String INITIATED = "INITIATED";
    private static final String userRetrievalFailedI18NKey = "i18n.common.user.retrieval.failed";

    private final CrmEmailDomainConverter entityEmailDomainConverter;
    private final EmailDomainService emailDomainService;
    private final CrmEmailDomainService crmEmailDomainService;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrganizationService organizationService;
    private final EmailDomainConverter emailDomainConverter;
    private final EmailDomainFilterService emailDomainFilterService;

    /**
     * <p>
     * Initializes domain services and converters.
     * </p>
     */
    public EmailDomainManagement() {
        this.entityEmailDomainConverter = CrmEmailDomainConverter.getInstance();
        this.emailDomainService = EmailDomainService.get();
        this.crmEmailDomainService = CrmEmailDomainServiceImpl.getInstance();
        this.userService = UserService.getInstance();
        this.crmUserService = UserServiceImpl.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.emailDomainConverter = EmailDomainConverter.getInstance();
        this.emailDomainFilterService = EmailDomainFilterService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Routes incoming requests to the appropriate
     * email domain operation based on action type.
     * </p>
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(EmailFields.DATA.getName());
        final String type = requestJson.getString(EmailFields.TYPE.getName());

        switch (type) {
            case "create" -> create(crmRequest, crmResponse, data);
            case "update" -> update(crmRequest, crmResponse, data);
            case "delete" -> delete(crmRequest, crmResponse, data);
            case "get" -> get(crmRequest, crmResponse, data);
            case "list" -> processDomainList(crmRequest, crmResponse, data);
            case "getDnsRecords" -> getDnsRecords(crmRequest, crmResponse, data);
            case "verifyDomain" -> verify(crmRequest, crmResponse, data);
            case "sendDnsRecordsViaMail" -> sendDnsRecordsViaMail(crmRequest, crmResponse, data);
            case "getFilter" -> getFilter(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Retrieves the email domain filter preference for the current user
     * and sets it in the response object.
     * </p>
     *
     * @param crmRequest  The {@link CrmRequest} containing organization and user details.
     * @param crmResponse The {@link CrmResponse} where the filter information will be added.
     */
    private void getFilter(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Optional<EmailDomainFilterPreference> optionalEmailDomainFilterPreference = emailDomainFilterService
                .get(crmRequest.orgId(), crmRequest.userId());

        crmResponse.put(EmailDomainFields.FILTER, getFilter(optionalEmailDomainFilterPreference.orElseGet(() -> null)));
    }

    /**
     * <p>
     * Builds a JSON representation of the given {@link EmailDomainFilterPreference}.
     * </p>
     *
     * @param emailDomainFilterPreference The filter preference to convert.
     * @return A {@link JsonObject} containing the filter details.
     */
    private JsonObject getFilter(final EmailDomainFilterPreference emailDomainFilterPreference) {
        final JsonObject jsonObject = Json.object();

        if (Objects.isNull(emailDomainFilterPreference)) {
            return jsonObject;
        }
        jsonObject.put(EmailDomainFields.STATUS, emailDomainFilterPreference.getStatus());

        return jsonObject;
    }

    /**
     * <p>
     * Verifies the DNS records of an email domain for the organization and retrieves
     * the latest verification status for the requested records.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param data        Represents the request data containing domain and DNS record IDs.
     */
    private void verify(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final Long orgId = crmRequest.orgId();
        final Collection<Long> dnsRecordIds = emailDomainConverter.getIds(data.getJsonArray(EmailFields.IDS.getName()));
        final Long domainId = data.getLong(EmailDomainFields.DOMAIN_ID);
        final Optional<CrmEmailDomain> optionalCrmDomain = crmEmailDomainService.get(orgId, domainId);

        if (optionalCrmDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found");
        }

        if (Objects.isNull(dnsRecordIds) || dnsRecordIds.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DNS_RECORD_IS_EMPTY);
        }
        final CrmEmailDomain crmEmailDomain = optionalCrmDomain.get();
        final Map<Long, CrmEmailDnsRecord> crmDnsRecordMap = crmEmailDomain.getCrmDnsRecords()
                .stream().collect(Collectors.toMap(CrmEmailDnsRecord::getEmailDnsRecordId, Function.identity()));
        final Collection<Long> emailDnsRecordIds = getEmailRecordIds(crmEmailDomain, dnsRecordIds);

        crmResponse.convertIntoAsync();
        emailDomainService.verifyAndFetchDnsRecords(crmEmailDomain.getEmailDomainId(), emailDnsRecordIds, new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<EmailDnsRecord> emailDnsRecords) {

                try {
                    final JsonArray jsonArray = Json.array();
                    boolean allAreVerified = true;

                    for (final EmailDnsRecord emailDnsRecord : emailDnsRecords) {
                        final Long id = emailDnsRecord.getId();
                        final JsonObject dnsJson = Json.object();
                        final Status status = emailDnsRecord.getStatus();

                        dnsJson.put(EmailDomainFields.ID, crmDnsRecordMap.get(id).id());
                        dnsJson.put(EmailDomainFields.STATUS, status.name());

                        if (!status.isVerified()) {
                            allAreVerified = false;
                        }
                        jsonArray.put(dnsJson);
                    }
                    updateDomainStatus(orgId, domainId, allAreVerified, crmEmailDomain);

                    crmResponse.put(
                            EmailDomainFields.DOMAIN, Json.object()
                                    .put(EmailDomainFields.DOMAIN_ID, domainId)
                                    .put(EmailDomainFields.DNS_RECORDS, jsonArray)
                                    .put(EmailDomainFields.CODE, allAreVerified ? SUCCESS : INITIATED)
                                    .put(EmailDomainFields.MESSAGE, crmResponse.resolveI18nString(allAreVerified ?
                                            "i18n.email.domain.verification.success" : "i18n.email.domain.verification.initiated"))
                    );
                } catch (Exception exception) {
                    LOGGER.error("Email domain: Failed to resolve dns records for the domain. Reason: {}, DomainId: {}, OrgId: {}",
                            exception.getMessage(), domainId, orgId);

                    sendError(crmResponse, null, exception);
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(EmailErrorCodes.DNS_RECORDS_RETRIEVE_ACTION_FAILED,
                        "i18n.email.domain.dns.retrieve.failed", throwable));
            }
        });
    }


    /**
     * <p>
     * Updates the domain status based on DNS verification results.
     * </p>
     *
     * @param orgId             Represents the organization ID
     * @param id                Represents the domain ID to be updated
     * @param allDnsAreVerified Represents the flag indicating if all DNS records are verified
     * @param crmEmailDomain    Represents the email domain object containing the current domain status
     */
    private void updateDomainStatus(final Long orgId, final Long id, final boolean allDnsAreVerified,
                                    final CrmEmailDomain crmEmailDomain) {
        if (Objects.nonNull(crmEmailDomain) && !crmEmailDomain.getDomainStatus().isNew()) {
            crmEmailDomainService.updateDomainStatus(orgId, id, allDnsAreVerified ? DomainStatus.VERIFIED : DomainStatus.PENDING);
        }
    }

    /**
     * <p>
     * Maps the provided CRM DNS record IDs to the corresponding internal Entity DNS record IDs
     * for the given email domain.
     * </p>
     *
     * @param crmEmailDomain The email domain containing the DNS records.
     * @param dnsRecordIds   The collection of CRM DNS record IDs to map.
     * @return A collection of internal Entity DNS record IDs corresponding to the given CRM IDs.
     */
    private Collection<Long> getEmailRecordIds(final CrmEmailDomain crmEmailDomain, final Collection<Long> dnsRecordIds) {
        final Map<Long, CrmEmailDnsRecord> entityDnsRecordMap = crmEmailDomain.getCrmDnsRecords()
                .stream().collect(Collectors.toMap(CrmEmailDnsRecord::id, Function.identity()));
        final Collection<Long> emailDnsRecordIds = new HashSet<>();

        for (final Long id : dnsRecordIds) {
            emailDnsRecordIds.add(entityDnsRecordMap.get(id).getEmailDnsRecordId());
        }

        return emailDnsRecordIds;
    }

    /**
     * <p>
     * Creates a new email domain mapping for the organization.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void create(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final CrmEmailDomain crmEmailDomain = entityEmailDomainConverter.convert(jsonObject);

        if (crmEmailDomainService.exists(crmRequest.orgId(), crmEmailDomain.getName())) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NAME_ALREADY_EXISTS, "i18n.email.domain.name.exists");
        }
        crmEmailDomain.setOrgId(crmRequest.orgId());
        crmEmailDomain.setUserId(crmRequest.userId());
        crmEmailDomainService.buildAndPublishDomainRequest(crmEmailDomain, jsonObject);
        crmResponse.put(EmailDomainFields.MESSAGE, crmResponse.resolveI18nString("i18n.email.domain.create.successfully"));
    }

    /**
     * <p>
     * Deletes an existing email domain mapping.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void delete(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Long orgId = crmRequest.orgId();
        final CrmEmailDomain crmEmailDomain = entityEmailDomainConverter.convert(jsonObject);
        final Optional<CrmEmailDomain> optionalCrmDomain = crmEmailDomainService.get(orgId, crmEmailDomain.id());

        if (optionalCrmDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found");
        }
        crmEmailDomainService.updateState(orgId, optionalCrmDomain.get().id(), false);
        crmResponse.put(EmailDomainFields.MESSAGE, crmResponse.resolveI18nString("i18n.email.domain.delete.successfully"));
    }

    /**
     * <p>
     * Updates metadata for an existing email domain.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void update(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Long orgId = crmRequest.orgId();
        final CrmEmailDomain crmEmailDomain = entityEmailDomainConverter.convert(jsonObject);
        final Long domainId = crmEmailDomain.id();
        final Optional<CrmEmailDomain> optionalCrmDomain = crmEmailDomainService.get(orgId, domainId);

        if (optionalCrmDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found");
        }
        final CrmEmailDomain existingCrmEmailDomain = optionalCrmDomain.get();

        if (!Objects.equals(existingCrmEmailDomain.getName(), crmEmailDomain.getName())) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NAME_MODIFICATION_ACTION_RESTRICTED, "i18n.email.domain.name.modification.restricted");
        }
        final DomainConfig domainConfig = entityEmailDomainConverter.convertDomainConfig(jsonObject);

        loadExistingValues(crmEmailDomain, existingCrmEmailDomain);
        domainConfig.setDomainId(domainId);

        crmEmailDomainService.validateDomainConfig(orgId, crmEmailDomain, domainConfig);
        crmEmailDomainService.updateDomainConfig(domainConfig);

        crmResponse.put(EmailDomainFields.MESSAGE, crmResponse.resolveI18nString("i18n.email.domain.update.successfully"));
    }

    /**
     * <p>
     * Copies existing domain field values into the given {@link CrmEmailDomain} instance
     * to preserve fields that should not be overwritten during an update.
     * </p>
     *
     * @param crmEmailDomain         Represents the entity domain being updated.
     * @param existingCrmEmailDomain Represents the existing entity domain loaded from the database.
     */
    private void loadExistingValues(final CrmEmailDomain crmEmailDomain, final CrmEmailDomain existingCrmEmailDomain) {
        crmEmailDomain.setDomainStatus(existingCrmEmailDomain.getDomainStatus());
        crmEmailDomain.setCreatedAt(existingCrmEmailDomain.getCreatedAt());
        crmEmailDomain.setUserId(existingCrmEmailDomain.getUserId());
        crmEmailDomain.setOrgId(existingCrmEmailDomain.getOrgId());
        crmEmailDomain.setActive(existingCrmEmailDomain.isActive());
        crmEmailDomain.setId(existingCrmEmailDomain.id());
    }

    /**
     * <p>
     * Retrieves a single domain without DNS enrichment.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void get(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Long orgId = crmRequest.orgId();
        final Optional<CrmEmailDomain> optionalCrmDomain = crmEmailDomainService.get(orgId, jsonObject.getLong(EmailDomainFields.ID));

        if (optionalCrmDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found");
        }
        final CrmEmailDomain crmEmailDomain = optionalCrmDomain.get();
        final Long domainId = crmEmailDomain.id();

        if (jsonObject.optBoolean(EmailDomainFields.INCLUDE_DOMAIN_CONFIG)) {
            final Optional<DomainConfig> optionalDomainConfig = crmEmailDomainService.getDomainConfig(orgId, domainId);

            optionalDomainConfig.ifPresent(domainConfig ->
                    crmResponse.put(EmailDomainFields.CONFIG, entityEmailDomainConverter.convertDomainConfig(domainConfig)));
        }
        crmResponse.put(EmailDomainFields.ID, domainId);
        crmResponse.put(EmailDomainFields.NAME, crmEmailDomain.getName());
        crmResponse.put(EmailDomainFields.CREATED_AT, crmEmailDomain.getCreatedAt());
    }

    /**
     * <p>
     * Maps DNS records from Email service to entity records.
     * </p>
     *
     * @param emailDomain    Represents the domain.
     * @param crmEmailDomain Represents the crm domain.
     */
    private void loadData(final EmailDomain emailDomain, final CrmEmailDomain crmEmailDomain) {
        final Map<Long, EmailDnsRecord> dnsRecordMap = emailDomain.getEmailDnsRecords().stream()
                .collect(Collectors.toMap(EmailDnsRecord::getId, Function.identity()));

        for (final CrmEmailDnsRecord crmEmailDnsRecord : crmEmailDomain.getCrmDnsRecords()) {
            final EmailDnsRecord emailDnsRecord = dnsRecordMap.get(crmEmailDnsRecord.getEmailDnsRecordId());

            if (Objects.nonNull(emailDnsRecord)) {
                loadDnsRecord(emailDnsRecord, crmEmailDnsRecord);
            }
        }
    }

    /**
     * <p>
     * Copies DNS attributes to entity DNS record.
     * </p>
     *
     * @param emailDnsRecord    Represents the dnsRecord.
     * @param crmEmailDnsRecord Represents the entity dns record.
     */
    private void loadDnsRecord(final EmailDnsRecord emailDnsRecord, final CrmEmailDnsRecord crmEmailDnsRecord) {
        crmEmailDnsRecord.setHost(emailDnsRecord.getHost());
        crmEmailDnsRecord.setValue(emailDnsRecord.getValue());
        crmEmailDnsRecord.setRecordType(emailDnsRecord.getRecordType());
        crmEmailDnsRecord.setCategory(emailDnsRecord.getCategory());
        crmEmailDnsRecord.setStatus(emailDnsRecord.getStatus());
    }

    /**
     * <p>
     * Retrieves DNS records for the given domain.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void getDnsRecords(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final CrmEmailDomain crmEmailDomain = getEntityDomainOrThrow(crmRequest.orgId(), jsonObject.getLong(EmailDomainFields.ID));

        crmResponse.convertIntoAsync();

        emailDomainService.fetchDomainById(crmEmailDomain.getEmailDomainId(), new EventCallback<>() {

            @Override
            public void onSuccess(final EmailDomain emailDomain) {

                try {
                    final JsonArray dnsArray = Json.array();
                    final Map<Long, CrmEmailDnsRecord> crmDnsRecordMap = crmEmailDomain.getCrmDnsRecords().stream()
                            .collect(Collectors.toMap(CrmEmailDnsRecord::getEmailDnsRecordId, Function.identity()));

                    emailDomain.getEmailDnsRecords().stream()
                            .sorted(Comparator.comparing(EmailDnsRecord::getId))
                            .forEach(record -> {
                                final CrmEmailDnsRecord crmEmailDnsRecord = crmDnsRecordMap.get(record.getId());

                                if (Objects.nonNull(crmEmailDnsRecord)) {
                                    dnsArray.put(entityEmailDomainConverter.convert(crmEmailDnsRecord.id(), record));
                                }
                            });
                    crmResponse.put(EmailDomainFields.DNS_RECORDS, dnsArray);
                } catch (Exception exception) {
                    LOGGER.error("Email domain: Failed to resolve dns records. Reason: {}, DomainId: {}, OrgId: {}",
                            exception.getMessage(), crmEmailDomain.getEmailDomainId(), crmRequest.orgId());

                    sendError(crmResponse, null, exception);
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(EmailErrorCodes.DOMAIN_RETRIEVAL_FAILED,
                        "i18n.email.domain.dns.retrieve.failed", throwable));
            }
        });
    }

    /**
     * <p>
     * Sends DNS records to the specified email addresses.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param jsonObject  Represents the jsonObject.
     */
    private void sendDnsRecordsViaMail(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Collection<String> emailAddresses = extractEmailAddresses(jsonObject);
        final Long orgId = crmRequest.orgId();
        final Long domainId = jsonObject.getLong(EmailDomainFields.ID);
        final CrmEmailDomain crmEmailDomain = getEntityDomainOrThrow(orgId, domainId);

        crmResponse.convertIntoAsync();
        emailDomainService.fetchDomainById(crmEmailDomain.getEmailDomainId(), new EventCallback<>() {

            @Override
            public void onSuccess(final EmailDomain emailDomain) {

                try {
                    loadData(emailDomain, crmEmailDomain);
                    crmEmailDomainService.sendDnsRecordsViaMail(emailAddresses, crmEmailDomain);

                    crmResponse.put(EmailDomainFields.MESSAGE, crmResponse.resolveI18nString("i18n.email.domain.dns.mail.sent.successfully"));
                } catch (Exception exception) {
                    LOGGER.error("Email domain: Failed to send dns records via mail. Reason: {}, DomainId: {}, OrgId: {}",
                            exception.getMessage(), domainId, orgId);

                    sendError(crmResponse, null, exception);
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(EmailErrorCodes.DOMAIN_RETRIEVAL_FAILED,
                        "i18n.email.domain.dns.retrieve.failed", throwable));
            }
        });
    }

    /**
     * <p>
     * Returns a paginated list of domains.
     * </p>
     *
     * @param crmRequest  Represents the request.
     * @param crmResponse Represents the response.
     * @param data        Represents te jsonObject.
     */
    private void processDomainList(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final int start = Math.max(data.optInt(CoreEmailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(CoreEmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();
        final EmailDomainFilterPreference emailDomainFilterPreference = getEmailDomainFilterPreference(orgId, userId, data);
        final Criteria criteria = entityEmailDomainConverter.getCriteria(data);
        final CrmEmailDomainPageHandler crmEmailDomainPageHandler = new CrmEmailDomainPageHandler(orgId, start, limit, emailDomainFilterPreference, criteria);
        final long totalRecords = crmEmailDomainPageHandler.getTotalRecordsCount();
        final JsonArray jsonArray = Json.array();

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<CrmEmailDomain> crmEmailDomains = crmEmailDomainPageHandler.getCurrentPage().getData();
            final Collection<Long> userIds = crmEmailDomains.stream().map(CrmEmailDomain::getUserId).collect(Collectors.toSet());
            final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds);
            final Map<Long, CrmUser> crmUserIdToCrmUserMap = crmUsers.stream()
                    .collect(Collectors.toMap(CrmUser::id, Function.identity()));
            final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId).collect(Collectors.toSet());
            final Long iamOrgId = organizationService.getIamOrgId(orgId);

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {

                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final CrmEmailDomain crmDomain : crmEmailDomains) {
                            final Long crmUserId = crmDomain.getUserId();
                            final User user = userMap.get(crmUserIdToCrmUserMap.get(crmUserId).getIamId());
                            final JsonObject jsonObject = entityEmailDomainConverter.convert(crmDomain);
                            final JsonObject createdByJson = Json.object();

                            createdByJson.put(EmailDomainFields.ID, crmUserId)
                                    .put(EmailDomainFields.NAME, user.getFullName());

                            jsonObject.put(EmailDomainFields.CREATED_BY, createdByJson);
                            jsonObject.put(EmailDomainFields.STATUS, getDomainStatus(crmDomain.getDomainStatus()).name().toUpperCase());
                            jsonArray.put(jsonObject);
                        }
                        loadListResponse(orgId, (start + limit) <= totalRecords, totalRecords, jsonArray, crmResponse);
                    } catch (Exception exception) {
                        LOGGER.error("Email domain: Failed to fetch domains. Reason: {}, OrgId: {}", exception.getMessage(), orgId);

                        sendError(crmResponse, null, exception);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, userRetrievalFailedI18NKey, throwable));
                }
            };
            userService.fetchUsers(iamOrgId, iamUserIds, usersFetchCallBack);
        } else {
            loadListResponse(orgId, (start + limit) <= totalRecords, totalRecords, jsonArray, crmResponse);
        }
    }

    /**
     * <p>
     * Retrieves the {@link EmailDomainFilterPreference} from the given JSON input.
     * </p>
     *
     * @param orgId      The organization ID.
     * @param userId     The user ID.
     * @param jsonObject The request {@link JsonObject} containing filter details.
     * @return The resolved {@link EmailDomainFilterPreference}, or null if none exists.
     */
    private EmailDomainFilterPreference getEmailDomainFilterPreference(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final Optional<EmailDomainFilterPreference> optionalEmailDomainFilterPreference =
                entityEmailDomainConverter.getFilterPreference(orgId, userId, jsonObject);

        if (optionalEmailDomainFilterPreference.isPresent()) {
            final EmailDomainFilterPreference emailDomainFilterPreference = optionalEmailDomainFilterPreference.get();

            emailDomainFilterService.save(orgId, emailDomainFilterPreference);

            return emailDomainFilterPreference;
        }
        final Optional<EmailDomainFilterPreference> existingEmailDomainFilterPreference = emailDomainFilterService.get(orgId, userId);

        return existingEmailDomainFilterPreference.orElseGet(() -> null);
    }

    /**
     * <p>
     * Builds the domain status.
     * </p>
     *
     * @param domainStatus Represents the domain status.
     * @return domain status Represents the final domain status.
     */
    private DomainStatus getDomainStatus(final DomainStatus domainStatus) {
        if (Objects.equals(domainStatus, DomainStatus.VERIFIED)) {
            return DomainStatus.VERIFIED;
        }

        return DomainStatus.PENDING;
    }

    /**
     * <p>
     * Retrieves entity domain or throws error.
     * </p>
     *
     * @param orgId    Represents the orgId.
     * @param domainId Represents the domainId.
     */
    private CrmEmailDomain getEntityDomainOrThrow(final Long orgId, final Long domainId) {
        return crmEmailDomainService.get(orgId, domainId)
                .orElseThrow(() -> ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND, "i18n.email.domain.not.found"));
    }

    /**
     * <p>
     * Extracts email recipients from request.
     * </p>
     *
     * @param jsonObject Represent the JSON object.
     */
    private Collection<String> extractEmailAddresses(final JsonObject jsonObject) {
        final JsonArray emailArray = jsonObject.getJsonArray(EmailDomainFields.EMAIL_ADDRESSES);
        final Collection<String> emails = new HashSet<>();
        final int size = emailArray.size();

        for (int index = 0; index < size; index++) {
            emails.add(emailArray.getString(index));
        }

        if (emails.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.NO_RECIPIENTS_FOUND, "i18n.email.domain.dns.recipients.is.empty");
        }

        return emails;
    }

    /**
     * <p>
     * Loads paginated list metadata into response.
     * </p>
     *
     * @param orgId        Represents the org id.
     * @param hasMore      Represents whether it have more records or not.
     * @param totalRecords Represents the total records.
     * @param result       Represents the result.
     * @param crmResponse  Represents the crm response.
     */
    private void loadListResponse(final Long orgId, final boolean hasMore, final long totalRecords,
                                  final JsonArray result, final CrmResponse crmResponse) {
        crmResponse.put(CoreEmailFields.DOMAINS.getName(), result);
        crmResponse.put(CoreEmailFields.HAS_MORE.getName(), hasMore);
        crmResponse.put(CoreEmailFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(CoreEmailFields.HAS_EMPTY_DOMAINS.getName(), crmEmailDomainService.getDomainCount(orgId) == 0);
    }
}