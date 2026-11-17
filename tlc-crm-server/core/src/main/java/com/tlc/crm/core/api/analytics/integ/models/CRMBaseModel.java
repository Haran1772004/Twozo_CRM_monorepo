package com.tlc.crm.core.api.analytics.integ.models;

import com.tlc.analytics.config.keys.BaseModelKeys;

import com.tlc.entity.Entity;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * An entity that holds the {@link CRMBaseModel} details
 *
 * @author jamesh
 * @version 1.0
 */
@SuperBuilder
public class CRMBaseModel implements TlcModel, Entity<Long>
{
    @JsonKey( key = BaseModelKeys.ID)
    @Column( name = BaseModelKeys.ID_COLUMN, isPrimaryKey = true )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
            message = "i18n.analytics.base.model.invalid.id" )
    private Long id;

    @JsonKey( key = BaseModelKeys.ORG_ID )
    @Setter
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class }, message = "i18n.analytics.base.model.invalid.org.id" )
    private Long orgId;

    /**
     * Returns primary identifier of this model.
     *
     * @return The entity id.
     */
    @Override
    public Long id()
    {
        return id;
    }

    /**
     * Returns organisation identifier associated with this model.
     *
     * @return The organization id.
     */
    @Override
    public Long orgId()
    {
        return orgId;
    }

    /**
     * Returns identity object used by framework identity checks.
     *
     * @return Identity object, currently not implemented.
     */
    @Override
    public Object identity() {
        return null;
    }
}
