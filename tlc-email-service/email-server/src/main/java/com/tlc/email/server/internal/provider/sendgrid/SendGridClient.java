package com.tlc.email.server.internal.provider.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.ClickTrackingSetting;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.sendgrid.helpers.mail.objects.TrackingSettings;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.commons.dns.model.RecordType;
import com.tlc.commons.dns.model.Status;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.Category;
import com.tlc.email.common.model.EmailDnsRecord;
import com.tlc.email.common.model.EmailDomain;
import com.tlc.email.common.model.EmailDomainMetaData;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.server.internal.provider.EmailServiceProviderClient;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <p>
 * A REST-based client wrapper for SendGrid Email and Domain APIs.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public final class SendGridClient implements EmailServiceProviderClient {

    private static final String SEND_ENDPOINT = "mail/send";
    private static final String DOMAIN_ENDPOINT = "whitelabel/domains";
    private static final String DNS_ENDPOINT_TEMPLATE = "whitelabel/domains/%s"; // %s = internalId
    private static final String MESSAGE_ID = "Message-ID";
    private static final String X_MESSAGE_ID = "X-Message-Id";
    private static final String IN_REPLY_TO = "In-Reply-To";
    private static final String REFERENCES = "References";
    private static final String RFC_ID_PREFIX = "<";
    private static final String RFC_ID_SUFFIX = ">";
    private static final String RFC_ID_SEPARATOR = "@";

    private static final String KEY_ID = "id";
    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_AUTOMATIC_SECURITY = "automatic_security";
    private static final String KEY_DNS = "dns";
    private static final String KEY_TYPE = "type";
    private static final String KEY_HOST = "host";
    private static final String KEY_DATA = "data";
    private static final String KEY_VALID = "valid";
    private static final String KEY_SPF = "spf";
    private static final String KEY_MAIL = "mail_cname";
    private static final String KEY_TRACKING = "tracking";
    private static final String KEY_DKIM = "dkim";

    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String SUBJECT = " ";

    private final SendGrid sendGridClient;

    /**
     * <p>
     * Creates a new {@link SendGridClient} using the provided SendGrid SDK client.
     * </p>
     *
     * @param sendGridClient The SendGrid client instance.
     */
    private SendGridClient(final SendGrid sendGridClient) {
        this.sendGridClient = sendGridClient;
    }

    /**
     * <p>
     * Sends an email using SendGrid.
     * </p>
     *
     * @param email The internal email model containing sender, recipients, and content.
     */
    public void send(final com.tlc.email.common.model.type.Email email) {

        try {
            final Mail mail = new Mail();

            mail.setFrom(new Email(email.getSender().getEmailAddress()));
            final String subject = email.getSubject();

            mail.setSubject(Objects.isNull(subject) || subject.isEmpty() ? SUBJECT : subject);
            mail.addContent(new Content(CONTENT_TYPE_HTML, email.getContent()));
            final Personalization personalization = new Personalization();
            final EmailRecipients recipients = email.getEmailRecipients();

            addRecipients(recipients.toRecipients(), personalization::addTo);
            addRecipients(recipients.ccRecipients(), personalization::addCc);
            addRecipients(recipients.bccRecipients(), personalization::addBcc);

            mail.addPersonalization(personalization);
            final String rfcId = getRfcId(email);

            mail.addHeader(MESSAGE_ID, rfcId);
            final EmailMetaData emailMetaData = email.getMetaData();

            if (Objects.nonNull(emailMetaData)) {
                final String parentMessageId = emailMetaData.getMessageId();

                if (Objects.nonNull(parentMessageId) && !parentMessageId.isEmpty()) {
                    mail.addHeader(IN_REPLY_TO, parentMessageId);
                    mail.addHeader(REFERENCES, parentMessageId);
                }
            }
            final TrackingSettings trackingSettings = new TrackingSettings();
            final ClickTrackingSetting clickTrackingSetting = new ClickTrackingSetting();

            clickTrackingSetting.setEnable(false);
            clickTrackingSetting.setEnableText(false);
            trackingSettings.setClickTrackingSetting(clickTrackingSetting);
            mail.setTrackingSettings(trackingSettings);

            final Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint(SEND_ENDPOINT);
            request.setBody(mail.build());
            final Response response = sendGridClient.api(request);

            loadSendGridMetaData(response, email, rfcId);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED, exception);
        }
    }

    /**
     * <p>
     * Creates and authenticates a domain in SendGrid and extracts DNS records.
     * </p>
     *
     * @param emailDomain The domain to be authenticated.
     */
    public void addDomain(final EmailDomain emailDomain) {
        try {
            final EmailDomain existingDomain = getDomainByName(emailDomain.getName());

            if (Objects.isNull(existingDomain)) {
                final JsonObject requestBody = Json.object()
                        .put(KEY_DOMAIN, emailDomain.getName())
                        .put(KEY_AUTOMATIC_SECURITY, true);
                final Request request = new Request();

                request.setMethod(Method.POST);
                request.setEndpoint(DOMAIN_ENDPOINT);
                request.setBody(requestBody.toString());
                final Response response = sendGridClient.api(request);

                if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                    throw ErrorCode.get(EmailErrorCodes.DOMAIN_CREATE_ACTION_FAILED);
                }
                final JsonObject responseJson = Json.object(response.getBody());

                if (responseJson.containsKey(KEY_ID)) {
                    final EmailDomainMetaData emailDomainMetaData = new EmailDomainMetaData();

                    emailDomainMetaData.setExternalId(responseJson.getLong(KEY_ID));

                    emailDomain.setEmailDomainMetaData(emailDomainMetaData);
                }
                emailDomain.setDnsRecords(getDnsRecords(responseJson));
            } else {
                emailDomain.setEmailDomainMetaData(existingDomain.getEmailDomainMetaData());
                emailDomain.setDnsRecords(existingDomain.getDnsRecords());
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_CREATE_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Deletes an authenticated domain from SendGrid.
     * </p>
     *
     * @param emailDomain The domain to delete.
     */
    public void deleteDomain(final EmailDomain emailDomain) {
        try {
            final Request request = new Request();

            request.setMethod(Method.DELETE);
            request.setEndpoint(DOMAIN_ENDPOINT + StringUtil.Delimiter.FORWARD_SLASH.getDelimiter() + emailDomain.getName());

            sendGridClient.api(request);
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_DELETE_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Retrieves all authenticated domains available to the SendGrid API key.
     * </p>
     *
     * @return Collection of authenticated {@link EmailDomain} objects.
     */
    public Collection<EmailDomain> getAllDomain() {
        final Collection<EmailDomain> emailDomains = new HashSet<>();
        final JsonArray jsonArray = Json.array(getAllDomains());
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final EmailDomain emailDomain = new EmailDomain();

            emailDomain.setName(jsonArray.getJsonObject(index).getString(KEY_DOMAIN));

            emailDomains.add(emailDomain);
        }

        return emailDomains;
    }

    /**
     * <p>
     * Retrieves a specific authenticated domain by its name.
     * </p>
     *
     * @param domainName The name of the domain to retrieve.
     * @return The {@link EmailDomain} object if found, otherwise null.
     */
    public EmailDomain getDomainByName(final String domainName) {
        final JsonArray jsonArray = Json.array(getAllDomains());
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject domainJson = jsonArray.getJsonObject(index);

            if (domainJson.getString(KEY_DOMAIN).equalsIgnoreCase(domainName)) {
                final EmailDomain emailDomain = new EmailDomain();

                emailDomain.setName(domainJson.getString(KEY_DOMAIN));

                if (domainJson.containsKey(KEY_ID)) {
                    final EmailDomainMetaData emailDomainMetaData = new EmailDomainMetaData();

                    emailDomainMetaData.setExternalId(domainJson.getLong(KEY_ID));
                    emailDomain.setEmailDomainMetaData(emailDomainMetaData);
                }
                emailDomain.setDnsRecords(getDnsRecords(domainJson));

                return emailDomain;
            }
        }
        return null;
    }

    /**
     * <p>
     * Retrieves DNS records for a domain by its internal ID.
     * </p>
     *
     * @param internalId The internal ID of the domain in SendGrid.
     * @return Collection of {@link EmailDnsRecord} objects.
     */
    public Collection<DnsRecord> getDnsRecords(final long internalId) {
        try {
            final Request request = new Request();

            request.setMethod(Method.GET);
            request.setEndpoint(String.format(DNS_ENDPOINT_TEMPLATE, internalId));

            return getDnsRecords(Json.object(sendGridClient.api(request).getBody()));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_FETCH_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Retrieves all domains available to the SendGrid API key.
     * </p>
     *
     * @return JSON string of domains.
     */
    private String getAllDomains() {
        try {
            final Request request = new Request();

            request.setMethod(Method.GET);
            request.setEndpoint(DOMAIN_ENDPOINT);

            return sendGridClient.api(request).getBody();
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.DOMAIN_FETCH_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Gets the rfc id.
     * </p>
     *
     * @param email Represents the email.
     */
    private String getRfcId(final com.tlc.email.common.model.type.Email email) {
        final String senderAddress = email.getSender().getEmailAddress();
        final String domainName = senderAddress.substring(senderAddress.indexOf('@') + 1).toLowerCase();
        final String uniqueId = UUID.randomUUID().toString();

        return RFC_ID_PREFIX + uniqueId + RFC_ID_SEPARATOR + domainName + RFC_ID_SUFFIX; //TODO this has to be updated by keeping the rfc if unique as much as possible
    }

    /**
     * <p>
     * Loads SendGrid response metadata into the Email object.
     * </p>
     *
     * @param response SendGrid API response
     * @param email    Internal Email object
     * @param rfcId    Represent the rfc id
     */
    private void loadSendGridMetaData(final Response response, final com.tlc.email.common.model.type.Email email, final String rfcId) {
        final EmailMetaData emailMetaData = email.getMetaData();
        final String sendGridMessageId = response.getHeaders().get(X_MESSAGE_ID);

        if (Objects.nonNull(emailMetaData)) {
            emailMetaData.setDate(System.currentTimeMillis());
            emailMetaData.setMessageId(rfcId);
            emailMetaData.setInternalMessageId(sendGridMessageId);
        }
    }

    /**
     * <p>
     * Converts a JSON object into a collection of {@link EmailDnsRecord} objects.
     * </p>
     *
     * @param jsonObject JSON object containing DNS record information.
     * @return Collection of DNS records.
     */
    private Collection<DnsRecord> getDnsRecords(final JsonObject jsonObject) {
        final Collection<DnsRecord> emailDnsRecords = new HashSet<>();

        if (jsonObject.containsKey(KEY_DNS)) {
            final JsonObject dnsObject = jsonObject.getJsonObject(KEY_DNS);

            for (final String key : dnsObject.keySet()) {
                final JsonObject dnsRecordJson = dnsObject.getJsonObject(key);
                final EmailDnsRecord emailDnsRecord = new EmailDnsRecord();

                emailDnsRecord.setHost(dnsRecordJson.getString(KEY_HOST));
                emailDnsRecord.setValue(dnsRecordJson.getString(KEY_DATA));
                emailDnsRecord.setRecordType(RecordType.valueOf(dnsRecordJson.getString(KEY_TYPE).toUpperCase()));
                emailDnsRecord.setStatus(dnsRecordJson.containsKey(KEY_VALID) ? Status.VERIFIED : Status.PENDING);
                emailDnsRecord.setCategory(resolveCategory(key));
                emailDnsRecord.setRecordProvider(EmailDnsRecord.RecordProvider.SENDGRID);

                emailDnsRecords.add(emailDnsRecord);
            }
        }

        return emailDnsRecords;
    }

    /**
     * <p>
     * Adds recipients safely to the SendGrid personalization object.
     * </p>
     *
     * @param recipients Collection of {@link EmailAddress} recipients.
     * @param consumer   Consumer operation to add recipients to personalization.
     */
    private void addRecipients(final Collection<EmailAddress> recipients, final Consumer<Email> consumer) {
        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            return;
        }

        recipients.stream()
                .map(EmailAddress::getEmailAddress)
                .filter(address -> Objects.nonNull(address) && !address.isBlank())
                .map(Email::new)
                .forEach(consumer);
    }

    /**
     * Resolves DNS category based on SendGrid DNS key naming.
     *
     * @param dnsKey The DNS key from SendGrid.
     * @return The resolved {@link Category}.
     */
    private Category resolveCategory(final String dnsKey) {
        final String key = dnsKey.toLowerCase();

        if (key.contains(KEY_DKIM)) {
            return Category.DKIM;
        } else if (key.contains(KEY_SPF) || key.contains(KEY_MAIL)) {
            return Category.SPF;
        } else if (key.contains(KEY_TRACKING)) {
            return Category.TRACKING;
        }

        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_DOMAIN_CATEGORY);
    }

    /**
     * <p>
     * Builder for constructing a {@link SendGridClient}.
     * </p>
     *
     * @author Mohamed Yasar
     * @version 1.0
     */
    public static class Builder {

        private String apiKey;

        /**
         * <p>
         * Sets the API key for SendGrid.
         * </p>
         *
         * @param apiKey The SendGrid API key.
         * @return The builder instance.
         */
        public Builder setApiKey(final String apiKey) {
            this.apiKey = apiKey;

            return this;
        }

        /**
         * <p>
         * Builds a {@link SendGridClient} using the configured API key.
         * </p>
         *
         * @return A new {@link SendGridClient} instance.
         */
        public SendGridClient build() {
            return new SendGridClient(new SendGrid(apiKey));
        }
    }
}