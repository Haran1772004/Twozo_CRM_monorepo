package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link DealTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class DealTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "DEAL_TIMELINE";

    private Long id;
    private String title;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
