package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.crm.core.sql.resource.CRMREPORTFIELDMAPPERCOLUMN;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Manages CRM Filed and Analytics Column mapper
 * </p>
 *
 * @author James
 * @version 1.0
 */

@Getter
@SuperBuilder
@Table( name = CRMREPORTFIELDMAPPERCOLUMN.TABLE )
public class CRMRMapperColumn extends CRMBaseModel
{
    @Column( name = CRMREPORTFIELDMAPPERCOLUMN.ANA_MAPPER_COLUMN )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.crm.analytics.invalid.mapper.column" )
    private Long anaMapperColumn;

    @Column( name = CRMREPORTFIELDMAPPERCOLUMN.IS_FILTER_FILED )
    private boolean isFilterColumn;
}
