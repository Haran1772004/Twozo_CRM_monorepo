package com.tlc.crm.common.event.model;

import com.tlc.crm.common.models.Action;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Entity holding relevant information for an event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class EventData {

    protected long orgId;
    protected long userId;

    private Map<Long, TlcModel> previousValues;
    private Collection<? extends TlcModel> values;
    private Action action;
    private String apiType;
    private Locale locale;

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public long getOrgId() {
        return orgId;
    }

    public long getUserId() {
        return userId;
    }

    public Map<Long, TlcModel> getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(final Collection<? extends TlcModel> previousValues) {
        this.previousValues = Objects.isNull(this.previousValues) ? new HashMap<>() : this.previousValues;

        previousValues.forEach(oldValue -> this.previousValues.put(oldValue.id(), oldValue));
    }

    public Collection<TlcModel> getValues() {
        return Objects.isNull(values) ? Collections.emptyList() : Collections.unmodifiableCollection(values);
    }

    public void setValues(final Collection<? extends TlcModel> values) {
        this.values = values;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(final String apiType) {
        this.apiType = apiType;
    }

    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }
}