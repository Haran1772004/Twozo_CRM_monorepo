package com.tlc.plans.and.billings.models;

import com.tlc.i18n.I18nKey;

import java.util.Objects;

/**
 * Represents a cancellation reason entity with id and description properties.
 *
 * @author Sathish
 * @version 1.0
 */
public class CancellationReason implements Comparable<CancellationReason> {

    private Long id;
    private I18nKey reason;
    private Short order;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public I18nKey getReason() {
        return reason;
    }

    public void setReason(final I18nKey reason) {
        this.reason = reason;
    }

    public Short getOrder() {
        return order;
    }

    public void setOrder(final Short order) {
        this.order = order;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final CancellationReason cancellationReason = (CancellationReason) object;

        return Objects.equals(id, cancellationReason.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final CancellationReason cancellationReason) {
        return Short.compare(order, cancellationReason.order);
    }
}
