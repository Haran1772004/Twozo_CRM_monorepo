package com.tlc.crm.core.api.analytics.entity.config.fields;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANAFIELD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the analytics related field details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = MCANAFIELD.TABLE )
public class AnalyticsField extends CRMBaseModel
{
    @Column( name = MCANAFIELD.MODULE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.module.id" )
    private Integer moduleId;

    @Column( name = MCANAFIELD.NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.field.name" )
    @Size( max = 100, message = "i18n.crm.analytics.field.name.max.length" )
    private String name;

    @Column( name = MCANAFIELD.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.field.alias.name" )
    @Size( max = 100, message = "i18n.crm.analytics.field.alias.name.max.length" )
    private String aliasName;

    @Column( name = MCANAFIELD.FIELD_TYPE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.field.type" )
    private Long fieldTypeId;
}
