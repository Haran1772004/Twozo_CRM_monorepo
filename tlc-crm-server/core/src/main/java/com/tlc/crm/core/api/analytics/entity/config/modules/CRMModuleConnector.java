package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMMODULECONNECTOR;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the crm module column mapping
 * </p>
 *
 * @author karthick
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMMODULECONNECTOR.TABLE )
public class CRMModuleConnector extends CRMBaseModel
{
    @Column( name = CRMMODULECONNECTOR.METRICS_MODULE_ID )
    private Integer metricsModuleId;

    @Column( name = CRMMODULECONNECTOR.VIEW_BY_MODULE_ID )
    private Integer viewByModuleId;

    @Column( name = CRMMODULECONNECTOR.COLUMN_ID )
    private Long columnId;
}
