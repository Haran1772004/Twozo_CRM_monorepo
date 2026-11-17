package com.tlc.crm.entity.fieldaudit.models;


import com.tlc.validator.TlcModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Per (org, source) tracking configuration.
 *
 * <p>Persisted in {@code OMFATrackingConfig}. The single boolean
 * {@link #isTrackingEnabled} is the master gate: when {@code false} the
 * {@code FieldAuditEntryHelper} is a complete no-op for that source. When
 * {@code true}, the {@link #trackedFields} set narrows what is tracked; when the set
 * is <em>empty</em>, <strong>all editable fields</strong> for the source are
 * tracked.</p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class FieldTrackingConfig implements TlcModel {

    public static final int MAX_TRACKED_FIELDS_PER_MODULE = 50;

    private Long id;
    private final Long orgId;
    private final short sourceId;
    private final boolean isTrackingEnabled;
    private final Set<String> trackedFields;
    private final long updatedAt;
    private final Long updatedBy;

    public FieldTrackingConfig(final Long id, final Long orgId, final short sourceId,
            final Set<String> trackedFields, final long updatedAt, final Long updatedBy) {
        this.id = id;
        this.orgId = orgId;
        this.sourceId = sourceId;
        this.trackedFields = Objects.isNull(trackedFields) ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(trackedFields));
        this.isTrackingEnabled = !trackedFields.isEmpty();
        this.updatedAt = updatedAt == 0L ? System.currentTimeMillis() : updatedAt;
        this.updatedBy = updatedBy;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public short sourceId() {
        return sourceId;
    }

    public boolean isTrackingEnabled() {
        return isTrackingEnabled;
    }

    public Set<String> trackedFields() {
        return trackedFields;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public Long updatedBy() {
        return updatedBy;
    }

    /**
     * Returns whether the supplied field is currently tracked.
     *
     * <p>Behaviour:</p>
     * <ul>
     *   <li>{@code isTrackingEnabled = false}  &rarr; {@code false} (master gate off).</li>
     *   <li>{@code trackedFields} is empty     &rarr; {@code true} (track every editable field).</li>
     *   <li>otherwise                          &rarr; whether the field is in the set.</li>
     * </ul>
     */
    public boolean isFieldTracked(final String fieldName) {
        if (!isTrackingEnabled) {
            return false;
        }
        return trackedFields.isEmpty() || trackedFields.contains(fieldName);
    }

    /**
     * "Tracking disabled" default configuration; persisted only when the admin
     * explicitly enables tracking.
     */
    public static FieldTrackingConfig getDefaultConfig(final Long orgId, final short sourceId) {
        return new FieldTrackingConfig(null, orgId, sourceId, Collections.emptySet(), System.currentTimeMillis(), null);
    }
}