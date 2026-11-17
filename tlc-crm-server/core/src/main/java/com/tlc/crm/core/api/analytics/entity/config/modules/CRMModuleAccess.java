package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.crm.core.sql.resource.CRMMODULEACCESS;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm module access
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMMODULEACCESS.TABLE )
public class CRMModuleAccess extends CRMBaseModel
{
    @Column( name = CRMMODULEACCESS.MODULE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.modular.id" )
    private Integer moduleId;

    @Column( name = CRMMODULEACCESS.ACCESS_MODULE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.modular.id" )
    private Integer accessModuleId;
}
