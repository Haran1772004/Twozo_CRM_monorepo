package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link ActivityTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ActivityTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "ACTIVITY_TIMELINE";

    private Long id;
    private String name;
    private Long IconId;
    private String activityNote;
    private boolean isDoneActivity;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setIconId(final Long iconId) {
        IconId = iconId;
    }

    public void setActivityNote(final String activityNote) {
        this.activityNote = activityNote;
    }

    public void setDoneActivity(final boolean doneActivity) {
        isDoneActivity = doneActivity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getIconId() {
        return IconId;
    }

    public boolean isDoneActivity() {
        return isDoneActivity;
    }

    public String getActivityNote() {
        return activityNote;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
