package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link NotesTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class NotesTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "NOTES_TIMELINE";

    private String notes;

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
