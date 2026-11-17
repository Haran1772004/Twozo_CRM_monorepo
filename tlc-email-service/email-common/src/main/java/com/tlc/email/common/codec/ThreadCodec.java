package com.tlc.email.common.codec;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Codec;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides the services for encoding and decoding the {@link Thread}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see Codec
 */
public final class ThreadCodec extends AbstractEmailCodec<ThreadHolder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadCodec.class);

    private ThreadCodec() {
    }

    /**
     * <p>
     * Holds the single instance of {@link ThreadCodec}.
     * </p>
     */
    private static class Instance {
        private static final ThreadCodec INSTANCE = new ThreadCodec();
    }

    public static ThreadCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param threadHolder the {@link ThreadHolder} to be encoded
     * @return the encoded bytes of data
     */
    public byte[] encode(final ThreadHolder threadHolder) {
        final JsonObject jsonObject = Json.object();
        final JsonArray threadArray = Json.array();
        final Collection<Thread> threads = threadHolder.getThreads();
        final StateData stateData = threadHolder.getStateData();
        final EmailAddress syncedEmailAddress = threadHolder.getSyncedEmailAddress();

        if (Objects.nonNull(syncedEmailAddress)) {
            final JsonObject addressJson = Json.object();

            addressJson.put(EmailFields.ID, syncedEmailAddress.id());
            addressJson.put(EmailFields.EMAIL_ADDRESS, syncedEmailAddress.getEmailAddress());

            jsonObject.put(EmailFields.SYNC_EMAIL_ADDRESS, addressJson);
        }

        try {

            for (final Thread thread : threads) {
                final JsonObject threadJson = Json.object();

                threadJson.put(EmailFields.ID, thread.id());
                threadJson.put(EmailFields.HAS_ACTIVE_MESSAGES, thread.hasActiveMessages());
                threadJson.put(EmailFields.HAS_DRAFTS, thread.hasDrafts());
                threadJson.put(EmailFields.HAS_ATTACHMENTS, thread.hasAttachments());
                threadJson.put(EmailFields.IS_ARCHIVED, thread.isArchived());
                threadJson.put(EmailFields.IS_STARRED, thread.isStarred());
                final JsonArray array = Json.array();
                final Collection<Email> emails = thread.getEmails();

                for (final Email email : emails) {

                    array.put(switch (email.getMessageCategory()) {
                        case INBOUND -> getInboundEmailAsJson((InboundEmail) email);
                        case OUTBOUND -> getOutboundEmailAsJson((OutboundEmail) email);
                        case DRAFT -> getDraftEmailAsJson((DraftEmail) email);
                        default -> getEmailAsJson(email);
                    });
                }
                threadJson.put(EmailFields.EMAILS, array);
                threadArray.put(threadJson);
            }

            jsonObject.put(EmailFields.THREADS, threadArray);

            if (Objects.nonNull(stateData)) {
                final JsonObject data = stateData.toJson().optJsonObject(EmailFields.DATA);

                if (Objects.nonNull(data)) {
                    if (data.containsKey(EmailFields.MESSAGE)) {
                        data.put(EmailFields.MESSAGE, emailSanitizeService.trimContent(data.getString(EmailFields.MESSAGE)));
                    }

                    if (data.containsKey(EmailFields.SUBJECT)) {
                        final String subject = data.getString(EmailFields.SUBJECT);

                        data.put(EmailFields.SUBJECT, emailSanitizeService.trimSubject(subject));
                    }
                }
                jsonObject.put(EmailFields.STATE_DATA, stateData.toJson());
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Send: Failed to encode thread. Reason: {}, Stack: ", throwable.getMessage(), throwable);
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_ENCODE_THREAD, throwable);
        }

        return jsonObject.getBytes();
    }

    /**
     * {@inheritDoc}
     *
     * @param data the bytes to decode
     * @return Represents the {@link Thread}
     */
    @Override
    public ThreadHolder decode(final byte[] data) {
        if (Objects.isNull(data) || data.length == 0) {
            return null;
        }
        final JsonObject threadJson = Json.object(data);
        final JsonArray threadArray = threadJson.getJsonArray(EmailFields.THREADS);
        final Collection<Thread> threads = new ArrayList<>();
        final int threadSize = threadArray.size();
        final ThreadHolder threadHolder = new ThreadHolder();
        final JsonObject syncAddressJson = threadJson.optJsonObject(EmailFields.SYNC_EMAIL_ADDRESS);

        if (Objects.nonNull(syncAddressJson)) {
            final EmailAddress emailAddress = new EmailAddress(syncAddressJson.getString(EmailFields.EMAIL_ADDRESS));

            if (syncAddressJson.containsKey(EmailFields.ID)) {
                emailAddress.setId(syncAddressJson.getLong(EmailFields.ID));
            }
            threadHolder.setSyncedEmailAddress(emailAddress);
        }

        try {
            for (int threadIndex = 0; threadIndex < threadSize; threadIndex++) {
                final JsonObject jsonObject = threadArray.getJsonObject(threadIndex);
                final Thread thread = new Thread();

                thread.setId(jsonObject.getLong(EmailFields.ID));
                thread.setHasActiveMessages(jsonObject.optBoolean(EmailFields.HAS_ACTIVE_MESSAGES, true));
                thread.setHasDrafts(jsonObject.optBoolean(EmailFields.HAS_DRAFTS));
                thread.setHasAttachments(jsonObject.optBoolean(EmailFields.HAS_ATTACHMENTS));
                thread.setArchived(jsonObject.optBoolean(EmailFields.IS_ARCHIVED));
                thread.setStarred(jsonObject.optBoolean(EmailFields.IS_STARRED));

                final JsonArray emailsArray = jsonObject.getJsonArray(EmailFields.EMAILS);
                final Collection<Email> emails = new ArrayList<>();
                final int emailSize = emailsArray.size();

                for (int emailIndex = 0; emailIndex < emailSize; emailIndex++) {
                    final JsonObject emailJson = emailsArray.getJsonObject(emailIndex);
                    final Email.MessageCategory messageCategory = Email.MessageCategory.get(emailJson.getShort(EmailFields.MESSAGE_CATEGORY_ID));

                    emails.add(switch (messageCategory) {
                        case INBOUND -> getInboundEmail(emailJson);
                        case OUTBOUND -> getOutboundEmail(emailJson);
                        case DRAFT -> getDraftEmail(emailJson);
                        default -> getEmail(emailJson);
                    });
                }

                thread.setEmails(emails);
                threads.add(thread);
            }
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Send: Failed to decode thread. Reason: {}, Stack: ", throwable.getMessage(), throwable);
            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_DECODE_THREAD, throwable);
        }

        threadHolder.setThreads(threads);
        threadHolder.setStateData(getStateDataAsJson(threadJson));

        return threadHolder;
    }

    /**
     * <p>
     * Deep-copies threads so per-sync-setting mutations do not affect other settings.
     * </p>
     *
     * @param threads provider threads from {@link ThreadHolder}
     * @return isolated thread collection for the current sync setting iteration
     */
    public Collection<Thread> cloneThreads(final Collection<Thread> threads) {
        if (Objects.isNull(threads) || threads.isEmpty()) {
            return threads;
        }
        final Collection<Thread> clonedThreads = new ArrayList<>(threads.size());

        for (final Thread thread : threads) {
            clonedThreads.add(cloneThread(thread));
        }

        return clonedThreads;
    }

    /**
     * <p>
     * Creates a copy of the given {@link Thread}, including a copy of each {@link Email} it holds.
     * </p>
     *
     * @param source Represents the {@link Thread} to copy
     * @return a new {@link Thread} with the same field values and copied emails
     */
    private Thread cloneThread(final Thread source) {
        final Thread copy = new Thread();

        copy.setId(source.id());
        copy.setHasActiveMessages(source.hasActiveMessages());
        copy.setHasDrafts(source.hasDrafts());
        copy.setArchived(source.isArchived());
        copy.setStarred(source.isStarred());
        copy.setHasAttachments(source.hasAttachments());
        copy.setConversationCount(source.getConversationCount());

        if (Objects.nonNull(source.getEmails())) {
            final Collection<Email> emailCopies = new ArrayList<>(source.getEmails().size());

            for (final Email email : source.getEmails()) {
                emailCopies.add(cloneEmail(email));
            }
            copy.setEmails(emailCopies);
        }

        return copy;
    }
}