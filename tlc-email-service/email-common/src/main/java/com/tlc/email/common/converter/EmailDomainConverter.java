package com.tlc.email.common.converter;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.dns.model.RecordType;
import com.tlc.commons.dns.model.Status;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.Category;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Responsible for converting the {@link EmailDomain}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainConverter implements Converter<EmailDomain, JsonObject> {

    private EmailDomainConverter() {
    }

    /**
     * <p>
     * Gets the instance of {@link EmailDomainConverter}.
     * </p>
     */
    private static class DomainConverterInstance {
        private static final EmailDomainConverter INSTANCE = new EmailDomainConverter();
    }

    public static EmailDomainConverter getInstance() {
        return DomainConverterInstance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailDomain represents the {@link EmailDomain}
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final EmailDomain emailDomain) {
        final JsonObject result = Json.object();

        if (Objects.isNull(emailDomain)) {
            return result;
        }
        result.put(EmailFields.ID, emailDomain.id()).put(EmailFields.NAME, emailDomain.getName());
        final Collection<EmailDnsRecord> emailDnsRecords = emailDomain.getEmailDnsRecords();

        if (Objects.nonNull(emailDnsRecords) && !emailDnsRecords.isEmpty()) {
            final JsonArray dnsRecordsArray = Json.array();

            for (final EmailDnsRecord record : emailDnsRecords) {
                dnsRecordsArray.put(convertDnsRecord(record));
            }

            result.put(EmailFields.DNS_RECORDS, dnsRecordsArray);
        }

        return result;
    }

    /**
     * <p>
     * Converts the {@link EmailDnsRecord} to {@link JsonObject}.
     * </p>
     *
     * @param emailDnsRecord represents the {@link EmailDnsRecord}
     * @return {@link JsonObject}
     */
    public JsonObject convertDnsRecord(final EmailDnsRecord emailDnsRecord) {
        final JsonObject jsonObject = Json.object();

        final RecordType recordType = emailDnsRecord.getRecordType();
        final Category category = emailDnsRecord.getCategory();
        final Status status = emailDnsRecord.getStatus();

        jsonObject.put(EmailFields.ID, emailDnsRecord.id())
                .put(EmailFields.DOMAIN_ID, emailDnsRecord.getDomainId())
                .put(EmailFields.HOST, emailDnsRecord.getHost())
                .put(EmailFields.VALUE, emailDnsRecord.getValue());

        if (Objects.nonNull(recordType)) {
            jsonObject.put(EmailFields.RECORD_TYPE, recordType.getId());
        }

        if (Objects.nonNull(category)) {
            jsonObject.put(EmailFields.CATEGORY, category.getId());
        }

        if (Objects.nonNull(status)) {
            jsonObject.put(EmailFields.STATUS, status.getId());
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to {@link EmailDomain}.
     * </p>
     *
     * @param jsonObject represents the {@link JsonObject}
     * @return {@link EmailDomain}
     */
    public EmailDomain convert(final JsonObject jsonObject) {
        if (Objects.isNull(jsonObject)) {
            return null;
        }
        final EmailDomain emailDomain = new EmailDomain();

        if (jsonObject.containsKey(EmailFields.ID)) {
            emailDomain.setId(jsonObject.getLong(EmailFields.ID));
        }

        if (jsonObject.containsKey(EmailFields.NAME)) {
            emailDomain.setName(jsonObject.getString(EmailFields.NAME));
        }
        final JsonArray dnsArray = jsonObject.optJsonArray(EmailFields.DNS_RECORDS);

        if (Objects.nonNull(dnsArray)) {
            final Collection<DnsRecord> records = new ArrayList<>();
            final int size = dnsArray.size();

            for (int index = 0; index < size; index++) {
                records.add(convertDnsRecord(dnsArray.getJsonObject(index)));
            }
            emailDomain.setDnsRecords(records);
        }

        return emailDomain;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to {@link EmailDnsRecord}.
     * </p>
     *
     * @param jsonObject represents the {@link JsonObject}
     * @return {@link EmailDnsRecord}
     */
    public EmailDnsRecord convertDnsRecord(final JsonObject jsonObject) {
        final EmailDnsRecord record = new EmailDnsRecord();

        if (jsonObject.containsKey(EmailFields.ID)) {
            record.setId(jsonObject.getLong(EmailFields.ID));
        }

        if (jsonObject.containsKey(EmailFields.DOMAIN_ID)) {
            record.setDomainId(jsonObject.getLong(EmailFields.DOMAIN_ID));
        }

        if (jsonObject.containsKey(EmailFields.HOST)) {
            record.setHost(jsonObject.getString(EmailFields.HOST));
        }

        if (jsonObject.containsKey(EmailFields.VALUE)) {
            record.setValue(jsonObject.getString(EmailFields.VALUE));
        }

        if (jsonObject.containsKey(EmailFields.CATEGORY)) {
            record.setCategory(
                    Category.get(jsonObject.getShort(EmailFields.CATEGORY))
            );
        }

        if (jsonObject.containsKey(EmailFields.RECORD_TYPE)) {
            record.setRecordType(
                    RecordType.get(jsonObject.getShort(EmailFields.RECORD_TYPE))
            );
        }

        if (jsonObject.containsKey(EmailFields.STATUS)) {
            record.setStatus(
                    Status.get(jsonObject.getShort(EmailFields.STATUS))
            );
        }

        return record;
    }

    /**
     * <p>
     * Extracts IDs from the given JSON array.
     * </p>
     *
     * @param jsonArray Represents the JSON array containing DNS record IDs.
     * @return Collection of DNS record IDs.
     */
    public Collection<Long> getIds(final JsonArray jsonArray) {
        final Collection<Long> ids = new HashSet<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            ids.add(jsonArray.getLong(index));
        }

        return ids;
    }

    /**
     * <p>
     * Builds the JSON array.
     * </p>
     *
     * @param ids Represents the ids.
     * @return the JSON array.
     */
    public JsonArray getJsonArray(final Collection<Long> ids) {
        final JsonArray jsonArray = Json.array();

        for (final Long id : ids) {
            jsonArray.put(id);
        }

        return jsonArray;
    }
}