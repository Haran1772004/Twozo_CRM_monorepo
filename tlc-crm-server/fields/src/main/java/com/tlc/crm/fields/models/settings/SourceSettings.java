package com.tlc.crm.fields.models.settings;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * <p>
 * A model that holds the information of source settings
 * </p>
 *
 * @author PandiarajkumarG
 */
@Notifiable(types = AppNotificationEventType.PRODUCTS_MODULE_ENABLED, modelName = CrmModelType.SETTINGS)
public class SourceSettings implements TlcModel, Serializable {

    private Long id;

    @NotNull(groups = {Group.Update.class}, message = "i18n.invalid.orgId")
    private Long orgId;

    @Size(max = 50, groups = {Group.Update.class}, message = "i18n.account.source.name.length.exceeded")
    private String alternateName;

    private Long sourceId;

    private boolean isEnabled;

    private String config;

    private long userId;

    public SourceSettings() {
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getAlternateName() {
        return alternateName;
    }

    public void setAlternateName(final String alternateName) {
        this.alternateName = alternateName;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean enabled) {
        isEnabled = enabled;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = config;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
