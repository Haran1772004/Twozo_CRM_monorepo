package com.tlc.crm.core.notification.email.alert.processor.impl;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.MentionAlertRequest;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.notification.email.alert.handler.EmailAlertHandler;
import com.tlc.crm.core.notification.email.alert.processor.EmailAlertProcessor;
import com.tlc.crm.core.notification.email.alert.util.MessageTruncator;
import com.tlc.crm.entity.features.api.note.manager.CommentManager;
import com.tlc.crm.entity.features.api.note.manager.NoteManager;
import com.tlc.crm.entity.features.api.note.model.Comment;
import com.tlc.crm.entity.features.api.note.model.Message;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.models.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * <p>
 * Processor which process email alerts for mention email alert.
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public class MentionAlertProcessor implements EmailAlertProcessor {

    private final EmailAlertHandler emailAlertHandler;
    private final NoteManager noteManager;
    private final SourceNoteManager sourceNoteManager;
    private final CommentManager commentManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final SourceActivityManager sourceActivityManager;
    private final MessageTruncator messageTruncator;

    private MentionAlertProcessor() {
        this.emailAlertHandler = EmailAlertHandler.getInstance();
        this.noteManager = NoteManager.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.commentManager = CommentManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.sourceActivityManager = SourceActivityManager.getInstance();
        this.messageTruncator = MessageTruncator.getInstance();
    }

    /**
     * <p>
     * Holder class for {@link MentionAlertProcessor} instance.
     * </p>
     */
    private static final class InstanceHolder {
        public static final MentionAlertProcessor INSTANCE = new MentionAlertProcessor();
    }

    /**
     * <p>
     * Gets the instance of {@link MentionAlertProcessor}
     * </p>
     *
     * @return The {@link MentionAlertProcessor} instance
     */
    public static MentionAlertProcessor getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Processes the email alert
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param template Represents the template of the email alert
     * @param ids      Represents the collection of model IDs which triggered the email alert
     */
    @Override
    public void process(final long orgId, final long userId,
                        final EmailNotificationTemplate template, final Collection<Long> ids) {
        final Collection<MentionAlertRequest> requests;

        if (template.isNoteMentionTemplate()) {
            requests = getNoteEmailRequests(orgId, template, ids);
        } else if (template.isNoteCommentMentionTemplate()) {
            requests = getCommentEmailRequests(orgId, template, ids);
        } else {
            requests = Collections.emptyList();
        }

        if (!requests.isEmpty()) {
            emailAlertHandler.sendEmailAlerts(orgId, userId, requests,
                    null, Action.CREATE, template.getName());
            markMentionsAsNotified(orgId, template, requests);
        }
    }

    /**
     * <p>
     * Fetches the necessary data for note mentions and constructs the {@link MentionAlertRequest} for each unnotified mention.
     * </p>
     *
     * @param orgId         Represents the organization ID.
     * @param emailTemplate Represents the emailTemplate of the mention (note).
     * @param ids           Represents the collection of note IDs for which the mentions were made.
     * @return Collection containing constructed {@link MentionAlertRequest} for each unnotified mention.
     */
    private Collection<MentionAlertRequest> getNoteEmailRequests(final long orgId, final EmailNotificationTemplate emailTemplate,
                                                                 final Collection<Long> ids) {
        final Map<Long, Collection<Long>> unnotifiedMentionUserIds = noteManager.getUnnotifiedMentionedUserIds(orgId, ids);

        if (unnotifiedMentionUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Map<Long, Note> notes = noteManager.getNotes(orgId, ids);
        final Collection<Long> noteIds = notes.keySet();
        final boolean isPrivateNote = emailTemplate.isPrivateNoteMentionTemplate();
        final Map<Long, Entity> entities;
        final Map<Long, ActivityHolder> activityHolders;

        if (isPrivateNote) {
            entities = Collections.emptyMap();
            activityHolders = sourceActivityManager.getActivityHoldersByNoteIds(orgId, noteIds);
        } else {
            entities = loadSourceEntities(orgId, emailTemplate, sourceNoteManager.getEntities(orgId, noteIds));
            activityHolders = Collections.emptyMap();
        }
        final Collection<MentionAlertRequest> requests = new ArrayList<>();

        for (final Long noteId : ids) {
            final Note note = notes.get(noteId);

            if (Objects.isNull(note)) {
                continue;
            }
            final Collection<Long> mentionedUserIds = unnotifiedMentionUserIds.get(noteId);

            if (Objects.isNull(mentionedUserIds) || mentionedUserIds.isEmpty()) {
                continue;
            }
            messageTruncator.truncate(note);

            if (isPrivateNote) {
                loadMentionRequests(requests, note, mentionedUserIds, activityHolders.get(noteId));
            } else {
                loadMentionRequests(requests, note, emailTemplate, mentionedUserIds, entities.get(noteId));
            }
        }

        return requests;
    }

    /**
     * <p>
     * Fetches the necessary data for comment mentions and constructs the {@link MentionAlertRequest} for each unnotified mention.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param template Represents the template of the mention (comment).
     * @param ids      Represents the collection of comment IDs for which the mentions were made.
     * @return Collection containing constructed {@link MentionAlertRequest} for each unnotified mention.
     */
    private Collection<MentionAlertRequest> getCommentEmailRequests(final long orgId, final EmailNotificationTemplate template,
                                                                    final Collection<Long> ids) {
        final Map<Long, Collection<Long>> unnotifiedMentionUserIds = commentManager.getUnnotifiedMentionedUserIds(orgId, ids);

        if (unnotifiedMentionUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        final Map<Long, Comment> comments = commentManager.getComments(orgId, ids);
        final Map<Long, Long> noteIdsByCommentId = commentManager.getNoteIds(orgId, comments.keySet());
        final Collection<Long> noteIds = noteIdsByCommentId.values();
        final boolean isNoteIdsEmpty = noteIds.isEmpty();

        if (isNoteIdsEmpty) {
            return Collections.emptyList();
        }
        final Map<Long, Entity> entities = loadSourceEntities(orgId, template, sourceNoteManager.getEntities(orgId, noteIds));
        final Map<Long, String> noteContentByIds = noteManager.getNoteContent(orgId, noteIds);
        final Collection<MentionAlertRequest> requests = new ArrayList<>();

        for (final Long commentId : ids) {
            final Comment comment = comments.get(commentId);

            if (Objects.isNull(comment)) {
                continue;
            }
            final Collection<Long> mentionedUserIds = unnotifiedMentionUserIds.get(commentId);

            if (Objects.isNull(mentionedUserIds) || mentionedUserIds.isEmpty()) {
                continue;
            }
            final Long noteId = Objects.requireNonNullElse(comment.getNoteId(), noteIdsByCommentId.get(commentId));

            comment.setNoteId(noteId);
            comment.setContent(noteContentByIds.get(noteId));
            messageTruncator.truncate(comment);
            loadMentionRequests(requests, comment, template, mentionedUserIds, entities.get(noteId));
        }

        return requests;
    }

    /**
     * <p>
     * Constructs the {@link MentionAlertRequest} for template-based note mention alerts.
     * </p>
     *
     * @param template Represents the template entity type
     * @return Collection containing constructed {@link MentionAlertRequest}
     */
    private MentionAlertRequest constructMessageEmailRequest(final EmailNotificationTemplate template, final Message message,
                                                             final long mentionedUserId, final Entity entity) {
        final MentionAlertRequest mentionAlertRequest = buildRequestWithMentionUserId(message, mentionedUserId);

        switch (template) {
            case CONTACT_NOTE_MENTION,
                 CONTACT_NOTE_COMMENT_MENTION -> mentionAlertRequest.setContacts(List.of((Contact) entity));
            case DEAL_NOTE_MENTION,
                 DEAL_NOTE_COMMENT_MENTION -> mentionAlertRequest.setDeal((Deal) entity);
            case COMPANY_NOTE_MENTION,
                 COMPANY_NOTE_COMMENT_MENTION -> mentionAlertRequest.setCompany((Company) entity);
        }

        return mentionAlertRequest;
    }

    /**
     * <p>
     * Loads the {@link MentionAlertRequest} for mentions in activity private notes.
     * </p>
     *
     * @param requests         Collection to which the constructed {@link MentionAlertRequest} will be added
     * @param message          Represents the message details
     * @param mentionedUserIds Represents the collection of mentioned user IDs for the message
     * @param template         Represents the email alert template
     * @param sourceEntity     Represents the template entity associated with the note (contact, deal, or company) if applicable,
     *                         null for activity private notes
     */
    private void loadMentionRequests(final Collection<MentionAlertRequest> requests, final Message message,
                                     final EmailNotificationTemplate template, final Collection<Long> mentionedUserIds,
                                     final Entity sourceEntity) {
        if (Objects.isNull(sourceEntity)) {
            return;
        }
        mentionedUserIds.forEach(mentionedUserId -> {
            requests.add(constructMessageEmailRequest(template, message, mentionedUserId, sourceEntity));
        });
    }

    /**
     * <p>
     * Loads the {@link MentionAlertRequest} for mentions in activity private notes.
     * </p>
     *
     * @param requests         Collection to which the constructed {@link MentionAlertRequest} will be added
     * @param message          Represents the message details
     * @param mentionedUserIds Represents the collection of mentioned user IDs for the message
     * @param activityHolder   Represents the activity holder details associated with the private note
     */
    private void loadMentionRequests(final Collection<MentionAlertRequest> requests, final Message message,
                                     final Collection<Long> mentionedUserIds, final ActivityHolder activityHolder) {
        if (Objects.isNull(activityHolder)) {
            return;
        }
        mentionedUserIds.forEach(mentionedUserId -> {
                    final MentionAlertRequest request = buildRequestWithMentionUserId(message, mentionedUserId);

                    request.setActivityHolder(activityHolder);
                    requests.add(request);
                }
        );
    }

    /**
     * <p>
     * Helper method to construct a basic {@link MentionAlertRequest} with note and mentioned user details.
     * </p>
     *
     * @param message         Represents the message details
     * @param mentionedUserId Represents the mentioned user ID
     * @return Constructed {@link MentionAlertRequest} with note and mentioned user information
     */
    private MentionAlertRequest buildRequestWithMentionUserId(final Message message, final long mentionedUserId) {
        final MentionAlertRequest mentionAlertRequest = new MentionAlertRequest();

        mentionAlertRequest.setMessage(message);
        mentionAlertRequest.setMentionedUserId(mentionedUserId);

        return mentionAlertRequest;
    }

    /**
     * <p>
     * Loads the template entities (contact, deal, or company) for the given template and note IDs.
     * Fetches the entity IDs associated with the notes based on the template and then retrieves the corresponding entities.
     * Returns a map of note ID to the associated template entity.
     * </p>
     *
     * @param orgId                  Represents the organization ID.
     * @param template               Represents the template of the mention (note or comment).
     * @param sourceEntitiesByNoteId Represents the map of note ID to its associated template entities for different sources.
     * @return Map of note ID to the associated template entity based on the given template.
     */
    private Map<Long, Entity> loadSourceEntities(final long orgId, final EmailNotificationTemplate template,
                                                 final Map<Long, Map<NoteHolder.Source, Entity>> sourceEntitiesByNoteId) {
        final Map<Long, Long> entityIdByNoteId = new HashMap<>(sourceEntitiesByNoteId.size());

        sourceEntitiesByNoteId.forEach((noteId, entities) -> {
            final Entity sourceEntity = switch (template) {
                case CONTACT_NOTE_MENTION,
                     CONTACT_NOTE_COMMENT_MENTION -> entities.get(NoteHolder.Source.CONTACT);
                case DEAL_NOTE_MENTION,
                     DEAL_NOTE_COMMENT_MENTION -> entities.get(NoteHolder.Source.DEAL);
                case COMPANY_NOTE_MENTION,
                     COMPANY_NOTE_COMMENT_MENTION -> entities.get(NoteHolder.Source.COMPANY);
                default -> null;
            };

            if (Objects.nonNull(sourceEntity)) {
                entityIdByNoteId.put(noteId, sourceEntity.id());
            }
        });

        if (entityIdByNoteId.isEmpty()) {
            return Collections.emptyMap();
        }
        final Collection<Long> entityIds = new HashSet<>(entityIdByNoteId.values());
        final Map<Long, Entity> entitiesById = switch (template) {
            case CONTACT_NOTE_MENTION, CONTACT_NOTE_COMMENT_MENTION ->
                    contactManager.get(orgId, entityIds).stream().collect(
                            Collectors.toMap(Contact::id, contact -> contact)
                    );
            case DEAL_NOTE_MENTION, DEAL_NOTE_COMMENT_MENTION ->
                    dealManager.get(orgId, entityIds).stream().collect(
                            Collectors.toMap(Deal::id, deal -> deal)
                    );
            case COMPANY_NOTE_MENTION, COMPANY_NOTE_COMMENT_MENTION ->
                    companyManager.partialGet(orgId, entityIds).stream().collect(
                            Collectors.toMap(Company::id, company -> company)
                    );
            default -> Collections.emptyMap();
        };

        return entityIdByNoteId.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entitiesById.get(entry.getValue()))
        );
    }

    /**
     * <p>
     * Marks the mentions as notified for the given template and message email requests.
     * </p>
     *
     * @param orgId    Represents the organization ID.
     * @param template Represents the template of the mention (note or comment).
     * @param requests Represents the collection of {@link MentionAlertRequest}
     *                 for which the mentions need to be marked as notified.
     */
    private void markMentionsAsNotified(final long orgId, final EmailNotificationTemplate template,
                                        final Collection<MentionAlertRequest> requests) {
        final Map<Long, Collection<Long>> userIdsByMessageId = groupUserIdsByMessageId(requests);

        if (template.isNoteMentionTemplate()) {
            noteManager.markMentionedUsersAsNotified(orgId, userIdsByMessageId);
        } else if (template.isNoteCommentMentionTemplate()) {
            commentManager.markMentionedUsersAsNotified(orgId, userIdsByMessageId);
        }
    }

    /**
     * <p>
     * Groups the mentioned user IDs by their corresponding message IDs from the collection of {@link MentionAlertRequest}.
     * </p>
     *
     * @param requests Represents the collection of {@link MentionAlertRequest} containing message and mentioned user details.
     * @return A map where the key is the message ID and the value is a collection of mentioned user IDs associated with that message.
     */
    private Map<Long, Collection<Long>> groupUserIdsByMessageId(final Collection<MentionAlertRequest> requests) {
        final Map<Long, Collection<Long>> userIdsByMessageId = new HashMap<>(requests.size());

        for (final MentionAlertRequest request : requests) {
            userIdsByMessageId.computeIfAbsent(
                    request.id(), k -> new HashSet<>()).add(request.getMentionedUserId()
            );
        }
        return userIdsByMessageId;
    }
}