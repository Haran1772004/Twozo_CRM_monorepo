package com.tlc.crm.entity.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmRequest.ClientType;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides the request for the dropdown.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DropdownRequest {

    private final Long orgId;
    private final Long userId;
    private final JsonObject data;
    private final Locale locale;
    private final I18nResolver i18nResolver;
    private final I18nResolutionAdapter i18NResolutionAdapter;
    private ClientType clientType;
    private DropdownCriteria criteria;
    private DropdownLimit limit;
    private FieldInfo field;
    private String order;
    private final TimezoneInfo timezoneInfo;

    public DropdownRequest(final CrmRequest crmRequest) {
        orgId = crmRequest.orgId();
        userId = crmRequest.userId();
        data = Optional.ofNullable(crmRequest.getRequestJson().optJsonObject(FieldKeys.DATA.getName())).orElseGet(Json::object);
        locale = crmRequest.locale();
        i18nResolver = crmRequest.i18nResolver();
        i18NResolutionAdapter = crmRequest.getI18NCRMResolutionAdapter();
        clientType = crmRequest.getClientType();
        timezoneInfo = crmRequest.timeZone();
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public final JsonObject data() {
        return data;
    }

    public Locale locale() {
        return locale;
    }

    public I18nResolver i18nResolver() {
        return i18nResolver;
    }

    public DropdownCriteria getCriteria() {
        return criteria;
    }

    public void setCriteria(final DropdownCriteria criteria) {
        this.criteria = criteria;
    }

    public DropdownLimit getLimit() {
        return limit;
    }

    public void setLimit(final DropdownLimit limit) {
        this.limit = limit;
    }

    public FieldInfo getField() {
        return field;
    }

    public void setField(final FieldInfo field) {
        this.field = field;
    }

    public String getOrder() {
        return Objects.isNull(order) ? data.optString(FieldKeys.ORDER.getName(), FieldKeys.ASC.getName()) : order;
    }

    public void setClientType(final ClientType clientType) {
        this.clientType = clientType;
    }

    /**
     * <p>
     * Gets the client type of the request
     * </p>
     *
     * @return {@link ClientType}
     */
    public ClientType getClientType() {
        return clientType;
    }

    /**
     * <p>
     * Gets The {@link I18nResolutionAdapter}
     * </p>
     *
     * @return The {@link I18nResolutionAdapter}
     */
    public I18nResolutionAdapter getI18NResolutionAdapter() {
        return i18NResolutionAdapter;
    }

    public TimezoneInfo getTimezoneInfo() {
        return timezoneInfo;
    }
}
