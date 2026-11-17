package com.tlc.crm.core.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.conversation.status.EntityConversationLinkErrorCode;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.service.EmailConversationService;
import com.tlc.crm.core.notification.email.bulk.service.DeleteMode;
import com.tlc.crm.core.notification.email.bulk.service.EntityThreadService;
import com.tlc.crm.core.notification.email.bulk.fields.BulkMailFields;

import com.tlc.crm.core.notification.email.list.api.filter.model.EmailListFilter.Folder;
import com.tlc.crm.core.notification.email.list.api.filter.service.EmailFilterService;
import com.tlc.crm.core.notification.email.list.api.list.EmailListManager;
import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;
import com.tlc.crm.email.notification.email.EmailDispatcher;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.email.client.service.ConversationService;
import com.tlc.crm.core.notification.email.template.service.MailPlaceHolderResolver;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.EmailAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService;
import com.tlc.crm.core.notification.email.org.api.OrgEmailManager;
import com.tlc.crm.core.notification.email.org.api.model.OrgEmail;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;

import com.tlc.crm.email.notification.email.EmailSyncService;
import com.tlc.crm.email.notification.email.api.model.MailResponse;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig.ConfigState;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.Date;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.email.common.model.type.OutboundEmail.OutboundType;
import com.tlc.email.common.model.EmailAttachment;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo.Type.SYNC_EMAIL;
import static com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo.Type.TEAM_INBOX;
import static com.tlc.email.common.model.address.AddressType.BCC;
import static com.tlc.email.common.model.address.AddressType.CC;
import static com.tlc.email.common.model.address.AddressType.TO;
import static com.tlc.email.common.model.type.DraftEmail.DraftAction.CREATE;
import static com.tlc.email.common.model.type.Email.State.*;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.FORWARD;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.REPLY;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.REPLY_ALL;
import static com.tlc.email.common.model.type.OutboundEmail.OutboundType.SEND;

/**
 * <p>
 * Manages email related web actions for sending, retrieving the emails from respective mail boxes
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@LimitAuthorizer(types = LimitActionType.INDIVIDUAL_EMAIL, apiTypes = {"send", "reply", "replyAll", "forward"})
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.EMAIL, needRelatedApiHandling = true)
@EmailAuthorizer(subCategories = {SubCategory.Type.VIEW_EMAIL_CONVERSATION, SubCategory.Type.INDIVIDUAL_EMAIL_LIMIT})
@WebAction(path = "/notifications/email")
public class EmailManagement extends AsyncBulkConfigAction<EntityThread> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailManagement.class);

    private static final String FIRST_NAME_FIELD_ID = "firstNameFieldId";
    private static final String EMAILS_FIELD_ID = "emailsFieldId";
    private static final String EMAILS_SUB_TYPE_FIELD = "emailsSubTypeField";
    private static final String TYPE_FIELD_ID = "typeFieldId";
    private static final String VALUE_FIELD_ID = "valueFieldId";

    private static final int DEFAULT_START_FOR_CONVERSATION = 1;
    private static final int DEFAULT_LIMIT_FOR_CONVERSATION = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final OutboundEmailService outboundEmailService;
    private final EmailConversationService emailConversationService;
    private final EmailConverter emailConverter;
    private final DealConverter dealConverter;
    private final ContactManager contactManager;
    private final EmailSyncService emailSyncService;
    private final CoreMailManager mailManager;
    private final ContactConverter contactConverter;
    private final AttachmentService attachmentService;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final MailPlaceHolderResolver placeHolderResolver;
    private final MailPlaceHolderExtractor placeHolderExtractor;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrgEmailManager orgEmailManager;
    private final FieldService fieldService;
    private final EmailDispatcher emailDispatcher;
    private final EntityMailManager entityMailManager;
    private final FieldTypeService fieldTypeService;
    private final ConversationService conversationService;
    private final EmailTrackingService emailTrackingService;
    private final EntityThreadService entityThreadService;
    private final EmailFilterService emailFilterService;
    private final EmailListManager emailListManager;
    private final TeamInboxManager teamInboxManager;
    private final EmailSyncSettingsService emailSyncSettingsService;

    public EmailManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        outboundEmailService = OutboundEmailService.getInstance();
        emailConversationService = EmailConversationService.getInstance();
        emailConverter = EmailConverter.getInstance();
        dealConverter = DealConverter.getInstance();
        contactManager = ContactManager.getInstance();
        emailSyncService = EmailSyncService.getInstance();
        mailManager = CoreMailManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        attachmentService = AttachmentService.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        placeHolderResolver = MailPlaceHolderResolver.getInstance();
        placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        orgEmailManager = OrgEmailManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        emailDispatcher = EmailDispatcher.get();
        entityMailManager = EntityMailManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        conversationService = ConversationService.get();
        this.emailTrackingService = EmailTrackingService.getInstance();
        this.entityThreadService = EntityThreadService.getInstance();
        this.emailFilterService = EmailFilterService.get();
        this.emailListManager = EmailListManager.getInstance();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The received request
     * @param crmResponse The response to be sent
     */
    @Override
    public final void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        handle(request, crmResponse);
    }

    /**
     * <p>
     * Handles the incoming CRM request for email management and performs actions based on the request type.
     * </p>
     *
     * @param request     Represents the {@link CrmRequest} containing details of the email action to be performed
     * @param crmResponse Represents the {@link CrmResponse} to which the results of the email action will be added
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(CoreEmailFields.DATA.getName());
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case "send" -> {
                final EntityThread thread = emailConverter.convert(converterConfigBuilder.build(request, false, data), data);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, data);
                final EntityOutboundEmail mail = thread.getComposedEmail();

                if (Objects.nonNull(mail)) {
                    mail.setOutboundType(SEND);

                    setAttachmentsToMail(orgId, mail);

                    requestJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                    requestJson.put(CoreEmailFields.USER_ID.getName(), userId);

                    setOutboundTypeResponse(orgId, thread, configuration, crmResponse, requestJson, request);
                }
            }
            case "draft" -> {
                final EntityThread thread = emailConverter.convert(converterConfigBuilder.build(request, false, data), data);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, data);
                requestJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                requestJson.put(CoreEmailFields.USER_ID.getName(), userId);
                final EntityOutboundEmail mail = thread.getComposedEmail();

                if (Objects.nonNull(mail)) {
                    final String outboundTypeKey = CoreEmailFields.OUTBOUND_TYPE.getName();
                    final Long conversationId = mail.getConversationId();
                    final boolean isCreateEvent = mail instanceof EntityDraftEmail entityDraftEmail && Objects.equals(CREATE, entityDraftEmail.getDraftAction());

                    if (!isCreateEvent && Objects.isNull(conversationId)) {
                        throw ErrorCode.get(EmailErrorCodes.CONVERSATION_ID_NOT_FOUND, "i18n.notification.mail.invalid.conversation.id");
                    }

                    if (Objects.nonNull(conversationId)) {
                        validateComposeState(mail, isCreateEvent);
                    }
                    final boolean isDraftSendAction = mail instanceof EntityDraftEmail entityDraftEmail && entityDraftEmail.getDraftAction().isSend();
                    final FromAddressInfo fromAddressInfo = mail.getSenderFromAddressInfo();
                    final boolean isNotSyncType = Objects.nonNull(fromAddressInfo) && !fromAddressInfo.getType().isSyncType();

                    if (doesParentConversationExistInMailbox(orgId, mail.id(), mail, isDraftSendAction)) {

                        if (data.containsKey(outboundTypeKey) && isCreateEvent) {
                            mail.setDraftedConversationId(conversationId);
                            mail.setOutboundType(OutboundType.get(data.getShort(outboundTypeKey)));

                            loadMailsByOutboundType(orgId, thread, data, false);
                        } else {
                            mail.setDraftedConversationId(conversationId);
                            mail.setOutboundType(outboundEmailService.getOutboundType(orgId, conversationId));
                            processOutboundType(orgId, mail, conversationId);

                            loadMailsByOutboundType(orgId, thread, data, isDraftSendAction);
                        }
                        mail.setConversationId(conversationId);
                        final Long emailConversationId = mail.id();

                        mail.setId(mailManager.isInternalConversation(orgId, conversationId) ? null : emailConversationId);
                    } else if (mail instanceof EntityDraftEmail entityDraftEmail && entityDraftEmail.getDraftAction().isUpdate()) {
                        if (isNotSyncType && Objects.nonNull(conversationId)) {
                            mail.setOutboundType(outboundEmailService.getOutboundType(orgId, conversationId));
                        }
                    } else {
                        mail.setId(null);
                    }
                    setAttachmentsToMail(orgId, mail);
                    setDraftResponse(orgId, thread, crmResponse, configuration, requestJson, request);
                }
            }
            case "reply" -> {
                final EntityThread thread = emailConverter.convert(converterConfigBuilder.build(request, false, data), data);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, data);
                final EntityOutboundEmail mail = thread.getComposedEmail();

                if (Objects.nonNull(mail)) {
                    validateComposeState(mail);
                    final Long emailConversationId = mail.id();

                    if (doesParentConversationExistInMailbox(orgId, emailConversationId, thread.getComposedEmail())) {
                        mail.setOutboundType(REPLY);

                        outboundEmailService.composeReply(orgId, thread, data);
                    } else {
                        mail.setId(null);
                        mail.setOutboundType(SEND);
                    }
                    setAttachmentsToMail(orgId, mail);

                    requestJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                    requestJson.put(CoreEmailFields.USER_ID.getName(), userId);

                    setOutboundTypeResponse(orgId, thread, configuration, crmResponse, requestJson, request);
                }
            }
            case "replyAll" -> {
                final EntityThread thread = emailConverter.convert(converterConfigBuilder.build(request, false, data), data);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, data);
                final EntityOutboundEmail mail = thread.getComposedEmail();

                if (Objects.nonNull(mail)) {
                    validateComposeState(mail);
                    final Long emailConversationId = mail.id();

                    if (doesParentConversationExistInMailbox(orgId, emailConversationId, mail)) {
                        mail.setOutboundType(REPLY_ALL);

                        outboundEmailService.composeReply(orgId, thread, data);
                    } else {
                        mail.setId(null);
                        mail.setOutboundType(SEND);
                    }
                    setAttachmentsToMail(orgId, mail);

                    requestJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                    requestJson.put(CoreEmailFields.USER_ID.getName(), userId);

                    setOutboundTypeResponse(orgId, thread, configuration, crmResponse, requestJson, request);
                }
            }
            case "forward" -> {
                final EntityThread thread = emailConverter.convert(converterConfigBuilder.build(request, false, data), data);
                final EmailReceiverConfiguration configuration = new EmailReceiverConfiguration();

                outboundEmailService.setEmailThreadDetails(orgId, userId, thread, configuration, data);
                final EntityOutboundEmail mail = thread.getComposedEmail();

                if (Objects.nonNull(mail)) {
                    validateComposeState(mail);
                    final Long emailConversationId = mail.id();

                    if (doesParentConversationExistInMailbox(orgId, emailConversationId, mail)) {
                        mail.setOutboundType(FORWARD);

                        outboundEmailService.composeForward(orgId, thread, data);
                    } else {
                        mail.setId(null);
                        mail.setOutboundType(SEND);
                    }
                    setAttachmentsToMail(orgId, mail);

                    requestJson.put(CoreEmailFields.ORG_ID.getName(), orgId);
                    requestJson.put(CoreEmailFields.USER_ID.getName(), userId);

                    setOutboundTypeResponse(orgId, thread, configuration, crmResponse, requestJson, request);
                }
            }
            case "getMails" -> processThreadMails(orgId, userId, data, crmResponse);
            case "delete" -> crmResponse.put(CoreEmailFields.ID.getName(), deleteMail(orgId, userId, data));
            case "updateState" -> processAction(orgId, userId, data, null, crmResponse);
            case "markAsDone" -> {
                data.put(CoreEmailFields.IS_DONE.getName(), true);
                processAction(orgId, userId, data, Folder.AWAITING, crmResponse);
            }
            case "deleteForever" -> {
                data.put(CoreEmailFields.IS_SOFT_DELETE.getName(), true);
                processAction(orgId, userId, data, Folder.TRASH, crmResponse);
            }
            case "discardDrafts" -> {
                data.put(CoreEmailFields.IS_DRAFT_DELETE.getName(), true);
                processAction(orgId, userId, data, Folder.DRAFT, crmResponse);
            }
            case "unsubscribe" ->
                    outboundEmailService.unsubscribe(orgId, userId, data.getString(CoreEmailFields.EMAIL.getName()));
            case "getSingleConversation" -> loadSingleConversationMail(orgId, data, crmResponse);
            case "getRelatedContactsAndDeal" ->
                    crmResponse.put(ContactFields.CONTACTS, getRelatedContactsAndDeal(orgId, userId, data));
            case "updateReadStatus" ->
                    crmResponse.put(CoreEmailFields.UPDATED_ID.getName(), updateReadStatus(orgId, userId, data));
            case "getContent" ->
                    crmResponse.put(CoreEmailFields.CONTENT.getName(), this.getContent(orgId, data.getLong(CoreEmailFields.ID.getName())));
        }
    }

    /**
     * <p>
     * Gets the config action name
     * </p>
     *
     * @return The config action name
     */
    @Override
    public String getConfigActionName() {
        return CoreEmailFields.EMAIL.getName();
    }


    /**
     * <p>
     * Determines whether the parent email conversation exists in the mailbox
     * associated with the sender of the given email.
     * </p>
     *
     * @param orgId          The organization ID under which the conversation exists.
     * @param conversationId The unique identifier of the email conversation.
     * @param entityEmail    The email entity containing sender and conversation information.
     * @return {@code true} if the parent conversation exists in the sender's mailbox;
     */
    private boolean doesParentConversationExistInMailbox(final Long orgId, final Long conversationId,
                                                         final EntityEmail entityEmail) {
        return doesParentConversationExistInMailbox(orgId, conversationId, entityEmail, false);
    }

    /**
     * <p>
     * Determines whether the parent email conversation exists in the mailbox
     * associated with the sender of the given email.
     * </p>
     *
     * @param orgId             The organization ID under which the conversation exists.
     * @param conversationId    The unique identifier of the email conversation.
     * @param entityEmail       The email entity containing sender and conversation information.
     * @param isDraftSendAction Indicates whether the action corresponds to sending a draft email.
     * @return {@code true} if the parent conversation exists in the sender's mailbox;
     */
    private boolean doesParentConversationExistInMailbox(final Long orgId, final Long conversationId, final EntityEmail entityEmail, final boolean isDraftSendAction) {
        final FromAddressInfo fromAddressInfo = (FromAddressInfo) entityEmail.getSender();
        final Long parentConversationId = entityEmail.getParentConversationId();
        Long conversationPkValue = conversationId;

        if (entityEmail instanceof EntityDraftEmail entityDraftEmail && entityDraftEmail.getDraftAction().isUpdate()) {
            return false;
        }

        if (Objects.isNull(conversationPkValue) && Objects.nonNull(parentConversationId) && isDraftSendAction) {
            conversationPkValue = mailManager.getEmailServiceConvId(orgId, entityEmail.getParentConversationId()).orElseGet(() -> null);
        }

        return conversationService.getConversationMailBoxAddress(conversationPkValue, fromAddressInfo.getType().isSyncType())
                .map(mappedEmail -> Objects.equals(fromAddressInfo.getEmailAddress(), mappedEmail))
                .orElseGet(() -> false);
    }

    /**
     * <p>
     * Retrieves the content of an email conversation for the given organization, user, and conversation ID.
     * </p>
     *
     * @param orgId          The organization ID under which the conversation exists. Cannot be {@code null}.
     * @param conversationId The unique identifier of the email conversation.
     * @return The content of the email conversation as a {@link String}.
     */
    private String getContent(final long orgId, final Long conversationId) {
        if (emailConversationService.exists(orgId, null, conversationId)) {
            return emailConversationService.getContent(orgId, conversationId);
        } else {
            throw ErrorCode.get(EmailErrorCodes.CONVERSATION_NOT_FOUND, "i18n.notifications.mail.conversation.not.found");
        }
    }

    /**
     * <p>
     * Update the read status of the given conversation id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject}
     * @return the updated conversation id
     */
    private Long updateReadStatus(final long orgId, final Long userId, final JsonObject jsonObject) {
        final long conversationId = jsonObject.getLong(CoreEmailFields.CONVERSATION_ID.getName());
        final boolean isRead = jsonObject.getBoolean(CoreEmailFields.IS_READ.getName());

        if (teamInboxManager.isActiveTeamInboxConversation(orgId, userId, conversationId, true)) {
            mailManager.updateConversationReadStatus(orgId, conversationId, isRead);
        } else {
            throw ErrorCode.get(EmailErrorCodes.CONVERSATION_NOT_FOUND, "i18n.notifications.mail.conversation.not.found");
        }
        return conversationId;
    }

    /**
     * <p>
     * Verify the validity of compose action by performing various checks on the incoming request.
     * </p>
     *
     * @param mail An entity that holds {@link EntityEmail} attributes
     */
    private void validateComposeState(final EntityEmail mail) {
        validateComposeState(mail, true);
    }

    /**
     * <p>
     * Verify the validity of compose action by performing various checks on the incoming request.
     * </p>
     *
     * @param mail          An entity that holds {@link EntityEmail} attributes
     * @param isCreateEvent A flag indicates whether the action is create or update event
     */
    private void validateComposeState(final EntityEmail mail, final boolean isCreateEvent) {
        final Long orgId = mail.orgId();
        final Long conversationId = mail.getConversationId();

        if (Objects.nonNull(conversationId)) {
            mailManager.loadConversationAndParentConversationId(orgId, mail);

            if (isCreateEvent && mailManager.threadHasUnsentMail(orgId, conversationId)) {
                throw ErrorCode.get(EmailErrorCodes.COMPOSE_RESTRICTED, "i18n.notifications.mail.compose.restricted");
            }
        } else {
            throw ErrorCode.get(EmailErrorCodes.CONVERSATION_NOT_FOUND, "i18n.notifications.mail.conversation.not.found");
        }
    }

    /**
     * <p>
     * Processes all mails belongs to a specific thread.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void processThreadMails(final Long orgId, final Long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long id = data.getLong(CoreEmailFields.ID.getName());
        final int start = Math.max(data.optInt(CoreEmailFields.START.getName(), DEFAULT_START_FOR_CONVERSATION), DEFAULT_START_FOR_CONVERSATION);
        final int limit = Math.min(data.optInt(CoreEmailFields.LIMIT.getName(), DEFAULT_LIMIT_FOR_CONVERSATION), DEFAULT_LIMIT_FOR_CONVERSATION);
        final boolean isActiveFolder = !data.optBoolean(CoreEmailFields.IS_TRASH.getName());
        final boolean isThreadExists = isActiveFolder ? mailManager.isThreadExists(orgId, Set.of(id), true) :
                mailManager.isThreadExists(orgId, List.of(id));
        final EntityThread thread;
        final long totalRecords;

        if (isThreadExists) {
            totalRecords = mailManager.getMailsCount(orgId, id, isActiveFolder);
            thread = totalRecords > 0 && start <= totalRecords ?
                    mailManager.getThread(orgId, id, start, limit, isActiveFolder) : null;
        } else {
            throw ErrorCode.get(EmailErrorCodes.THREAD_NOT_FOUND, crmResponse.resolveI18nString("i18n.notifications.mail.thread.not.found"));
        }

        if (Objects.isNull(thread)) {
            crmResponse.put(CoreEmailFields.THREAD.getName(), Json.object());
        } else {
            final Collection<Long> syncedEmailIds = new HashSet<>();
            final Collection<Long> orgEmailIds = new HashSet<>();
            final String emailsKey = CoreEmailFields.EMAILS.getName();
            final String recipientKey = CoreEmailFields.RECIPIENTS.getName();
            final String fromKey = CoreEmailFields.FROM.getName();
            final String nameKey = CoreEmailFields.NAME.getName();
            final String emailKey = CoreEmailFields.EMAIL.getName();
            final String mailIdKey = CoreEmailFields.MAIL_ID.getName();
            final String contactNameKey = CoreEmailFields.CONTACT_NAME.getName();
            final String idKey = CoreEmailFields.ID.getName();
            final Collection<EntityEmail> mails = thread.getEntityEmails();
            final Collection<Long> conversationIds = new HashSet<>();
            final Optional<UserSyncAddressInfo> optionalSyncedEmail = emailSyncService.getSyncEmailAddress(orgId, userId);

            for (final EntityEmail mail : mails) {
                final EmailAddress emailAddress = mail.getSender();
                final Collection<Long> mailAttachmentIds = mail.getEntityAttachmentIds();
                final Collection<EmailRecipients> recipients = mail.getRecipients();

                if (Objects.nonNull(mailAttachmentIds) && !mailAttachmentIds.isEmpty()) {
                    mail.setEntityAttachments(attachmentService.getAttachments(mailAttachmentIds));
                }

                if (emailAddress instanceof final FromAddressInfo fromAddress) {
                    switch (fromAddress.getType()) {
                        case ORG_EMAIL -> orgEmailIds.add(fromAddress.getCrmEmailId());
                        case SYNC_EMAIL -> syncedEmailIds.add(fromAddress.getCrmEmailId());
                    }
                }

                if (optionalSyncedEmail.isPresent()) {
                    final UserSyncAddressInfo userSyncAddressInfo = optionalSyncedEmail.get();
                    final String syncedEmailAddress = userSyncAddressInfo.getEmailAddress();

                    final Predicate<Collection<EmailAddress>> syncedEmailMatchCriteria = emailRecipients -> {
                        if (Objects.nonNull(emailRecipients)) {
                            for (final EmailAddress recipient : emailRecipients) {
                                if (Objects.equals(syncedEmailAddress, recipient.getEmailAddress())) {
                                    syncedEmailIds.add(userSyncAddressInfo.getCrmEmailId());

                                    return true;
                                }
                            }
                        }
                        return false;
                    };

                    for (final EmailRecipients emailRecipients : recipients) {

                        if (syncedEmailMatchCriteria.test(emailRecipients.toRecipients())
                                || syncedEmailMatchCriteria.test(emailRecipients.ccRecipients())
                                || syncedEmailMatchCriteria.test(emailRecipients.bccRecipients())) {
                            break;
                        }
                    }
                }
            }
            loadContent(orgId, mails.stream().reduce((first, second) -> second).orElseGet(() -> null));
            final JsonObject emailsJson = emailConverter.convert(thread);
            final JsonArray jsonArray = emailsJson.getJsonArray(emailsKey);
            final int totalCount = jsonArray.size();

            for (int index = 0; index < totalCount; index++) {
                final JsonObject conversationJson = jsonArray.optJsonObject(index);

                if (Objects.nonNull(conversationJson)) {
                    processRecipientJson(thread, conversationJson.getJsonObject(recipientKey));
                    loadFromAddressInfo(orgId, userId, conversationJson);
                }
            }

            if (!orgEmailIds.isEmpty()) {
                final Collection<OrgEmail> orgEmails = orgEmailManager.getOrgEmails(orgId, orgEmailIds);
                final JsonArray emailArray = emailsJson.getJsonArray(emailsKey);
                final int totalEmailCount = emailArray.size();
                final Map<String, OrgEmail> orgEmailMap = orgEmails.stream()
                        .collect(Collectors.toMap(orgEmail -> orgEmail.getAddress().getEmailAddress(), Function.identity()));

                for (int index = 0; index < totalEmailCount; index++) {
                    final JsonObject conversationJson = emailArray.getJsonObject(index);

                    if (conversationJson.containsKey(fromKey)) {
                        final JsonObject fromJson = conversationJson.getJsonObject(fromKey);
                        final String senderAddress = fromJson.getString(emailKey);

                        if (orgEmailMap.containsKey(senderAddress)) {

                            if (conversationJson.containsKey(recipientKey)) {
                                processRecipientJson(thread, conversationJson.getJsonObject(recipientKey));
                            }
                            fromJson.put(nameKey, orgEmailMap.get(senderAddress).getName());
                            conversationIds.add(conversationJson.getLong(idKey));
                        }
                    }
                }
            }

            if (!syncedEmailIds.isEmpty()) {
                final Collection<Long> userIds = emailSyncService.getUserIds(orgId, syncedEmailIds);

                if (!userIds.isEmpty()) {
                    final Long iamOrgId = organizationService.getIamOrgId(orgId);
                    final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds);
                    final Map<Long, Long> iamUserIds = crmUsers.stream().collect(Collectors.toMap(CrmUser::id, CrmUser::getIamId));

                    crmResponse.convertIntoAsync();
                    final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                        @Override
                        public void onSuccess(final Collection<User> users) {
                            final JsonArray emailArray = emailsJson.getJsonArray(emailsKey);
                            final Map<String, User> iamUserMap = new HashMap<>();
                            final Long iamUserId = iamUserIds.get(userId);
                            User currentUser = null;

                            for (final User user : users) {
                                iamUserMap.put(user.getEmail(), user);

                                if (user.id().equals(iamUserId)) {
                                    currentUser = user;
                                }
                            }
                            final int totalEmailCount = emailArray.size();

                            for (int index = 0; index < totalEmailCount; index++) {
                                final JsonObject conversationJson = emailArray.getJsonObject(index);

                                if (!conversationIds.contains(conversationJson.getLong(idKey)) && conversationJson.containsKey(fromKey)) {
                                    final JsonObject fromJson = conversationJson.getJsonObject(fromKey);

                                    if (Objects.nonNull(currentUser)) {
                                        final String userFullName = currentUser.getFullName();

                                        if (fromJson.containsKey(contactNameKey)) {
                                            fromJson.put(contactNameKey, userFullName);
                                        } else {
                                            fromJson.put(nameKey, userFullName);
                                        }
                                    }
                                }
                                final JsonObject recipientJson = conversationJson.getJsonObject(recipientKey);
                                final Collection<String> recipientTypes = Set.of(TO.getType(), CC.getType(), BCC.getType());

                                for (final String recipientType : recipientTypes) {
                                    if (recipientJson.containsKey(recipientType)) {
                                        final JsonArray recipientArray = recipientJson.getJsonArray(recipientType);
                                        final int recipientSize = recipientArray.size();

                                        for (int recipientIndex = 0; recipientIndex < recipientSize; recipientIndex++) {
                                            final JsonObject recipientEmailJson = recipientArray.getJsonObject(recipientIndex);
                                            final User iamUser = iamUserMap.get(recipientEmailJson.getString(mailIdKey));

                                            if (Objects.nonNull(iamUser)) {
                                                final String userFullName = iamUser.getFullName();

                                                if (recipientEmailJson.containsKey(contactNameKey)) {
                                                    recipientEmailJson.put(contactNameKey, userFullName);
                                                } else {
                                                    recipientEmailJson.put(nameKey, userFullName);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            crmResponse.put(CoreEmailFields.THREAD.getName(), emailsJson);
                            crmResponse.send();
                        }

                        @Override
                        public void onFailure(final Throwable throwable) {
                            crmResponse.setErrorCode(ErrorCode.get(EmailErrorCodes.USER_NOT_FOUND,
                                    "i18n.notifications.from.address.user.not.found"));
                            crmResponse.send();
                        }
                    };
                    userService.fetchUsers(iamOrgId, iamUserIds.values(), usersFetchCallBack);
                }
            }
            crmResponse.put(CoreEmailFields.THREAD.getName(), emailsJson);
        }
        crmResponse.put(CoreEmailFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(CoreEmailFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
    }

    /**
     * <p>
     * Loads and sets the content and subject of the given {@link EntityEmail},
     * if a valid conversation ID is present and content is retrievable.
     * </p>
     *
     * @param orgId       The organization ID.
     * @param entityEmail The email entity to enrich with content and subject.
     */
    private void loadContent(final Long orgId, final EntityEmail entityEmail) {
        if (Objects.isNull(entityEmail)) {
            return;
        }
        final Long conversationId = entityEmail.getConversationId();

        if (Objects.isNull(conversationId)) {
            return;
        }
        emailConversationService.getMailContent(orgId, conversationId)
                .ifPresent(mailContent -> {
                    final String content = mailContent.content();

                    entityEmail.setContent(Objects.equals(mailContent.messageCategory(), Email.MessageCategory.INBOUND)
                            ? content : emailTrackingService.removeTrackingDetails(content));
                    entityEmail.setSubject(mailContent.subject());
                });
    }

    /**
     * <p>
     * Deletes the email for the given conversation id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param data   Represents the {@link JsonObject}
     * @return the deleted mail id
     */
    private Long deleteMail(final Long orgId, final Long userId, final JsonObject data) {
        final Long id = data.getLong(CoreEmailFields.ID.getName());
        final boolean isActive = data.getBoolean(CoreEmailFields.IS_ACTIVE.getName());
        final boolean isActiveTeamInboxConversation = teamInboxManager.isActiveTeamInboxConversation(orgId, id);

        if (isActiveTeamInboxConversation || mailManager.isConversationExists(orgId, userId, id, !isActive)) {
            mailManager.deleteMail(orgId, isActiveTeamInboxConversation ? null : userId, id, isActive);
            final Long syncId = isActiveTeamInboxConversation ? null : mailManager.getSyncId(orgId, id);

            if (Objects.nonNull(syncId)) {
                final Optional<EmailSyncSettings> optionalEmailSyncSettings = emailSyncSettingsService.getSyncSetting(orgId, userId, syncId);

                optionalEmailSyncSettings.ifPresent(emailSyncSettings -> {
                    if (emailSyncSettings.isSynced() && ConfigState.get(emailSyncSettings.getEmailSyncConfig().getDeleteMode()).equals(ConfigState.ACTIVE)) {
                        final EmailStateTransitionInfo stateTransitionInfo = new EmailStateTransitionInfo();

                        mailManager.fillTransitionData(orgId, userId, id, INACTIVE, stateTransitionInfo);
                        final Map<String, Collection<EmailStateTransitionInfo>> emailStateTransitionMap = new HashMap<>();

                        emailStateTransitionMap.put(emailSyncSettings.getAddress().getEmailAddress(), List.of(stateTransitionInfo));

                        emailDispatcher.update(emailStateTransitionMap, INACTIVE);
                    }
                });
            }

            return id;
        } else {
            throw ErrorCode.get(EntityConversationLinkErrorCode.UNLINK_CONVERSATION_ACTION_RESTRICTED, "i18n.notification.mail.conversation.action.restricted");
        }
    }

    /**
     * <p>
     * Process and performs the respective action (update, delete or mark as done) on the email threads based on the incoming request.
     * The action is performed either on the specified thread ids or on all the thread ids belongs to a specific folder based on the applied filter.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param data   Represents the {@link JsonObject}
     */
    private void processAction(final Long orgId, final Long userId, final JsonObject data,
                               final Folder defaultFolder, final CrmResponse crmResponse) {
        final String idsKey = CoreEmailFields.IDS.getName();

        if (data.containsKey(idsKey)) {
            final Set<Long> threadIds = getSpecifiedIds(data, idsKey);

            validateThreadIds(orgId, userId, threadIds);
            crmResponse.put(CoreEmailFields.IDS.getName(), performAction(orgId, userId, threadIds, data));
        } else if (data.containsKey(IS_ALL_SELECTED)) {
            final String folderIdKey = CoreEmailFields.FOLDER_ID.getName();
            final Folder folder = data.containsKey(folderIdKey) ?
                    Folder.valueOf(data.getInt(folderIdKey)) : defaultFolder;

            if (Objects.nonNull(folder)) {
                final Set<Long> threadIds = emailListManager.fetchThreadIds(orgId, userId,
                        emailFilterService.getExistingEmailListFilter(orgId, userId, folder), folder);

                if (Objects.nonNull(threadIds) && !threadIds.isEmpty()) {
                    final Collection<Long> excludedIds = data.containsKey(EXCLUDED_IDS) ?
                            getSpecifiedIds(data, EXCLUDED_IDS) : Collections.emptyList();

                    if (!excludedIds.isEmpty()) {
                        threadIds.removeAll(excludedIds);
                    }
                    final boolean isDeleteAction = isDeleteAction(data);

                    publish(orgId, userId, data, threadIds, isDeleteAction ? Action.DELETE : Action.UPDATE);
                    crmResponse.put(ActionFields.MESSAGE, isDeleteAction ?
                            BULK_DELETE_RESPONSE_MESSAGE : BULK_UPDATE_RESPONSE_MESSAGE);
                }
            } else {
                throw ErrorCode.get(EmailErrorCodes.FOLDER_ID_REQUIRED);
            }
        }
    }

    /**
     * <p>
     * Fetches the specified ids from the incoming request
     * </p>
     *
     * @param data   Represents the {@link JsonObject}
     * @param idsKey Represents the key name for fetching the ids from the incoming request
     * @return a set of ids specified
     */
    private Set<Long> getSpecifiedIds(final JsonObject data, final String idsKey) {
        final Set<Long> ids = new HashSet<>();

        data.getJsonArray(idsKey).forEach(
                id -> ids.add((Long) id)
        );

        return ids;
    }

    /**
     * <p>
     * Validates the thread ids based on the organization and user context. If the threads belong to team inbox, then it is considered valid.
     * For non-team inbox threads, it checks whether the threads are mapped to the user. If any of the thread is not mapped to the user, then it throws an exception.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents an user id
     * @param threadIds Represents the thread ids
     */
    private void validateThreadIds(final Long orgId, final Long userId, final Set<Long> threadIds) {

        if (!teamInboxManager.areTeamInboxThreads(orgId, threadIds)) {

            if (mailManager.isThreadExists(orgId, userId, threadIds)) {

                for (final Map.Entry<Long, Long> threadUserId :
                        entityMailManager.getThreadsMappedUserIds(orgId, threadIds).entrySet()) {

                    if (!threadUserId.getValue().equals(userId)) {
                        throw ErrorCode.get(EntityConversationLinkErrorCode.UNLINK_CONVERSATION_ACTION_RESTRICTED,
                                "i18n.notification.mail.conversation.action.restricted");
                    }
                }

            } else {
                throw ErrorCode.get(EmailErrorCodes.THREAD_NOT_FOUND,
                        "i18n.notifications.mail.thread.not.found");
            }
        }
    }

    /**
     * <p>
     * Performs the respective action based on the incoming request
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents an user id
     * @param threadIds Represents the thread ids
     * @param data      Represents the {@link JsonObject}
     * @return collection of thread ids on which the action performed
     */
    private Collection<Long> performAction(final Long orgId, final Long userId,
                                           final Set<Long> threadIds, final JsonObject data) {
        if (isDeleteAction(data)) {
            return entityThreadService.delete(orgId, userId, threadIds, DeleteMode.get(data));
        } else {
            return entityThreadService.updateState(orgId, userId, threadIds, data);
        }
    }

    /**
     * <p>
     * Determines whether the incoming request represents a delete action.
     * </p>
     *
     * @param data Represents the {@link JsonObject}
     * @return {@code true} if the request is a delete action; {@code false} otherwise
     */
    private boolean isDeleteAction(final JsonObject data) {
        return data.containsKey(CoreEmailFields.IS_DRAFT_DELETE.getName()) ||
                data.containsKey(CoreEmailFields.IS_SOFT_DELETE.getName());
    }

    /**
     * <p>
     * Sets the response for send related apis
     * </p>
     *
     * @param mailResponse Represents the {@link MailResponse}
     * @param crmResponse  Represents the {@link CrmResponse}
     * @param crmRequest   Represents the {@link CrmRequest}
     */
    private void setResponse(final MailResponse mailResponse, final CrmResponse crmResponse, final CrmRequest crmRequest) {
        final Collection<ErrorDescriptor> errors = (Collection<ErrorDescriptor>) mailResponse.map().get(CoreEmailFields.VALIDATION_ERRORS.getName());

        if (Objects.nonNull(errors) && !errors.isEmpty()) {
            final ErrorCode errorCode = ErrorCode.get(EmailErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        } else if (!mailResponse.map().isEmpty()) {
            final String message = (String) mailResponse.map().get(EmailFields.MESSAGE.getName());
            if (Objects.nonNull(message)) {
                crmResponse.put(EmailFields.MESSAGE.getName(), crmRequest.i18nResolver().get(message));
            }

        }
    }

    /**
     * Fetches the attachments associated with an outbound email and sets the response for outbound processing.
     *
     * @param orgId Refers the ID of the organization associated with this operation.
     * @param mail  Refers the {@link EntityOutboundEmail} entity containing the details of the email to be processed.
     */
    private void setAttachmentsToMail(final long orgId, final EntityOutboundEmail mail) {
        final Collection<Long> attachmentIds = mail.getEntityAttachmentIds();

        if (!attachmentIds.isEmpty()) {
            mail.setFiles(attachmentService.getFiles(organizationService.getIamOrgId(orgId), attachmentIds));
            final Collection<EmailAttachment> emailAttachments = new HashSet<>();

            for (final Long attachmentId : attachmentIds) {
                final EmailAttachment emailAttachment = new EmailAttachment();

                emailAttachment.setId(attachmentId);
                emailAttachments.add(emailAttachment);
            }

            mail.setAttachments(emailAttachments);
        }
    }

    /**
     * <p>
     * Sets the {@link CrmResponse} based on type and provided organization.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param thread        Represents the {@link Thread} which contains mail details
     * @param configuration Represents the email configuration
     * @param crmResponse   Represents the {@link CrmResponse} to be sent
     * @param requestJson   Represents the json which contains request data
     * @param crmRequest    Represents the {@link CrmRequest}
     */
    private void setOutboundTypeResponse(final Long orgId, final EntityThread thread, final EmailConfiguration configuration,
                                         final CrmResponse crmResponse, final JsonObject requestJson, final CrmRequest crmRequest) {
        final EntityEmail entityEmail = thread.getComposedEmail();

        if (Objects.nonNull(entityEmail)) {
            if (!requestJson.getJsonObject(CoreEmailFields.DATA.getName()).containsKey(BulkMailFields.IGNORE_UNSUBSCRIBED.getName()) &&
                    !entityEmail.hasUnsubscribeLink() && outboundEmailService.hasUnsubscribedRecipients(orgId, entityEmail)) {
                crmResponse.setErrorCode(ErrorCode.get(EmailErrorCodes.UNSUBSCRIBED_EMAIL_ADDRESS_FOUND, "i18n.notifications.mail.recipients.unsubscribed"));
            } else {
                if (entityEmail.hasPlaceHolders()) {
                    resolveThreadPlaceHolders(thread, crmResponse, false, configuration, requestJson, crmRequest);
                } else {
                    setResponse(outboundEmailService.send(orgId, thread, requestJson), crmResponse, crmRequest);
                }
            }
        }
    }

    /**
     * Sets the {@link CrmResponse} data based on {@link MailResponse}.
     *
     * @param orgId         Represents an organization id
     * @param thread        Represents the {@link Thread} which contains mail details
     * @param configuration Represents the email configuration
     * @param crmResponse   Represents the {@link CrmResponse}
     * @param requestData   Represents the json which contains request data
     */
    private void setDraftResponse(final Long orgId, final EntityThread thread, final CrmResponse crmResponse,
                                  final EmailConfiguration configuration, final JsonObject requestData, final CrmRequest crmRequest) {
        final EntityEmail composedEmail = thread.getComposedEmail();

        if (Objects.nonNull(composedEmail)) {
            if (composedEmail.hasPlaceHolders()) {
                resolveThreadPlaceHolders(thread, crmResponse, true, configuration, requestData, crmRequest);
            } else {
                final FromAddressInfo fromAddressInfo = composedEmail.getSenderFromAddressInfo();
                final MailResponse mailResponse;

                if (!composedEmail.isDraft() && !fromAddressInfo.getType().isSyncType()) {
                    mailResponse = outboundEmailService.send(orgId, thread, requestData);
                } else {
                    mailResponse = outboundEmailService.draft(orgId, thread, configuration, requestData);
                }

                if (Objects.isNull(mailResponse)) {
                    crmResponse.setErrorCode(ErrorCode.get(EmailErrorCodes.UNSUBSCRIBED_RECIPIENTS, "i18n.notifications.mail.recipients.unsubscribed"));
                } else {
                    setResponse(mailResponse, crmResponse, crmRequest);
                }
            }
        }
    }

    /**
     * <p>
     * Process the mail in the thread for the placeholders and dispatch it
     * </p>
     *
     * @param thread        Denotes the {@link Thread}
     * @param response      Denotes the {@link CrmResponse}
     * @param isDraft       A flag to indicate whether it is a draft or send mail action
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     * @param crmRequest    Represents the {@link CrmRequest}
     */
    private void resolveThreadPlaceHolders(final EntityThread thread, final CrmResponse response, final boolean isDraft,
                                           final EmailConfiguration configuration, final JsonObject requestData, final CrmRequest crmRequest) {
        final EntityMailTemplate template = new EntityMailTemplate();
        final long orgId = thread.orgId();
        final EntityEmail mail = thread.getComposedEmail();

        if (Objects.nonNull(mail)) {
            template.setOrgId(orgId);
            template.setSubject(mail.getSubject());
            template.setContent(mail.getContent());
            template.setSource(Source.CONTACT);
            mail.getContactRecipients().toContactIds().stream().findFirst().ifPresentOrElse(
                    contactEmailId -> {
                        final Optional<Long> contactIds = contactManager.getContactIds(orgId, List.of(contactEmailId)).stream().findFirst();

                        contactIds.ifPresent(contactId -> {
                            template.setEntityId(contactId);
                            final Collection<MailPlaceHolder> placeHolders = placeHolderExtractor.getPlaceHolders(template);

                            if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {
                                resolveOwnerPlaceHoldersToDeliver(thread, response, isDraft, placeHolders, configuration, requestData, crmRequest);
                            } else {
                                formatAndDispatchThreadEmail(response, thread, isDraft, configuration, requestData, crmRequest);
                            }
                        });
                    },
                    () -> formatAndDispatchThreadEmail(response, thread, isDraft, configuration, requestData, crmRequest));
        }
    }

    /**
     * <p>
     * Handles the owner field placeHolders present in email data either to replace or fetch owner details and then
     * proceed with the dispatch process
     * </p>
     *
     * @param thread        An entity that holds {@link Thread} attributes
     * @param response      Represents the {@link CrmResponse}
     * @param isDraft       A flag to indicate whether it is a draft or send mail action
     * @param placeHolders  Represents the {@link Map} of {@link MailPlaceHolder}
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     * @param crmRequest    Represents the {@link CrmRequest}
     */
    private void resolveOwnerPlaceHoldersToDeliver(final EntityThread thread, final CrmResponse response, final boolean isDraft,
                                                   final Collection<MailPlaceHolder> placeHolders, final EmailConfiguration configuration,
                                                   final JsonObject requestData, final CrmRequest crmRequest) {
        final Collection<Long> userIds = new HashSet<>();
        final Collection<PlaceHolder> placeHolderList = new HashSet<>();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (final MailPlaceHolder placeHolder : placeHolders) {

                if (placeHolder.isOwnerField()) {
                    final Long userId = placeHolder.getLong();

                    if (Objects.nonNull(userId)) {
                        userIds.add(userId);
                    }
                }
                placeHolderList.add(placeHolder);
            }
        }

        if (userIds.isEmpty()) {
            final EntityEmail entityEmail = thread.getComposedEmail();

            if (Objects.nonNull(entityEmail)) {
                entityEmail.setPlaceHolders(placeHolderList);
                formatAndDispatchThreadEmail(response, thread, isDraft, configuration, requestData, crmRequest);
            }
        } else {
            fetchOwnersAndFillPlaceHolders(userIds, response, thread, isDraft, placeHolders, configuration, requestData, crmRequest);
        }
    }

    /**
     * <p>
     * Fetches the owner field and fills the place holders with its corresponding data.
     * </p>
     *
     * @param userIds       Represents the {@link Collection} of user ids
     * @param response      Represents the {@link CrmResponse}
     * @param thread        Represents the {@link Thread}
     * @param isDraft       A flag to indicate whether it is a draft or send mail action
     * @param placeHolders  Refers to {@link Collection} of {@link MailPlaceHolder}
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     * @param crmRequest    Represents the {@link CrmRequest}
     */
    private void fetchOwnersAndFillPlaceHolders(final Collection<Long> userIds, final CrmResponse response,
                                                final EntityThread thread, final boolean isDraft,
                                                final Collection<MailPlaceHolder> placeHolders, final EmailConfiguration configuration,
                                                final JsonObject requestData, final CrmRequest crmRequest) {

        response.convertIntoAsync();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final EntityEmail entityEmail = thread.getComposedEmail();

                    if (Objects.nonNull(entityEmail)) {
                        entityEmail.setPlaceHolders(placeHolderExtractor.resolvePlaceHoldersWithUser(placeHolders, crmUsers));
                        formatAndDispatchThreadEmail(response, thread, isDraft, configuration, requestData, crmRequest);
                    }
                } catch (final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(EmailErrorCodes.EMAIL_DISPATCH_FAILED, "i18n.notifications.mail.dispatch.failed", throwable));
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(EmailErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                response.send();
            }
        };
        userService.fetchUsers(organizationService.getIamOrgId(thread.orgId()), userIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Loads the mail based on outbound type from the request data
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param thread          Represents the {@link Thread}
     * @param data            Represents the request {@link JsonObject}
     * @param draftSendAction Represent whether it is a draft send action or not
     */
    private void loadMailsByOutboundType(final Long orgId, final EntityThread thread, final JsonObject data, final boolean draftSendAction) {
        final EntityOutboundEmail outboundEmail = thread.getComposedEmail();

        if (Objects.nonNull(outboundEmail) && !Objects.equals(SEND, outboundEmail.getOutboundType())) {

            switch (outboundEmail.getOutboundType()) {
                case REPLY, REPLY_ALL -> outboundEmailService.composeReply(orgId, thread, data, draftSendAction);
                case FORWARD -> outboundEmailService.composeForward(orgId, thread, data);
                default ->
                        throw ErrorCode.get(EmailErrorCodes.INVALID_OUTBOUND_TYPE, "i18n.notifications.mail.outbound.type.invalid");
            }
        }
    }

    /**
     * <p>
     * Post process outbound type when initially resolved from DB. If the outbound type is SEND,
     * and the sender's from-address type is a sync-type, set the email variant to SYNC_EMAIL.
     * </p>
     *
     * @param orgId          The organization id
     * @param mail           The composed outbound mail
     * @param conversationId The conversation id used to locate the thread
     */
    private void processOutboundType(final Long orgId, final EntityOutboundEmail mail, final Long conversationId) {
        if (Objects.isNull(mail) || Objects.isNull(conversationId)) {
            return;
        }
        final OutboundType outboundType = mail.getOutboundType();

        if (!Objects.equals(SEND, outboundType)) {
            return;
        }
        final FromAddressInfo fromAddressInfo = mail.getSenderFromAddressInfo();

        if (Objects.nonNull(fromAddressInfo) && fromAddressInfo.getType().isSyncType()) {
            mail.setVariant(EmailVariant.of(EmailVariant.Type.SYNC_EMAIL));
        }
        final Long threadId = mailManager.getThreadId(orgId, conversationId);

        if (Objects.nonNull(threadId)) {
            final long nonDraftScheduledCount = mailManager.getConversationCount(orgId, threadId);

            if (nonDraftScheduledCount >= 1) {
                mail.setOutboundType(REPLY);
            }
        }
    }

    /**
     * Loads the recipient information and messages associated with a single conversation using the provided request data.
     *
     * @param orgId       Represents an organization id
     * @param requestJson Represents the request {@link JsonObject} data
     */
    private void loadSingleConversationMail(final long orgId, final JsonObject requestJson, final CrmResponse crmResponse) {
        final long conversationId = requestJson.getLong(CoreEmailFields.ID.getName());

        if (mailManager.isConversationExists(orgId, conversationId, true)) {
            final EntityEmail mail = mailManager.getSingleConversation(orgId, conversationId);
            final Collection<Long> attachmentIds = mail.getEntityAttachmentIds();

            if (Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty()) {
                mail.setEntityAttachments(attachmentService.getAttachments(attachmentIds));
            }
            fetchAndDispatchMail(mail, crmResponse);
        } else {
            throw ErrorCode.get(EmailErrorCodes.CONVERSATION_NOT_FOUND, "i18n.notifications.mail.conversation.not.found");
        }
    }

    /**
     * <p>
     * Fetches user data and dispatches the mail
     * </p>
     *
     * @param mail        Represents the {@link EntityEmail}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchMail(final EntityEmail mail, final CrmResponse crmResponse) {
        final Long userId = mail.getUserId();
        final long orgId = mail.orgId();

        if (Objects.isNull(userId)) {
            emailConverter.convert(mail);
        } else {
            final CrmUser crmUser = crmUserService.getUser(orgId, userId);

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param users Response from the consumer
                 */
                @Override
                public void onSuccess(final Collection<User> users) {
                    final User user = users.stream().iterator().next();

                    crmUser.setUser(user);
                    mail.setUser(crmUser);
                    final JsonObject jsonObject = emailConverter.convert(mail);

                    loadFromAddressInfo(orgId, userId, jsonObject);
                    crmResponse.put(CoreEmailFields.DATA.getName(), jsonObject);
                    crmResponse.send();
                }

                /**
                 * {@inheritDoc}
                 *
                 * @param throwable Cause of the failure
                 */
                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, throwable);
                }
            };

            userService.fetchUsers(organizationService.getIamOrgId(orgId), List.of(crmUser.getIamId()), usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Resolves and updates the from-address id within the given JSON object
     * by mapping the email variant type to its corresponding from-address type.
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param userId     Represents the user id, used for sync-type from-address resolution
     * @param jsonObject Represents the JSON payload containing the from-address info
     *                   and email variant type used to determine the from-address record
     */
    private void loadFromAddressInfo(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final String idKey = CoreEmailFields.ID.getName();
        final String emailKey = CoreEmailFields.EMAIL.getName();
        final String variantKey = CoreEmailFields.VARIANT.getName();
        final String isActiveKey = CoreEmailFields.IS_ACTIVE.getName();

        final JsonObject fromAddressInfo = jsonObject.getJsonObject(CoreEmailFields.FROM.getName());
        final boolean hasId = fromAddressInfo.containsKey(idKey);

        if ((hasId || fromAddressInfo.containsKey(emailKey)) && jsonObject.containsKey(variantKey)) {
            final Long emailId;

            if (hasId) {
                emailId = fromAddressInfo.getLong(idKey);
            } else {
                emailId = mailManager.getEmailId(orgId, fromAddressInfo.getString(emailKey));
            }
            final Long fromAddressId = mailManager.getFromAddressId(orgId, userId, emailId,
                    EmailVariant.fromName(jsonObject.getString(variantKey)).isTeamInbox() ? TEAM_INBOX : SYNC_EMAIL);

            if (Objects.nonNull(fromAddressId)) {
                final Optional<FromAddressInfo> optionalFromAddressInfo = mailManager.getFromAddressInfo(orgId, fromAddressId, false);

                optionalFromAddressInfo.ifPresent(addressInfo -> {
                    fromAddressInfo.put(idKey, fromAddressId);
                    fromAddressInfo.put(isActiveKey, addressInfo.isActive());
                });
            }
        }
    }

    /**
     * <p>
     * Gets the deal and contacts associated with the email by using mail id.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents a user id.
     * @param data   Represents the {@link JsonObject}
     * @return {@link JsonArray} of contacts.
     */
    private JsonObject getRelatedContactsAndDeal(final long orgId, final long userId, final JsonObject data) {
        final Long id = data.getLong(CoreEmailFields.ID.getName());

        if (mailManager.isThreadExists(orgId, userId, Set.of(id))) {
            final Collection<Long> contactEmailIds = new HashSet<>();
            final Collection<String> emails = mailManager.fetchRelatedEmails(orgId, userId, id, contactEmailIds);
            final String firstNameKey = ContactFields.FIRST_NAME;
            final String emailsKey = ContactFields.EMAILS;
            final Map<String, FieldInfo> contactFieldMap = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, firstNameKey, emailsKey);
            final Map<StoreSubType, FieldSubType> emailsSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.EMAILS);
            final JsonObject jsonObject = Json.object();
            final JsonArray contacts = Json.array();
            final Map<String, Contact> relatedContacts = contactEmailIds.isEmpty() ? null : contactManager.retrieveContactsByEmailAddress(orgId, contactEmailIds);
            final Deal deal = entityMailManager.getRelatedDeal(orgId, userId, id);
            final JsonObject dealJson;

            if (Objects.nonNull(relatedContacts)) {

                for (final String email : emails) {
                    final Contact contact = relatedContacts.get(email);

                    contacts.put(Objects.isNull(contact) ? convertEmailAddressToJson(email)
                            : contactConverter.buildContactJsonObject(contact, email));
                }
            } else {
                emails.forEach(email -> contacts.put(convertEmailAddressToJson(email)));
            }
            jsonObject.put(CoreEmailFields.RELATED_CONTACTS.getName(), contacts);

            if (Objects.nonNull(deal)) {
                final Long currentTime = Instant.now().toEpochMilli();
                final Date expectedCloseDate = deal.getExpectedCloseDate();

                dealJson = dealConverter.buildDealJsonObject(deal);

                if ((deal.getState() == Deal.State.OPEN) && (Objects.nonNull(expectedCloseDate))) {
                    final Long expectedCloseDateStamp = expectedCloseDate.getTimeStamp();

                    if (expectedCloseDateStamp < currentTime) {
                        dealJson.put(DealFields.IS_OVERDUE, true);
                        dealJson.put(DealFields.TIME_DIFFERENCE, TimeUnit.MILLISECONDS.toDays(currentTime - expectedCloseDateStamp));
                    }
                } else {
                    dealJson.put(DealFields.IS_OVERDUE, false);
                }
                jsonObject.put(CoreEmailFields.IS_DEAL_LINKING_ENABLED.getName(), true);
                jsonObject.put(CoreEmailFields.DEAL.getName(), dealJson);
            } else {
                jsonObject.put(CoreEmailFields.IS_DEAL_LINKING_ENABLED.getName(), false);
            }
            jsonObject.put(FIRST_NAME_FIELD_ID, contactFieldMap.get(firstNameKey).id());
            jsonObject.put(EMAILS_FIELD_ID, contactFieldMap.get(emailsKey).id());
            final JsonObject subFieldTypeJson = Json.object();

            subFieldTypeJson.put(TYPE_FIELD_ID, emailsSubtypes.get(StoreSubType.EMAIL_TYPE).id());
            subFieldTypeJson.put(VALUE_FIELD_ID, emailsSubtypes.get(StoreSubType.EMAIL_VALUE).id());

            jsonObject.put(EMAILS_SUB_TYPE_FIELD, subFieldTypeJson);

            return jsonObject;
        } else {
            throw ErrorCode.get(EmailErrorCodes.THREAD_NOT_FOUND, "i18n.notifications.mail.thread.not.found");
        }
    }

    /**
     * <p>
     * Builds the json object for the emails that are not related to any contact.
     * </p>
     *
     * @param emailAddress Value of the email address to be converted into json object.
     * @return Entity of {@link JsonObject}
     */
    private JsonObject convertEmailAddressToJson(final String emailAddress) {
        final JsonObject emailObject = Json.object();

        emailObject.put(CoreEmailFields.EMAIL.getName(), emailAddress);
        emailObject.put(CoreEmailFields.CONTACT_NAME.getName(), emailAddress.split("@")[0]);
        emailObject.put(CoreEmailFields.IS_NEW_CONTACT.getName(), true);

        return emailObject;
    }

    /**
     * <p>
     * Process the email thread attributes formatting with appropriate placeholders and dispatches it based
     * on draft or send action
     * </p>
     *
     * @param response      Represents the {@link CrmResponse}
     * @param thread        Represents the {@link Thread}
     * @param isDraft       A flag to indicate whether it is a draft or send mail action
     * @param configuration Refers to the {@link EmailConfiguration}
     * @param requestData   Represents the json which contains request data
     * @param crmRequest    Represents the {@link CrmRequest}
     */
    private void formatAndDispatchThreadEmail(final CrmResponse response, final EntityThread thread, final boolean isDraft,
                                              final EmailConfiguration configuration, final JsonObject requestData, final CrmRequest crmRequest) {
        try {
            final EntityEmail mail = thread.getComposedEmail();

            if (Objects.nonNull(mail)) {
                final long orgId = mail.orgId();
                final String subject = mail.getSubject();
                final String content = mail.getContent();

                if (mail.hasPlaceHolders()) {
                    final Collection<PlaceHolder> placeHolders = mail.placeHolders();

                    if (mail.isSubjectHasPlaceHolders() && Objects.nonNull(subject)) {
                        mail.setSubject(placeHolderResolver.resolve(subject,
                                placeHolderExtractor.getSubjectPlaceHolders(placeHolders)));
                    }

                    if (mail.isContentHasPlaceHolders() && Objects.nonNull(content)) {
                        mail.setContent(placeHolderResolver.resolve(content,
                                placeHolderExtractor.getContentPlaceHolders(placeHolders)));
                    }
                }

                if (isDraft) {
                    final MailResponse mailResponse = outboundEmailService.draft(orgId, thread, configuration, requestData);

                    if (Objects.isNull(mailResponse)) {
                        response.setErrorCode(ErrorCode.get(EmailErrorCodes.UNSUBSCRIBED_RECIPIENTS, "i18n.notifications.mail.recipients.unsubscribed"));
                    } else {
                        setResponse(mailResponse, response, crmRequest);
                    }
                } else {
                    setResponse(outboundEmailService.send(orgId, thread, requestData), response, crmRequest);
                }
            }
        } finally {
            response.send();
        }
    }

    /**
     * <p>
     * Processes the email recipients from a given thread and populates a json object with the recipient information.
     * </p>
     *
     * @param thread        Represents the {@link Thread}
     * @param recipientJson which holds the recipient details
     */
    private void processRecipientJson(final EntityThread thread, final JsonObject recipientJson) {
        final Map<String, Contact> contactMap = Optional.ofNullable(thread.getComposedEmail())
                .map(composedEmail -> ((EntityEmail) composedEmail).getContactRecipients())
                .map(ContactRecipients::getContacts).orElseGet(Collections::emptyMap).values().stream().flatMap(Collection::stream)
                .collect(Collectors.toMap(contact -> contact.getPrimaryEmail().getValue(),
                        Function.identity(), (existing, replacement) -> existing));

        emailConverter.loadRecipientJson(contactMap, recipientJson);
    }
}