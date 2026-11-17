package com.tlc.crm.core.features.timeline.model;

import com.tlc.crm.entity.timeline.model.EntityTimeline;
import com.tlc.crm.entity.timeline.model.EntityTimelineType;

/**
 * A model tha represents the call timeline
 *
 * @author kishore J
 * @version 1.0
 */
public class CallTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "CALL_TIMELINE";

    private Long timelineId;
    private String agenda;
    private String duration;

    public Long getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(final Long timelineId) {
        this.timelineId = timelineId;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(final String agenda) {
        this.agenda = agenda;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
