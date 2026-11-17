package com.tlc.email.common.codec;

import com.tlc.attachment.io.File;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.service.Codec;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;

import com.tlc.email.common.service.EmailSanitizeService;
import com.tlc.file.client.service.AttachmentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.Map.Entry;

/**
 * <p>
 * Serves as a base class for encoding and decoding email-related data.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailCodec
 */
public abstract class AbstractEmailCodec<T> implements EmailCodec, Codec<T> {

    private static final String EMPTY_STRING = "";

    protected final HtmlParser htmlParser;
    private final AttachmentService attachmentService;
    protected final EmailSanitizeService emailSanitizeService;

    protected AbstractEmailCodec() {
        this.htmlParser = HtmlParser.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.emailSanitizeService = EmailSanitizeService.getInstance();
    }

    /**
     * <p>
     * Gets the sync email address as json.
     * </p>
     *
     * @param syncAddressInfo Represents the {@link SyncAddressInfo}
     * @return sync email address {@link JsonObject}
     */
    protected final JsonObject getSyncEmailAddressAsJson(final SyncAddressInfo syncAddressInfo) {
        final JsonObject emailJson = Json.object();

        emailJson.put(EmailFields.ID, syncAddressInfo.id());
        emailJson.put(EmailFields.EMAIL_ADDRESS, syncAddressInfo.getEmailAddress());
        emailJson.put(EmailFields.CONSENT_TYPE, syncAddressInfo.getConsentType().name());

        return emailJson;
    }

    /**
     * <p>
     * Gets the {@link SyncAddressInfo} based on given json.
     * </p>
     *
     * @param syncAddressJson Represents the sync email address as json
     * @return {@link SyncAddressInfo}
     */
    protected final SyncAddressInfo getSyncEmailAddress(final JsonObject syncAddressJson) {
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(syncAddressJson.getString(EmailFields.EMAIL_ADDRESS));

        if (syncAddressJson.containsKey(EmailFields.ID)) {
            syncAddressInfo.setId(syncAddressJson.getLong(EmailFields.ID));
        }
        syncAddressInfo.setConsentType(ConsentType.valueOf(syncAddressJson.getString(EmailFields.CONSENT_TYPE)));

        return syncAddressInfo;
    }

    /**
     * <p>
     * Gets the state data as {@link JsonObject}.
     * </p>
     *
     * @param requestJson Represents the request json
     * @return state data json
     */
    protected final StateData getStateDataAsJson(final JsonObject requestJson) {

        if (requestJson.containsKey(EmailFields.STATE_DATA)) {
            final JsonObject stateDataJson = requestJson.getJsonObject(EmailFields.STATE_DATA);

            if (stateDataJson.containsKey(EmailFields.ALL)) {
                return new StateData(stateDataJson.getJsonObject(EmailFields.ALL));
            }

            return new StateData(stateDataJson);
        }

        return new StateData(Json.object());
    }

    /**
     * <p>
     * Converts the given {@link JsonObject} to {@link Email}.
     * </p>
     *
     * @param emailJson Represents the email json
     * @return Represents the {@link Email}
     */
    protected final Email getEmail(final JsonObject emailJson) {
        final Email email = new Email();

        processEmail(email, emailJson);

        return email;
    }

    /**
     * <p>
     * Gets the {@link InboundEmail}.
     * </p>
     *
     * @param emailJson Represents the request email json
     * @return {@link InboundEmail}
     */
    protected final InboundEmail getInboundEmail(final JsonObject emailJson) {
        final InboundEmail inboundEmail = new InboundEmail();

        processEmail(inboundEmail, emailJson);
        inboundEmail.setRead(emailJson.optBoolean(EmailFields.IS_READ));

        return inboundEmail;
    }

    /**
     * <p>
     * Gets the {@link OutboundEmail}.
     * </p>
     *
     * @param emailJson Represents the request email json
     * @return {@link OutboundEmail}
     */
    protected final OutboundEmail getOutboundEmail(final JsonObject emailJson) {
        final OutboundEmail outboundEmail = new OutboundEmail();

        processEmail(outboundEmail, emailJson);

        outboundEmail.setOutboundType(OutboundEmail.OutboundType.get(emailJson.getShort(EmailFields.OUTBOUND_TYPE)));
        outboundEmail.setBounced(emailJson.optBoolean(EmailFields.IS_BOUNCED));
        outboundEmail.setRead(emailJson.optBoolean(EmailFields.IS_READ));

        return outboundEmail;
    }

    /**
     * <p>
     * Gets the {@link DraftEmail}.
     * </p>
     *
     * @param emailJson Represents the request email json
     * @return {@link DraftEmail}
     */
    protected final DraftEmail getDraftEmail(final JsonObject emailJson) {
        final DraftEmail draftEmail = new DraftEmail();

        processEmail(draftEmail, emailJson);

        draftEmail.setDraftAction(DraftEmail.DraftAction.valueOf(emailJson.getString(EmailFields.DRAFT_ACTION)));

        return draftEmail;
    }

    /**
     * <p>
     * Gets the email {@link JsonObject} from the given {@link Email}.
     * </p>
     *
     * @param email Represents the {@link Email}
     * @return email {@link JsonObject}
     */
    protected final JsonObject getEmailAsJson(final Email email) {
        final JsonObject emailJson = Json.object();
        final String subject = email.getSubject();
        final String content = email.getContent();

        emailJson.put(EmailFields.ID, email.id());
        emailJson.put(EmailFields.IS_ARCHIVED, email.isArchived());
        emailJson.put(EmailFields.IS_STARRED, email.isStarred());
        emailJson.put(EmailFields.IS_ACTIVE, email.isActive());
        emailJson.put(EmailFields.IS_DELETED, email.isDeleted());
        emailJson.put(EmailFields.IS_DRAFT, email.isDraft());
        emailJson.put(EmailFields.MESSAGE_CATEGORY_ID, email.getMessageCategory().getId());
        emailJson.put(EmailFields.RECIPIENTS_COUNT, email.getRecipientsCount());
        emailJson.put(EmailFields.CONTENT, emailSanitizeService.trimContent(content));
        emailJson.put(EmailFields.SUBJECT, emailSanitizeService.trimSubject(subject));

        final EmailMetaData metaData = email.getMetaData();
        final JsonObject metaDataJson = Json.object();

        if (Objects.nonNull(metaData)) {
            metaDataJson.put(EmailFields.CONVERSATION_THREAD_ID, metaData.getConversationThreadId());
            metaDataJson.put(EmailFields.DATE, metaData.getDate());
            metaDataJson.put(EmailFields.IS_INITIAL_MESSAGE, metaData.isInitialMessage());
            metaDataJson.put(EmailFields.EXTERNAL_REFERENCE, metaData.getExternalReference());
            metaDataJson.put(EmailFields.MESSAGE_ID, metaData.getMessageId());
            metaDataJson.put(EmailFields.IN_REPLY_TO, metaData.getInReplyTo());
            metaDataJson.put(EmailFields.PARENT_FOLDER_ID, metaData.getParentFolderId());
            metaDataJson.put(EmailFields.RETURN_PATH, metaData.getReturnPath());
            metaDataJson.put(EmailFields.SENDER_ID, metaData.getSenderId());
            metaDataJson.put(EmailFields.INTERNAL_DRAFT_ID, metaData.getInternalDraftId());
            metaDataJson.put(EmailFields.TRACKING_ID, metaData.getTrackingId());
            metaDataJson.put(EmailFields.CURRENT_MAIL_BOX_ID, metaData.getCurrentMailboxId());
            metaDataJson.put(EmailFields.AUTO_FORWARDING_ADDRESS, metaData.getAutoForwardingAddress());
            metaDataJson.put(EmailFields.ID, metaData.id());
        }
        emailJson.put(EmailFields.META_DATA, metaDataJson);
        final EmailRecipients emailRecipients = email.getEmailRecipients();

        if (Objects.nonNull(emailRecipients)) {
            final Map<AddressType, Collection<EmailAddress>> addressTypeMap = emailRecipients.get();

            if (Objects.nonNull(addressTypeMap) && !addressTypeMap.isEmpty()) {
                final JsonObject recipientJson = Json.object();
                final Collection<EmailAddress> toAddresses = addressTypeMap.get(AddressType.TO);
                final Collection<EmailAddress> bccAddresses = addressTypeMap.get(AddressType.BCC);
                final Collection<EmailAddress> ccAddresses = addressTypeMap.get(AddressType.CC);
                final Function<Collection<EmailAddress>, JsonArray> getAddressJson = (emailAddresses) -> {
                    final JsonArray addressArray = Json.array();

                    if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {

                        for (final EmailAddress address : emailAddresses) {
                            addressArray.put(convertEmailToJson(address));
                        }
                    }

                    return addressArray;
                };

                recipientJson.put(EmailFields.TO_ADDRESSES, getAddressJson.apply(toAddresses));
                recipientJson.put(EmailFields.CC_ADDRESSES, getAddressJson.apply(ccAddresses));
                recipientJson.put(EmailFields.BCC_ADDRESSES, getAddressJson.apply(bccAddresses));

                emailJson.put(EmailFields.RECIPIENTS, recipientJson);
            }
        }
        final SyncAddressInfo syncAddressInfo = email.getSender();

        if (Objects.nonNull(syncAddressInfo)) {
            emailJson.put(EmailFields.SENDER_ADDRESS, getSyncEmailAddressAsJson(syncAddressInfo));
        }
        final Collection<EmailAttachment> attachments = email.getAttachments();

        if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
            final JsonArray attachmentsArray = Json.array();

            for (final EmailAttachment attachment : attachments) {
                final JsonObject attachmentJson = Json.object();

                attachmentJson.put(EmailFields.ID, attachment.id());
                attachmentJson.put(EmailFields.FILE_NAME, attachment.getFileName());
                attachmentJson.put(EmailFields.MEDIA_TYPE, attachment.getMediaType());
                attachmentJson.put(EmailFields.EXTENSION, attachment.getExtension());
                attachmentJson.put(EmailFields.FILE_SIZE, attachment.getFileSize());

                attachmentsArray.put(attachmentJson);
            }
            emailJson.put(EmailFields.ATTACHMENTS, attachmentsArray);
        }

        return emailJson;
    }

    /**
     * <p>
     * Gets the email {@link JsonObject} from the given {@link InboundEmail}.
     * </p>
     *
     * @param inboundEmail Represents the {@link InboundEmail}
     * @return email {@link JsonObject}
     */
    protected final JsonObject getInboundEmailAsJson(final InboundEmail inboundEmail) {
        final JsonObject inboundEmailJson = getEmailAsJson(inboundEmail);

        inboundEmailJson.put(EmailFields.IS_READ, inboundEmail.isRead());

        return inboundEmailJson;
    }

    /**
     * <p>
     * Gets the email {@link JsonObject} from the given {@link OutboundEmail}.
     * </p>
     *
     * @param outboundEmail Represents the {@link OutboundEmail}
     * @return email {@link JsonObject}
     */
    protected final JsonObject getOutboundEmailAsJson(final OutboundEmail outboundEmail) {
        final JsonObject outboundEmailJson = getEmailAsJson(outboundEmail);

        outboundEmailJson.put(EmailFields.OUTBOUND_TYPE, outboundEmail.getOutboundType().getId());
        outboundEmailJson.put(EmailFields.IS_BOUNCED, outboundEmail.isBounced());
        outboundEmailJson.put(EmailFields.IS_READ, outboundEmail.isRead());

        return outboundEmailJson;
    }

    /**
     * <p>
     * Gets the email {@link JsonObject} from the given {@link DraftEmail}.
     * </p>
     *
     * @param draftEmail Represents the {@link DraftEmail}
     * @return email {@link JsonObject}
     */
    protected final JsonObject getDraftEmailAsJson(final DraftEmail draftEmail) {
        final JsonObject draftEmailJson = getEmailAsJson(draftEmail);

        draftEmailJson.put(EmailFields.DRAFT_ACTION, draftEmail.getDraftAction().name());

        return draftEmailJson;
    }

    /**
     * <p>
     * Converts the {@link Email} to json.
     * </p>
     *
     * @param address Represents the {@link EmailAddress}
     * @return email {@link JsonObject}
     */
    private JsonObject convertEmailToJson(final EmailAddress address) {
        final JsonObject json = Json.object();

        json.put(EmailFields.ID, address.id());
        json.put(EmailFields.EMAIL_ADDRESS, address.getEmailAddress());

        return json;
    }

    /**
     * <p>
     * Processes the request email json to {@link Email}.
     * </p>
     *
     * @param email     Represents the {@link Email}
     * @param emailJson Represents the email request {@link JsonObject}
     */
    private void processEmail(final Email email, final JsonObject emailJson) {

        if (emailJson.containsKey(EmailFields.ID)) {
            email.setId(emailJson.getLong(EmailFields.ID));
        }
        email.setArchived(emailJson.optBoolean(EmailFields.IS_ARCHIVED));
        email.setStarred(emailJson.optBoolean(EmailFields.IS_STARRED));
        email.isActive(emailJson.optBoolean(EmailFields.IS_ACTIVE, true));
        email.setDeleted(emailJson.optBoolean(EmailFields.IS_DELETED));
        email.setMessageCategory(Email.MessageCategory.get(emailJson.getShort(EmailFields.MESSAGE_CATEGORY_ID)));

        if (emailJson.containsKey(EmailFields.CONTENT)) {
            final String content = emailJson.getString(EmailFields.CONTENT);

            email.setContent(Objects.nonNull(content) ? content : EMPTY_STRING);
        } else {
            email.setContent(EMPTY_STRING);
        }

        if (emailJson.containsKey(EmailFields.SUBJECT)) {
            final String subject = emailJson.getString(EmailFields.SUBJECT);

            email.setSubject(Objects.nonNull(subject) ? subject : EMPTY_STRING);
        } else {
            email.setSubject(EMPTY_STRING);
        }
        email.setRecipientsCount(emailJson.optInt(EmailFields.RECIPIENTS_COUNT, 0));

        if (emailJson.containsKey(EmailFields.META_DATA)) {
            final JsonObject metaDataJson = emailJson.getJsonObject(EmailFields.META_DATA);
            final EmailMetaData metaData = new EmailMetaData();

            metaData.setConversationThreadId(metaDataJson.optString(EmailFields.CONVERSATION_THREAD_ID, null));

            if (metaDataJson.containsKey(EmailFields.DATE)) {
                metaData.setDate(metaDataJson.getLong(EmailFields.DATE));
            }
            metaData.setInternalMessageId(metaDataJson.optString(EmailFields.INTERNAL_MESSAGE_ID, null));
            metaData.setInternalDraftId(metaDataJson.optString(EmailFields.INTERNAL_DRAFT_ID, null));
            metaData.setInitialMessage(metaDataJson.optBoolean(EmailFields.IS_INITIAL_MESSAGE));
            metaData.setExternalReference(metaDataJson.optString(EmailFields.EXTERNAL_REFERENCE, null));
            metaData.setMessageId(metaDataJson.optString(EmailFields.MESSAGE_ID, null));
            metaData.setInReplyTo(metaDataJson.optString(EmailFields.IN_REPLY_TO, null));
            metaData.setParentFolderId(metaDataJson.optString(EmailFields.PARENT_FOLDER_ID, null));
            metaData.setReturnPath(metaDataJson.optString(EmailFields.RETURN_PATH, null));
            metaData.setSenderId(metaDataJson.optString(EmailFields.SENDER_ID, null));
            metaData.setTrackingId(metaDataJson.optString(EmailFields.TRACKING_ID, null));
            metaData.setCurrentMailboxId(metaDataJson.optString(EmailFields.CURRENT_MAIL_BOX_ID, null));
            metaData.setAutoForwardingAddress(metaDataJson.optString(EmailFields.AUTO_FORWARDING_ADDRESS, null));

            if (metaDataJson.containsKey(EmailFields.ID)) {
                metaData.setId(metaDataJson.getLong(EmailFields.ID));
            }
            email.setMetaData(metaData);
        }

        final EmailRecipients emailRecipients = new EmailRecipients();

        if (emailJson.containsKey(EmailFields.RECIPIENTS)) {
            final JsonObject recipientJson = emailJson.getJsonObject(EmailFields.RECIPIENTS);
            final Function<String, Collection<EmailAddress>> getEmailAddresses = recipientKey -> {
                final Collection<EmailAddress> addresses = new ArrayList<>();

                if (recipientJson.containsKey(recipientKey)) {
                    final JsonArray addressArray = recipientJson.getJsonArray(recipientKey);
                    final int addressSize = addressArray.size();

                    for (int addressIndex = 0; addressIndex < addressSize; addressIndex++) {
                        final JsonObject addressJson = addressArray.getJsonObject(addressIndex);
                        final Long emailId = addressJson.containsKey(EmailFields.ID) ? addressJson.getLong(EmailFields.ID) : null;

                        addresses.add(new EmailAddress(emailId, addressJson.getString(EmailFields.EMAIL_ADDRESS)));
                    }
                }

                return addresses;
            };

            emailRecipients.addRecipients(AddressType.TO, getEmailAddresses.apply(EmailFields.TO_ADDRESSES));
            emailRecipients.addRecipients(AddressType.BCC, getEmailAddresses.apply(EmailFields.BCC_ADDRESSES));
            emailRecipients.addRecipients(AddressType.CC, getEmailAddresses.apply(EmailFields.CC_ADDRESSES));
        }
        email.setRecipients(List.of(emailRecipients));

        if (emailJson.containsKey(EmailFields.SENDER_ADDRESS)) {
            final JsonObject senderJson = emailJson.getJsonObject(EmailFields.SENDER_ADDRESS);
            final SyncAddressInfo sender = getSyncEmailAddress(senderJson);

            email.setSender(sender);
        }

        if (emailJson.containsKey(EmailFields.ATTACHMENTS)) {
            final Collection<EmailAttachment> emailAttachments = new ArrayList<>();
            final JsonArray attachmentsArray = emailJson.getJsonArray(EmailFields.ATTACHMENTS);
            final int attachmentSize = attachmentsArray.size();
            final Collection<Long> attachmentIds = new HashSet<>();

            for (int index = 0; index < attachmentSize; index++) {
                final JsonObject attachmentJson = attachmentsArray.getJsonObject(index);
                final EmailAttachment emailAttachment = new EmailAttachment();
                final Long id = attachmentJson.getLong(EmailFields.ID);

                emailAttachment.setId(id);
                attachmentIds.add(id);

                if (attachmentJson.containsKey(EmailFields.FILE_NAME)) {
                    emailAttachment.setFileName(attachmentJson.getString(EmailFields.FILE_NAME));
                }

                if (attachmentJson.containsKey(EmailFields.MEDIA_TYPE)) {
                    emailAttachment.setMediaType(attachmentJson.getString(EmailFields.MEDIA_TYPE));
                }

                if (attachmentJson.containsKey(EmailFields.EXTENSION)) {
                    emailAttachment.setExtension(attachmentJson.getString(EmailFields.EXTENSION));
                }

                if (attachmentJson.containsKey(EmailFields.FILE_SIZE)) {
                    emailAttachment.setFileSize(attachmentJson.getLong(EmailFields.FILE_SIZE));
                }

                emailAttachments.add(emailAttachment);
            }

            final Collection<File> files = attachmentService.getFiles(attachmentIds);

            email.setFiles(files);
            email.setAttachments(emailAttachments);
        }
    }

    /**
     * <p>
     * Copies an {@link Email} into a new instance for isolated sync processing.
     * </p>
     *
     * @param source the email to copy
     * @return a new email with the same field values
     */
    protected final Email cloneEmail(final Email source) {
        final Email clonedEmail;

        if (source instanceof DraftEmail draftEmail) {
            final DraftEmail clonedDraft = new DraftEmail();

            cloneOutboundEmail((OutboundEmail) source, clonedDraft);
            clonedDraft.setDraftAction(draftEmail.getDraftAction());
            clonedEmail = clonedDraft;
        } else if (source instanceof OutboundEmail outboundEmail) {
            final OutboundEmail clonedOutbound = new OutboundEmail();

            cloneOutboundEmail(outboundEmail, clonedOutbound);
            clonedEmail = clonedOutbound;
        } else if (source instanceof InboundEmail) {
            clonedEmail = new InboundEmail();
        } else {
            clonedEmail = new Email();
        }
        cloneEmailFields(source, clonedEmail);

        return clonedEmail;
    }

    /**
     * <p>
     * Copies the outbound specific fields (bounced status and outbound type) from the source
     * {@link OutboundEmail} to the target {@link OutboundEmail}.
     * </p>
     *
     * @param source Represents the {@link OutboundEmail} to copy from
     * @param target Represents the {@link OutboundEmail} to copy into
     */
    private void cloneOutboundEmail(final OutboundEmail source, final OutboundEmail target) {
        target.setBounced(source.isBounced());
        target.setOutboundType(source.getOutboundType());
    }

    /**
     * <p>
     * Copies the common fields of the source {@link Email} to the target {@link Email}, including the
     * metadata, sender, recipients, attachments and files.
     * </p>
     *
     * @param source Represents the {@link Email} to copy from
     * @param target Represents the {@link Email} to copy into
     */
    private void cloneEmailFields(final Email source, final Email target) {
        target.setId(source.id());
        target.setArchived(source.isArchived());
        target.setStarred(source.isStarred());
        target.setRead(source.isRead());
        target.isActive(source.isActive());
        target.setDeleted(source.isDeleted());
        target.setRecipientsCount(source.getRecipientsCount());
        target.setSubject(source.getSubject());
        target.setContent(source.getContent());
        target.setMessageCategory(source.getMessageCategory());
        target.setConversationType(source.getConversationType());
        target.setActionTime(source.getActionTime());

        if (Objects.nonNull(source.getMetaData())) {
            target.setMetaData(cloneEmailMetaData(source.getMetaData()));
        }

        if (Objects.nonNull(source.getSender())) {
            final SyncAddressInfo sender = source.getSender();
            final SyncAddressInfo senderCopy = new SyncAddressInfo(sender.id(), sender.getEmailAddress());

            senderCopy.setConsentType(sender.getConsentType());
            target.setSender(senderCopy);
        }

        final EmailRecipients sourceRecipients = source.getEmailRecipients();

        if (Objects.nonNull(sourceRecipients)) {
            target.setRecipients(List.of(cloneEmailRecipients(sourceRecipients)));
        }

        if (Objects.nonNull(source.getAttachments())) {
            final Collection<EmailAttachment> attachmentCopies = new ArrayList<>(source.getAttachments().size());

            for (final EmailAttachment attachment : source.getAttachments()) {
                attachmentCopies.add(cloneEmailAttachment(attachment));
            }
            target.setAttachments(attachmentCopies);
        }

        if (Objects.nonNull(source.getFiles())) {
            target.setFiles(new ArrayList<>(source.getFiles()));
        }
    }

    /**
     * <p>
     * Creates a copy of the given {@link EmailMetaData}.
     * </p>
     *
     * @param source Represents the {@link EmailMetaData} to copy
     * @return a new {@link EmailMetaData} with the same field values
     */
    private EmailMetaData cloneEmailMetaData(final EmailMetaData source) {
        final EmailMetaData target = new EmailMetaData();

        target.setInitialMessage(source.isInitialMessage());
        target.setId(source.id());
        target.setDate(source.getDate());
        target.setMessageId(source.getMessageId());
        target.setReferences(source.getReferences());
        target.setInReplyTo(source.getInReplyTo());
        target.setExternalReference(source.getExternalReference());
        target.setReturnPath(source.getReturnPath());
        target.setConversationThreadId(source.getConversationThreadId());
        target.setReplyToAddress(source.getReplyToAddress());
        target.setParentFolderId(source.getParentFolderId());
        target.setSenderName(source.getSenderName());
        target.setInternalDraftId(source.getInternalDraftId());
        target.setInternalMessageId(source.getInternalMessageId());
        target.setTrackingId(source.getTrackingId());
        target.setReceived(source.getReceived());
        target.setCurrentMailboxId(source.getCurrentMailboxId());
        target.setSenderId(source.getSenderId());
        target.setAutoForwardingAddress(source.getAutoForwardingAddress());

        return target;
    }

    /**
     * <p>
     * Creates a copy of the given {@link EmailRecipients}, duplicating every {@link EmailAddress}
     * under its respective {@link AddressType}.
     * </p>
     *
     * @param source Represents the {@link EmailRecipients} to copy
     * @return a new {@link EmailRecipients} with the same addresses, or an empty one if the source has none
     */
    private EmailRecipients cloneEmailRecipients(final EmailRecipients source) {
        final Map<AddressType, Collection<EmailAddress>> sourceRecipients = source.get();

        if (Objects.isNull(sourceRecipients) || sourceRecipients.isEmpty()) {
            return new EmailRecipients();
        }
        final EmailRecipients copy = new EmailRecipients();

        for (final Entry<AddressType, Collection<EmailAddress>> entry : sourceRecipients.entrySet()) {
            final Collection<EmailAddress> addressCopies = new HashSet<>();

            for (final EmailAddress address : entry.getValue()) {
                addressCopies.add(new EmailAddress(address.id(), address.getEmailAddress()));
            }
            copy.addRecipients(entry.getKey(), addressCopies);
        }

        return copy;
    }

    /**
     * <p>
     * Creates a copy of the given {@link EmailAttachment}.
     * </p>
     *
     * @param source Represents the {@link EmailAttachment} to copy
     * @return a new {@link EmailAttachment} with the same field values
     */
    private EmailAttachment cloneEmailAttachment(final EmailAttachment source) {
        final EmailAttachment target = new EmailAttachment();

        target.setId(source.id());
        target.setFileName(source.getFileName());
        target.setMediaType(source.getMediaType());
        target.setFileSize(source.getFileSize());
        target.setExtension(source.getExtension());
        target.setCreatedTime(source.getCreatedTime());
        target.setUpdatedTime(source.getUpdatedTime());
        target.setInputStream(source.getInputStream());
        target.setInternalId(source.getInternalId());

        return target;
    }
}