package com.tlc.iam.common.models;

import com.tlc.commons.context.Session;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.models.converter.SessionConverter;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

/**
 * Holds the session information of the logged-in user
 *
 * @author Selvakumar
 * @version 1.0
 */
public class UserSession implements Session, TlcModel {

    private long id;
    private String browser;
    private String device;
    private String os;
    private DateTime createdTime;
    private Long orgId;
    private final Long userId;
    private String token;
    private User user;

    public UserSession(final Long orgId, final long userId) {
        this.device = null;
        this.userId = userId;
        this.orgId = orgId;
    }

    public UserSession(final User user) {
        this.userId = user.id();
        this.orgId = user.orgId();
        this.user = user;
    }

    public UserSession(final User user, final JsonObject userAgent) {
        this.userId = user.id();
        this.orgId = user.orgId();
        this.user = user;
        this.device = userAgent.getString("device");
        this.browser = userAgent.getString("browser");
        this.os = userAgent.getString("os");
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public JsonObject getData() {
        return SessionConverter.getInstance().convert(this);
    }

    @Override
    public String sessionId() {
        return String.valueOf(id);
    }

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

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
