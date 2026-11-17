package com.tlc.crm.zapier.internal.event;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.zapier.api.models.ZapEvent;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the {@code ZapEventData} attributes
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
@Setter
public final class ZapEventData {

    private final Long orgId;
    private final Long userId;
    private final Collection<TlcModel> models;

    private Map<Long, User> groupedUsers;
    private Action action;
    private Locale locale;
    private Source source;
    private ZapEvent zapEvent;
    private Collection<JsonObject> eventPayloads;

    public ZapEventData(final Long orgId, final Long userId, final Collection<TlcModel> models) {
        this.orgId = orgId;
        this.userId = userId;
        this.models = models;
    }

    public <T> Collection<T> getModels() {
        return (Collection<T>) models;
    }

}
