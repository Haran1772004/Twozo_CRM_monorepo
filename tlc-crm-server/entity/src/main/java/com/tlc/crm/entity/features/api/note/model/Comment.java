package com.tlc.crm.entity.features.api.note.model;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Model which represents the comment of the {@link Note}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Notifiable(types = {AppNotificationEventType.CONTACT_NOTE_COMMENT_MENTIONED, AppNotificationEventType.COMPANY_NOTE_COMMENT_MENTIONED, AppNotificationEventType.DEAL_NOTE_COMMENT_MENTIONED}, modelName = CrmModelType.COMMENT)
public class Comment extends Message {

    @NotNull(groups = {Group.Create.class}, message = "i18n.note.invalid.id")
    private Long noteId;

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(final Long noteId) {
        this.noteId = noteId;
    }
}