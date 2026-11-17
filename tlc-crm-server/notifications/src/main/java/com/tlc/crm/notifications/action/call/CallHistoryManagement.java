package com.tlc.crm.notifications.action.call;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CriteriaDetails;
import com.tlc.crm.common.models.CriteriaDetailsHolder;
import com.tlc.crm.common.models.LimitConfig;

import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.core.features.action.call.model.CallLog;
import com.tlc.crm.core.features.action.call.model.Outcome;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.field.call.CallLogFields;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.validator.internal.ContactDataValidator;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.note.model.converter.NoteConverter;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallHistory;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.converter.CallDetailsConverter;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;

import com.tlc.crm.security.permission.annotation.authorizer.PhoneAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import com.tlc.notification.model.call.CallStatus;
import com.tlc.sql.common.api.dml.Operator;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class handles the management of call history, including fetching call details and listing call records.
 * It extends {@link AuthorizationController} to ensure secure API access.
 *
 * @author Sathish
 * @version 1.0
 */
@PhoneAuthorizer(subCategories = {SubCategory.Type.CALL_LOGS, SubCategory.Type.ACCESS_TO_PHONE})
@WebAction(path = "/call/history/mgmt")
public class CallHistoryManagement extends SecureEventAction {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final Long NEGATIVE_ONE = -1L;

    private final CallHistoryManager callHistoryManager;
    private final CallDetailsConverter callDetailsConverter;
    private final com.tlc.iam.client.UserService userService;
    private final OrganizationService organizationService;
    private final CallLogManager callLogManager;
    private final UserService crmUserService;
    private final ModelValidator modelValidator;
    private final ContactDataValidator contactDataValidator;
    private final ContactConverter contactConverter;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final NoteConverter noteConverter;
    private final NoteManager noteManager;

    public CallHistoryManagement() {
        callHistoryManager = CallHistoryManager.getInstance();
        callDetailsConverter = CallDetailsConverter.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        callLogManager = CallLogManager.getInstance();
        crmUserService = UserService.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        contactDataValidator = ContactDataValidator.getInstance();
        contactConverter  = ContactConverter.getInstance();
        converterConfigBuilder =  ConverterConfigBuilder.getInstance();
        noteConverter = NoteConverter.getInstance();
        noteManager =  NoteManager.getInstance();
    }

    /**
     * Handles the processing of authorized requests.
     *
     * @param request     The request to be handled.
     * @param crmResponse The response.
     */
    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final Long userId = request.userId();

        switch (type) {
            case "log" -> log(orgId, userId, data, crmResponse);
            case "get" -> fetchAndDispatch(orgId, data, crmResponse);
            case "list" -> fetchAllAndDispatch(orgId, data, crmResponse);
            case "createNote" -> {
                final Note note = new Note();

                note.setContent(data.getString(NotificationFields.NOTE.getName()));
                note.setCreatedBy(request.getUser());
                note.setOrgId(orgId);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(note), Group.Create.class);

                if (errors.isEmpty()) {
                    callHistoryManager.createNote(data.getString(NotificationFields.CALL_SID.getName()), note);
                    crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.call.note.added"));
                } else {
                    final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.CALL_NOTE_CREATE_ACTION_FAILED);

                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
                }
            }
            case "isLoggable" -> {
                final String externalCallId = data.getString(NotificationFields.CALL_SID.getName());
                final boolean isLoggable = callHistoryManager.isLoggable(orgId, externalCallId);

                crmResponse.put(NotificationFields.IS__LOGGABLE.getName(), isLoggable);
            }
            case "addContact" -> {
                final long callHistoryId = data.getLong(NotificationFields.ID.getName());

                if (callHistoryManager.exists(orgId, callHistoryId)) {
                    final JsonObject contactJson = data.getJsonObject(NotificationFields.CONTACT.getName());
                    final Contact contact = contactConverter.convert(converterConfigBuilder.build(request,
                            false, contactJson), contactJson);
                    final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(contact), Group.Create.class);

                    if (errors.isEmpty()) {
                        final Collection<Contact> contacts = List.of(contact);

                        contactDataValidator.validate(orgId, contacts);
                        callHistoryManager.linkToNewContact(callHistoryId, contact);

                        crmResponse.put(NotificationFields.CONTACT_ID.getName(), contact.id());
                        final EventData eventData = ((CrmEventRequest) request).getEventData();

                        eventData.setUserId(userId);
                        eventData.setOrgId(orgId);
                        postAction(Action.CREATE, contacts, eventData);
                    } else {
                        final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.CONTACT_CREATE_ACTION_FAILED, "i18n.notifications.mail.contact.create.failed");

                        crmResponse.setErrorCode(errorCode);
                        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                    }
                } else {
                    throw ErrorCode.get(NotificationErrorCodes.CALL_DETAILS_NOT_FOUND, "i18n.call.detail.not.found");
                }
            }
            case "recentCalls" -> fetchRecentCalls(orgId, request, crmResponse);
            case "removeRecord" -> {
                callHistoryManager.removeRecord(orgId, data.getLong(NotificationFields.ID.getName()));
                crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.call.record.remove.succeed"));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  the request object representing a {@link CrmRequest}
     * @param crmResponse the response object representing a {@link CrmResponse}
     * @throws Exception if any error occurs during request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final Long orgId = crmRequest.orgId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final String type = requestJson.getString(FieldKeys.TYPE.getName());

        switch (type) {
            case "getRecentCallContacts" -> getRecentCallContacts(orgId, data, crmResponse);
            case "createNote" -> createNote(orgId, data, crmRequest.getUser(), crmResponse);
            case "updateNote" -> updateNote(orgId, data, crmRequest.getUser(), crmResponse);
            case "noteList" -> fetchNoteList(orgId, data, crmResponse);
            case "deleteNote" -> noteManager.delete(orgId, data.getLong(NotificationFields.ID.getName()));
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * Fetches all call history records for the given organization (with optional search/criteria and pagination),
     * enriches them with user details (if required), and dispatches the final result through {@link CrmResponse}.
     *
     * @param orgId       Organization ID
     * @param data        Request input containing pagination, criteria, and filters
     * @param crmResponse Response wrapper that supports sync/async response building
     */
    private void fetchAllAndDispatch(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final String criteriaKey = NotificationFields.CRITERIA.getName();
        final String searchText = data.optString(NotificationFields.SEARCH_TEXT.getName(), null);
        final int start = data.getInt(NotificationFields.START.getName());
        final CriteriaDetailsHolder criteriaDetailsHolder = data.containsKey(criteriaKey)
                ? buildCriteriaConfigs(data.getJsonArray(criteriaKey)) : new CriteriaDetailsHolder();
        final long totalCount = callHistoryManager.getTotalCount(orgId, searchText, criteriaDetailsHolder);
        final String hasMoreKey = FieldKeys.HAS_MORE.getName();
        final String listKey = FieldKeys.LIST.getName();
        final String totalRecordsKey = FieldKeys.TOTAL_RECORDS.getName();

        if (start <= totalCount) {
            final int limit = data.getInt(NotificationFields.LIMIT.getName());
            final LimitConfig limitConfig = new LimitConfig(start, limit);
            final CallHistory callHistory = callHistoryManager.get(orgId, searchText, criteriaDetailsHolder, limitConfig);
            final Collection<Long> iamUserIds = new HashSet<>();
            final JsonArray jsonArray = Json.array();

            for (final CallDetails callDetails : callHistory) {
                iamUserIds.addAll(callDetails.userIds());
            }
            crmResponse.put(hasMoreKey, start + limit <= totalCount);
            crmResponse.put(totalRecordsKey, totalCount);

            if (iamUserIds.isEmpty()) {
                for (final CallDetails callDetails : callHistory) {
                    jsonArray.put(callDetailsConverter.convert(callDetails));
                }
                crmResponse.put(listKey, jsonArray);
            } else {
                crmResponse.convertIntoAsync();
                final EventCallback<Collection<User>> eventCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                            for (final CallDetails callDetails : callHistory) {
                                loadUserDetailsIntoCallDetails(userMap, callDetails);
                                resetDurationIfNoAnswer(callDetails);
                                final JsonObject callJson = callDetailsConverter.convert(callDetails);

                                removeUserIfNotConnected(callJson, callDetails);
                                jsonArray.put(callJson);
                            }
                            crmResponse.put(listKey, jsonArray);
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        sendError(crmResponse, null, ErrorCode.get(ImportErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                    }
                };
                userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, eventCallback);
            }
        } else {
            crmResponse.put(hasMoreKey, false);
            crmResponse.put(listKey, Collections.emptyList());
            crmResponse.put(totalRecordsKey, totalCount);
        }
    }

    /**
     * Fetches a specific CallDetails record for the given ID, enriches it with user details if needed,
     * and dispatches the response accordingly.
     *
     * @param orgId       Organization ID
     * @param data        Request input containing CallDetails ID
     * @param crmResponse Response wrapper, handles sync/async lifecycle
     */
    private void fetchAndDispatch(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long callDetailsId = data.getLong(NotificationFields.ID.getName());
        final CallDetails callDetails = callHistoryManager.getCallDetails(orgId, callDetailsId);
        final Collection<Long> userIds = callDetails.userIds();
        final String callDetailsKey = NotificationFields.CALL_DETAILS.getName();

        if (userIds.isEmpty()) {
            crmResponse.put(callDetailsKey, callDetailsConverter.convert(callDetails));
        } else {
            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> userEventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        loadUserDetailsIntoCallDetails(userMap, callDetails);
                        resetDurationIfNoAnswer(callDetails);
                        final JsonObject callJson = callDetailsConverter.convert(callDetails);

                        removeUserIfNotConnected(callJson, callDetails);
                        crmResponse.put(callDetailsKey, callJson);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(ImportErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                }
            };
            userService.fetchUser(organizationService.getIamOrgId(orgId), userIds, userEventCallback);
        }
    }

    /**
     * Converts an existing {@link CallDetails} into a persistent {@link CallLog} entry and stores it.
     *
     * @param orgId        Organization ID
     * @param userId       User who triggers the log creation
     * @param data         Input payload with CallDetails ID or SID, Agenda, Outcome, etc.
     * @param crmResponse  Response container to return created CallLog ID or validation errors
     */
    private void log(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final String idKey = NotificationFields.ID.getName();
        final CallDetails callDetails;

        if (data.containsKey(idKey)) {
            final long callDetailsId = data.getLong(idKey);
            callDetails = callHistoryManager.getCallDetails(orgId, callDetailsId);
        } else {
            final String externalCallId = data.getString(NotificationFields.CALL_SID.getName());
            callDetails = callHistoryManager.getCallDetails(externalCallId).orElseGet(() -> null);
        }

        if (Objects.isNull(callDetails)) {
            throw ErrorCode.get(NotificationErrorCodes.CALL_DETAILS_NOT_FOUND);
        }
        final CallLog callLog = convertCallDetailsIntoCallLog(callDetails);

        callLog.setOrgId(orgId);
        callLog.setCreatedBy(crmUserService.getUser(orgId, userId));
        callLog.setAgenda(data.getNullableString(CallLogFields.AGENDA.getName()));

        if (data.containsKey(CallLogFields.OUTCOME_ID.getName())) {
            final Long outcomeId = data.getNullableValue(CallLogFields.OUTCOME_ID.getName());
            final Outcome outcome = new Outcome();

            outcome.setId(outcomeId);
            callLog.setOutcome(outcome);
        }
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(callLog), Group.Create.class);

        if (errors.isEmpty()) {
            callLogManager.create(callLog);
            callDetails.setStoredAsCallLog(true);
            callHistoryManager.update(callDetails);
            crmResponse.put(FieldKeys.CREATED_RECORD.getName(), callLog.id());
        } else {
            final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.CALL_LOG_CREATE_FAILED);

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
        }
    }

    /**
     * <p>
     * Gets the recent call contacts for the given organization and dispatches the response.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param data        The request data containing pagination parameters.
     * @param crmResponse The response to be sent.
     */
    private void getRecentCallContacts(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = Math.max(data.optInt(NotificationFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(NotificationFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Collection<Contact> contacts = callHistoryManager.getRecentCallContacts(orgId, new LimitConfig(start, limit));
        final JsonArray contactsArray = Json.array();

        for (final Contact contact : contacts) {
            final JsonObject contactJson = Json.object();
            final Long id = contact.id();
            final Phone primaryPhone = contact.getPrimaryPhone();
            final DisplayPicture thumbnail = contact.getThumbnail();

            contactJson.put(ContactFields.VALUE, id);
            contactJson.put(ContactFields.NAME, contact.getFullName());

            if (Objects.nonNull(primaryPhone)) {
                contactJson.put(ContactFields.PHONE, primaryPhone.getNumber());
            }

            if (Objects.nonNull(thumbnail)) {
                contactJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(id, Contact.ENTITY_TYPE, thumbnail));
            }
            contactsArray.put(contactJson);
        }
        crmResponse.put(FieldKeys.OPTIONS.getName(), contactsArray);
    }

    /**
     * <p>
     * Creates a note for a specific call history record and adds it to the database.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param data        The request data containing note details and call ID.
     * @param crmUser     The {@link CrmUser} who is creating the note.
     * @param crmResponse The {@link CrmResponse} where success messages or errors will be written.
     */
    private void createNote(final Long orgId, final JsonObject data, final CrmUser crmUser,
                            final CrmResponse crmResponse) {
        final Note note = new Note();

        note.setContent(data.getString(NoteFields.CONTENT.getName()));
        note.setCreatedBy(crmUser);
        note.setOrgId(orgId);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(note), Group.Create.class);

        if (errors.isEmpty()) {
            callHistoryManager.createNote(orgId, data.getLong(NotificationFields.ID.getName()), note);
            crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.call.note.added"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.CALL_NOTE_CREATE_ACTION_FAILED);

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
        }
    }

    /**
     * <p>
     * Updates the content of an existing note for a specific call history record.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param data        The request data containing note ID and updated content.
     * @param crmUser     The {@link CrmUser} who is updating the note.
     * @param crmResponse The {@link CrmResponse} where success messages or errors will be written.
     */
    private void updateNote(final Long orgId, final JsonObject data, final CrmUser crmUser,
                            final CrmResponse crmResponse) {
        final long noteId = data.getLong(NotificationFields.ID.getName());
        final Note note = noteManager.get(orgId, noteId);

        note.setContent(data.getString(NoteFields.CONTENT.getName()));
        note.setUpdatedBy(crmUser);
        note.setOrgId(orgId);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(note), Group.Update.class);

        if (errors.isEmpty()) {
            noteManager.update(note);
            crmResponse.put(NotificationFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.call.note.updated"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(NotificationErrorCodes.CALL_NOTE_UPDATE_ACTION_FAILED);

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
        }
    }

    /**
     * <p>
     * Fetches a paginated list of notes for the given call history record.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param data        The request data containing call history ID and pagination parameters.
     * @param crmResponse The response to be sent.
     */
    private void fetchNoteList(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long callHistoryId = data.getLong(NotificationFields.ID.getName());
        final int start = Math.max(data.optInt(NotificationFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(NotificationFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalCount = callHistoryManager.getNoteCount(orgId, callHistoryId);

        if (start <= totalCount) {
            final LimitConfig limitConfig = new LimitConfig(start, limit);
            final Collection<Note> notes = callHistoryManager.getNotes(orgId, callHistoryId, limitConfig);
            final JsonArray notesArray = Json.array();

            for (final Note note : notes) {
                notesArray.put(noteConverter.convert(note));
            }
            crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalCount);
            crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalCount);
            crmResponse.put(FieldKeys.LIST.getName(), notesArray);
        } else {
            crmResponse.put(FieldKeys.HAS_MORE.getName(), false);
            crmResponse.put(FieldKeys.LIST.getName(), Json.array());
            crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalCount);
        }
    }

    /**
     * <p>
     * Fetch and loads the recent calls based on the current user.
     * </p>
     *
     * @param orgId       Represents the organisation ID.
     * @param crmRequest  The request to be handled.
     * @param crmResponse The response to be sent.
     */
    private void fetchRecentCalls(final Long orgId, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Collection<CallDetails> recentCalls = callHistoryManager.getRecentCalls(orgId, crmRequest.userId(), new LimitConfig(DEFAULT_START, DEFAULT_LIMIT));
        final UserRegionalPreference regionalPreference = crmRequest.getUser().getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = regionalPreference.getTimezoneInfo();
        final JsonArray callsArray = Json.array();

        for (final CallDetails recentCall : recentCalls) {
            final JsonObject callJson = Json.object();
            final Contact contact = recentCall.getContact();
            final JsonObject numberJson = convertPhoneIntoJson(recentCall.getContactNumber());

            if (Objects.isNull(contact)) {
                callJson.put(NotificationFields.NUMBER.getName(), numberJson);
            } else {
                final JsonObject contactJson = convertContactIntoJson(contact);

                contactJson.put(NotificationFields.NUMBER.getName(), numberJson);
                callJson.put(NotificationFields.CONTACT.getName(), contactJson);
            }
            final TemporalService loggedTime = TemporalServiceProvider.get(recentCall.getLoggedTime().getUtcTimeStamp());

            callJson.put(NotificationFields.CALLED_TIME.getName(), loggedTime.convertTimeStampToRelativeTime(timezoneInfo));
            callJson.put(NotificationFields.CALL_TYPE.getName(), recentCall.getType().getId());
            callsArray.put(callJson);
        }
        crmResponse.put(NotificationFields.CALLS.getName(), callsArray);
    }

    /**
     * Converts a {@link Contact} into a JSON object.
     *
     * @param contact The {@link Contact} object to convert.
     * @return A JSON object containing contact details.
     */
    private JsonObject convertContactIntoJson(final Contact contact) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), contact.id());
        jsonObject.put(NotificationFields.NAME.getName(), contact.getFullName());//TODO Add profile pic
        return jsonObject;
    }

    /**
     * Converts a {@link Phone} into a JSON object.
     *
     * @param phone The {@link Phone} object to convert.
     * @return A JSON object containing {@link Phone} details.
     */
    private JsonObject convertPhoneIntoJson(final Phone phone) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), phone.id());
        jsonObject.put(NotificationFields.NUMBER.getName(), phone.getNumberWithCode());

        return jsonObject;
    }

    /**
     * <p>
     * Loads the user details into call details.
     * </p>
     *
     * @param userMap     Holds the details of the users.
     * @param callDetails Holds the details of the call details.
     */
    private void loadUserDetailsIntoCallDetails(final Map<Long, User> userMap, final CallDetails callDetails) {
        final CrmUser participantUser = callDetails.getParticipantUser();
        final Collection<Note> notes = callDetails.getNotes();

        if (Objects.nonNull(participantUser)) {
            final User user = userMap.get(participantUser.getIamId());

            if (Objects.nonNull(user)) {
                participantUser.setUser(user);
            }
        }

        if (Objects.nonNull(notes)) {
            for (final Note note : notes) {
                final CrmUser owner = note.getCreatedBy();
                final User user = userMap.get(owner.getIamId());

                if (Objects.nonNull(user)) {
                    owner.setUser(user);
                }
            }

        }
    }

    /**
     * <p>
     * Converts the {@link CallDetails} into {@link CallLog}.
     * </p>
     *
     * @param callDetails Holds the details of the {@link CallDetails}.
     * @return {@link CallLog}.
     */
    private CallLog convertCallDetailsIntoCallLog(final CallDetails callDetails) {
        final CallLog callLog = new CallLog();
        final CallSummary summary = callDetails.getSummary();

        callLog.setCallType(callDetails.getType());
        callLog.setParticipant(callDetails.getContact());
        callLog.setRelatedDeal(callDetails.getDeal());
        callLog.setStartTime(Objects.isNull(summary) ? callDetails.getLoggedTime() : summary.getStartTime());

        return callLog;
    }

    /**
     * Builds a collection of filtering criteria for call history retrieval.
     *
     * @param criteriaJson JSON array containing field-value pairs for filtering.
     * @return A {@link CriteriaDetailsHolder} objects to be used for filtering.
     */
    private CriteriaDetailsHolder buildCriteriaConfigs(final JsonArray criteriaJson) {
        final CriteriaDetailsHolder criteriaDetailsHolder = new CriteriaDetailsHolder();
        final int size = criteriaJson.size();
        final String valueKey = FieldKeys.VALUE.getName();
        final String fieldKey = FieldKeys.FIELD.getName();

        for (int index = 0; index < size; index++) {
            final JsonObject criteria = criteriaJson.getJsonObject(index);
            final Object value = criteria.get(valueKey);

            if (Objects.equals(value, NEGATIVE_ONE)) {
                continue;
            }
            final String field = criteria.getString(fieldKey);

            criteriaDetailsHolder.addCriteria(new CriteriaDetails(field, value, Operator.EQUAL, criteria.getBoolean(FieldKeys.IS_MANDATORY.getName())));
        }
        return criteriaDetailsHolder;
    }

    /**
     * <p>
     * Resets the call duration when the call status is {@link CallStatus#NO_ANSWER}.
     * </p>
     *
     * @param callDetails Represents the {@link CallDetails}
     */
    private void resetDurationIfNoAnswer(final CallDetails callDetails) {
        if (isNotConnected(callDetails.getStatus())) {
            final CallSummary summary = callDetails.getSummary();

            if (Objects.nonNull(summary)) {
                summary.setDuration(null);
            }
            callDetails.setParticipantUser(null);
        }
    }

    /**
     * <p>
     * Removes the user key from the JSON object when the call status is {@link CallStatus#NO_ANSWER}.
     * </p>
     *
     * @param callJson    Represents the {@link JsonObject} of the call details.
     * @param callDetails Represents the {@link CallDetails}.
     */
    private void removeUserIfNotConnected(final JsonObject callJson, final CallDetails callDetails) {
        final String userKey = NotificationFields.USER.getName();

        if (callJson.containsKey(userKey) && isNotConnected(callDetails.getStatus())) {
            callJson.remove(userKey);
        }
    }

    /**
     * <p>
     * Tells whether the given call status means the call was never actually connected/answered by anyone.
     * </p>
     *
     * @param callStatus Represents the {@link CallStatus} of the call.
     * @return {@code true} when the call was missed, busy, or canceled - {@code false} otherwise.
     */
    private boolean isNotConnected(final CallStatus callStatus) {
        return callStatus == CallStatus.NO_ANSWER || callStatus == CallStatus.BUSY || callStatus == CallStatus.CANCELED;
    }
}