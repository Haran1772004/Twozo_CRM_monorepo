package com.tlc.crm.core.api.analytics.entity.config.function;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMFIELDPERIODICFILTER;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the periodic filter function related fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = CRMFIELDPERIODICFILTER.TABLE )
public class FieldPeriodicFilter extends CRMBaseModel
{
    @Column( name = CRMFIELDPERIODICFILTER.CRM_VIEW_FIELD_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.field" )
    private Long viewFieldId;

    @Column( name = CRMFIELDPERIODICFILTER.FUNCTION_ID )
    private Integer functionId;
}
