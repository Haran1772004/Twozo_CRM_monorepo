package com.tlc.crm.core.api.analytics.entity.config.fields;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANAFIELDTYPECOMPARATOR;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the analytics related field type comparator details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = MCANAFIELDTYPECOMPARATOR.TABLE )
public class FieldTypeComparator extends CRMBaseModel
{
    @Column( name = MCANAFIELDTYPECOMPARATOR.FIELD_TYPE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.field.type" )
    private Long fieldTypeId;

    @Column( name = MCANAFIELDTYPECOMPARATOR.COMPARATOR_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.comparator" )
    private Long comparatorId;
}
