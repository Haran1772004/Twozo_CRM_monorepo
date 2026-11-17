package com.tlc.crm.core.notification.email.inbox.api.model.converter;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.notification.email.api.model.email.Criteria;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDnsRecord;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.model.email.DomainConfig;
import com.tlc.crm.core.notification.email.api.model.email.EmailDomainFilterPreference;
import com.tlc.crm.core.notification.email.inbox.fields.EmailDomainFields;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.email.common.converter.Converter;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.address.EmailAddress;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides different ways to convert the data from database into {@link CrmEmailDomain}
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class CrmEmailDomainConverter implements Converter<CrmEmailDomain, JsonObject> {

    public CrmEmailDomainConverter() {
    }

    private static class Instance {
        private static final CrmEmailDomainConverter INSTANCE = new CrmEmailDomainConverter();
    }

    /**
     * <p>
     * Gets the {@link CrmEmailDomainConverter} Instance
     * </p>
     *
     * @return {@link CrmEmailDomainConverter}
     */
    public static CrmEmailDomainConverter getInstance() {
        return CrmEmailDomainConverter.Instance.INSTANCE;
    }

    /**
     * <p>
     * Transforms the {@link CrmEmailDomain} entity to JSON object
     * </p>
     *
     * @param crmEmailDomain An entity that holds email domain attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final CrmEmailDomain crmEmailDomain) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(EmailDomainFields.ID, crmEmailDomain.id())
                .put(EmailDomainFields.NAME, crmEmailDomain.getName())
                .put(EmailDomainFields.CREATED_AT, crmEmailDomain.getCreatedAt());

        return jsonObject;
    }

    /**
     * <p>
     * Builds the {@link CrmEmailDomain} entity from the JSON object provided
     * </p>
     *
     * @param jsonObject Contains the JSON object
     * @return {@link CrmEmailDomain}
     */
    public CrmEmailDomain convert(final JsonObject jsonObject) {
        final CrmEmailDomain crmEmailDomain = new CrmEmailDomain();

        if (jsonObject.containsKey(EmailDomainFields.ID)) {
            crmEmailDomain.setId(jsonObject.getLong(EmailDomainFields.ID));
        }
        crmEmailDomain.setName(jsonObject.getNullableString(EmailDomainFields.NAME));

        return crmEmailDomain;
    }

    /**
     * <p>
     * Builds the {@link DomainConfig} domain config.
     * </p>
     *
     * @param jsonObject Contains the JSON object
     * @return {@link DomainConfig}
     */
    public DomainConfig convertDomainConfig(final JsonObject jsonObject) {
        final DomainConfig domainConfig = new DomainConfig();

        if (jsonObject.containsKey(EmailDomainFields.CONFIG)) {
            final JsonObject domainConfigJson = jsonObject.getJsonObject(EmailDomainFields.CONFIG);

            domainConfig.setDefaultSenderName(
                    domainConfigJson.getNullableString(EmailDomainFields.DEFAULT_SENDER_NAME)
            );
            domainConfig.setIncludeDefaultFromAsInReplyTo(
                    domainConfigJson.optBoolean(EmailDomainFields.INCLUDE_DEFAULT_FROM_AS_IN_REPLY_TO)
            );

            if (domainConfigJson.containsKey(EmailDomainFields.DEFAULT_FROM_EMAIL)) {
                final JsonObject fromJson = domainConfigJson.getJsonObject(EmailDomainFields.DEFAULT_FROM_EMAIL);

                if (Objects.nonNull(fromJson)) {
                    final FromAddressInfo fromAddress = new FromAddressInfo(fromJson.getNullableString(EmailDomainFields.VALUE));

                    if (fromJson.containsKey(EmailDomainFields.ID)) {
                        fromAddress.setId(fromJson.getLong(EmailDomainFields.ID));
                    }
                    domainConfig.setDefaultFromAddress(fromAddress);
                }
            }

            if (domainConfigJson.containsKey(EmailDomainFields.DEFAULT_IN_REPLY_TO)) {
                final JsonObject replyToJson = domainConfigJson.getJsonObject(EmailDomainFields.DEFAULT_IN_REPLY_TO);

                if (Objects.nonNull(replyToJson)) {
                    final EmailAddress replyToAddress = new EmailAddress();

                    if (replyToJson.containsKey(EmailDomainFields.ID)) {
                        replyToAddress.setId(replyToJson.getLong(EmailDomainFields.ID));
                    }
                    replyToAddress.setEmailAddress(
                            replyToJson.getNullableString(EmailDomainFields.VALUE)
                    );
                    domainConfig.setDefaultReplyToAddress(replyToAddress);
                }
            }
        }

        return domainConfig;
    }

    /**
     * <p>
     * Transforms the {@link DomainConfig} entity to a {@link JsonObject}.
     * </p>
     *
     * @param domainConfig Represents the domain config to convert.
     * @return {@link JsonObject}
     */
    public JsonObject convertDomainConfig(final DomainConfig domainConfig) {
        final JsonObject jsonObject = Json.object();
        final FromAddressInfo defaultFromAddress = domainConfig.getDefaultFromAddress();
        final EmailAddress defaultReplyToAddress = domainConfig.getDefaultReplyToAddress();

        jsonObject
                .put(EmailDomainFields.DEFAULT_SENDER_NAME, domainConfig.getDefaultSenderName())
                .put(EmailDomainFields.INCLUDE_DEFAULT_FROM_AS_IN_REPLY_TO,
                        domainConfig.isIncludeDefaultFromAsInReplyTo());

        if (Objects.nonNull(defaultFromAddress)) {
            jsonObject.put(
                    EmailDomainFields.DEFAULT_FROM_EMAIL,
                    Json.object()
                            .put(EmailDomainFields.ID, defaultFromAddress.id())
                            .put(EmailDomainFields.VALUE,
                                    defaultFromAddress.getEmailAddress())
            );
        }

        if (Objects.nonNull(defaultReplyToAddress)) {
            jsonObject.put(
                    EmailDomainFields.DEFAULT_IN_REPLY_TO,
                    Json.object()
                            .put(EmailDomainFields.ID, defaultReplyToAddress.id())
                            .put(EmailDomainFields.VALUE,
                                    defaultReplyToAddress.getEmailAddress())
            );
        }

        return jsonObject;
    }

    /**
     * <p>
     * Builds the {@link JsonObject} object from the value provided
     * </p>
     *
     * @param emailDnsRecord Represent the email dns record.
     * @return {@link JsonObject}
     */
    public JsonObject convert(final Long id, final EmailDnsRecord emailDnsRecord) {

        return Json.object()
                .put(EmailDomainFields.ID, id)
                .put(EmailDomainFields.HOST, emailDnsRecord.getHost())
                .put(EmailDomainFields.VALUE, emailDnsRecord.getValue())
                .put(EmailDomainFields.RECORD_TYPE, emailDnsRecord.getRecordType().name())
                .put(EmailDomainFields.CATEGORY, emailDnsRecord.getCategory().name())
                .put(EmailDomainFields.STATUS, emailDnsRecord.getStatus().name());
    }

    /**
     * <p>
     * Converts a {@link EmailDomain} object from the internal email system
     * into an {@link CrmEmailDomain} suitable for persistence or further processing
     * in the CRM system.
     * </p>
     *
     * @param orgId       Represents the orgId.
     * @param userId      Represents the userId.
     * @param emailDomain the internal {@link EmailDomain} object to be converted
     * @return a fully populated {@link CrmEmailDomain} object
     */
    public CrmEmailDomain convert(final Long orgId, final Long userId, final EmailDomain emailDomain) {
        final CrmEmailDomain crmEmailDomain = new CrmEmailDomain();
        final Long domainId = emailDomain.id();

        crmEmailDomain.setOrgId(orgId);
        crmEmailDomain.setUserId(userId);
        crmEmailDomain.setName(emailDomain.getName());
        crmEmailDomain.setEmailDomainId(emailDomain.id());
        crmEmailDomain.setDnsRecords(getDnsRecords(orgId, userId, domainId, emailDomain.getEmailDnsRecords()));

        return crmEmailDomain;
    }

    /**
     * <p>
     * Converts a collection of internal {@link EmailDnsRecord} objects.
     * </p>
     *
     * @param orgId           Represents the orgId.
     * @param userId          Represents the userId.
     * @param domainId        The ID of the domain to which these DNS records belong.
     * @param emailDnsRecords The collection of internal {@link EmailDnsRecord} objects.
     * @return a collection of {@link CrmEmailDnsRecord} objects mapped from the input.
     */
    public Collection<DnsRecord> getDnsRecords(final Long orgId, final Long userId,
                                               final Long domainId, final Collection<EmailDnsRecord> emailDnsRecords) {
        final Collection<DnsRecord> entityDnsRecords = new HashSet<>();

        for (final EmailDnsRecord emailDnsRecord : emailDnsRecords) {
            final CrmEmailDnsRecord dnsRecord = new CrmEmailDnsRecord();

            dnsRecord.setEmailDnsRecordId(emailDnsRecord.id());
            dnsRecord.setOrgId(orgId);
            dnsRecord.setUserId(userId);
            dnsRecord.setDomainId(domainId);

            entityDnsRecords.add(dnsRecord);
        }

        return entityDnsRecords;
    }

    /**
     * <p>
     * Retrieves the {@link EmailDomainFilterPreference} from the given JSON object for the specified organization
     * and user. Returns empty if no filter preference is found in the JSON.
     * </p>
     *
     * @param orgId      Represents the organization ID.
     * @param userId     Represents the user ID.
     * @param jsonObject The JSON object containing the filter preference criteria.
     * @return An {@link Optional} containing the {@link EmailDomainFilterPreference} if present, otherwise empty.
     */
    public Optional<EmailDomainFilterPreference> getFilterPreference(final Long orgId, final Long userId, final JsonObject jsonObject) {
        if (jsonObject.containsKey(EmailDomainFields.CRITERIA)) {
            final JsonObject criteria = jsonObject.getJsonObject(EmailDomainFields.CRITERIA);

            if (criteria.containsKey(EmailDomainFields.FILTER)) {
                final JsonObject filterCriteria = criteria.getJsonObject(EmailDomainFields.FILTER);
                final EmailDomainFilterPreference emailDomainFilterPreference = new EmailDomainFilterPreference();

                emailDomainFilterPreference.setOrgId(orgId);
                emailDomainFilterPreference.setUserId(userId);
                emailDomainFilterPreference.setStatusFilter(filterCriteria.getShort(EmailDomainFields.STATUS));

                return Optional.of(emailDomainFilterPreference);
            }
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Constructs a {@link Criteria} object from the JSON search criteria if present.
     * Returns null if no search criteria is found in the JSON.
     * </p>
     *
     * @param jsonObject The JSON object containing the search criteria.
     * @return A {@link Criteria} object if search criteria is found, otherwise null.
     */
    public Criteria getCriteria(final JsonObject jsonObject) {
        if (jsonObject.containsKey(EmailDomainFields.CRITERIA)) {
            final JsonObject jsonCriteria = jsonObject.getJsonObject(EmailDomainFields.CRITERIA);

            if (jsonCriteria.containsKey(EmailDomainFields.SEARCH)) {
                return new Criteria(jsonCriteria.getJsonObject(EmailDomainFields.SEARCH));
            }
        }

        return null;
    }
}