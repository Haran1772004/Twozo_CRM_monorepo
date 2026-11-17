package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link AttachmentTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class AttachmentTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "ATTACHMENT_TIMELINE";

    private Long id;
    private String name;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}