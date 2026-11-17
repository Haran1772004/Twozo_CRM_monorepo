package com.tlc.crm.entity.models;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.AccessControlledModel;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.handler.SourceAccessControlledModelHandler;
import com.tlc.crm.entity.models.app.CrmAppNotifiableModel;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A model that holds the basic information about the crm entity
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class Entity implements CrmModel, CrmAppNotifiableModel, EmailNotifiableModel, AccessControlledModel {

    private static final Map<Source, String> URLS = new EnumMap<>(Source.class);

    @NotNull(groups = {Create.class, Update.class, Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;
    @NotEmpty(groups = {Create.class, Update.class}, message = "i18n.entity.fields.entity.invalid.type")
    private final String entityType;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private DateTime lastAssignedAt;
    @Valid
    private ValueHolder valueHolder;
    private transient ValueHolder existingValueHolder;
    private Collection<Long> userIds;
    private State state;
    private Boolean isEditable;
    private Boolean isDeletable;
    @SearchField(name = "ownerId")
    private Long ownerId;

    @NotNull(groups = {Create.class}, message = "i18n.contact.createdBy.required")
    private CrmUser createdBy;
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.contact.updatedBy.required")
    private CrmUser updatedBy;
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.contact.salesOwner.notFound")
    private CrmUser salesOwner;
    private transient FieldSource fieldSource;
    private transient FieldAuditInfo fieldAuditInfo;
    private boolean isNewEntity;

    public Entity(final String entityType) {
        this.entityType = entityType;
    }

    public abstract void setId(Long id);

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getEntityType() {
        return entityType;
    }

    public FieldSource getEntitySource() {
        return Objects.isNull(fieldSource) ? FieldSourceService.getInstance().getSource(this.entityType) : fieldSource;
    }

    public void setEntitySource(final FieldSource fieldSource) {
        this.fieldSource = fieldSource;
    }

    /**
     * Returns the field edit context for this entity. Falls back to user/manual if not set.
     */
    public FieldAuditInfo getFieldAuditInfo() {
        return Objects.isNull(fieldAuditInfo) ? FieldAuditInfo.defaultContext() : fieldAuditInfo;
    }

    /**
     * Sets the field edit context for this entity. Should be set during updates to capture the edit source and user.
     *
     * @param fieldAuditInfo the context of the field edit, including source and user information
     */
    public void setFieldAuditInfo(final FieldAuditInfo fieldAuditInfo) {
        this.fieldAuditInfo = fieldAuditInfo;
    }

    public ValueHolder getValueHolder() {
        return valueHolder;
    }

    public void setValueHolder(final ValueHolder valueHolder) {
        this.valueHolder = valueHolder;
    }

    /**
     * Returns the previous value holder snapshot captured before the current update.
     * Used for field edit history comparison of custom/valueHolder-based fields.
     */
    public ValueHolder getExistingValueHolder() {
        return existingValueHolder;
    }

    /**
     * Sets the existing value holder snapshot for this entity. Should be called before any updates to the current value holder.
     *
     * @param previousValueHolder the value holder snapshot captured before the current update
     */
    public void setExistingValueHolder(final ValueHolder previousValueHolder) {
        this.existingValueHolder = previousValueHolder;
    }

    public DateTime getLastAssignedAt() {
        return lastAssignedAt;
    }

    public void setLastAssignedAt(final DateTime lastAssignedAt) {
        this.lastAssignedAt = lastAssignedAt;
    }

    @Override
    public Collection<Long> userIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public CrmUser getSalesOwner() {
        return salesOwner;
    }

    public void setSalesOwner(final CrmUser salesOwner) {
        this.ownerId = salesOwner.id();
        this.salesOwner = salesOwner;
    }

    public boolean hasSalesOwner() {
        return Objects.nonNull(salesOwner);
    }
    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setEntityState(final State state) {
        this.state = state;
    }

    public State getEntityState() {
        return state;
    }

    public static void setURLS(final String domain, final Configurations configurations) {
        URLS.put(Source.CONTACT, domain + configurations.getConfig(Source.CONTACT.getName().toLowerCase()));
        URLS.put(Source.COMPANY, domain + configurations.getConfig(Source.COMPANY.getName().toLowerCase()));
        URLS.put(Source.DEAL, domain + configurations.getConfig(Source.DEAL.getName().toLowerCase()));
        URLS.put(Source.PRODUCT, domain + configurations.getConfig(Source.PRODUCT.getName().toLowerCase()));
    }

    public String getURL(final String source) {
        return URLS.get(Source.get(source));
    }

    public enum State {

        NEW, EXISTING
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can be edited, false otherwise
     */
    public boolean isEditable() {

        if (Objects.isNull(isEditable)) {
            this.isEditable = SourceAccessControlledModelHandler.getInstance().isEditable(this.orgId(), this.id());

            return isEditable;
        }
        return isEditable;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can be deleted, false otherwise
     */
    public boolean isDeletable() {

        if (Objects.isNull(isDeletable)) {
            this.isDeletable = SourceAccessControlledModelHandler.getInstance().isDeletable(this.orgId(), this.id());

            return isDeletable;
        }
        return isDeletable;
    }

    /**
     * <p>
     * Checks whether this entity is newly created
     * </p>
     *
     * @return true if the entity is new, false otherwise
     */
    public boolean isNewEntity() {
        return isNewEntity;
    }

    /**
     * <p>
     * Sets whether this entity is newly created
     * </p>
     *
     * @param isNewEntity the new entity status
     */
    public void setNewEntity(final boolean isNewEntity) {
        this.isNewEntity = isNewEntity;
    }

    /**
     * <p>
     * Gets the name of the entity
     * </p>
     *
     * @return The name of the entity
     */
    public abstract String getName();
}
