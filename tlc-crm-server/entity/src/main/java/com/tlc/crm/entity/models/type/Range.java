package com.tlc.crm.entity.models.type;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.temporal.model.Temporal;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents date and time related range data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public abstract class Range<T extends Temporal> implements TlcModel {

    @NotNull(groups = Group.Delete.class, message = "i18n.entity.field.type.rangeId.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.storeType.required")
    private final StoreType storeType;

    protected Range(final StoreType storeType) {
        this.storeType = storeType;
    }

    @NotNull(message = "i18n.entity.field.range.from.invalid", groups = {Group.Create.class, Group.Update.class})
    private T from;

    @NotNull(message = "i18n.entity.field.range.to.invalid", groups = {Group.Create.class, Group.Update.class})
    private T to;

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public T getFrom() {
        return from;
    }

    public void setFrom(T from) {
        this.from = from;
    }

    public T getTo() {
        return to;
    }

    public void setTo(T to) {
        this.to = to;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    @Override
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

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(storeType, from, to, orgId);
    }

    /**
     *{@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Range) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

}
