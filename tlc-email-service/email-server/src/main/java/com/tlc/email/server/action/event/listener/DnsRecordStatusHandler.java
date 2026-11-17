package com.tlc.email.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.dns.model.Status;
import com.tlc.commons.dns.util.DnsValidatorUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.converter.EmailDomainConverter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.server.events.EmailEvents;
import com.tlc.email.server.fields.EmailFields;
import com.tlc.email.server.internal.manager.EmailDomainManager;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Event listener responsible for validating DNS records and updating
 * their verification status.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@EventListener(name = EmailEvents.VALIDATE_AND_FETCH_RECORDS)
public class DnsRecordStatusHandler implements EventConsumer<byte[]> {

    private final EmailDomainManager emailDomainManager;
    private final EmailDomainConverter emailDomainConverter;

    /**
     * <p>
     * Constructs the DNS record status handler and initializes
     * required service dependencies.
     * </p>
     */
    public DnsRecordStatusHandler() {
        this.emailDomainManager = EmailDomainManager.getInstance();
        this.emailDomainConverter = EmailDomainConverter.getInstance();
    }

    /**
     * <p>
     * Consumes the DNS validation event and updates DNS record statuses.
     * </p>
     *
     * @param data Represents the incoming event data in byte array format.
     * @return Byte array representing the JSON response containing updated DNS records.
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject request = Json.object(data);
        final Collection<Long> dnsRecordIds = emailDomainConverter.getIds(request.getJsonArray(EmailFields.IDS));
        final Optional<EmailDomain> optionalDomain = emailDomainManager.get(request.getLong(EmailFields.DOMAIN_ID));

        if (optionalDomain.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_NOT_FOUND);
        }
        final EmailDomain emailDomain = optionalDomain.get();
        final Map<Long, EmailDnsRecord> existingDnsRecordMap = emailDomain.getEmailDnsRecords().stream()
                .collect(Collectors.toMap(EmailDnsRecord::getId, Function.identity()));
        final Map<String, DnsRecord> currentDnsRecordMap = DomainAuthenticatedEmailServiceProvider.getInstance().getService()
                .getDnsRecords(emailDomain.getEmailDomainMetaData().getExternalId())
                .stream().collect(Collectors.toMap(DnsRecord::getHost, Function.identity()));
        final Collection<EmailDnsRecord> emailDnsRecords = new HashSet<>();

        for (final Long dnsRecordId : dnsRecordIds) {
            final EmailDnsRecord existingEmailDnsRecord = existingDnsRecordMap.get(dnsRecordId);
            final EmailDnsRecord.RecordProvider recordProvider = existingEmailDnsRecord.getRecordProvider();

            if (recordProvider.isInternal()) {
                existingEmailDnsRecord.setStatus(DnsValidatorUtil.isCnameValid(
                        existingEmailDnsRecord.getHost(), existingEmailDnsRecord.getValue()) ? Status.VERIFIED : Status.PENDING
                );
            } else {
                existingEmailDnsRecord.setStatus(currentDnsRecordMap.get(existingEmailDnsRecord.getHost()).getStatus());
            }
            emailDnsRecords.add(existingEmailDnsRecord);
        }
        emailDomainManager.updateDnsStatus(emailDnsRecords);

        return getDnsRecords(emailDnsRecords).getBytes();
    }

    /**
     * <p>
     * Converts the given DNS records into a JSON object.
     * </p>
     *
     * @param emailDnsRecords Represents the collection of DNS records.
     * @return {@link JsonObject} containing the DNS records.
     */
    private JsonObject getDnsRecords(final Collection<EmailDnsRecord> emailDnsRecords) {
        final JsonObject result = Json.object();
        final JsonArray jsonArray = Json.array();

        for (final EmailDnsRecord emailDnsRecord : emailDnsRecords) {
            jsonArray.put(emailDomainConverter.convertDnsRecord(emailDnsRecord));
        }
        result.put(EmailFields.DNS_RECORDS, jsonArray);

        return result;
    }
}