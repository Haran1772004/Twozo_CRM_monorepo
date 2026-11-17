package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link ContactTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ContactTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "CONTACT_TIMELINE";

    private Long id;
    private String name;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
