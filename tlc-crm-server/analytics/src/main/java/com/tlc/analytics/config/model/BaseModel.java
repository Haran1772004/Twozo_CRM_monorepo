package com.tlc.analytics.config.model;

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
 * Base model implementation providing common attributes for entities.
 * {@link BaseModel} implements the {@link TlcModel} interface,
 * offering a structure for entities with shared attributes such as ID and organization ID.
 * This class includes setter methods for these attributes and implements the methods specified in the {@link TlcModel} interface.
 *
 * <p>
 * id: Represents the unique identifier of the entity.
 * orgId: Represents the organization ID associated with the entity.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder( builderMethodName = "baseModelBuilder" )
public class BaseModel implements TlcModel, Entity<Long>
{

    @JsonKey( key = BaseModelKeys.ID)
    @Column( name = BaseModelKeys.ID_COLUMN, isPrimaryKey = true )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.base.model.invalid.id" )
    private Long id;

    @JsonKey( key = BaseModelKeys.ORG_ID )
    @Setter
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.base.model.invalid.org.id" )
    private Long orgId;

    /**
     * Retrieves the ID of the entity.
     *
     * @return The ID as a Long.
     */
    @Override
    public Long id()
    {
        return id;
    }

    /**
     * Retrieves the organization ID of the entity.
     *
     * @return The organization ID as a Long.
     */
    @Override
    public Long orgId()
    {
        return orgId;
    }

    /**
     * Retrieves the identity object of the entity.
     *
     * @return The identity object.
     */
    @Override
    public Object identity()
    {
        return null;
    }
}
