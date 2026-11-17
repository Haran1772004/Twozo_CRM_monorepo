package com.tlc.crm.core.api.analytics.entity.data.underlying;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMREPORTFIELDCONFIG;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the field configuration details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = CRMREPORTFIELDCONFIG.TABLE )
public class CRMFieldConfig extends CRMBaseModel
{

    @Column( name = CRMREPORTFIELDCONFIG.VIEW_ACCESS_ID )
    private Long viewAccessId;

    @Column( name = CRMREPORTFIELDCONFIG.FIELD_ID )
    private Long fieldId;

    @Column( name = CRMREPORTFIELDCONFIG.MODULE_ID )
    private Integer moduleId;
}
