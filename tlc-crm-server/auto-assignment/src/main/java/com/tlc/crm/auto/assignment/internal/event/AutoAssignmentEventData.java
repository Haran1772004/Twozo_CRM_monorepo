package com.tlc.crm.auto.assignment.internal.event;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents the consolidated data required for processing an auto-assignment event.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentEventData {

    private final Long orgId;
    private final Long userId;
    private final Collection<TlcModel> models;
    private final Consumer<Void> cleanupConsumer;

    private Map<Long, User> userMap;

    public AutoAssignmentEventData(final Long orgId, final Long userId, final Collection<TlcModel> models,
                                   final Consumer<Void> cleanupConsumer) {
        this.orgId = orgId;
        this.userId = userId;
        this.models = models;
        this.cleanupConsumer = cleanupConsumer;
    }

    public void setUserMap(final Map<Long, User> userMap) {
        this.userMap = userMap;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public Collection<TlcModel> models() {
        return models;
    }

    public Map<Long, User> getUserMap() {
        return userMap;
    }

    public void clean() {
        Objects.requireNonNull(cleanupConsumer, "Auto-assignment execution request cleanup activity should not be null").accept(null);
    }
}
