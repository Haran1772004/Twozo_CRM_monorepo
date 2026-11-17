package com.tlc.crm.workflow.internal.trigger.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.validator.TlcModel;
import com.tlc.iam.common.models.User;

import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.workflow.api.model.TriggerSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Holds the data required for the trigger event.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TriggerContext<T extends TlcModel> {

    private final Long orgId;
    private final Collection<Pair<T, T>> models;
    private final TriggerSource source;
    private final ModelSource modelSource;
    private final boolean needNestedTrigger;
    private final Map<Long, User> groupedUsers;
    private final Consumer<Void> cleanUpConsumer;
    private final Map<Long, Long> entityStateIdMap;
    private final JsonObject nestedTriggerInfo;

    public TriggerContext(final Long orgId, final Collection<Pair<T, T>> models,
                          final TriggerSource source, final ModelSource modelSource,
                          final boolean needNestedTrigger, final Consumer<Void> cleanUpConsumer,
                          final Map<Long, Long> entityStateIdMap, final JsonObject nestedTriggerInfo) {
        this.orgId = orgId;
        this.models = models;
        this.source = source;
        this.modelSource = modelSource;
        this.needNestedTrigger = needNestedTrigger;
        this.groupedUsers = new HashMap<>();
        this.cleanUpConsumer = cleanUpConsumer;
        this.entityStateIdMap = entityStateIdMap;
        this.nestedTriggerInfo = nestedTriggerInfo;
    }

    public Long orgId() {
        return orgId;
    }

    public Collection<Pair<T, T>> models() {
        return models;
    }

    public TriggerSource source() {
        return source;
    }

    public ModelSource modelSource() {
        return modelSource;
    }

    public boolean needNestedTrigger() {
        return needNestedTrigger;
    }

    public Map<Long, User> groupedUsers() {
        return groupedUsers;
    }

    public Map<Long, Long> getEntityStateIdMap() {
        return entityStateIdMap;
    }

    public JsonObject getNestedTriggerInfo() {
        return nestedTriggerInfo;
    }

    /**
     * Adds the grouped user map with all the provided users.
     *
     * @param users Refers the users to be added.
     */
    public void addUsers(final Map<Long, User> users) {
        this.groupedUsers.putAll(users);
    }

    public void clean() {
        Objects.requireNonNull(cleanUpConsumer, "Workflow trigger request cleanup activity should not be null").accept(null);
    }
}