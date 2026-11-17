package com.tlc.email.server.internal.field;

/**
 * <p>
 * Represents fields associated with email headers
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public final class EmailHeaderFields {

    private EmailHeaderFields() {
    }

    public static final String ID = "id";
    public static final String TO_RECIPIENTS = "toRecipients";
    public static final String BCC_RECIPIENTS = "bccRecipients";
    public static final String CC_RECIPIENTS = "ccRecipients";
    public static final String SUBJECT = "subject";
    public static final String BODY = "body";
    public static final String SENDER = "sender";
    public static final String IS_READ = "isRead";
    public static final String IS_DRAFT = "isDraft";
    public static final String HAS_ATTACHMENTS = "hasAttachments";
    public static final String SENT_DATE_TIME = "sentDateTime";
    public static final String RECEIVED_DATE_TIME = "receivedDateTime";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String INTERNET_MESSAGE_ID = "internetMessageId";
    public static final String INTERNET_MESSAGE_HEADERS = "internetMessageHeaders";
    public static final String IN_REPLY_TO = "In-Reply-To";
    public static final String FAILED_RECIPIENTS = "X-Failed-Recipients";
    public static final String REFERENCES = "References";
    public static final String RETURN_PATH = "Return-Path";
    public static final String BCC = "Bcc";
    public static final String PARENT_FOLDER_ID = "parentFolderId";
    public static final String BODY_PREVIEW = "bodyPreview";
    public static final String REPLY_TO = "replyTo";
    public static final String CONTENT = "content";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String ADDRESS = "address";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String CONTENT_ID = "contentId";
    public static final String SRC = "src";
    public static final String INBOX = "inbox";
    public static final String ARCHIVE = "archive";
    public static final String THREAD_ID = "threadId";
    public static final String SENT_MAIL = "sent mail";
    public static final String DRAFTS = "drafts";
    public static final String SENT_ITEMS = "sent items";
    public static final String DELETED_ITEMS = "deletedItems";
}