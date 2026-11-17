package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.workflow.api.model.TriggerSource;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Trigger execution data for the workflow V2.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public class TriggerExecutionData {

    private final Long orgId;
    private final Collection<Pair<TlcModel, TlcModel>> models;
    private final TriggerSource source;
    private final ModelSource modelSource;
    private final boolean needNestedTrigger;
    private final Map<Long, User> groupedUsers;
    private final Consumer<Void> cleanUpConsumer;
    private final Map<Long, Long> entityStateIdMap;
    private final JsonObject nestedTriggerInfo;

    public TriggerExecutionData(final Long orgId, final Collection<Pair<TlcModel, TlcModel>> models,
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

    public Collection<Pair<TlcModel, TlcModel>> models() {
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
