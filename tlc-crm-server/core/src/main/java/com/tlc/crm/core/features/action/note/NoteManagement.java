package com.tlc.crm.core.features.action.note;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.holder.ConfigManagerHolder;
import com.tlc.crm.core.internal.event.email.EmailAlertDataPublisher;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.NoteHolder.Source;
import com.tlc.crm.core.features.api.note.model.converter.SourceNoteConverter;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.internal.status.NoteErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link Note} related web actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.NOTE, needRelatedApiHandling = true)
@WebAction(path = "/note/mgmt")
public class NoteManagement extends CrmConfigAction<NoteHolder> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final SourceNoteConverter sourceNoteConverter;
    private final SourceNoteManager sourceNoteManager;
    private final NoteManager noteManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final ConfigManagerHolder configManagerHolder;
    private final EmailAlertDataPublisher emailAlertDataPublisher;

    public NoteManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        sourceNoteManager = SourceNoteManager.getInstance();
        noteManager = NoteManager.getInstance();
        sourceNoteConverter = SourceNoteConverter.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        dealManager = DealManager.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        configManagerHolder = ConfigManagerHolder.getInstance();
        this.emailAlertDataPublisher = EmailAlertDataPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(NoteFields.DATA.getName());
        final String type = requestJson.getString(NoteFields.TYPE.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "getNotes" -> getSourceNotes(orgId, data, response, request);
            case "updatePinStatus" -> {
                final long id = data.getLong(NoteFields.ID.getName());
                final boolean isPinned = data.getBoolean(NoteFields.IS_PINNED.getName());

                noteManager.updatePinStatus(orgId, id, isPinned);
                response.put(FieldKeys.MESSAGE.getName(), "i18n.note.update.pin.status.success");
            }
            case "recentlyPinnedNote" -> recentlyPinnedNote(orgId, data, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AbstractConfigManager} of Note
     */
    @Override
    public ConfigManager<NoteHolder> getConfigManager() {
        return sourceNoteManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Note} filled with the input json data
     */
    @Override
    public NoteHolder convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return sourceNoteConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonObject} filled with the note data
     */
    @Override
    public JsonObject convert(final NoteHolder noteHolder) {
        return sourceNoteConverter.convert(noteHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Note} filled with the data
     */
    @Override
    public NoteHolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return sourceNoteConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId,
                       final Collection<NoteHolder> noteHolders, final EventData eventData) {
        process(crmResponse, orgId, userId, true, noteHolders, eventData, Action.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId,
                       final Collection<NoteHolder> noteHolders, final EventData eventData) {
        process(crmResponse, orgId, userId, false, noteHolders, eventData, Action.UPDATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUser(final NoteHolder noteHolder, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final Note note = noteHolder.getNote();
        final CrmUser createdBy = note.getCreatedBy();
        final CrmUser updatedBy = note.getUpdatedBy();

        if (Objects.nonNull(createdBy)) {
            loadUserDetails(createdBy, userMap);
        }

        if (Objects.nonNull(updatedBy)) {
            loadUserDetails(updatedBy, userMap);
        }
        note.getMentionedUsers().forEach(mentionedUser -> loadUserDetails(mentionedUser, userMap));
    }

    /**
     * <p>
     * To retrieve notes mapped for the given source entity id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Contains the Json object
     * @param response Response of the source entity id
     */
    private void getSourceNotes(final Long orgId, final JsonObject data, final CrmResponse response, final CrmRequest request) {
        final int start = Math.max(data.optInt(NoteFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(NoteFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Source source = Source.getType(data.getString(NoteFields.SOURCE.getName()));
        final Long entityId = data.getLong(NoteFields.ENTITY_ID.getName());
        long totalRecords = 0L;

        switch (source) {
            case DEAL -> {

                if (dealManager.exists(orgId, entityId)) {
                    totalRecords = sourceNoteManager.getDealNotesCount(orgId, entityId);
                } else {
                    throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
                }
            }
            case CONTACT -> {

                if (contactManager.exists(orgId, entityId)) {
                    totalRecords = sourceNoteManager.getContactNotesCount(orgId, entityId);
                } else {
                    throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                }
            }
            case COMPANY -> {

                if (companyManager.exists(orgId, entityId)) {
                    totalRecords = sourceNoteManager.getCompanyNotesCount(orgId, entityId);
                } else {
                    throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
                }
            }
        }
        final Collection<NoteHolder> noteHolders = totalRecords > 0 && start <= totalRecords ?
                sourceNoteManager.getSourceNotes(orgId, source, entityId, start, limit) : Collections.emptyList();

        if (!noteHolders.isEmpty()) {
            fetchAndDispatchUser(request, response, noteHolders);
        }
        response.put(NoteFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(NoteFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
    }

    /**
     * <p>
     * Fetches and dispatch the users in notes
     * </p>
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param noteHolders Holds the attributes of notes
     */
    private void fetchAndDispatchUser(final CrmRequest request, final CrmResponse crmResponse,
                                      final Collection<NoteHolder> noteHolders) {
        final Long orgId = request.orgId();
        final Collection<Long> iamUserIds = new HashSet<>();

        for (final NoteHolder noteHolder : noteHolders) {
            final Note note = noteHolder.getNote();
            final CrmUser createdBy = note.getCreatedBy();
            final CrmUser updatedBy = note.getUpdatedBy();

            if (Objects.nonNull(createdBy)) {
                iamUserIds.add(createdBy.getIamId());
            }

            if (Objects.nonNull(updatedBy)) {
                iamUserIds.add(updatedBy.getIamId());
            }
            iamUserIds.addAll(note.getMentionedUsers().stream().map(CrmUser::getIamId).toList());
        }

        if (iamUserIds.isEmpty()) {
            final JsonArray jsonArray = Json.array();

            for (final NoteHolder noteHolder : noteHolders) {
                jsonArray.put(convert(noteHolder));
            }
            crmResponse.put(NoteFields.NOTES.getName(), jsonArray);
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    final JsonArray jsonArray = Json.array();
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (final NoteHolder noteHolder : noteHolders) {
                        final Note note = noteHolder.getNote();
                        final CrmUser createdBy = note.getCreatedBy();
                        final CrmUser updatedBy = note.getUpdatedBy();

                        if (Objects.nonNull(createdBy)) {
                            loadUserDetails(createdBy, userMap);
                        }

                        if (Objects.nonNull(updatedBy)) {
                            loadUserDetails(updatedBy, userMap);
                        }
                        note.getMentionedUsers().forEach(mentionedUser -> loadUserDetails(mentionedUser, userMap));
                        jsonArray.put(convert(noteHolder));
                    }
                    crmResponse.put(NoteFields.NOTES.getName(), jsonArray);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(NoteErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id.
     * @param action Perform action like create, update or delete
     * @param notes  Collection of models
     * @return true, if the given data are valid
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<NoteHolder> notes) {
        final Source companySource = Source.COMPANY;
        final Source contactSource = Source.CONTACT;
        final Source dealSource = Source.DEAL;

        if (!Action.DELETE.equals(action)) {
            final Map<Source, Long> sourceIdsMap = notes.stream().filter(Objects::nonNull)
                    .collect(Collectors.toMap(NoteHolder::getSource, NoteHolder::getEntityId));

            switch (action) {
                case CREATE -> {

                    if (sourceIdsMap.containsKey(companySource) && !companyManager.exists(orgId,
                            sourceIdsMap.get(companySource))) {
                        throw ErrorCode.get(ContactErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
                    } else if (sourceIdsMap.containsKey(contactSource) && !contactManager.exists(orgId,
                            sourceIdsMap.get(contactSource))) {
                        throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                    } else if (sourceIdsMap.containsKey(dealSource) && !dealManager.exists(orgId,
                            sourceIdsMap.get(dealSource))) {
                        throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
                    }
                }
                case UPDATE -> {
                    final Collection<Long> noteIds = notes.stream().filter(Objects::nonNull).map(NoteHolder::id)
                            .collect(Collectors.toSet());

                    if (sourceIdsMap.containsKey(companySource) && !sourceNoteManager.isCompanyNoteIdsExists(orgId,
                            noteIds, sourceIdsMap.get(companySource))) {
                        throw ErrorCode.get(NoteErrorCodes.NOTE_NOT_FOUND);
                    } else if (sourceIdsMap.containsKey(contactSource) && !sourceNoteManager.isContactNoteIdsExists(orgId,
                            noteIds, sourceIdsMap.get(contactSource))) {
                        throw ErrorCode.get(NoteErrorCodes.NOTE_NOT_FOUND);
                    } else if (sourceIdsMap.containsKey(dealSource) && !sourceNoteManager.isDealNoteIdsExists(orgId,
                            noteIds, sourceIdsMap.get(dealSource))) {
                        throw ErrorCode.get(NoteErrorCodes.NOTE_NOT_FOUND);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Populates the full IAM user info into a CrmUser wrapper from the userMap.
     *
     * @param crmUser The wrapper object to enrich
     * @param userMap Lookup map of IAM user ID to User object
     */
    private void loadUserDetails(final CrmUser crmUser, final Map<Long, User> userMap) {
        crmUser.setUser(userMap.get(crmUser.getIamId()));
    }

    /**
     * <p>
     * Fetches the recent pinned notes for the given organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the data {@link JsonObject}
     * @param response Represents the {@link CrmResponse}
     */
    private void recentlyPinnedNote(final long orgId, final JsonObject data, final CrmResponse response) {
        final NoteHolder noteHolder = new NoteHolder();
        final Long entityId = data.getLong(NoteFields.ENTITY_ID.getName());
        final com.tlc.crm.entity.fields.source.Source source = com.tlc.crm.entity.fields.source.Source.get(data.getString(NoteFields.SOURCE.getName()));

        if (!configManagerHolder.getManager(source).exists(orgId, entityId)) {
            throw ErrorCode.get(CoreErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
        noteHolder.setOrgId(orgId);
        noteHolder.setSource(Source.getType(source.getName()));
        noteHolder.setEntityId(entityId);
        response.put(NoteFields.HAS_MORE.getName(), sourceNoteManager.getTotalPinnedNotesCount(noteHolder) > NumericalConstant.TWO);
        response.put(NoteFields.NOTES.getName(), convert(sourceNoteManager.getRecentlyPinnedNotes(noteHolder, NumericalConstant.TWO)));
    }

    /**
     * <p>
     * Processes the create and update action
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param isCreate    Represents whether the action is create or not
     * @param noteHolders Represents the collection of {@link NoteHolder}
     * @param eventData   Represents the {@link EventData}
     * @param action      Represents the {@link Action}
     */
    private void process(final CrmResponse crmResponse, final Long orgId, final Long userId, final boolean isCreate,
                         final Collection<NoteHolder> noteHolders, final EventData eventData, final Action action) {
        final Collection<ErrorDescriptor> errors = validate(noteHolders, isCreate ? Group.Create.class : Group.Update.class);

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, noteHolders)) {
            final ProcessOutcome processOutcome;

            if (isCreate) {
                processOutcome = getConfigManager().create(orgId, noteHolders);
            } else {
                preAction(action, noteHolders, eventData);
                processOutcome = getConfigManager().bulkUpdate(orgId, noteHolders);
            }

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, noteHolders, eventData);
            triggerMentionedEmailAlert(orgId, userId, action, noteHolders);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Triggers the email alert for the mentioned users in notes, if any
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param userId      Represents the user ID
     * @param action      Represents the {@link Action}
     * @param noteHolders Represents the collection of {@link NoteHolder}
     */
    private void triggerMentionedEmailAlert(final long orgId, final long userId, final Action action,
                                            final Collection<NoteHolder> noteHolders) {
        final Source source = noteHolders.stream()
                .map(NoteHolder::getSource).findAny().orElseGet(() -> null);
        final Collection<Long> noteIds = noteHolders.stream().map(NoteHolder::getNote)
                .filter(note -> Objects.nonNull(note) && !note.getMentionedUsers().isEmpty())
                .map(Note::id).collect(Collectors.toSet());

        if (Objects.nonNull(source) && !noteIds.isEmpty()) {
            this.emailAlertDataPublisher.publish(orgId, userId, action, noteIds, getAlertTemplate(source));
        }
    }

    /**
     * <p>
     * Retrieves the source based Alert Template.
     * </p>
     *
     * @param source Represents the source of note
     * @return the {@link EmailNotificationTemplate} name based on the source
     */
    private String getAlertTemplate(final Source source) {
        return switch (source) {
            case CONTACT -> EmailNotificationTemplate.CONTACT_NOTE_MENTION.getName();
            case DEAL -> EmailNotificationTemplate.DEAL_NOTE_MENTION.getName();
            case COMPANY -> EmailNotificationTemplate.COMPANY_NOTE_MENTION.getName();
        };
    }
}