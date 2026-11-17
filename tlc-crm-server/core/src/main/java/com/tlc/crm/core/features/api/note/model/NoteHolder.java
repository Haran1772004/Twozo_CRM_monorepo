package com.tlc.crm.core.features.api.note.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.internal.status.NoteErrorCodes;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * A Model that represents the {@link NoteHolder} which holds the note details
 *
 * @author kowsalya SP
 * @version 1.0
 */
@Searchable(index = SearchIndex.NOTE)
@Notifiable(types = {AppNotificationEventType.CONTACT_NOTE_MENTIONED, AppNotificationEventType.COMPANY_NOTE_MENTIONED, AppNotificationEventType.DEAL_NOTE_MENTIONED}, modelName = CrmModelType.NOTE)
public class NoteHolder implements CrmModel {

    /**
     * <p>
     * Represents the source names
     * </p>
     */
    public enum Source {

        DEAL,
        CONTACT,
        COMPANY,
        ;

        /**
         * <p>
         * Gets the {@link Source} by type
         * </p>
         *
         * @param source Represents the source name
         * @return {@link Source}
         */
        public static Source getType(final String source) {
            return switch (source.toLowerCase()) {
                case "deal" -> DEAL;
                case "contact" -> CONTACT;
                case "company" -> COMPANY;
                default -> throw ErrorCode.get(NoteErrorCodes.SOURCE_NOT_FOUND);
            };
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    @SearchField(name = "note")
    @Valid
    private Note note;

    @SearchField(name = "entityId")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.note.invalid.entityId")
    private Long entityId;

    @SearchField(name = "entitySource", readMethodName = "getSource")
    private Source source;

    @SearchField(name = "relatedEntityIds")
    private Map<Source, Long> relatedSourceIds;

    private Long orgId;

    public NoteHolder() {

    }

    public NoteHolder(final Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(final Note note) {
        this.note = note;
    }

    @Override
    public Long id() {
        return note.id();
    }

    @Override
    public Long orgId() {
        return Objects.isNull(orgId) ? note.orgId() : orgId;
    }

    /**
     * <p>
     * Gets the collection of user ids
     * </p>
     *
     * @return The collection of user ids
     */
    @Override
    public Collection<Long> userIds() {
        final CrmUser createdBy = note.getCreatedBy();
        final CrmUser updatedBy = note.getUpdatedBy();
        final Collection<Long> userIds = new HashSet<>();

        if (Objects.nonNull(createdBy)) {
            userIds.add(createdBy.id());
        }

        if (Objects.nonNull(updatedBy)) {
            userIds.add(updatedBy.id());
        }
        userIds.addAll(note.getMentionedUsers().stream().map(CrmUser::id).toList());
        return userIds;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Map<Source, Long> getRelatedSourceIds() {
        return relatedSourceIds;
    }

    public void setRelatedSourceIds(final Map<Source, Long> relatedSourceIds) {
        this.relatedSourceIds = relatedSourceIds;
    }

    public void addRelatedSourceId(final Source source, final Long id) {
        this.relatedSourceIds = Objects.isNull(this.relatedSourceIds) ? new HashMap<>() : this.relatedSourceIds;

        this.relatedSourceIds.put(source, id);
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }
}