package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMSUBMODULEMAPPER;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.i18n.I18nKey;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manager modules with corresponding submodules
 * </p>
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMSUBMODULEMAPPER.TABLE )
public class CRMSubModuleMapper extends CRMBaseModel
{

    @Column( name = CRMSUBMODULEMAPPER.ALIAS_NAME )
    private String aliasName;

    @Column( name = CRMSUBMODULEMAPPER.DISPLAY_NAME )
    private I18nKey displayName;

    @Column( name = CRMSUBMODULEMAPPER.MODULE_ID )
    private Integer moduleId;

    @Column( name = CRMSUBMODULEMAPPER.SUB_MODULE_ID )
    private Integer subModuleId;
}
