package com.tlc.crm.core.api.analytics.entity.config.fields.bucket;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMFIELDBUCKETUNIT;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Manages CRM BucketBY
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMFIELDBUCKETUNIT.TABLE )
public class FieldBucketUnit extends CRMBaseModel
{
    @Column( name = CRMFIELDBUCKETUNIT.CRM_VIEW_FIELD_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.field" )
    private Long viewFieldId;

    @Column( name = CRMFIELDBUCKETUNIT.UNIT_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.unit" )
    private Integer unitId;
}
