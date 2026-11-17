package com.tlc.email.server.internal.field;

/**
 * <p>
 * Provides the keys of the email notification properties file.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum EmailKeys {

    SYNC_SERVICE("sync-service"),
    IMAP("imap"),
    DNS("dns:/"),
    MX("MX"),
    EMAIL_IMAP_PORT("mail.imap.port"),
    EMAIL_IMAP_HOST("mail.imap.host"),
    EMAIL_IMAP_SSL("mail.imap.ssl.enable"),
    EMAIL_IMAP_SASL("mail.imap.sasl.enable"),
    EMAIL_IMAP_SASL_MECHANISM("mail.imap.sasl.mechanisms"),
    EMAIL_IMAP_AUTH_LOGIN("mail.imap.auth.login.disable"),
    EMAIL_IMAP_AUTH_PLAIN("mail.imap.auth.plain.disable"),
    EMAIL_SMTP_AUTH("mail.smtp.auth"),
    EMAIL_SMTP_HOST("mail.smtp.host"),
    EMAIL_SMTP_PORT("mail.smtp.port"),
    EMAIL_SMTP_SSL("mail.smtp.ssl.enable"),
    EMAIL_SMTP_TLS("mail.smtp.starttls.enable"),
    EMAIL_SMTP_AUTH_MECHANISM("mail.smtp.auth.mechanisms"),
    EMAIL_DEBUG_AUTH("mail.debug.auth"),
    OAUTH("XOAUTH2"),
    CLIENT_CREDENTIAL("clientCredential"),
    EMAIL_SENDER_ADDRESS("email.sender.address"),
    APP_SMART_BCC_CREATE_ALIAS_PREFIX("smartBcc.createAlias.prefix"),
    APP_SMART_BCC_CREATE_ALIAS_SUFFIX("smartBcc.createAlias.suffix"),
    APP_SMART_BCC_ID("smartBcc.id"),
    APP_SMART_BCC_CLIENT_ID("smartBcc.client.id"),
    APP_SMART_BCC_CLIENT_SECRET("smartBcc.client.secret"),
    GMAIL_DRAFT("[Gmail]/Drafts"),
    GMAIL_TRASH("[Gmail]/Trash"),
    GMAIL_SMTP_HOST("email.smtp.gmail.host"),
    GMAIL_SMTP_PORT("email.smtp.gmail.port"),
    GMAIL_IMAP_HOST("email.imap.gmail.host"),
    GMAIL_IMAP_PORT("email.imap.gmail.port"),
    EMAIL_SENDER_ID("email.sender.id"),
    OUTLOOK_DRAFTS("drafts"),
    OUTLOOK_INBOX("inbox"),
    OUTLOOK_SENT_ITEMS("sentitems"),
    INBOX("inbox"),
    SENT("sent"),
    ARCHIVE("archive"),
    MAIL("mail"),
    EMAIL("email"),
    OUTLOOK_SENT_FOLDER("sent items"),
    DRAFT("draft"),
    SENT_MAIL("Sent Mail"),
    DELETED_ITEMS("Deleted Items"),
    APP_CREATE_ALIAS_PREFIX("alias.createAlias.prefix"),
    APP_CREATE_ALIAS_SUFFIX("alias.createAlias.suffix"),
    IS_TENANT("isTenant"),
    STATE_DATA("stateData"),
    CONTENT_TYPE_HTML_UTF8("text/html; charset=UTF-8"),
    MESSAGE_BATCH_SIZE("batch.size.message"),
    APP_NAME("name"),
    HISTORY_BATCH_SIZE("batch.size.history"),
    WEBHOOK_RENEWAL_LIMIT("webhook.renewal.limit"),
    DOMAIN("domain"),
    NAME("name"),
    NODE("node"),
    ;

    private final String key;

    EmailKeys(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
