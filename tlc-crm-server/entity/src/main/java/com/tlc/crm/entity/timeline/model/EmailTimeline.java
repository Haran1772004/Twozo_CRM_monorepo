package com.tlc.crm.entity.timeline.model;

import com.tlc.email.common.model.type.Email.MessageCategory;

import java.util.List;

/**
 * <p>
 * Holds the email related properties for Timeline related actions.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public class EmailTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "EMAIL_TIMELINE";

    private Long id;
    private long orgId;
    private String subject;
    private List<String> toName;
    private String fromName;
    private String content;
    private long userId;
    private MessageCategory messageCategory;

    public long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public MessageCategory getMessageCategory() {
        return messageCategory;
    }

    public void setMessageCategory(final MessageCategory messageCategory) {
        this.messageCategory = messageCategory;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public List<String> getToName() {
        return toName;
    }

    public void setToName(final List<String> toName) {
        this.toName = toName;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(final String fromName) {
        this.fromName = fromName;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the EntityTimeline type
     */
    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
