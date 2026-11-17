package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link CompanyTimeline}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class CompanyTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "COMPANY_TIMELINE";

    private String name;

    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}
