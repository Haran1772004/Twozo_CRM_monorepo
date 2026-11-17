package com.tlc.crm.entity.timeline.model;

/**
 * <p>
 * A model which represents the {@link InvoiceTimeline}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceTimeline implements EntityTimeline {

    public static final EntityTimelineType TYPE = () -> "INVOICE_TIMELINE";

    private Long id;
    private String number;

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public EntityTimelineType getType() {
        return TYPE;
    }
}