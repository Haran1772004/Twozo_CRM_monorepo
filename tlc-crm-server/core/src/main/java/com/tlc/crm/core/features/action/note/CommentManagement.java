package com.tlc.crm.core.features.action.note;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.internal.event.email.EmailAlertDataPublisher;
import com.tlc.crm.entity.features.api.note.manager.CommentManager;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.features.api.note.model.converter.CommentConverter;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.NoteErrorCodes;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.notification.app.event.NotificationEvent;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles web actions related to comment management for Notes.
 * <p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/note/comment/mgmt")
public class CommentManagement extends SecureEventAction {

    // Default pagination start index and limit
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final CommentConverter commentConverter;
    private final CommentManager commentManager;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final EmailAlertDataPublisher emailAlertDataPublisher;

    public CommentManagement() {
        commentConverter = CommentConverter.getInstance();
        commentManager = CommentManager.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        this.emailAlertDataPublisher = EmailAlertDataPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void secureProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(NoteFields.DATA.getName());
        final String type = requestJson.getString(NoteFields.TYPE.getName());
        final String source = data.optString(NoteFields.SOURCE.getName(), null);
        final Long orgId = request.orgId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;

        switch (type) {
            case "get" -> {
                final long id = data.getLong(NoteFields.ID.getName());
                final Comment comment = commentManager.get(orgId, id);

                fetchAndDispatchUser(request, response, List.of(comment), false);
            }
            case "add" -> {
                final Comment comment = convert(request, data);
                final EventData eventData = crmEventRequest.getEventData();

                comment.setNoteId(data.getLong(NoteFields.NOTE_ID.getName()));
                validate(comment, Group.Create.class);
                commentManager.create(comment);
                response.put(FieldKeys.CREATED_RECORD.getName(), comment.id());
                final Collection<Comment> comments = List.of(comment);

                eventData.setValues(comments);
                eventData.setAction(Action.CREATE);
                invokePostEvents(source, comments, eventData, Action.CREATE);
            }
            case "update" -> {
                final Comment comment = convert(request, data);
                final EventData eventData = crmEventRequest.getEventData();

                validate(comment, Group.Update.class);
                commentManager.update(comment);
                response.put(FieldKeys.UPDATED_RECORD.getName(), comment.id());
                final Collection<Comment> comments = List.of(comment);

                eventData.setValues(comments);
                eventData.setAction(Action.UPDATE);
                invokePostEvents(source, comments, eventData, Action.UPDATE);
            }
            case "delete" -> {
                final long id = data.getLong(NoteFields.ID.getName());

                commentManager.delete(orgId, id);
                response.put(FieldKeys.MESSAGE.getName(), "i18n.note.comment.delete.success");
            }
            case "getComments" -> {
                final long noteId = data.getLong(NoteFields.NOTE_ID.getName());
                final int start = data.optInt(NoteFields.START.getName(), DEFAULT_START_VALUE);
                final int limit = data.optInt(NoteFields.LIMIT.getName(), DEFAULT_LIMIT);
                final Long totalRecords = commentManager.getCommentsCount(orgId, noteId);
                final Collection<Comment> comments = totalRecords > 0 ?
                        commentManager.getComments(orgId, noteId, new LimitConfig(start, limit)) : Collections.emptyList();

                response.put(NoteFields.TOTAL_RECORDS.getName(), totalRecords);
                response.put(NoteFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                fetchAndDispatchUser(request, response, comments, true);
            }
            case "updatePinStatus" -> {
                final long id = data.getLong(NoteFields.ID.getName());
                final boolean isPinned = data.getBoolean(NoteFields.IS_PINNED.getName());

                commentManager.updatePinStatus(orgId, id, isPinned);
                response.put(FieldKeys.MESSAGE.getName(), "i18n.note.comment.update.pin.status.success");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of api type
     */
    protected Collection<String> stateRequiredTypes() {
        return Set.of(ActionFields.UPDATE);
    }

    /**
     *
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @returnt The collection of {@link TlcModel}
     */
    protected Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final long orgId = request.orgId();

        return switch (type) {
            case ActionFields.UPDATE -> {
                if (data.containsKey(ActionFields.ID)) {
                    yield List.of(commentManager.get(orgId, data.getLong(ActionFields.ID)));
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    /**
     * Validates the given comment object against the specified validator group.
     * If there are any constraint violations, an error code is thrown with the violation message.
     *
     * @param comment        The {@link Comment} object to validate
     * @param validatorGroup The validation group class to use for validation
     */
    private void validate(final Comment comment, final Class<?> validatorGroup) {
        final Collection<ConstraintViolation<Comment>> violations = ValidatorServiceProvider.get().modelValidator()
                .validate(comment, validatorGroup);

        if (!violations.isEmpty()) {

            for (final ConstraintViolation<Comment> violation : violations) {
                throw ErrorCode.get(NoteErrorCodes.COMMENT_VALIDATION_FAILED, violation.getMessage());
            }
        }
    }

    /**
     * <p>
     * Triggers post action events for comment operations.
     * </p>
     *
     * @param eventData Event data containing the comment details
     * @param action    The action performed (CREATE, UPDATE)
     */
    private void invokePostEvents(final String source, final Collection<Comment> comments,
                                  final EventData eventData, final Action action) {
        triggerEvent(NotificationEvent.APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        triggerMentionEmailAlerts(eventData.getOrgId(), eventData.getUserId(), source, comments, action);
    }

    /**
     * <p>
     * Triggers email alerts for mentioned users in the comment.
     * It collects the IDs of comments and publishes alert data for them.
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param source   Represents the comment source
     * @param comments Represents the {@link Collection} of {@link Comment}
     * @param action   Represents the {@link Action}
     */
    private void triggerMentionEmailAlerts(final long orgId, final long userId, final String source,
                                           final Collection<Comment> comments, final Action action) {
        final Collection<Long> mentionedCommentIds = new HashSet<>();

        comments.forEach(comment -> {
            final Collection<CrmUser> mentionedUsers = comment.getMentionedUsers();

            if (Objects.nonNull(mentionedUsers) && !mentionedUsers.isEmpty()) {
                mentionedCommentIds.add(comment.id());
            }
        });

        if (!mentionedCommentIds.isEmpty() && Objects.nonNull(source)) {
            this.emailAlertDataPublisher.publish(orgId, userId, action,
                    mentionedCommentIds, getAlertTemplate(NoteHolder.Source.getType(source)));
        }
    }

    /**
     * Converts JSON payload into {@link Comment} model using the converter.
     *
     * @param request CRM context (org ID, user ID, locale)
     * @param data    Raw JSON data for comment
     * @return Converted {@link Comment} object
     */
    private Comment convert(final CrmRequest request, final JsonObject data) {
        return commentConverter.convert(new ConverterConfig(request.orgId(), request.userId(), request.locale(), false, data), data);
    }

    /**
     * Converts a {@link Comment} object to JSON for response building.
     */
    private JsonObject convert(final Comment comment) {
        return commentConverter.convert(comment);
    }

    /**
     * Enriches comment(s) with user details like createdBy, updatedBy, and mentioned users.
     * If user IDs are present, performs async fetch and then sends enriched response.
     * Else, builds and returns comment response immediately.
     *
     * @param request     Request context for org ID
     * @param crmResponse Response object to send results
     * @param comments    List of comments to enrich
     * @param isBulkGet   Whether to return a list or a single comment
     */
    private void fetchAndDispatchUser(final CrmRequest request, final CrmResponse crmResponse,
                                      final Collection<Comment> comments, final boolean isBulkGet) {
        final Long orgId = request.orgId();
        final Collection<Long> iamUserIds = new HashSet<>();

        for (final Comment comment : comments) {
            final CrmUser createdBy = comment.getCreatedBy();
            final CrmUser updatedBy = comment.getUpdatedBy();

            if (Objects.nonNull(createdBy)) {
                iamUserIds.add(createdBy.getIamId());
            }

            if (Objects.nonNull(updatedBy)) {
                iamUserIds.add(updatedBy.getIamId());
            }
            iamUserIds.addAll(comment.getMentionedUsers().stream().map(CrmUser::getIamId).toList());
        }

        if (iamUserIds.isEmpty()) {
            final JsonArray jsonArray = Json.array();

            for (final Comment comment : comments) {
                jsonArray.put(convert(comment));
            }
            crmResponse.put(NoteFields.COMMENTS.getName(), jsonArray);
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {
                @Override
                public void onSuccess(final Collection<User> users) {
                    final JsonArray jsonArray = Json.array();
                    final Map<Long, User> userMap = users.stream()
                            .collect(Collectors.toMap(User::id, Function.identity()));

                    for (final Comment comment : comments) {
                        if (comment.getCreatedBy() != null) {
                            loadUserDetails(comment.getCreatedBy(), userMap);
                        }

                        if (comment.getUpdatedBy() != null) {
                            loadUserDetails(comment.getUpdatedBy(), userMap);
                        }
                        comment.getMentionedUsers().forEach(
                                mentionedUser -> loadUserDetails(mentionedUser, userMap)
                        );

                        final JsonObject commentJson = convert(comment);

                        if (isBulkGet) {
                            jsonArray.put(commentJson);
                        } else {
                            crmResponse.put(NoteFields.COMMENT.getName(), commentJson);
                            crmResponse.send();
                        }
                    }
                    crmResponse.put(NoteFields.COMMENTS.getName(), jsonArray);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(NoteErrorCodes.USER_RETRIEVAL_FAILED,
                            "i18n.common.user.retrieval.failed", throwable
                    ));
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
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
     * Retrieves the source based alert template for the given source
     * </p>
     *
     * @param source Represents the source of note
     * @return The alert template for the given source
     */
    private String getAlertTemplate(final NoteHolder.Source source) {
        return switch (source) {
            case CONTACT -> EmailNotificationTemplate.CONTACT_NOTE_COMMENT_MENTION.getName();
            case DEAL -> EmailNotificationTemplate.DEAL_NOTE_COMMENT_MENTION.getName();
            case COMPANY -> EmailNotificationTemplate.COMPANY_NOTE_COMMENT_MENTION.getName();
        };
    }
}