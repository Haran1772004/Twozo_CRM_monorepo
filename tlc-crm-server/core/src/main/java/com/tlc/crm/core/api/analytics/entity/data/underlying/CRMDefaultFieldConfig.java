package com.tlc.crm.core.api.analytics.entity.data.underlying;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMREPORTDEFAULTFIELDCONFIG;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the default field configuration details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = CRMREPORTDEFAULTFIELDCONFIG.TABLE )
public class CRMDefaultFieldConfig extends CRMBaseModel
{
    @Column( name = CRMREPORTDEFAULTFIELDCONFIG.FIELD_ID )
    private Long fieldId;

    @Column( name = CRMREPORTDEFAULTFIELDCONFIG.MODULE_ID )
    private Integer moduleId;
}
