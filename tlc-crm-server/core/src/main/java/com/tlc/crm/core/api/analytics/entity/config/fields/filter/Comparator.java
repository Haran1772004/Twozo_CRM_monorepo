package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANACOMPARATOR;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the comparator details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name= MCANACOMPARATOR.TABLE )
public class Comparator extends CRMBaseModel
{
    @Column( name = MCANACOMPARATOR.NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.comparator.name" )
    @Size( max = 100, message = "i18n.crm.analytics.comparator.name.max.length" )
    private String name;

    @Column( name = MCANACOMPARATOR.ALIAS_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.comparator.alias.name" )
    @Size( max = 100, message = "i18n.crm.analytics.comparator.alias.name.max.length" )
    private String aliasName;

    @Column( name = MCANACOMPARATOR.HAS_VALUE )
    private boolean hasValue;

    @Column(name = MCANACOMPARATOR.HAS_FUNCTION)
    private boolean hasFunction;

    @Column( name = MCANACOMPARATOR.IS_ENABLED )
    private boolean isEnabled;

    @Column( name = MCANACOMPARATOR.IS_PERIODIC_FILTER_ENABLED )
    private boolean isPeriodicFilterEnabled;
}
