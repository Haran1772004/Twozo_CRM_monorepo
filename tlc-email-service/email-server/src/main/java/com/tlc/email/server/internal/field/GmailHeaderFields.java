package com.tlc.email.server.internal.field;

/**
 * <p>
 * Represents fields associated with gmail headers
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GmailHeaderFields {

    private GmailHeaderFields() {
    }

    public static final String FROM = "From";
    public static final String SENT = "sent";
    public static final String DRAFT = "draft";
    public static final String SENT_MESSAGE_ID = "Message-ID";
    public static final String DRAFT_MESSAGE_ID = "Message-Id";
    public static final String MESSAGE_ID = "Message-id";
    public static final String SUBJECT = "Subject";
}