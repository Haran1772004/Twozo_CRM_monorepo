package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMMODULE;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm module
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMMODULE.TABLE )
public class CRMModule extends CRMBaseModel
{
    @Column( name = CRMMODULE.MODULE_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.module.name" )
    @Size( max = 50, message = "i18n.crm.analytics.module.name.max.length" )
    private String moduleName;

    @Column( name = CRMMODULE.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.module.alias.name" )
    @Size( max = 50, message = "i18n.crm.analytics.module.alias.name.max.length" )
    private String aliasName;

    @Column( name = CRMMODULE.DISPLAY_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.module.display.name" )
    @Size( max = 255, message = "i18n.crm.analytics.module.display.name.max.length" )
    private String displayName;

    @Column( name = CRMMODULE.IS_ENABLED )
    private Boolean isEnabled;
}
