package com.tlc.crm.workflow.v2.api.model;

import jakarta.validation.Valid;

/**
 * Delay node configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class DelayConfiguration implements NodeConfiguration {

    @Valid
    private SpanConfiguration span;

    public SpanConfiguration getSpan() {
        return span;
    }

    public void setSpan(final SpanConfiguration span) {
        this.span = span;
    }
}
