package com.tlc.crm.common.list.model;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.Locale;

/**
 * <p>
 * A model which represents the {@link ListRequestData}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ListRequestData {

    private long orgId;
    private Long userId;
    private I18nResolver i18nResolver;
    private I18nResolutionAdapter i18NResolutionAdapter;
    private JsonObject requestJson;
    private TimezoneInfo timezoneInfo;
    private CrmRequest crmRequest;

    public void setI18nPlaceholderResolver(final I18nResolutionAdapter i18NResolutionAdapter) {
        this.i18NResolutionAdapter = i18NResolutionAdapter;
    }

    public void setI18nResolver(final I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    public void setRequestJson(final JsonObject requestJson) {
        this.requestJson = requestJson;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public I18nResolutionAdapter getI18nPlaceholderResolver() {
        return i18NResolutionAdapter;
    }

    public I18nResolver getI18nResolver() {
        return i18nResolver;
    }

    public JsonObject getRequestJson() {
        return requestJson;
    }

    public Long getUserId() {
        return userId;
    }

    public long getOrgId() {
        return orgId;
    }

    public TimezoneInfo getTimezoneInfo() {
        return timezoneInfo;
    }

    public void setTimezoneInfo(final TimezoneInfo timezoneInfo) {
        this.timezoneInfo = timezoneInfo;
    }

    public void setCrmRequest(final CrmRequest crmRequest) {
        this.crmRequest = crmRequest;
    }

    /**
     * Gets the parameter value from the request
     *
     * @param name Name of the query parameter
     * @return Value of query parameter
     */
    public String getParameter(final String name) {
        return crmRequest.exchange().param(name).findAny().orElse(null);
    }

    public Locale getLocale() {
       return crmRequest.locale();
    }
}
