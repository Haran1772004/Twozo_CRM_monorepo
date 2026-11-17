package com.tlc.crm.conversation.handler.api.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.conversation.handler.fields.EntityConversationFields;
import com.tlc.crm.core.conversation.status.EntityConversationErrorCodes;
import com.tlc.crm.core.field.call.CallLogFields;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityInboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.models.type.converter.IconConverter;
import com.tlc.crm.entity.timeline.field.TimelineFields;

import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.Email;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallType;
import com.tlc.social.common.model.Platform;

import java.util.Collection;
import java.util.Objects;

import static com.tlc.crm.core.notification.email.api.model.email.EntityEmail.Category.INBOUND_MAIL;
import static com.tlc.crm.core.notification.email.api.model.email.EntityEmail.Category.OUTBOUND_MAIL;
import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.SCHEDULED;
import static com.tlc.notification.model.call.CallType.OUTGOING;

/**
 * Class for converting {@link EntityConversation} to and from JSON representations.
 * Acts as a bridge between internal {@link EntityConversation} models and their external JSON formats.
 *
 * @author kishore J
 * @version 1.0
 */
public class EntityConversationConverter extends EntityConverter<EntityConversation<ConversableModel>> {

    private static final byte DEFAULT_LIMIT = 50;

    private final IconService iconService;
    private final IconConverter iconConverter;
    private final ContactManager contactManager;
    private final AttachmentService attachmentService;
    private final AttachmentConverter attachmentConverter;
    private final UserService userService;
    private final HtmlParser htmlParser;

    private EntityConversationConverter() {
        iconService = IconService.getInstance();
        iconConverter = IconConverter.getInstance();
        contactManager = ContactManager.getInstance();
        attachmentService = AttachmentService.getInstance();
        userService = UserService.getInstance();
        attachmentConverter = AttachmentConverter.getInstance();
        htmlParser = HtmlParser.getInstance();
    }

    private static class InstanceHolder {
        private static final EntityConversationConverter INSTANCE = new EntityConversationConverter();
    }

    public static EntityConversationConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Represents the {@link JsonObject}
     * @return The {@link EntityConversation<ConversableModel>}
     */
    @Override
    public EntityConversation<ConversableModel> convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        throw ErrorCode.get(EntityConversationErrorCodes.UNSUPPORTED_OPERATION);
    }

    /**
     * <p>
     * Converts the {@link EntityConversation} to {@link JsonObject}
     * </p>
     *
     * @param converterConfig    Represents the {@link ConverterConfig}
     * @param entityConversation Represents the entity model
     * @return {@link JsonObject}
     */
    public JsonObject convert(final ConverterConfig converterConfig, final EntityConversation<? extends ConversableModel> entityConversation) {
        final EntityConversation.Type type = entityConversation.getType();
        final ConversableModel conversableModel = entityConversation.getConversableModel();
        final JsonObject jsonObject = switch (type) {
            case EMAIL -> convertEmailToJson(converterConfig, (EntityEmail) conversableModel);
            case CALL -> convertCallDetailsToJson(converterConfig, (CallDetails) conversableModel);
            case MANUALLY_LOGGED_CALL -> convertCallLogToJson(converterConfig, (CallLog) conversableModel);
            case FACEBOOK, INSTAGRAM, WHATSAPP -> convertChatToJson(converterConfig, (CrmChat) conversableModel);
        };

        jsonObject.put(EntityConversationFields.TIME_STAMP.getName(), entityConversation.getCreatedAt())
                .put(EntityConversationFields.TYPE.getName(), entityConversation.getType().name())
                .put(EntityConversationFields.ID.getName(), entityConversation.id())
                .put(EntityConversationFields.ORG_ID.getName(), entityConversation.orgId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link EntityEmail} to {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param entityEmail     Represents the {@link EntityEmail}
     * @return The email {@link JsonObject}
     */
    private JsonObject convertEmailToJson(final ConverterConfig converterConfig, final EntityEmail entityEmail) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(entityEmail)) {
            final Collection<Long> emailAttachmentIds = entityEmail.getEntityAttachmentIds();

            if (Objects.nonNull(emailAttachmentIds) && !emailAttachmentIds.isEmpty()) {
                entityEmail.setEntityAttachments(attachmentService.getAttachments(emailAttachmentIds));
            }

            jsonObject.put(CallLogFields.ICON.getName(), iconConverter.convert(converterConfig, iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, CoreEmailFields.MAIL.getName())))
                    .put(EntityConversationFields.PARTICIPANT.getName(), getParticipantName(entityEmail))
                    .put(EntityConversationFields.CATEGORY.getName(), INBOUND == entityEmail.getMessageCategory() ? INBOUND_MAIL : OUTBOUND_MAIL)
                    .put(EntityConversationFields.DATA.getName(), getEmailDataJson(entityEmail))
                    .put(EntityConversationFields.VARIANT.getName(), entityEmail.getEmailVariant().getType().name());
        }
        return jsonObject;
    }

    /**
     * <p>
     * Gets the data JSON for an email represented by {@link EntityEmail}.
     * </p>
     *
     * @param entityEmail An entity which holds the {@link EntityEmail} attributes
     * @return {@link JsonObject}
     */
    private JsonObject getEmailDataJson(final EntityEmail entityEmail) {
        final JsonObject dataJson = Json.object();
        final Collection<Attachment> attachments = entityEmail.getEntityAttachments();
        final Email.MessageCategory messageCategory = entityEmail.getMessageCategory();
        final String content = entityEmail.getContent();
        final short messageCategoryId = messageCategory.getId();

        if (Objects.nonNull(attachments)) {
            final JsonArray attachmentArray = Json.array();

            attachments.forEach(attachment -> attachmentArray.put(attachmentConverter.convert(attachment)));
            dataJson.put(CoreEmailFields.ATTACHMENTS.getName(), attachmentArray);
        }

        dataJson.put(CoreEmailFields.IS_DRAFT.getName(), entityEmail.isDraft())
                .put(CoreEmailFields.SUBJECT.getName(), entityEmail.getSubject())
                .put(CoreEmailFields.MESSAGE.getName(), Objects.isNull(content) ? null : htmlParser.truncateContent(content, DEFAULT_LIMIT))
                .put(CoreEmailFields.THREAD_ID.getName(), entityEmail.getThreadId())
                .put(CoreEmailFields.MESSAGE_CATEGORY.getName(), messageCategory)
                .put(CoreEmailFields.CONVERSATION_ID.getName(), entityEmail.getConversationId());
        final EntityScheduledEmail entityScheduledEmail = messageCategoryId
                == SCHEDULED.getId() ? (EntityScheduledEmail) entityEmail : null;

        if (Objects.nonNull(entityScheduledEmail)) {
            dataJson.put(CoreEmailFields.SCHEDULED_TIME.getName(), entityScheduledEmail.getScheduledTime());
            dataJson.put(CoreEmailFields.SEND_STATUS.getName(), entityScheduledEmail.sendStatus());
        }
        final EntityInboundEmail inboundEmail = messageCategoryId == INBOUND.getId() ? (EntityInboundEmail) entityEmail : null;
        final EntityOutboundEmail outboundEmail = messageCategoryId == DRAFT.getId() ? (EntityOutboundEmail) entityEmail : null;

        if (Objects.nonNull(outboundEmail)) {
            dataJson.put(CoreEmailFields.OUTBOUND_TYPE.getName(), outboundEmail.getOutboundType());
        }

        if (Objects.nonNull(inboundEmail) && !inboundEmail.isRead()) {
            dataJson.put(CoreEmailFields.HIGHLIGHT.getName(), true);
        }

        if (entityEmail.hasAttachments()) {
            dataJson.put(CoreEmailFields.HAS_ATTACHMENT.getName(), true);
        }

        return dataJson;
    }

    /**
     * <p>
     * Retrieves the participant name based on message category
     * </p>
     *
     * @param entityEmail Represents the {@link EntityEmail}
     * @return {@link String} of participant name
     */
    private String getParticipantName(final EntityEmail entityEmail) {

        if (INBOUND == entityEmail.getMessageCategory()) {
            final Contact contact = entityEmail.getContact();

            if (Objects.isNull(contact)) {
                final EmailAddress emailAddress = entityEmail.getSender();

                if (Objects.nonNull(emailAddress)) {
                    return emailAddress.getEmailAddress();
                }

                return null;
            } else {
                return contact.getFullName();
            }
        }
        return userService.getUser(entityEmail.orgId(), entityEmail.getUserId()).getFullName();
    }

    /**
     * <p>
     * Converts to {@link CallLog} to {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param callLog         Represents the {@link CallLog}
     * @return The call log {@link JsonObject}
     */
    private JsonObject convertCallLogToJson(final ConverterConfig converterConfig, final CallLog callLog) {
        final JsonObject callLogJson = Json.object();
        final Outcome outcome = callLog.getOutcome();

        callLogJson.put(CallLogFields.ICON.getName(), iconConverter.convert(converterConfig,
                iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION, CallLogFields.CALL.getName())));
        callLogJson.put(CallLogFields.IS_CALL_LOGGED.getName(), true);
        callLogJson.put(CallLogFields.AGENDA.getName(), callLog.getAgenda());
        callLogJson.put(CallLogFields.CALL_TYPE.getName(), callLog.getCallType().getName());
        callLogJson.put(NotificationFields.PARTICIPANT.getName(), callLog.getParticipant().getFullName());
        callLogJson.put(NotificationFields.CALLLOG_ID.getName(), callLog.id());

        if (Objects.nonNull(outcome)) {
            callLogJson.put(CallLogFields.OUTCOME.getName(), i18nService.getResolver(converterConfig.locale()).get(outcome.getName()));
        }

        return callLogJson;
    }

    /**
     * <p>
     * Converts to {@link CallLog} to {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param crmChat         Represents the {@link CrmChat}
     * @return The call log {@link JsonObject}
     */
    private JsonObject convertChatToJson(final ConverterConfig converterConfig, final CrmChat crmChat) {
        final JsonObject chatJson = Json.object();
        final Platform platform = crmChat.getPlatform();

        chatJson.put(MessengerFields.ICON, iconConverter.convert(converterConfig, iconService.fetchIconByTypeAndName(Icon.Type.TIMELINE,
                switch (platform) {
                    case FACEBOOK -> TimelineFields.FACEBOOK.getName();
                    case WHATSAPP -> TimelineFields.WHATSAPP.getName();
                    case INSTAGRAM -> TimelineFields.INSTAGRAM.getName();
                })));
        chatJson.put(MessengerFields.MESSAGE, crmChat.getRecentMessagePreview(NumericalConstant.TWO_HUNDRED));
        chatJson.put(MessengerFields.CONTACT_NAME, crmChat.getContact().getFullName());
        chatJson.put(MessengerFields.CHAT_ID, crmChat.getCrmChatId());
        chatJson.put(MessengerFields.CHANNEL_NAME, crmChat.getChannel().getName());
        chatJson.put(MessengerFields.PLATFORM, platform.getId());

        return chatJson;
    }

    /**
     * <p>
     * Converts the {@link CallDetails} to {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param callDetails     Represents the {@link CallDetails}
     * @return The call {@link JsonObject}
     */
    private JsonObject convertCallDetailsToJson(final ConverterConfig converterConfig, final CallDetails callDetails) {
        final JsonObject callDetailsJson = Json.object();
        final Collection<CallRecord> callRecordings = callDetails.getCallRecordings();
        final CallLog callLog = callDetails.getCallLog();
        final CallType callType = callDetails.getType();
        final String iconKey = NotificationFields.ICON.getName();
        final CallSummary summary = callDetails.getSummary();

        if (Objects.nonNull(callLog)) {
            final Outcome outcome = callLog.getOutcome();

            if (Objects.nonNull(outcome)) {
                callDetailsJson.put(NotificationFields.OUTCOME.getName(), i18nService.getResolver(converterConfig.locale())
                        .get(outcome.getName()));
            }
            callDetailsJson.put(NotificationFields.AGENDA.getName(), callLog.getAgenda());
        }
        callDetailsJson.put(iconKey, iconConverter.convert(converterConfig, iconService.fetchIconByTypeAndName(Icon.Type.NOTIFICATION,
                Objects.equals(callType, OUTGOING) ? NotificationFields.OUTGOING_CALL.getName() : NotificationFields.INCOMING_CALL.getName())));
        final Contact contact = callDetails.getContact();

        if (Objects.nonNull(contact)) {
            callDetailsJson.put(NotificationFields.PARTICIPANT.getName(), contact.getFullName());
        }

        if (Objects.nonNull(summary)) {
            callDetailsJson.put(NotificationFields.DURATION.getName(), TemporalServiceProvider.get().formatDuration(NumberUtils.parseNumber(summary.getDuration()).longValue()));
        }
        callDetailsJson.put(NotificationFields.CALL_TYPE.getName(), callDetails.getType().getName());

        if (Objects.nonNull(callRecordings) && !callRecordings.isEmpty()) {
            callDetailsJson.put(NotificationFields.RECORDINGS.getName(), convertRecordingsIntoJson(callRecordings));
        }
        callDetailsJson.put(NotificationFields.CALL_ID.getName(), callDetails.id());

        return callDetailsJson;
    }

    /**
     * Converts a collection of CallRecordingDetails into a JSON array.
     *
     * @param callRecordings The collection of call recordings to convert.
     * @return A JSON array containing call recording details.
     */
    private JsonArray convertRecordingsIntoJson(final Collection<CallRecord> callRecordings) {
        final JsonArray jsonArray = Json.array();

        for (final CallRecord callRecording : callRecordings) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(NotificationFields.RECORD_URL.getName(), callRecording.getUri());
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
}
