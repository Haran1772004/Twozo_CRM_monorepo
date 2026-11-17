package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANACOMPARATORFILTERVALUETYPE;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the comparator function details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name= MCANACOMPARATORFILTERVALUETYPE.TABLE )
public class ComparatorFilterType extends CRMBaseModel
{
    @Column( name = MCANACOMPARATORFILTERVALUETYPE.COMPARATOR_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.comparator" )
    private Long comparatorId;

    @Column( name = MCANACOMPARATORFILTERVALUETYPE.FILTER_VALUE_TYPE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.filter.value.type" )
    private Integer filterTypeId;

    public FilterValueTypes getFilterValueTypes()
    {
        return FilterValueTypes.getFilterValueTypes( filterTypeId );
    }
}
