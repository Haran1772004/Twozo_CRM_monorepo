package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANACOMPARATORFUNCTION;

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
@Table( name= MCANACOMPARATORFUNCTION.TABLE )
public class ComparatorFunction extends CRMBaseModel
{
    @Column( name = MCANACOMPARATORFUNCTION.COMPARATOR_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.comparator" )
    private Long comparatorId;

    @Column( name = MCANACOMPARATORFUNCTION.FUNCTION_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.function" )
    private Integer functionId;
}
